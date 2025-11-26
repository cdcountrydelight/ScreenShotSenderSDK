package com.cd.screenshotsender.data.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal object HttpClientManager {

    @Volatile
    private var retrofit: Retrofit? = null

    @Volatile
    private var apiService: SendScreenShotApiService? = null

    var isProdEnv = true

    fun getApiService(context: Context): SendScreenShotApiService {
        return apiService ?: synchronized(this) {
            apiService ?: createRetrofit(context).create(SendScreenShotApiService::class.java)
                .also {
                    apiService = it
                }
        }
    }

    private fun createRetrofit(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: Retrofit.Builder()
                .baseUrl(if (isProdEnv) "https://stock.countrydelight.in/api/cd_training/" else "https://qa-stock.countrydelight.in/api/cd_training/")
                .client(createOkHttpClient(context))
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder()
                            .setPrettyPrinting()
                            .disableHtmlEscaping()
                            .create()
                    )
                )
                .build()
                .also { retrofit = it }
        }
    }

    private fun createOkHttpClient(context: Context): OkHttpClient {
        val baseClient = OkHttpClient.Builder()
            .addInterceptor(AppNetworkInterceptorImpl(context))
        if (!isProdEnv) {
            baseClient.addInterceptor(getChuckerInterceptor(context))
        }
        return baseClient.connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }


    private fun getChuckerInterceptor(context: Context): ChuckerInterceptor {
        val chuckerCollector = ChuckerCollector(
            context = context,
            showNotification = true,
            retentionPeriod = RetentionManager.Period.ONE_HOUR
        )
        return ChuckerInterceptor.Builder(context)
            .collector(chuckerCollector)
            .maxContentLength(250_000L)
            .build()
    }

    fun clearInstance() {
        synchronized(this) {
            retrofit = null
            apiService = null
        }
    }
}