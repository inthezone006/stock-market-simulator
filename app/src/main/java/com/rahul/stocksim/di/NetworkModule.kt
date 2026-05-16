package com.rahul.stocksim.di

import android.content.Context
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.data.FinnhubApi
import com.rahul.stocksim.data.TwelveDataApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository {
        return AuthRepository()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // 50 MB Cache
        val cacheSize = 50 * 1024 * 1024L
        val cache = okhttp3.Cache(context.cacheDir, cacheSize)

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                var request = chain.request()
                request = if (isNetworkAvailable(context))
                    request.newBuilder().header("Cache-Control", "public, max-age=" + 60).build()
                else
                    request.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    @Provides
    @Singleton
    fun provideFinnhubApi(okHttpClient: OkHttpClient): FinnhubApi {
        return Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinnhubApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTwelveDataApi(okHttpClient: OkHttpClient): TwelveDataApi {
        return Retrofit.Builder()
            .baseUrl("https://api.twelvedata.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TwelveDataApi::class.java)
    }
}
