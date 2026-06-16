package com.joon.chalkak.data.camera.remote

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object PublicDataCameraJsonParser {
    fun parse(json: String): List<PublicDataCameraDto> {
        val root = JSONObject(json)
        root.logResultHeader()
        val items = root.findItemsArray() ?: return emptyList()

        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val manageNo = item.optNullableString("mnlssRegltCameraManageNo") ?: continue
                add(
                    PublicDataCameraDto(
                        manageNo = manageNo,
                        provinceName = item.optNullableString("ctprvnNm"),
                        cityName = item.optNullableString("signguNm"),
                        roadKind = item.optNullableString("roadKnd"),
                        roadRouteNo = item.optNullableString("roadRouteNo"),
                        roadRouteName = item.optNullableString("roadRouteNm"),
                        roadRouteDirection = item.optNullableString("roadRouteDrc"),
                        roadAddress = item.optNullableString("rdnmadr"),
                        lotAddress = item.optNullableString("lnmadr"),
                        latitude = item.optNullableDouble("latitude"),
                        longitude = item.optNullableDouble("longitude"),
                        location = item.optNullableString("itlpc"),
                        enforcementTypeCode = item.optNullableString("regltSe"),
                        speedLimitKmh = item.optNullableInt("lmttVe"),
                        sectionPositionCode = item.optNullableString("regltSctnLcSe"),
                        sectionLengthMeters = item.optNullableInt("ovrspdRegltSctnLt"),
                        protectedAreaTypeCode = item.optNullableString("prtcareaType"),
                        installationYear = item.optNullableString("installationYear"),
                        institutionName = item.optNullableString("institutionNm"),
                        phoneNumber = item.optNullableString("phoneNumber"),
                        referenceDate = item.optNullableString("referenceDate"),
                        institutionCode = item.optNullableString("instt_code")
                    )
                )
            }
        }
    }

    private fun JSONObject.findItemsArray(): JSONArray? {
        val response = optJSONObject("response")
        val body = response?.optJSONObject("body")
        val nestedItems = body?.opt("items")

        return when (nestedItems) {
            is JSONArray -> nestedItems
            is JSONObject -> nestedItems.optJSONArray("item")
            else -> optJSONArray("items")
        }
    }

    private fun JSONObject.optNullableString(name: String): String? =
        optString(name).takeIf { it.isNotBlank() }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        optNullableString(name)?.toDoubleOrNull()

    private fun JSONObject.optNullableInt(name: String): Int? =
        optNullableString(name)?.toIntOrNull()

    private fun JSONObject.logResultHeader() {
        val header = optJSONObject("response")?.optJSONObject("header")
        val resultCode = header?.optString("resultCode").orEmpty()
        val resultMsg = header?.optString("resultMsg").orEmpty()
        if (resultCode.isNotBlank() || resultMsg.isNotBlank()) {
            Log.d(TAG, "Result header: code=$resultCode, message=$resultMsg")
        }
    }

    private const val TAG = "CameraApi"
}
