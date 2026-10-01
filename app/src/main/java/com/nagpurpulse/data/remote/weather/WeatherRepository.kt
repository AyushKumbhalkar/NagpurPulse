package com.nagpurpulse.data.remote.weather

import com.nagpurpulse.BuildConfig
import javax.inject.Inject

class WeatherRepository @Inject constructor(
    private val api: WeatherApi
) {

    suspend fun getWeatherAndAqi(
        lat: Double,
        lon: Double
    ): Pair<Int, Int> {

        val weather = api.getWeatherByLocation(
            lat = lat,
            lon = lon,
            apiKey = BuildConfig.OPENWEATHER_API_KEY
        )

        val pollution = api.getAirPollution(
            lat = lat,
            lon = lon,
            apiKey = BuildConfig.OPENWEATHER_API_KEY
        )

        val pm25 =
            pollution.list.firstOrNull()?.components?.pm2_5?.toInt()
                ?: 0

        return Pair(
            weather.main.temp.toInt(),
            pm25
        )
    }
}