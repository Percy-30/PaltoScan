package com.atpdev.paltoscan.features.history.adapter

data class HistoryItem(
    val id: Int, // Ya está presente el identificador
    val diseaseName: String,
    val section: String,
    val timestamp: Long = System.currentTimeMillis(),
    // val imageUri: ByteArray?
    val imagePath: String, // Cambiado a String
)
