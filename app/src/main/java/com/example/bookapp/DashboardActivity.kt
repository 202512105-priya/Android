package com.example.bookapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.bookapp.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var categoryArrayList: ArrayList<ModelCategory>
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        setupViewPager(binding.viewPager)
        binding.tabLayout.setupWithViewPager(binding.viewPager)

        binding.logoutBtn.setOnClickListener {
            firebaseAuth.signOut()
            checkUser()
        }

        binding.profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.myRentalsBtn.setOnClickListener {
            startActivity(Intent(this, RentalsActivity::class.java))
        }

    }

    private fun setupViewPager(viewPager: ViewPager) {
        viewPagerAdapter = ViewPagerAdapter(
            supportFragmentManager,
            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
            this
        )
        categoryArrayList = ArrayList()

        // Add static categories first
        val modelAll = ModelCategory("01", "ALL", 1, "")
        val modelMostViewed = ModelCategory("02", "Most Viewed", 1, "")
        val modelMostDownloaded = ModelCategory("03", "Most Downloaded", 1, "")

        val staticCategories = listOf(modelAll, modelMostViewed, modelMostDownloaded)

        staticCategories.forEach { category ->
            categoryArrayList.add(category)
            viewPagerAdapter.addFragment(
                BlankUserFragment.newInstance(category.id, category.category, category.uid),
                category.category
            )
        }

        // Load categories from Firebase
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelCategory::class.java)
                    model?.let {
                        categoryArrayList.add(it)
                        viewPagerAdapter.addFragment(
                            BlankUserFragment.newInstance(it.id, it.category, it.uid),
                            it.category
                        )
                    }
                }
                viewPagerAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        viewPager.adapter = viewPagerAdapter
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            binding.subTitleTv.text = "Not Logged In"
            binding.profileBtn.visibility = View.GONE
            binding.logoutBtn.visibility = View.GONE
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            binding.subTitleTv.text = firebaseUser.email
            binding.profileBtn.visibility = View.VISIBLE
            binding.logoutBtn.visibility = View.VISIBLE
        }
    }

    class ViewPagerAdapter(
        fm: FragmentManager,
        behavior: Int,
        private val context: Context
    ) : FragmentPagerAdapter(fm, behavior) {

        private val fragmentList = ArrayList<Fragment>()
        private val fragmentTitleList = ArrayList<String>()

        override fun getCount(): Int = fragmentList.size
        override fun getItem(position: Int): Fragment = fragmentList[position]
        override fun getPageTitle(position: Int): CharSequence = fragmentTitleList[position]

        fun addFragment(fragment: Fragment, title: String) {
            fragmentList.add(fragment)
            fragmentTitleList.add(title)
        }
    }
}
