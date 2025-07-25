package com.example.bookapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bookapp.databinding.ActivityMain2Binding

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMain2Binding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding!!.root)

        binding!!.loginBtn.setOnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    LoginActivity::class.java
                )
            )
        }
        binding!!.skipBtn.setOnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    DashboardActivity::class.java
                )
            )
        }
    }
}