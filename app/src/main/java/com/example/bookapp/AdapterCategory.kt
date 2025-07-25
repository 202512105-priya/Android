package com.example.bookapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.bookapp.databinding.RowCategoryBinding
import com.google.firebase.database.FirebaseDatabase

class AdapterCategory(
    private val context: Context,
    var categoryArrayList: ArrayList<ModelCategory>
) : RecyclerView.Adapter<AdapterCategory.HolderCategory>(), Filterable {

    private val filterList: ArrayList<ModelCategory> = ArrayList(categoryArrayList)
    private var filter: FilterCategory? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderCategory {
        val binding = RowCategoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderCategory(binding)
    }

    override fun getItemCount(): Int = categoryArrayList.size

    override fun onBindViewHolder(holder: HolderCategory, position: Int) {
        val model = categoryArrayList[position]

        holder.categoryTv.text = model.category

        // Delete category
        holder.deleteBtn.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete this category?")
                .setPositiveButton("Confirm") { _, _ ->
                    Toast.makeText(context, "Deleting...", Toast.LENGTH_SHORT).show()
                    deleteCategory(model.id)
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        // Open category details
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PdfListAdminActivity::class.java).apply {
                putExtra("categoryId", model.id)
                putExtra("category", model.category)
            }
            context.startActivity(intent)
        }
    }

    private fun deleteCategory(categoryId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child(categoryId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Unable to delete: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    inner class HolderCategory(binding: RowCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        val categoryTv: TextView = binding.categoryTv
        val deleteBtn: ImageButton = binding.deleteBtn
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = FilterCategory(filterList, this)
        }
        return filter as FilterCategory
    }
}
