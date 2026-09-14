package com.atpdev.paltoscan.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.atpdev.paltoscan.data.local.db.HistoryDb
import com.atpdev.paltoscan.data.local.entity.History
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryRepositoryTest {

    private lateinit var historyDb: HistoryDb
    private lateinit var repository: HistoryRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        historyDb = Room.inMemoryDatabaseBuilder(
            context, HistoryDb::class.java).build()
        repository = HistoryRepository(historyDb)
    }

    @After
    fun closeDb() {
        historyDb.close()
    }

    @Test
    fun insertAndGetHistory() = runBlocking {
        // Arrange
        val history = History(
            diseaseName = "Antracnosis",
            section = "PaltoScan",
            description = "Enfermedad fúngica del palto causada por Colletotrichum gloeosporioides",
            prevention = "Poda sanitaria y fungicidas preventivos",
            causes = "Hongo Colletotrichum gloeosporioides",
            treatment = "Fungicidas específicos (Azoxystrobina, Tebuconazol)",
            timestamp = System.currentTimeMillis(),
            imagePath = "hash_12345"
        )
        
        // Act
        val insertedId = repository.insertHistory(history)
        val retrieved = repository.getHistoryByImagePath("hash_12345")
        
        // Assert
        assertThat(insertedId).isGreaterThan(0L)
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.diseaseName).isEqualTo("Antracnosis")
    }

    @Test
    fun clearAllHistory() = runBlocking {
        // Arrange
        val history = History(
            diseaseName = "Cercospora",
            section = "PaltoScan",
            description = "Mancha foliar causada por Cercospora purpurea",
            prevention = "Ventilación adecuada y fungicidas preventivos",
            causes = "Hongo Cercospora purpurea",
            treatment = "Mancozeb o Clorotalonil",
            timestamp = System.currentTimeMillis(),
            imagePath = "hash_67890"
        )
        repository.insertHistory(history)
        
        // Act
        repository.clearAllHistory()
        val allHistory = repository.getAllHistory()
        
        // Assert
        assertThat(allHistory).isEmpty()
    }
}
