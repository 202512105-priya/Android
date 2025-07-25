package com.example.bookapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private var binding: ActivityLoginBinding? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("please Wait")
        progressDialog!!.setCanceledOnTouchOutside(false)

        binding!!.noAccountTv.setOnClickListener {
            startActivity(
                Intent(this@LoginActivity, RegisterActivity::class.java)
            )
        }

        binding!!.loginBtn.setOnClickListener { validateData() }

        binding!!.forgotTv.setOnClickListener {
            startActivity(
                Intent(this@LoginActivity, ForgotPasswordActivity::class.java)
            )
        }
    }

    private var email = ""
    private var password = ""

    private fun validateData() {
        email = binding!!.emailEt.text.toString().trim { it <= ' ' }
        password = binding!!.passwordEt.text.toString().trim { it <= ' ' }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid Email", Toast.LENGTH_SHORT).show()
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "enter password", Toast.LENGTH_SHORT).show()
        } else {
            signInUser(email, password)
        }
    }

    // This method will handle user authentication using FirebaseAuth
    private fun signInUser(email: String, password: String) {
        progressDialog!!.setMessage("Logging in")
        progressDialog!!.show()

        firebaseAuth!!.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User signed in successfully
                    val user = firebaseAuth!!.currentUser
                    Toast.makeText(this@LoginActivity, "Welcome ${user?.email}", Toast.LENGTH_SHORT).show()
                    checkUser()
                } else {
                    // Handle sign-in failure
                    progressDialog!!.dismiss()
                    Toast.makeText(this@LoginActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUser() {
        progressDialog!!.setMessage("Checking User")
        val firebaseUser = firebaseAuth!!.currentUser
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    progressDialog!!.dismiss()
                    val userType = "" + dataSnapshot.child("userType").value
                    if (userType == "user") {
                        startActivity(
                            Intent(this@LoginActivity, DashboardActivity::class.java)
                        )
                        finish()
                    } else if (userType == "admin") {
                        startActivity(
                            Intent(this@LoginActivity, DashboardAdminActivity::class.java)
                        )
                        finish()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    progressDialog!!.dismiss()
                    Toast.makeText(this@LoginActivity, "Failed to check user type", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
