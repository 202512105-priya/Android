package com.example.bookapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookapp.databinding.ActivityPdfListAdminBinding
import com.google.firebase.database.*

class PdfListAdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfListAdminBinding
    private lateinit var pdfArrayList: ArrayList<ModelPdf>
    private lateinit var adapterPdfAdmin: AdapterPdfAdmin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfListAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val categoryId = intent.getStringExtra("categoryId") ?: ""
        val category = intent.getStringExtra("category") ?: ""

        binding.subTitleTv.text = category
        loadPdfList(categoryId)

        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                adapterPdfAdmin.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadPdfList(categoryId: String) {
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")

        ref.orderByChild("categoryId").equalTo(categoryId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    pdfArrayList.clear()
                    for (ds in snapshot.children) {
                        val model = ds.getValue(ModelPdf::class.java)
                        model?.let {
                            if (isValidPdfBase64(it.pdfBase64)) { // ✅ Check pdfBase64, NOT url
                                pdfArrayList.add(it)
                            } else {
                                Log.e("PDF_LIST", "Invalid PDF Base64 for: ${it.title}")
                            }
                        }
                    }
                    // ✅ Pass only 2 arguments, no extra lambda
                    adapterPdfAdmin = AdapterPdfAdmin(this@PdfListAdminActivity, pdfArrayList)
                    binding.booksRv.adapter = adapterPdfAdmin
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PDF_LIST", "Failed to load: ${error.message}")
                }
            })
    }

    private fun isValidPdfBase64(encodedPdf: String?): Boolean {
        if (encodedPdf.isNullOrEmpty()) return false
        return try {
            Base64.decode(encodedPdf, Base64.DEFAULT)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
