package com.example.bookapp

import android.widget.Filter

class FilterPdfUser: Filter {
    var filterList: ArrayList<ModelPdf>
    var AdapterPdfUser: AdapterPdfUser

    constructor(filterList: ArrayList<ModelPdf>, AdapterPdfUser: AdapterPdfUser) : super() {
        this.filterList = filterList
        this.AdapterPdfUser = AdapterPdfUser
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val results = FilterResults()
        if (!constraint.isNullOrEmpty()) {
            val searchQuery = constraint.toString().lowercase()
            val filteredModels = filterList.filter { it.title.lowercase().contains(searchQuery) }

            results.count = filteredModels.size
            results.values = ArrayList(filteredModels)
        } else {
            results.count = filterList.size
            results.values = filterList
        }
        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
        AdapterPdfUser.pdfArrayList= results!!.values as ArrayList<ModelPdf>

    }
}