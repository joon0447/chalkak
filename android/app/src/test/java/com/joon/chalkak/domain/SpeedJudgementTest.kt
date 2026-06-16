package com.joon.chalkak.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedJudgementTest {
    @Test
    fun judgeSpeed_returnsSafe_whenMeasuredSpeedIsWithinLimit() {
        val judgement = judgeSpeed(measuredSpeedKmh = 60, speedLimitKmh = 60)

        assertEquals(SpeedJudgementResult.SAFE, judgement.result)
        assertEquals(70, judgement.enforcementThresholdKmh)
        assertFalse(judgement.isEnforcementRisk)
    }

    @Test
    fun judgeSpeed_returnsWarning_whenMeasuredSpeedExceedsLimitButIsUnderEnforcementThreshold() {
        val judgement = judgeSpeed(measuredSpeedKmh = 69, speedLimitKmh = 60)

        assertEquals(SpeedJudgementResult.WARNING, judgement.result)
        assertEquals(70, judgement.enforcementThresholdKmh)
        assertFalse(judgement.isEnforcementRisk)
    }

    @Test
    fun judgeSpeed_returnsEnforcementRisk_whenMeasuredSpeedIsLimitPlusTenOrHigher() {
        val judgement = judgeSpeed(measuredSpeedKmh = 70, speedLimitKmh = 60)

        assertEquals(SpeedJudgementResult.ENFORCEMENT_RISK, judgement.result)
        assertEquals(70, judgement.enforcementThresholdKmh)
        assertTrue(judgement.isEnforcementRisk)
    }

    @Test
    fun judgeSpeed_returnsUnknown_whenSpeedLimitIsMissing() {
        val judgement = judgeSpeed(measuredSpeedKmh = 60, speedLimitKmh = null)

        assertEquals(SpeedJudgementResult.UNKNOWN, judgement.result)
        assertEquals(null, judgement.enforcementThresholdKmh)
    }
}
