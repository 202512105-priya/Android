package com.example.bookapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bookapp.databinding.ActivityPdfViewBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.log

class PdfViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfViewBinding
    private companion object{
        const val TAG= "PDF_VIEW_TAG"
    }
    var bookId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPdfViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bookId= intent.getStringExtra("bookId")!!
        loadBookDetails()
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadBookDetails() {
        Log.d(TAG, "loadBookDetails: Get PDF from db")

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pdfBase64 = snapshot.child("pdfBase64").value?.toString()

                    if (pdfBase64 != null) {
                        try {
                            val bytes = android.util.Base64.decode(pdfBase64, android.util.Base64.DEFAULT)
                            binding.progressBar.visibility = View.VISIBLE

                            binding.pdfView.fromBytes(bytes)
                                .swipeHorizontal(false)
                                .onPageChange { page, pageCount ->
                                    val currentPage = page + 1
                                    binding.toolbarSubtitleTv.text = "$currentPage / $pageCount"
                                }
                                .onLoad {
                                    binding.progressBar.visibility = View.GONE
                                }
                                .onError { t ->
                                    binding.progressBar.visibility = View.GONE
                                    Log.e(TAG, "Error loading PDF: ${t.message}")
                                }
                                .load()
                        } catch (e: Exception) {
                            Log.e(TAG, "Base64 decoding failed: ${e.message}")
                            binding.progressBar.visibility = View.GONE
                        }
                    } else {
                        Log.e(TAG, "PDF Base64 is null!")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase error: ${error.message}")
                }
            })
    }


}