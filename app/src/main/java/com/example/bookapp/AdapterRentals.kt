package com.example.bookapp

import android.content.Context
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AdapterRentals(
    private val context: Context,
    private val rentalsList: ArrayList<ModelPdf>
) : RecyclerView.Adapter<AdapterRentals.ViewHolder>() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTv: TextView = itemView.findViewById(R.id.titleTv)
        val descriptionTv: TextView = itemView.findViewById(R.id.descriptionTv)
        val categoryTv: TextView = itemView.findViewById(R.id.categoryTv)
        val sizeTv: TextView = itemView.findViewById(R.id.sizeTv)
        val dateTv: TextView = itemView.findViewById(R.id.dateTv)
        val rentedOnTv: TextView = itemView.findViewById(R.id.rentedOnTv)
        val returnByTv: TextView = itemView.findViewById(R.id.returnByTv)
        val btnRent: Button = itemView.findViewById(R.id.btnRent)
        val btnReturn: Button = itemView.findViewById(R.id.btnReturn)
        val pdfView: PDFView = itemView.findViewById(R.id.pdfView)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_rental_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = rentalsList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = rentalsList[position]

        holder.titleTv.text = model.title
        holder.descriptionTv.text = model.description
        holder.categoryTv.text = model.categoryId
        holder.dateTv.text = android.text.format.DateFormat.format("dd/MM/yyyy", Date(model.timestamp))
        holder.sizeTv.text = "N/A"

        val userId = auth.currentUser?.uid ?: return
        val rentalRef = database.getReference("Rentals").child(userId).child(model.id)

        // Load rental info
        rentalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val rentedOnMillis = snapshot.child("rentedOn").getValue(Long::class.java) ?: 0L
                    val returnByMillis = snapshot.child("returnBy").getValue(Long::class.java) ?: 0L

                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    holder.rentedOnTv.text = "Rented On: ${sdf.format(Date(rentedOnMillis))}"
                    holder.returnByTv.text = "Return By: ${sdf.format(Date(returnByMillis))}"

                    holder.btnReturn.visibility = View.VISIBLE
                    holder.btnRent.visibility = View.GONE
                } else {
                    holder.rentedOnTv.text = "Rented On: --"
                    holder.returnByTv.text = "Return By: --"
                    holder.btnReturn.visibility = View.GONE
                    holder.btnRent.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // PDF Preview
        holder.progressBar.visibility = View.VISIBLE
        try {
            val decodedBytes = Base64.decode(model.pdfBase64, Base64.DEFAULT)
            holder.pdfView.fromBytes(decodedBytes)
                .pages(0)
                .spacing(0)
                .onLoad {
                    holder.progressBar.visibility = View.GONE
                }
                .load()
        } catch (e: Exception) {
            holder.progressBar.visibility = View.GONE
            Toast.makeText(context, "Error loading PDF", Toast.LENGTH_SHORT).show()
        }

        holder.btnRent.setOnClickListener {
            rentBook(model)
        }

        holder.btnReturn.setOnClickListener {
            returnBook(model)
        }
    }

    private fun rentBook(model: ModelPdf) {
        val userId = auth.currentUser?.uid ?: return
        val rentedOn = System.currentTimeMillis()
        val returnBy = rentedOn + 7 * 24 * 60 * 60 * 1000 // 7 days

        val rentalData = hashMapOf(
            "rentedOn" to rentedOn,
            "returnBy" to returnBy,
            "status" to "active"
        )

        val rentalRef = database.getReference("Rentals").child(userId).child(model.id)

        rentalRef.setValue(rentalData)
            .addOnSuccessListener {
                Toast.makeText(context, "Book rented!", Toast.LENGTH_SHORT).show()
                notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to rent: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun returnBook(model: ModelPdf) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val rentalRef = FirebaseDatabase.getInstance().getReference("Rentals")
            .child(userId)
            .child(model.id) // Reference to the rented book by the user

        rentalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Retrieve the rental data from the snapshot
                    val rentalData = snapshot.value as? Map<String, Any> ?: return

                    // Create a new map to update the rental data
                    val updatedRentalData = HashMap<String, Any>()
                    updatedRentalData.putAll(rentalData) // Keep the old data
                    updatedRentalData["status"] = "Returned" // Mark the status as returned
                    updatedRentalData["returnedOn"] = System.currentTimeMillis() // Add the return date

                    // Update the rental record in the Rentals node
                    rentalRef.updateChildren(updatedRentalData)
                        .addOnSuccessListener {
                            // After updating, show a success message
                            Toast.makeText(context, "Book returned successfully", Toast.LENGTH_SHORT).show()
                            notifyDataSetChanged() // Notify adapter of data change
                        }
                        .addOnFailureListener { e ->
                            // Log or show an error if something goes wrong
                            Toast.makeText(context, "Failed to return book: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // If no rental data found, show an error
                    Toast.makeText(context, "No rental data found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle cancellation or database error
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
