package com.atpdev.paltoscan.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.atpdev.paltoscan.data.local.entity.History

@Dao
interface HistoryDao {
    // consulta para realizar a la base de datos

    // @Insert(onConflict = OnConflictStrategy.IGNORE) // Evita duplicados automáticamente
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: History): Long

    @Update
    suspend fun updateHistory(history: History)

    @Query("SELECT * FROM history WHERE id_History = :id")
    suspend fun getById(id: Int): History?

    @Query("DELETE FROM history WHERE id_History = :id")
    suspend fun deleteHistoryById(id: Int)

    /*@Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateHistory(history: History)*/
    @Query("SELECT * FROM History WHERE id_History = :id")
    suspend fun getHistoryById(id: Int): History?

    @Query("SELECT id_History FROM History ORDER BY id_History DESC LIMIT 1")
    suspend fun getHistoryId(): Int?

    @Query("SELECT MAX(id_History) FROM History")
    suspend fun getLastId(): Int?

    @Query("SELECT * FROM history WHERE imagePath = :imagePath LIMIT 1")
    suspend fun getHistoryByPhotoHash(imagePath: String): History?

    @Query("SELECT * FROM History ORDER BY id_History DESC")
    suspend fun getAllHistory(): List<History>

    // @Query("SELECT * FROM History WHERE diseaseName = :diseaseName AND section = :section")
    // suspend fun getHistoryByDiseaseAndSection(diseaseName: String, section: String): History?
    @Query("SELECT * FROM History WHERE diseaseName = :diseaseName AND section = 'paltoscan'")
    suspend fun getHistoryByDiseaseAndSection(diseaseName: String): History?

    // @Query("SELECT * FROM history WHERE diseaseName = :diseaseName")
    // fun getHistoryByDiseaseAndSection(diseaseName: String): List<History>

    @Delete
    suspend fun deleteHistory(history: History)

    //
    @Query("DELETE FROM History")
    suspend fun deleteAll()

    @Query("SELECT * FROM History ORDER BY id_History DESC")
    fun getAllHistoryItems(): LiveData<List<History>>

    // @Query("SELECT * FROM History")
    // fun getAllHistoryItems(): Flow<List<History>>
}
