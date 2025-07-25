package com.example.bookapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookapp.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private var binding: ActivityRegisterBinding? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)

        setContentView(binding!!.root)
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please wait")
        progressDialog!!.setCanceledOnTouchOutside(false)

        binding!!.backBtn.setOnClickListener { onBackPressed() }
        binding!!.registerBtn.setOnClickListener { validateData() }
    }

    private var name = ""
    private var email = ""
    private var password = ""
    private fun validateData() {
        name = binding!!.nameEt.text.toString().trim { it <= ' ' }
        email = binding!!.emailEt.text.toString().trim { it <= ' ' }
        password = binding!!.passwordEt.text.toString().trim { it <= ' ' }
        val cpassword = binding!!.cpasswordEt.text.toString().trim { it <= ' ' }

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "enter your name", Toast.LENGTH_SHORT).show()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid Email", Toast.LENGTH_SHORT).show()
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "enter password", Toast.LENGTH_SHORT).show()
        } else if (TextUtils.isEmpty(cpassword)) {
            Toast.makeText(this, "enter confirm password", Toast.LENGTH_SHORT).show()
        } else if (password != cpassword) {
            Toast.makeText(this, "password not matches", Toast.LENGTH_SHORT).show()
        } else {
            createUserAccount()
        }
    }

    private fun createUserAccount() {
        progressDialog!!.setMessage("creating account")
        progressDialog!!.show()
        firebaseAuth!!.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { updateUserInfo() }
            .addOnFailureListener { e ->
                progressDialog!!.dismiss()
                Toast.makeText(
                    this@RegisterActivity,
                    "" + e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateUserInfo() {
        progressDialog!!.setMessage("Saving user info")
        val timestamp = System.currentTimeMillis()
        val uid = firebaseAuth!!.uid
        val hashMap = HashMap<String, Any?>()
        hashMap["uid"] = uid
        hashMap["email"] = email
        hashMap["name"] = name
        hashMap["profileImage"] = ""
        hashMap["userType"] = "user"
        hashMap["timestamp"] = timestamp
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog!!.dismiss()
                Toast.makeText(
                    this@RegisterActivity,
                    "Account Created",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(
                    Intent(
                        this@RegisterActivity,
                        DashboardActivity::class.java
                    )
                )
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog!!.dismiss()
                Toast.makeText(
                    this@RegisterActivity,
                    "" + e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}