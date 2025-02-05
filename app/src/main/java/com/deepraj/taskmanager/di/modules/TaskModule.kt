package com.deepraj.taskmanager.di.modules

import android.content.Context
import com.deepraj.taskmanager.BuildConfig
import com.deepraj.taskmanager.database.TaskDatabase
import com.deepraj.taskmanager.datasource.TaskApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
class TaskModule {

    @Provides
    fun provideTaskApi(): TaskApi =
        Retrofit.Builder()
            .client(getOkHttpClient())
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TaskApi::class.java)

    private fun getOkHttpClient(): OkHttpClient {
        val client = OkHttpClient.Builder()
            .connectTimeout(NETWORK_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NETWORK_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NETWORK_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })

        return client.build()
    }

    @Provides
    fun provideDatabase(@ApplicationContext context: Context): TaskDatabase {
        return TaskDatabase.invoke(context = context)
    }


    companion object {
        private const val NETWORK_REQUEST_TIMEOUT_SECONDS = 15L
        private const val BASE_URL = BuildConfig.BASE_URL
    }

}