package com.atpdev.paltoscan.features.recognition

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atpdev.paltoscan.core.ml.TensorFlowHelper
import com.atpdev.paltoscan.domain.model.RecognitionResult
import com.atpdev.paltoscan.domain.repository.ImageRecognitionRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import com.google.firebase.analytics.FirebaseAnalytics

@OptIn(ExperimentalCoroutinesApi::class)
class RecognitionViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var repository: ImageRecognitionRepository
    private lateinit var viewModel: RecognitionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        application = mockk(relaxed = true)
        repository = mockk()
        
        // Mock FirebaseAnalytics
        mockkStatic(FirebaseAnalytics::class)
        val firebaseAnalytics = mockk<FirebaseAnalytics>(relaxed = true)
        every { FirebaseAnalytics.getInstance(application) } returns firebaseAnalytics
        
        // Mock TensorFlowHelper directly instead of mockkConstructor
        val tensorFlowHelper = mockk<TensorFlowHelper>(relaxed = true)
        
        // Mock android.os.Bundle for local JVM tests
        mockkConstructor(android.os.Bundle::class)
        every { anyConstructed<android.os.Bundle>().putString(any(), any()) } returns Unit
        every { anyConstructed<android.os.Bundle>().putFloat(any(), any()) } returns Unit

        viewModel = RecognitionViewModel(application, repository, tensorFlowHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `processImage with valid file updates UI state to Success`() = runTest {
        // Arrange
        val file = mockk<File>()
        every { file.path } returns "test_path"
        every { file.name } returns "test_name.jpg"
        
        mockkStatic(BitmapFactory::class)
        mockkStatic(Bitmap::class)
        val bitmap = mockk<Bitmap>()
        every { BitmapFactory.decodeFile("test_path") } returns bitmap
        every { Bitmap.createScaledBitmap(bitmap, 250, 250, true) } returns bitmap
        every { bitmap.width } returns 250
        every { bitmap.height } returns 250

        val expectedResult = RecognitionResult("Tizón Tardío", 0.95f)
        coEvery { repository.getRecognitionResult(any()) } returns expectedResult

        // Act
        val result = viewModel.processImage(file)

        // Assert
        assertThat(result).isEqualTo(expectedResult)
        
        // Wait for coroutines to finish processing LiveData
        testDispatcher.scheduler.advanceUntilIdle()
        
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(RecognitionUiState.Success::class.java)
        assertThat((uiState as RecognitionUiState.Success).result).isEqualTo(expectedResult)
    }

    @Test
    fun `processImage with exception updates UI state to Error`() = runTest {
        // Arrange
        val file = mockk<File>()
        every { file.path } returns "test_path"
        every { file.name } returns "test_name.jpg"
        
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile("test_path") } throws RuntimeException("Decoding failed")

        // Act
        viewModel.processImage(file)
        
        // Wait for coroutines to finish processing LiveData
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(RecognitionUiState.Error::class.java)
        assertThat((uiState as RecognitionUiState.Error).message).isEqualTo("Error al procesar la imagen")
    }

    @Test
    fun `processImage with low confidence updates UI state to Success with INCONCLUSIVE status`() = runTest {
        // Arrange
        val file = mockk<File>()
        every { file.path } returns "test_path"
        every { file.name } returns "test_name.jpg"
        
        mockkStatic(BitmapFactory::class)
        mockkStatic(Bitmap::class)
        val bitmap = mockk<Bitmap>()
        every { BitmapFactory.decodeFile("test_path") } returns bitmap
        every { Bitmap.createScaledBitmap(bitmap, 250, 250, true) } returns bitmap
        every { bitmap.width } returns 250
        every { bitmap.height } returns 250

        // Estado Inconclusive (<0.70 confidence)
        val expectedResult = RecognitionResult(
            diseaseName = "Tizón Temprano", 
            probability = 0.65f, 
            status = com.atpdev.paltoscan.domain.model.RecognitionStatus.INCONCLUSIVE
        ).apply { heatmapBitmap = mockk() } // Simula el mapa de calor

        coEvery { repository.getRecognitionResult(any()) } returns expectedResult

        // Act
        val result = viewModel.processImage(file)

        // Assert
        assertThat(result).isEqualTo(expectedResult)
        
        // Wait for coroutines to finish processing LiveData
        testDispatcher.scheduler.advanceUntilIdle()
        
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(RecognitionUiState.Success::class.java)
        val successState = uiState as RecognitionUiState.Success
        assertThat(successState.result.status).isEqualTo(com.atpdev.paltoscan.domain.model.RecognitionStatus.INCONCLUSIVE)
        assertThat(successState.result.heatmapBitmap).isNotNull()
    }
}
