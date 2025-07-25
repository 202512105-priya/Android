package com.example.bookapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SplashActivity : AppCompatActivity() {
    private var firebaseAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        firebaseAuth = FirebaseAuth.getInstance()
        Handler().postDelayed({ checkUser() }, 2000)
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth!!.currentUser
        if (firebaseUser == null) {
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        } else {
            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val userType = "" + dataSnapshot.child("userType").value
                        if (userType == "user") {
                            startActivity(
                                Intent(
                                    this@SplashActivity,
                                    DashboardActivity::class.java
                                )
                            )
                            finish()
                        } else if (userType == "admin") {
                            startActivity(
                                Intent(
                                    this@SplashActivity,
                                    DashboardAdminActivity::class.java
                                )
                            )
                            finish()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                    }
                })
        }
    }
}