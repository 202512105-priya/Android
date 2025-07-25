package com.example.bookapp

class ModelPdf {
    var uid: String = ""
    var id: String = ""
    var title: String = ""
    var description: String = ""
    var categoryId: String = ""
    var url: String = "" // Firebase Storage URL (if available)
    var pdfBase64: String = "" // ✅ Added for Base64 PDF support
    var timestamp: Long = 0
    var viewsCount: Long = 0
    var downloadsCount: Long = 0

    constructor()

    constructor(
        uid: String,
        id: String,
        title: String,
        description: String,
        categoryId: String,
        url: String,
        pdfBase64: String, // ✅ Added parameter
        timestamp: Long,
        viewsCount: Long,
        downloadsCount: Long
    ) {
        this.uid = uid
        this.id = id
        this.title = title
        this.description = description
        this.categoryId = categoryId
        this.url = url
        this.pdfBase64 = pdfBase64 // ✅ Store Base64 data
        this.timestamp = timestamp
        this.viewsCount = viewsCount
        this.downloadsCount = downloadsCount
    }
}
