package com.example.bookapp

import android.widget.Filter

class FilterCategory: Filter {
    private var filterlist: ArrayList<ModelCategory>
    private var adapterCategory:AdapterCategory

    constructor(filterlist: ArrayList<ModelCategory>, adapterCategory: AdapterCategory) : super() {
        this.filterlist = filterlist
        this.adapterCategory = adapterCategory
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint= constraint
        val results=FilterResults()
        if (constraint!= null && constraint.isNotEmpty())
        {
            constraint=constraint.toString().uppercase()
            val filteredModels:ArrayList<ModelCategory> = ArrayList()
            for (i in 0 until filterlist.size){
                if (filterlist[i].category.uppercase().contains(constraint)){
                    filteredModels.add(filterlist[i])
                }
            }
            results.count=filteredModels.size
            results.values=filteredModels
        }
        else{
            results.count=filterlist.size
            results.values=filterlist
        }
        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
        if (results != null) {
            adapterCategory.categoryArrayList=results.values as ArrayList<ModelCategory>
        }
        adapterCategory.notifyDataSetChanged()
    }
}