package com.example.bookapp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bookapp.databinding.ActivityPdfAddBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.io.InputStream

class PdfAddActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfAddBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var categoryArrayList: ArrayList<ModelCategory>
    private var pdfUri: Uri? = null
    private val TAG = "PDF_ADD_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setCanceledOnTouchOutside(false)

        loadPdfCategories()

        binding.backBtn.setOnClickListener { onBackPressed() }
        binding.categoryTv.setOnClickListener { categoryPickDialog() }
        binding.attachPdfBtn.setOnClickListener { pdfPickIntent() }
        binding.submitBtn.setOnClickListener { validateData() }
    }

    private var title = ""
    private var description = ""
    private var category = ""
    private var selectedCategoryId = ""

    private fun validateData() {
        Log.d(TAG, "validateData: Validating data")
        title = binding.titleet.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()
        category = binding.categoryTv.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Enter Title", Toast.LENGTH_SHORT).show()
        } else if (description.isEmpty()) {
            Toast.makeText(this, "Enter Description", Toast.LENGTH_SHORT).show()
        } else if (category.isEmpty()) {
            Toast.makeText(this, "Select a Category", Toast.LENGTH_SHORT).show()
        } else if (pdfUri == null) {
            Toast.makeText(this, "Pick a PDF", Toast.LENGTH_SHORT).show()
        } else {
            uploadPdfToDatabase()
        }
    }

    private fun convertPdfToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert PDF to Base64: ${e.message}")
            null
        }
    }

    private fun uploadPdfToDatabase() {
        Log.d(TAG, "Uploading PDF as Base64 to Realtime Database")
        progressDialog.setMessage("Uploading Pdf...")
        progressDialog.show()

        val timestamp = System.currentTimeMillis()
        val base64Pdf = convertPdfToBase64(pdfUri!!)

        if (base64Pdf == null) {
            Toast.makeText(this, "Failed to convert PDF", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return
        }

        val uid = firebaseAuth.uid
        val pdfData = hashMapOf(
            "uid" to uid,
            "id" to "$timestamp",
            "title" to title,
            "description" to description,
            "categoryId" to selectedCategoryId,
            "pdfBase64" to base64Pdf,  // ✅ Store Base64 string instead of URL
            "timestamp" to timestamp,
            "viewsCount" to 0,
            "downloadsCount" to 0
        )

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child("$timestamp")
            .setValue(pdfData)
            .addOnSuccessListener {
                Log.d(TAG, "Upload successful to Firebase Realtime Database")
                progressDialog.dismiss()
                Toast.makeText(this, "Upload successful!", Toast.LENGTH_SHORT).show()
                pdfUri = null
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Upload failed: ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPdfCategories() {
        Log.d(TAG, "Loading PDF Categories")
        categoryArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.get().addOnSuccessListener { snapshot ->
            categoryArrayList.clear()
            for (ds in snapshot.children) {
                val model = ds.getValue(ModelCategory::class.java)
                categoryArrayList.add(model!!)
                Log.d(TAG, "Category: ${model.category}")
            }
        }
    }

    private fun categoryPickDialog() {
        Log.d(TAG, "Showing category pick dialog")
        val categoriesArray = arrayOfNulls<String>(categoryArrayList.size)
        for (i in categoryArrayList.indices) {
            categoriesArray[i] = categoryArrayList[i].category
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Category")
            .setItems(categoriesArray) { _, which ->
                selectedCategoryId = categoryArrayList[which].id
                binding.categoryTv.text = categoryArrayList[which].category
                Log.d(TAG, "Selected Category ID: $selectedCategoryId")
            }
            .show()
    }

    private fun pdfPickIntent() {
        Log.d(TAG, "Starting PDF pick intent")
        val intent = Intent()
        intent.type = "application/pdf"
        intent.action = Intent.ACTION_GET_CONTENT
        pdfActivityResultLauncher.launch(intent)
    }

    private fun fetchAndDecodePdf(pdfBase64: String) {
        try {
            val decodedBytes = Base64.decode(pdfBase64, Base64.DEFAULT)
            val file = File(cacheDir, "downloaded.pdf")
            file.writeBytes(decodedBytes)

            Log.d(TAG, "PDF saved to: ${file.absolutePath}")

            // Open the PDF with an Intent
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(file), "application/pdf")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode and save PDF: ${e.message}")
        }
    }

    private val pdfActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d(TAG, "PDF picked successfully")
                pdfUri = result.data!!.data
            } else {
                Log.d(TAG, "PDF pick cancelled")
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
