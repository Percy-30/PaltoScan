package com.atpdev.paltoscan.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity // Para que reconosca como una tabla de base de datos
// @Entity(tableName = "history_table", indices = [Index(value = ["diseaseName", "section"], unique = true)])
data class History(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_History") var id: Int = 0,
    @ColumnInfo(name = "diseaseName") var diseaseName: String,
    @ColumnInfo(name = "section") val section: String = "PaltoScan",
    @ColumnInfo(name = "description") var description: String,
    @ColumnInfo(name = "prevention") var prevention: String,
    @ColumnInfo(name = "causes") var causes: String,
    @ColumnInfo(name = "treatment") var treatment: String,
    @ColumnInfo(name = "timestamp") var timestamp: Long,
    @ColumnInfo(name = "imagePath") var imagePath: String?,
    @ColumnInfo(name = "latitude") var latitude: Double? = null,
    @ColumnInfo(name = "longitude") var longitude: Double? = null,
) : Serializable
