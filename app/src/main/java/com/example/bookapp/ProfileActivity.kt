package com.example.bookapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookapp.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Load user information
        loadUserInfo()

        // Back button click
        binding.backBtn.setOnClickListener { onBackPressed() }

        // Edit profile button click
        binding.profileEditBtn.setOnClickListener {
            startActivity(Intent(this, ProfileEditActivity::class.java))
        }
    }

    private fun loadUserInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val email = snapshot.child("email").value?.toString() ?: "No Email"
                    val name = snapshot.child("name").value?.toString() ?: "No Name"
                    val profileImage = snapshot.child("profileImage").value?.toString() ?: ""
                    val timestamp = snapshot.child("timestamp").value?.toString() ?: "0"
                    val userType = snapshot.child("userType").value?.toString() ?: "User"

                    // Format timestamp safely
                    val formattedDate = try {
                        MyApplication.formatTimeStamp(timestamp.toLong())
                    } catch (e: Exception) {
                        "Unknown Date"
                    }

                    // Set user data in UI
                    binding.nameTv.text = name
                    binding.emailTv.text = email
                    binding.memberDateTv.text = formattedDate
                    binding.accountTypeTv.text = userType

                    // Load Profile Image (Base64 Handling)
                    if (profileImage.isNotEmpty()) {
                        try {
                            val imageBytes = Base64.decode(profileImage, Base64.DEFAULT)
                            val bitmap: Bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            binding.profileTv.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Toast.makeText(this@ProfileActivity, "Error loading profile image", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Use default placeholder if no image
                        binding.profileTv.setImageResource(R.drawable.ic_person_gray)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
