package com.example.bookapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bookapp.databinding.FragmentBlankUserBinding
import com.google.firebase.database.*

class BlankUserFragment : Fragment() {

    private lateinit var binding: FragmentBlankUserBinding

    private var categoryId = ""
    private var category = ""
    private var uid = ""

    private lateinit var pdfArrayList: ArrayList<ModelPdf>
    private lateinit var adapterPdfUser: AdapterPdfUser

    companion object {
        private const val TAG = "BOOK_USER_TAG"

        fun newInstance(categoryId: String, category: String, uid: String): BlankUserFragment {
            val fragment = BlankUserFragment()
            val args = Bundle().apply {
                putString("categoryId", categoryId)
                putString("category", category)
                putString("uid", uid)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString("categoryId") ?: ""
            category = it.getString("category") ?: ""
            uid = it.getString("uid") ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBlankUserBinding.inflate(inflater, container, false)

        Log.d(TAG, "onCreateView: Category: $category")

        when (category.trim()) {
            "ALL" -> loadAllBooks()
            "Most Viewed" -> loadBooksSortedBy("viewsCount")
            "Most Downloaded" -> loadBooksSortedBy("downloadsCount")
            else -> loadCategorizedBooks()
        }

        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapterPdfUser.filter.filter(s)
            }
        })

        return binding.root
    }

    private fun loadAllBooks() {
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                pdfArrayList.clear()
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelPdf::class.java)
                    model?.let { pdfArrayList.add(it) }
                }
                adapterPdfUser = AdapterPdfUser(requireContext(), pdfArrayList)
                binding.booksRv.adapter = adapterPdfUser
            }
        })
    }

    private fun loadBooksSortedBy(orderBy: String) {
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild(orderBy).limitToLast(10).addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                pdfArrayList.clear()
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelPdf::class.java)
                    model?.let { pdfArrayList.add(it) }
                }
                pdfArrayList.reverse() // to show highest values first
                adapterPdfUser = AdapterPdfUser(requireContext(), pdfArrayList)
                binding.booksRv.adapter = adapterPdfUser
            }
        })
    }

    private fun loadCategorizedBooks() {
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild("categoryId").equalTo(categoryId)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}

                override fun onDataChange(snapshot: DataSnapshot) {
                    pdfArrayList.clear()
                    for (ds in snapshot.children) {
                        val model = ds.getValue(ModelPdf::class.java)
                        model?.let { pdfArrayList.add(it) }
                    }
                    adapterPdfUser = AdapterPdfUser(requireContext(), pdfArrayList)
                    binding.booksRv.adapter = adapterPdfUser
                }
            })
    }
    fun rentBook(bookId: String) {
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val userId = firebaseUser.uid

        val rentedOn = System.currentTimeMillis()
        val returnBy = rentedOn + 7 * 24 * 60 * 60 * 1000 // 7 days

        val rentalData = hashMapOf(
            "rentedOn" to rentedOn,
            "returnBy" to returnBy,
            "status" to "active"
        )

        val rentalRef = FirebaseDatabase.getInstance()
            .getReference("Rentals")
            .child(userId)
            .child(bookId)

        rentalRef.setValue(rentalData)
            .addOnSuccessListener {
                android.widget.Toast.makeText(requireContext(), "Book rented!", android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(requireContext(), "Failed to rent: ${it.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

}
