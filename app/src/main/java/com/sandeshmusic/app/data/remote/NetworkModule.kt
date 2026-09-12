package com.sandeshmusic.app.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object NetworkModule {

    private class CoverFallbackInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)
            val urlString = request.url.toString()

            // If a cover URL fails with 404, automatically try common name variations
            if (response.code == 404 && urlString.contains("/covers/")) {
                val alternatives = mutableListOf<String>()

                val singleDigit = Regex("/covers/song([1-9])\\.(png|jpg|jpeg)", RegexOption.IGNORE_CASE)
                if (singleDigit.containsMatchIn(urlString)) {
                    alternatives.add(singleDigit.replace(urlString) { "/covers/song0${it.groupValues[1]}.png" })
                    alternatives.add(singleDigit.replace(urlString) { "/covers/song0${it.groupValues[1]}.jpg" })
                }

                val doubleDigit = Regex("/covers/song0([1-9])\\.(png|jpg|jpeg)", RegexOption.IGNORE_CASE)
                if (doubleDigit.containsMatchIn(urlString)) {
                    alternatives.add(doubleDigit.replace(urlString) { "/covers/song${it.groupValues[1]}.png" })
                    alternatives.add(doubleDigit.replace(urlString) { "/covers/song${it.groupValues[1]}.jpg" })
                }

                if (urlString.endsWith(".jpg", ignoreCase = true)) {
                    alternatives.add(urlString.substringBeforeLast(".jpg") + ".png")
                } else if (urlString.endsWith(".png", ignoreCase = true)) {
                    alternatives.add(urlString.substringBeforeLast(".png") + ".jpg")
                }

                for (alt in alternatives.distinct()) {
                    if (alt != urlString) {
                        response.close()
                        val retryReq = request.newBuilder().url(alt).build()
                        val altResponse = chain.proceed(retryReq)
                        if (altResponse.isSuccessful) {
                            return altResponse
                        }
                        altResponse.close()
                    }
                }
            }

            return response
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(CoverFallbackInterceptor())
            .addInterceptor(logging)
            .retryOnConnectionFailure(true)
            .build()
    }

    val musicApi: MusicApi by lazy {
        MusicApiImpl(okHttpClient)
    }
}
