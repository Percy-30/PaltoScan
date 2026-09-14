package com.atpdev.paltoscan.data.repository

import androidx.lifecycle.LiveData
import timber.log.Timber
import com.atpdev.paltoscan.data.local.db.HistoryDb
import com.atpdev.paltoscan.data.local.entity.History
import javax.inject.Inject

class HistoryRepository
    @Inject
    constructor(private val historyDb: HistoryDb) {
        suspend fun getHistoryByPhotoHash(photoHash: String): History? {
            return historyDb.HistoryDao().getHistoryByPhotoHash(photoHash)
        }

        // Actualizar un historial existente en la base de datos
        suspend fun updateHistory(history: History) {
            Timber.tag("HistoryRepository").d("Actualizando historial: ${history.id}")
            historyDb.HistoryDao().updateHistory(history)
        }

        // Insertar un nuevo historial en la base de datos
        suspend fun insertHistory(history: History): Long {
            // Inserta el historial y obtiene el ID generado
            val insertedId = historyDb.HistoryDao().insertHistory(history)
            Timber.tag("HistoryRepository").d("Nuevo ID generado: $insertedId")

            val insertedHistory = historyDb.HistoryDao().getHistoryById(insertedId.toInt())
            // Timber.tag("HistoryRepository").d("Historial insertado: ${insertedHistory?.id}")

            // Devuelve el ID generado
            return insertedId
        }

        suspend fun getHistoryId(): Int? {
            return historyDb.HistoryDao().getHistoryId()
        }

        suspend fun getLastId(): Int? {
            return historyDb.HistoryDao().getLastId()
        }

        suspend fun getHistoryById(id: Int): History? {
            return historyDb.HistoryDao().getHistoryById(id)
        }

        // Get all history entries
        suspend fun getAllHistory(): List<History> {
            val historyList = historyDb.HistoryDao().getAllHistory()
            Timber.tag("HistoryRepository").d("Historial devuelto por BD: $historyList")
            return historyList
        }

        // Get history by disease name
        suspend fun getHistoryByDiseaseAndSection(diseaseName: String): History? {
            return historyDb.HistoryDao().getHistoryByDiseaseAndSection(diseaseName)
        }

        suspend fun getHistoryByImagePath(imagePath: String): History? {
            // return historyDb.HistoryDao().getHistoryByPhotoHash(imagePath)
            Timber.tag("HistoryRepository").d("Buscando historial con hash: $imagePath")
            val history = historyDb.HistoryDao().getHistoryByPhotoHash(imagePath)
            // Timber.tag("HistoryRepository").d("Historial encontrado: ${history?.diseaseName}")
            return history
        }

    /*suspend fun getHistoryByDiseaseAndSection(diseaseName: String, section: String): History? {
        return historyDb.HistoryDao().getHistoryByDiseaseAndSection(diseaseName, section)
    }*/

        // Delete specific history entry
        suspend fun deleteHistory(history: History) {
            Timber.tag("HistoryRepository").d("Eliminando historial con ID: ${history.id}")
            historyDb.HistoryDao().deleteHistory(history)
        }

        suspend fun clearAllHistory() {
            historyDb.HistoryDao().deleteAll()
        }

        suspend fun removeHistoryById(id: Int) {
            historyDb.HistoryDao().deleteHistoryById(id)
        }

        // Get all history as LiveData
        fun getAllHistoryItems(): LiveData<List<History>> {
            val historyList = historyDb.HistoryDao().getAllHistoryItems()
            Timber.tag("HistoryRepository").d("Historial devuelto por BD: $historyList")
            return historyList
        }

    /*suspend fun insertHistory(historyItem: HistoryItem) {
        val historyEntity = History(
            id = 0, // Room generará el ID automáticamente
            diseaseName = historyItem.diseaseName,
            section = historyItem.section,
            timestamp = historyItem.timestamp,
            imageUri = historyItem.imageUri
        )
        historyDb.HistoryDao().insertHistory(historyEntity)
    }

    // Obtener todo el historial desde la base de datos
    suspend fun getAllHistory(): List<HistoryItem> {
        return historyDb.HistoryDao().getAll().map { entity ->
            HistoryItem(
                diseaseName = entity.diseaseName,
                section = entity.section,
                timestamp = entity.timestamp,
                imageUri = entity.imageUri
            )
        }
    }

    // Borrar todo el historial de la base de datos
    suspend fun clearHistory() {
        historyDb.HistoryDao().deleteAll()
    }

    // Obtener un historial específico desde la base de datos
    suspend fun getHistoryByDiseaseAndSection(diseaseName: String, section: String): HistoryItem? {
        val historyEntity = historyDb.HistoryDao().getItemByDiseaseAndSection(diseaseName, section)
        return historyEntity?.let { entity ->
            HistoryItem(
                diseaseName = entity.diseaseName,
                section = entity.section,
                timestamp = entity.timestamp,
                imageUri = entity.imageUri
            )
        }
    }

    // Eliminar un historial específico desde la base de datos
    suspend fun deleteHistoryByDiseaseAndSection(diseaseName: String, section: String) {
        val historyEntity = historyDb.HistoryDao().getItemByDiseaseAndSection(diseaseName, section)
        historyEntity?.let { entity ->
            historyDb.HistoryDao().delete(entity)
        }
    }

    // Obtener todos los historiales como LiveData
    // Obtener todo el historial desde la base de datos
    // Obtener todos los historiales como LiveData
    fun getAllHistoryAsLiveData(): LiveData<List<HistoryItem>> {
        return historyDb.HistoryDao().getAllHistoryItems().switchMap { entities ->
            liveData {
                emit(entities.map { entity ->
                    HistoryItem(
                        diseaseName = entity.diseaseName,
                        section = entity.section,
                        timestamp = entity.timestamp,
                        imageUri = entity.imageUri
                    )
                })
            }
        }
    }*/
    }
