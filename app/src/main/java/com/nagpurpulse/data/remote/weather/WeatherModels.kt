package com.nagpurpulse.data.remote.weather

data class WeatherResponse(
    val main: MainData,
    val coord: Coord
)

data class Coord(
    val lat: Double,
    val lon: Double
)

data class MainData(
    val temp: Double
)

data class AirPollutionResponse(
    val list: List<AqiItem>
)

data class AqiItem(
    val main: AqiMain,
    val components: Components
)

data class Components(
    val pm2_5: Double
)

data class AqiMain(
    val aqi: Int
)
