package com.example.bookapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RentalsActivity : AppCompatActivity() {

    private lateinit var rentalsRv: RecyclerView
    private lateinit var rentalsList: ArrayList<ModelPdf>
    private lateinit var adapter: AdapterPdfUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rentals)

        rentalsRv = findViewById(R.id.rentalsRv)
        rentalsRv.layoutManager = LinearLayoutManager(this)
        rentalsList = ArrayList()
        val rentalsRv = findViewById<RecyclerView>(R.id.rentalsRv)
        rentalsRv.layoutManager = LinearLayoutManager(this)

        val sampleList = arrayListOf<ModelPdf>() // TODO: Load from Firebase or local list
        val adapter = AdapterRentals(this, sampleList)
        rentalsRv.adapter = adapter


        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val rentalRef = FirebaseDatabase.getInstance().getReference("Rentals").child(userId)

        rentalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rentalsList.clear()
                for (rentalSnap in snapshot.children) {
                    val bookId = rentalSnap.key ?: continue
                    loadBookDetails(bookId)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadBookDetails(bookId: String) {
        val bookRef = FirebaseDatabase.getInstance().getReference("Books").child(bookId)
        bookRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val model = snapshot.getValue(ModelPdf::class.java)
                model?.let {
                    rentalsList.add(it)
                    adapter = AdapterPdfUser(this@RentalsActivity, rentalsList)
                    rentalsRv.adapter = adapter
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
