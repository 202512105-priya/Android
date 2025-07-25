package com.example.bookapp

import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.os.Environment
import android.text.format.DateFormat
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.HashMap

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }

    companion object {
        // ✅ Format timestamp to readable date
        fun formatTimeStamp(timestamp: Long): String {
            val cal = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis = timestamp
            return DateFormat.format("dd/MM/yyyy", cal).toString()
        }

        // ✅ Load category name from Firebase Realtime Database
        fun loadCategory(categoryId: String, callback: (String) -> Unit) {
            val ref = FirebaseDatabase.getInstance().getReference("Categories").child(categoryId)
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val category = snapshot.child("category").value?.toString() ?: "Unknown"
                    callback(category)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback("Unknown")
                }
            })
        }

        // ✅ Download PDF from Base64 stored in Firebase Database
        fun downloadPdf(context: Context, pdfBase64: String, title: String) {
            val progressDialog = ProgressDialog(context)
            progressDialog.setMessage("Downloading $title...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            try {
                // Convert Base64 string to byte array
                val pdfBytes = Base64.decode(pdfBase64, Base64.DEFAULT)

                // Save PDF to device
                savePdfToStorage(context, title, pdfBytes)
                progressDialog.dismiss()
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Save Base64-decoded PDF to local storage
        private fun savePdfToStorage(context: Context, fileName: String, bytes: ByteArray) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, "$fileName.pdf")

                FileOutputStream(file).apply {
                    write(bytes)
                    close()
                }

                Toast.makeText(context, "Saved to Downloads: $fileName.pdf", Toast.LENGTH_SHORT).show()
                Log.d("PDF_DOWNLOAD", "File saved at: ${file.absolutePath}")
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("PDF_DOWNLOAD_ERROR", "Failed to save PDF: ${e.message}")
            }
        }
        fun incrementBooksViewCount(bookId: String){
            val ref= FirebaseDatabase.getInstance().getReference("Books")
            ref.child(bookId).addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(error: DatabaseError) {

                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    var viewsCount = "${snapshot.child("viewsCount").value}"

                    if (viewsCount== ""  || viewsCount =="null"){
                        viewsCount ="0";
                    }
                    val newViewsCount= viewsCount.toLong()+1
                    val hashMap= HashMap<String,Any>()
                    hashMap["viewsCount"] = newViewsCount

                    val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                    dbRef.child(bookId)
                        .updateChildren(hashMap)
                }
            })

        }
        fun loadPdfSize(pdfBase64: String, callback: (String) -> Unit) {
            try {
                val fileSizeInBytes = Base64.decode(pdfBase64, Base64.DEFAULT).size
                val fileSizeInKB = fileSizeInBytes / 1024
                val fileSizeInMB = fileSizeInKB / 1024

                val sizeText = when {
                    fileSizeInMB > 0 -> "$fileSizeInMB MB"
                    else -> "$fileSizeInKB KB"
                }
                callback(sizeText)
            } catch (e: Exception) {
                callback("Unknown size")
            }
        }
    }
}
