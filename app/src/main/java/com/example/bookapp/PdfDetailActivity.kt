package com.example.bookapp

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.Tag
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bookapp.databinding.ActivityPdfDetailBinding
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

class PdfDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfDetailBinding
    private companion object{
        const val TAG="BOOK_DETAILS_TAG"
    }
    private var bookId=""
    private var bookTitle=""
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bookId= intent.getStringExtra("bookId")!!
        progressDialog= ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        MyApplication.incrementBooksViewCount(bookId)

        loadBookDetails()
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        binding.readBookBtn.setOnClickListener{
            val intent= Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId",bookId)
            startActivity(intent)
        }
        binding.downloadsBooksBtn.setOnClickListener{
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                Log.d(TAG,"onCreate:Storage Permission is already Granted")
                downloadBook()

            }
            else{
                Log.d(TAG,"onCreate:Storage Permission is not Granted")
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

            }
        }


    }


    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        isGranted:Boolean->
       if(isGranted){
           Log.d(TAG,"onCreate:Storage Permission is already Granted")
           downloadBook()
        }
        else{
           Log.d(TAG,"onCreate:Storage Permission is not Granted")
           Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show()
       }
    }
    private fun downloadBook() {
        progressDialog.setMessage("Downloading Book...")
        progressDialog.show()

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pdfBase64 = snapshot.child("pdfBase64").value?.toString()

                if (!pdfBase64.isNullOrEmpty()) {
                    try {
                        val pdfBytes = android.util.Base64.decode(pdfBase64, android.util.Base64.DEFAULT)
                        saveToDownloads(pdfBytes)
                    } catch (e: Exception) {
                        progressDialog.dismiss()
                        Toast.makeText(this@PdfDetailActivity, "Decode error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this@PdfDetailActivity, "PDF data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog.dismiss()
                Toast.makeText(this@PdfDetailActivity, "Firebase error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveToDownloads(bytes: ByteArray) {
        val nameWithExtension = "$bookTitle.pdf"
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs() // Ensures the folder exists

            val filePath = downloadsFolder.path+"/"+ nameWithExtension
            val outputStream = FileOutputStream(filePath)
            outputStream.write(bytes)
            outputStream.close()


            Toast.makeText(this, "Saved to Downloads:", Toast.LENGTH_LONG).show()
            progressDialog.dismiss()
            incrementDownloadCount()
        } catch (e: Exception) {
            progressDialog.dismiss()
            Toast.makeText(this, "Failed to save PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "saveToDownloads: ", e)
        }
    }
    private fun incrementDownloadCount(){
        Log.d(TAG,"incrementDownloadsCount")
        val ref=FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onCancelled(error: DatabaseError) {

                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    var downloadsCount= "${snapshot.child("downloadsCount").value}"
                    Log.d(TAG,"onDataChange: Current Downloads Count: $downloadsCount " )
                    if (downloadsCount=="" || downloadsCount=="null"){
                        downloadsCount= "0"
                    }
                    val newDownloadCount:Long= downloadsCount.toLong()+1
                    Log.d(TAG,"onDataChange: New Downloads Count: $newDownloadCount " )
                    val hashMap:HashMap<String, Any> = HashMap()
                    hashMap["downloadsCount"]= newDownloadCount
                    val dbref= FirebaseDatabase.getInstance().getReference("Books")
                    dbref.child(bookId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "incrementDownloadCount: Successfully updated download count")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "incrementDownloadCount: Failed to update download count", e)
                        }

                }
            })
    }
    private fun loadBookDetails(){
        val ref= FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val categoryId= "${snapshot.child("categoriesId").value}"
                    val description= "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp= "${snapshot.child("timestamp").value}"
                    val title= "${snapshot.child("title").value}"
                    val uid= "${snapshot.child("uid").value}"
                    val viewsCount= "${snapshot.child("viewsCount").value}"
                    val pdfBase64= "${snapshot.child("pdfBase64").value}"
                    val date= MyApplication.formatTimeStamp(timestamp.toLong())

                    MyApplication.loadCategory(categoryId) { category -> binding.categoryTv.text = category }

                    pdfBase64.let {
                        val pdfBytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)

                        binding.progressBar.visibility = android.view.View.VISIBLE

                        binding.pdfView.fromBytes(pdfBytes)
                            .defaultPage(0)
                            .enableSwipe(true)
                            .swipeHorizontal(false)
                            .pageFitPolicy(FitPolicy.WIDTH)
                            .onLoad { nbPages ->
                                binding.progressBar.visibility = android.view.View.GONE
                                binding.pagesTv.text = "$nbPages pages"
                            }
                            .onError { t ->
                                binding.progressBar.visibility = android.view.View.GONE
                                Toast.makeText(this@PdfDetailActivity, "Failed to load PDF: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                            .load()

                        // Set file size
                        MyApplication.loadPdfSize(it) { size -> binding.sizeTv.text = size }
                    }

                    binding.titleTv.text=title
                    binding.descriptionTv.text=description
                    binding.viewsTv.text=viewsCount
                    binding.DownloadsTv.text=downloadsCount
                    binding.dateTv.text=date
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}