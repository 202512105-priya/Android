package com.example.bookapp

import android.widget.Filter

class FilterPdfAdmin(
    private val filterList: ArrayList<ModelPdf>,
    private val adapterPdfAdmin: AdapterPdfAdmin
) : Filter() {

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
        val filteredList = results?.values as? ArrayList<ModelPdf>
        if (filteredList != null) {
            adapterPdfAdmin.pdfArrayList = filteredList
            adapterPdfAdmin.notifyDataSetChanged()
        }
    }
}
