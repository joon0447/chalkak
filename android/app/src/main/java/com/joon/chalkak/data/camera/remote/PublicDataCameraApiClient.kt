package com.joon.chalkak.data.camera.remote

import android.util.Log
import com.joon.chalkak.BuildConfig
import com.joon.chalkak.domain.SpeedCamera
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PublicDataCameraApiClient(
    private val serviceKey: String = BuildConfig.PUBLIC_DATA_SERVICE_KEY,
    private val baseUrl: String = BASE_URL
) {
    suspend fun fetchCameras(
        pageNo: Int = 1,
        numOfRows: Int = 1000,
        provinceName: String? = null,
        cityName: String? = null
    ): List<SpeedCamera> {
        require(pageNo > 0) { "pageNo must be greater than 0." }
        require(numOfRows in 1..1000) { "numOfRows must be between 1 and 1000." }
        require(serviceKey.isNotBlank()) {
            "PUBLIC_DATA_SERVICE_KEY is missing. Add it to local.properties."
        }

        val response = request(
            query = buildQuery(
                pageNo = pageNo,
                numOfRows = numOfRows,
                provinceName = provinceName,
                cityName = cityName
            )
        )
        val cameras = PublicDataCameraJsonParser.parse(response).map { it.toDomain() }
        Log.d(
            TAG,
            "Parsed cameras: count=${cameras.size}, first=${cameras.firstOrNull()?.toLogText()}"
        )
        return cameras
    }

    private fun buildQuery(
        pageNo: Int,
        numOfRows: Int,
        provinceName: String?,
        cityName: String?
    ): String {
        val params = buildList<QueryParameter> {
            add(QueryParameter("serviceKey", serviceKey, encodeValue = !serviceKey.contains("%")))
            add(QueryParameter("pageNo", pageNo.toString()))
            add(QueryParameter("numOfRows", numOfRows.toString()))
            add(QueryParameter("type", "json"))
            provinceName?.takeIf { it.isNotBlank() }?.let { add(QueryParameter("ctprvnNm", it)) }
            cityName?.takeIf { it.isNotBlank() }?.let { add(QueryParameter("signguNm", it)) }
        }

        return params.joinToString("&") { parameter ->
            "${parameter.key}=${parameter.encodedValue()}"
        }
    }

    private fun request(query: String): String {
        val connection = URL("$baseUrl?$query").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS

        return try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: throw IOException("HTTP $responseCode")
            }

            stream.bufferedReader().use { it.readText() }.also {
                if (responseCode !in 200..299) {
                    throw IOException("HTTP $responseCode: $it")
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private data class QueryParameter(
        val key: String,
        val value: String,
        val encodeValue: Boolean = true
    ) {
        fun encodedValue(): String =
            if (encodeValue) URLEncoder.encode(value, Charsets.UTF_8.name()) else value
    }

    private fun String.preview(maxLength: Int = 240): String =
        replace("\n", "").take(maxLength)

    private fun SpeedCamera.toLogText(): String =
        "id=$id, location=$location, limit=$speedLimitKmh, lat=$latitude, lng=$longitude"

    private companion object {
        const val TAG = "CameraApi"
        const val BASE_URL = "https://api.data.go.kr/openapi/tn_pubr_public_unmanned_traffic_camera_api"
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}
