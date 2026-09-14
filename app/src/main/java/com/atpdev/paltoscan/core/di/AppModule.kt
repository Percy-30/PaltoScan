package com.atpdev.paltoscan.core.di

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.atpdev.paltoscan.data.remote.ApiService
import com.atpdev.paltoscan.data.remote.OpenMeteoService
import com.atpdev.paltoscan.data.repository.CameraRepositoryImpl
import com.atpdev.paltoscan.data.repository.ImageRecognitionRepositoryImpl
import com.atpdev.paltoscan.domain.repository.CameraRepository
import com.atpdev.paltoscan.domain.repository.ImageRecognitionRepository
import com.atpdev.paltoscan.data.repository.WeatherRepository
import com.atpdev.paltoscan.data.repository.WeatherRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideImageRecognitionRepository(
        @ApplicationContext context: Context,
    ): ImageRecognitionRepository = ImageRecognitionRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideWeatherRepository(
        openMeteoService: com.atpdev.paltoscan.data.remote.OpenMeteoService,
        @ApplicationContext context: Context,
    ): WeatherRepository = WeatherRepositoryImpl(openMeteoService, context)
}

@Module
@InstallIn(ActivityComponent::class)
object CameraModule {
    @Provides
    fun provideCameraRepository(
        @ActivityContext activity: Context,
    ): CameraRepository = CameraRepositoryImpl(activity as FragmentActivity)

    /*@Provides
    fun provideCameraRepository(
        activity: FragmentActivity
    ): CameraRepository = CameraRepositoryImpl(activity)*/
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/") // Cambia por URL base
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenMeteoService(): OpenMeteoService {
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoService::class.java)
    }
}
