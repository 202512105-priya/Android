package com.example.bookapp

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.bookapp.databinding.RowPdfUserBinding
import com.github.barteksc.pdfviewer.PDFView
import java.io.ByteArrayInputStream

class AdapterPdfUser(
    private val context: Context,
    var pdfArrayList: ArrayList<ModelPdf>
) : RecyclerView.Adapter<AdapterPdfUser.HolderPdfUser>(), Filterable {

    var filterList: ArrayList<ModelPdf> = pdfArrayList
    private var filter: FilterPdfUser? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPdfUser {
        val binding = RowPdfUserBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderPdfUser(binding)
    }

    override fun getItemCount(): Int = pdfArrayList.size

    override fun onBindViewHolder(holder: HolderPdfUser, position: Int) {
        val model = pdfArrayList[position]
        val title = model.title
        val description = model.description
        val date = MyApplication.formatTimeStamp(model.timestamp)



        holder.apply {
            titleTv.text = title
            descriptionTv.text = description
            dateTv.text = date

            // Load category name
            MyApplication.loadCategory(model.categoryId) { categoryName ->
                categoryTv.text = categoryName
            }

            // Load PDF preview
            progressBar.visibility = View.VISIBLE
            if (!model.pdfBase64.isNullOrEmpty()) {
                loadPdfFromBase64(model.pdfBase64, pdfView, progressBar)
                MyApplication.loadPdfSize(model.pdfBase64) { sizeText ->
                    sizeTv.text = sizeText
                }
            } else {
                sizeTv.text = "Unknown"
                progressBar.visibility = View.GONE
                Toast.makeText(context, "No PDF available", Toast.LENGTH_SHORT).show()
            }

            itemView.setOnClickListener {
                val intent = Intent(context, PdfDetailActivity::class.java)
                intent.putExtra("bookId", model.id)
                context.startActivity(intent)
            }
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                val rentalRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("Rentals")
                    .child(firebaseUser.uid)
                    .child(model.id)

                rentalRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val status = snapshot.child("status").value.toString()
                        if (status == "active") {
                            btnRent.text = "Rented"
                            btnRent.isEnabled = false
                        }
                    }
                }
            }

            holder.btnRent.setOnClickListener {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (firebaseUser == null) {
                    Toast.makeText(context, "Please log in to rent books.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val userId = firebaseUser.uid
                val rentedOn = System.currentTimeMillis()
                val returnBy = rentedOn + 7 * 24 * 60 * 60 * 1000 // 7 days

                val rentalData = hashMapOf(
                    "rentedOn" to rentedOn,
                    "returnBy" to returnBy,
                    "status" to "active"
                )

                val rentalRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("Rentals")
                    .child(userId)
                    .child(model.id)

                rentalRef.setValue(rentalData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Book rented successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Rent failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

        }
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = FilterPdfUser(filterList, this)
        }
        return filter!!
    }

    inner class HolderPdfUser(binding: RowPdfUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val pdfView: PDFView = binding.pdfView
        val progressBar = binding.progressBar
        val titleTv = binding.titleTv
        val descriptionTv = binding.descriptionTv
        val categoryTv = binding.categoryTv
        val sizeTv = binding.sizeTv
        val dateTv = binding.dateTv
        val btnRent= binding.btnRent
    }

    private fun loadPdfFromBase64(pdfBase64: String, pdfView: PDFView, progressBar: View) {
        try {
            val pdfBytes: ByteArray = Base64.decode(pdfBase64, Base64.DEFAULT)
            val inputStream = ByteArrayInputStream(pdfBytes)

            pdfView.fromStream(inputStream)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .onLoad { progressBar.visibility = View.GONE }
                .onError {
                    progressBar.visibility = View.GONE
                    Log.e("PDF_ERROR", "Error loading PDF")
                }
                .load()
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Log.e("PDF_ERROR", "Error decoding Base64 PDF: ${e.message}")
        }
    }
}
