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
import com.example.bookapp.databinding.RowPdfAdminBinding
import com.github.barteksc.pdfviewer.PDFView
import java.io.ByteArrayInputStream
import java.io.File

class AdapterPdfAdmin(
    private val context: Context,
    var pdfArrayList: ArrayList<ModelPdf>
) : RecyclerView.Adapter<AdapterPdfAdmin.HolderPdfAdmin>(), Filterable {

    private var filterList: ArrayList<ModelPdf> = pdfArrayList
    private val filter: FilterPdfAdmin = FilterPdfAdmin(filterList, this)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPdfAdmin {
        val binding = RowPdfAdminBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderPdfAdmin(binding)
    }

    override fun getItemCount(): Int = pdfArrayList.size

    override fun onBindViewHolder(holder: HolderPdfAdmin, position: Int) {
        val model = pdfArrayList[position]

        with(model) {
            val formattedDate = MyApplication.formatTimeStamp(timestamp)

            Log.d("FIREBASE_DATA", "Title: $title, Category: $categoryId")

            holder.apply {

                titleTv.text = title
                descriptionTv.text = description
                dateTv.text = formattedDate

                // ✅ Load category properly
                MyApplication.loadCategory(categoryId) { categoryName ->
                    holder.categoryTv.text = categoryName
                }
                // ✅ Show progress while loading
                progressBar.visibility = View.VISIBLE

                if (!pdfBase64.isNullOrEmpty()) {
                    Log.d("PDF_LOAD", "Loading Base64 PDF for $title")
                    loadPdfFromBase64(pdfBase64, pdfView, progressBar)

                    // ✅ Calculate and display Base64 PDF size
                    sizeTv.text = getFormattedSize(pdfBase64)
                } else {
                    Log.e("PDF_LOAD", "No valid PDF data for $title")
                    sizeTv.text = "Unknown"
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "No PDF available", Toast.LENGTH_SHORT).show()
                }

                // ✅ Click listener to download PDF
                itemView.setOnClickListener {
                    if (!pdfBase64.isNullOrEmpty()) {
                        Log.d("PDF_DOWNLOAD", "Downloading Base64 PDF for $title")
                        downloadPdfFromBase64(pdfBase64, title)
                    } else {
                        Toast.makeText(context, "Invalid PDF Data", Toast.LENGTH_SHORT).show()
                    }
                }
                itemView.setOnClickListener{
                    val intent = Intent(context, PdfDetailActivity::class.java)
                    intent.putExtra("bookId", model.id)
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun getFilter(): Filter = filter

    inner class HolderPdfAdmin(binding: RowPdfAdminBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val pdfView = binding.pdfView
        val progressBar = binding.progressBar
        val titleTv = binding.titleTv
        val descriptionTv = binding.descriptionTv
        val categoryTv = binding.categoryTv
        val sizeTv = binding.sizeTv
        val dateTv = binding.dateTv
        val moreBtn = binding.moreBtn
    }

    // ✅ Load Base64 PDF into PDFView (with Progress Bar)
    private fun loadPdfFromBase64(pdfBase64: String, pdfView: PDFView, progressBar: View) {
        try {
            val pdfBytes: ByteArray = Base64.decode(pdfBase64, Base64.DEFAULT)
            val inputStream = ByteArrayInputStream(pdfBytes)

            pdfView.fromStream(inputStream)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .onLoad { progressBar.visibility = View.GONE } // ✅ Hide progress when loaded
                .onError {
                    progressBar.visibility = View.GONE // ✅ Hide on error
                    Log.e("PDF_ERROR", "Error loading PDF")
                }
                .load()
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Log.e("PDF_ERROR", "Error decoding Base64 PDF: ${e.message}")
        }
    }

    // ✅ Download Base64 PDF to Local Storage
    private fun downloadPdfFromBase64(pdfBase64: String, fileName: String) {
        try {
            val pdfBytes: ByteArray = Base64.decode(pdfBase64, Base64.DEFAULT)
            val file = File(context.getExternalFilesDir(null), "$fileName.pdf")

            file.writeBytes(pdfBytes)
            Toast.makeText(context, "PDF downloaded: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            Log.d("PDF_DOWNLOAD", "Downloaded PDF to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("PDF_DOWNLOAD_ERROR", "Error downloading PDF: ${e.message}")
            Toast.makeText(context, "Failed to download PDF", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Calculate File Size from Base64 String
    private fun getFormattedSize(base64String: String): String {
        return try {
            val fileSizeKB = (base64String.length * 3) / 4 / 1024.0
            if (fileSizeKB >= 1024) {
                "${String.format("%.2f", fileSizeKB / 1024)} MB"
            } else {
                "${String.format("%.2f", fileSizeKB)} KB"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
