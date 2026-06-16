package com.joon.chalkak.domain

private const val DEFAULT_ENFORCEMENT_MARGIN_KMH = 10

data class SpeedJudgement(
    val result: SpeedJudgementResult,
    val measuredSpeedKmh: Int,
    val speedLimitKmh: Int?,
    val enforcementThresholdKmh: Int?
) {
    val isEnforcementRisk: Boolean
        get() = result == SpeedJudgementResult.ENFORCEMENT_RISK
}

enum class SpeedJudgementResult {
    SAFE,
    WARNING,
    ENFORCEMENT_RISK,
    UNKNOWN
}

fun judgeSpeed(
    measuredSpeedKmh: Int,
    speedLimitKmh: Int?,
    enforcementMarginKmh: Int = DEFAULT_ENFORCEMENT_MARGIN_KMH
): SpeedJudgement {
    if (speedLimitKmh == null || speedLimitKmh <= 0 || measuredSpeedKmh < 0) {
        return SpeedJudgement(
            result = SpeedJudgementResult.UNKNOWN,
            measuredSpeedKmh = measuredSpeedKmh,
            speedLimitKmh = speedLimitKmh,
            enforcementThresholdKmh = null
        )
    }

    val enforcementThresholdKmh = speedLimitKmh + enforcementMarginKmh
    val result = when {
        measuredSpeedKmh <= speedLimitKmh -> SpeedJudgementResult.SAFE
        measuredSpeedKmh >= enforcementThresholdKmh -> SpeedJudgementResult.ENFORCEMENT_RISK
        else -> SpeedJudgementResult.WARNING
    }

    return SpeedJudgement(
        result = result,
        measuredSpeedKmh = measuredSpeedKmh,
        speedLimitKmh = speedLimitKmh,
        enforcementThresholdKmh = enforcementThresholdKmh
    )
}
