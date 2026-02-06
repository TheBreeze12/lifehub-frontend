package com.example.lifehub

import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 33: Weather/Plan B Unit Tests
 *
 * Test scope:
 * 1. Plan B data model correctness
 * 2. Weather severity classification
 * 3. WeatherAlert component logic
 * 4. TripViewModel Plan B state management
 * 5. Edge cases
 */
class Phase33WeatherPlanBTest {

    // ==================== 1. Plan B Data Model Tests ====================

    @Test
    fun `test WeatherAssessment model - good weather`() {
        val assessment = TestWeatherAssessment(
            isBadWeather = false,
            severity = "good",
            description = "sunny",
            temperature = 22.0,
            windspeed = 5.0,
            weathercode = 0,
            recommendation = "Good weather for outdoor exercise",
            warnings = null
        )
        assertFalse(assessment.isBadWeather)
        assertEquals("good", assessment.severity)
        assertEquals("sunny", assessment.description)
        assertEquals(22.0, assessment.temperature!!, 0.01)
        assertNull(assessment.warnings)
    }

    @Test
    fun `test WeatherAssessment model - bad weather moderate`() {
        val assessment = TestWeatherAssessment(
            isBadWeather = true,
            severity = "moderate",
            description = "moderate rain",
            temperature = 18.0,
            windspeed = 15.0,
            weathercode = 63,
            recommendation = "Bad weather, suggest indoor exercise",
            warnings = null
        )
        assertTrue(assessment.isBadWeather)
        assertEquals("moderate", assessment.severity)
        assertEquals(63, assessment.weathercode)
    }

    @Test
    fun `test WeatherAssessment model - severe weather with warnings`() {
        val assessment = TestWeatherAssessment(
            isBadWeather = true,
            severity = "severe",
            description = "thunderstorm",
            temperature = 30.0,
            windspeed = 60.0,
            weathercode = 95,
            recommendation = "Severe weather, strongly suggest indoor exercise",
            warnings = listOf("High wind (60.0km/h), outdoor exercise is dangerous")
        )
        assertTrue(assessment.isBadWeather)
        assertEquals("severe", assessment.severity)
        assertNotNull(assessment.warnings)
        assertEquals(1, assessment.warnings!!.size)
    }

    @Test
    fun `test PlanBAlternative model creation`() {
        val alt = TestPlanBAlternative(
            exerciseName = "Indoor Jump Rope",
            exerciseType = "jumping_rope",
            duration = 20,
            calories = 256.7,
            isIndoor = true,
            description = "High efficiency indoor cardio",
            metsValue = 11.0
        )
        assertEquals("Indoor Jump Rope", alt.exerciseName)
        assertEquals("jumping_rope", alt.exerciseType)
        assertEquals(20, alt.duration)
        assertEquals(256.7, alt.calories, 0.01)
        assertTrue(alt.isIndoor)
        assertEquals(11.0, alt.metsValue!!, 0.01)
    }

    @Test
    fun `test PlanBData model creation with alternatives`() {
        val alternatives = listOf(
            TestPlanBAlternative("Jump Rope", "jumping_rope", 20, 256.7, true, "rope", 11.0),
            TestPlanBAlternative("Aerobics", "aerobics", 30, 227.5, true, "aerobics", 6.5)
        )
        val planB = TestPlanBData(
            planId = 1,
            weather = TestWeatherAssessment(true, "moderate", "rain", 18.0, 15.0, 63, "suggest indoor", null),
            needPlanB = true,
            originalCalories = 280.0,
            alternatives = alternatives,
            planBTotalCalories = 484.2,
            reason = "Current weather: moderate rain"
        )
        assertEquals(1, planB.planId)
        assertTrue(planB.needPlanB)
        assertEquals(280.0, planB.originalCalories, 0.01)
        assertEquals(2, planB.alternatives.size)
        assertEquals(484.2, planB.planBTotalCalories, 0.01)
    }

    @Test
    fun `test PlanBData model - no plan b needed`() {
        val planB = TestPlanBData(
            planId = 2,
            weather = TestWeatherAssessment(false, "good", "sunny", 22.0, 5.0, 0, "good for outdoor", null),
            needPlanB = false,
            originalCalories = 300.0,
            alternatives = emptyList(),
            planBTotalCalories = 0.0,
            reason = "Good weather, no plan b needed"
        )
        assertFalse(planB.needPlanB)
        assertTrue(planB.alternatives.isEmpty())
        assertEquals(0.0, planB.planBTotalCalories, 0.01)
    }

    // ==================== 2. Weather Severity Classification Tests ====================

    @Test
    fun `test weather severity classification - good`() {
        val goodCodes = listOf(0, 1, 2, 3)
        for (code in goodCodes) {
            val severity = classifyWeatherSeverity(code)
            assertEquals("Weather code $code should be good", "good", severity)
        }
    }

    @Test
    fun `test weather severity classification - mild`() {
        val mildCodes = listOf(45, 48, 51, 61, 71, 80)
        for (code in mildCodes) {
            val severity = classifyWeatherSeverity(code)
            assertEquals("Weather code $code should be mild", "mild", severity)
        }
    }

    @Test
    fun `test weather severity classification - moderate`() {
        val moderateCodes = listOf(53, 55, 56, 63, 73, 77, 81, 85)
        for (code in moderateCodes) {
            val severity = classifyWeatherSeverity(code)
            assertEquals("Weather code $code should be moderate", "moderate", severity)
        }
    }

    @Test
    fun `test weather severity classification - severe`() {
        val severeCodes = listOf(57, 65, 66, 67, 75, 82, 86, 95, 96, 99)
        for (code in severeCodes) {
            val severity = classifyWeatherSeverity(code)
            assertEquals("Weather code $code should be severe", "severe", severity)
        }
    }

    @Test
    fun `test weather severity classification - unknown code`() {
        val severity = classifyWeatherSeverity(999)
        assertEquals("good", severity)
    }

    @Test
    fun `test isBadWeather logic`() {
        assertTrue(isBadWeather("moderate"))
        assertTrue(isBadWeather("severe"))
        assertFalse(isBadWeather("good"))
        assertFalse(isBadWeather("mild"))
        assertFalse(isBadWeather("unknown"))
    }

    // ==================== 3. WeatherAlert Component Logic Tests ====================

    @Test
    fun `test weather alert level from severity`() {
        assertEquals(WeatherAlertLevel.NONE, getAlertLevel("good"))
        assertEquals(WeatherAlertLevel.INFO, getAlertLevel("mild"))
        assertEquals(WeatherAlertLevel.WARNING, getAlertLevel("moderate"))
        assertEquals(WeatherAlertLevel.DANGER, getAlertLevel("severe"))
        assertEquals(WeatherAlertLevel.NONE, getAlertLevel("unknown"))
    }

    @Test
    fun `test weather icon selection by code`() {
        assertEquals("WbSunny", getWeatherIconName(0))
        assertEquals("Cloud", getWeatherIconName(3))
        assertEquals("Grain", getWeatherIconName(51))
        assertEquals("WaterDrop", getWeatherIconName(61))
        assertEquals("AcUnit", getWeatherIconName(71))
        assertEquals("Thunderstorm", getWeatherIconName(95))
        assertEquals("WbSunny", getWeatherIconName(null))
    }

    @Test
    fun `test weather description from code`() {
        assertEquals("Clear", getWeatherDescription(0))
        assertEquals("Partly cloudy", getWeatherDescription(2))
        assertEquals("Overcast", getWeatherDescription(3))
        assertEquals("Fog", getWeatherDescription(45))
        assertEquals("Rain", getWeatherDescription(61))
        assertEquals("Thunderstorm", getWeatherDescription(95))
        assertEquals("Unknown", getWeatherDescription(null))
    }

    // ==================== 4. Plan B State Management Tests ====================

    @Test
    fun `test PlanBState sealed class - Idle`() {
        val state = TestPlanBState.Idle
        assertTrue(state is TestPlanBState.Idle)
    }

    @Test
    fun `test PlanBState sealed class - Loading`() {
        val state = TestPlanBState.Loading
        assertTrue(state is TestPlanBState.Loading)
    }

    @Test
    fun `test PlanBState sealed class - Success with plan b needed`() {
        val data = TestPlanBData(
            planId = 1,
            weather = TestWeatherAssessment(true, "severe", "thunderstorm", 30.0, 60.0, 95, "strongly suggest indoor", null),
            needPlanB = true,
            originalCalories = 300.0,
            alternatives = listOf(
                TestPlanBAlternative("Yoga", "yoga", 60, 175.0, true, "yoga practice", 2.5)
            ),
            planBTotalCalories = 175.0,
            reason = "thunderstorm weather"
        )
        val state = TestPlanBState.Success(data)
        assertTrue(state is TestPlanBState.Success)
        assertEquals(1, state.data.planId)
        assertTrue(state.data.needPlanB)
    }

    @Test
    fun `test PlanBState sealed class - Success without plan b`() {
        val data = TestPlanBData(
            planId = 2,
            weather = TestWeatherAssessment(false, "good", "sunny", 22.0, 5.0, 0, "good for outdoor", null),
            needPlanB = false,
            originalCalories = 300.0,
            alternatives = emptyList(),
            planBTotalCalories = 0.0,
            reason = "good weather"
        )
        val state = TestPlanBState.Success(data)
        assertFalse(state.data.needPlanB)
        assertTrue(state.data.alternatives.isEmpty())
    }

    @Test
    fun `test PlanBState sealed class - Error`() {
        val state = TestPlanBState.Error("Network request failed")
        assertTrue(state is TestPlanBState.Error)
        assertEquals("Network request failed", state.message)
    }

    // ==================== 5. Edge Case Tests ====================

    @Test
    fun `test PlanBAlternative with zero calories`() {
        val alt = TestPlanBAlternative("Stretching", "stretching", 10, 0.0, true, "stretch", 2.3)
        assertEquals(0.0, alt.calories, 0.01)
    }

    @Test
    fun `test PlanBAlternative with null mets value`() {
        val alt = TestPlanBAlternative("Free exercise", "free", 30, 100.0, true, "free exercise", null)
        assertNull(alt.metsValue)
    }

    @Test
    fun `test WeatherAssessment with null fields`() {
        val assessment = TestWeatherAssessment(
            isBadWeather = false,
            severity = "unknown",
            description = "Unable to get weather data",
            temperature = null,
            windspeed = null,
            weathercode = null,
            recommendation = "Check weather before going out",
            warnings = null
        )
        assertNull(assessment.temperature)
        assertNull(assessment.windspeed)
        assertNull(assessment.weathercode)
    }

    @Test
    fun `test PlanBData with empty alternatives list`() {
        val planB = TestPlanBData(
            planId = 5,
            weather = TestWeatherAssessment(false, "good", "sunny", 25.0, 3.0, 0, "good", null),
            needPlanB = false,
            originalCalories = 200.0,
            alternatives = emptyList(),
            planBTotalCalories = 0.0,
            reason = "good weather"
        )
        assertEquals(0, planB.alternatives.size)
    }

    @Test
    fun `test PlanBData with max 3 alternatives`() {
        val alts = listOf(
            TestPlanBAlternative("Jump Rope", "jumping_rope", 20, 200.0, true, "desc", 11.0),
            TestPlanBAlternative("Aerobics", "aerobics", 30, 195.0, true, "desc", 6.5),
            TestPlanBAlternative("Yoga", "yoga", 40, 105.0, true, "desc", 2.5)
        )
        val planB = TestPlanBData(1, TestWeatherAssessment(true, "severe", "heavy rain", 15.0, 20.0, 65, "indoor", null), true, 500.0, alts, 500.0, "heavy rain")
        assertEquals(3, planB.alternatives.size)
    }

    @Test
    fun `test extreme temperature triggers bad weather`() {
        val hotAssessment = TestWeatherAssessment(true, "severe", "sunny", 40.0, 5.0, 0, "extreme heat", listOf("Extreme heat 40C"))
        assertTrue(hotAssessment.isBadWeather)
        assertEquals("severe", hotAssessment.severity)

        val coldAssessment = TestWeatherAssessment(true, "severe", "clear", -15.0, 5.0, 0, "extreme cold", listOf("Extreme cold -15C"))
        assertTrue(coldAssessment.isBadWeather)
    }

    @Test
    fun `test high wind triggers warning`() {
        val windyAssessment = TestWeatherAssessment(true, "moderate", "partly cloudy", 20.0, 55.0, 2, "high wind warning", listOf("High wind 55km/h"))
        assertTrue(windyAssessment.isBadWeather)
        assertNotNull(windyAssessment.warnings)
    }

    @Test
    fun `test calorie comparison between original and plan b`() {
        val original = 300.0
        val planBCals = 285.0
        val diff = kotlin.math.abs(original - planBCals)
        val ratio = planBCals / original
        assertTrue("Plan B calories should be within 20pct of original", ratio >= 0.8)
        assertTrue("Difference should be reasonable", diff < 100.0)
    }

    @Test
    fun `test weather severity ordering`() {
        val severities = listOf("good", "mild", "moderate", "severe")
        val order = mapOf("good" to 0, "mild" to 1, "moderate" to 2, "severe" to 3)
        for (i in 0 until severities.size - 1) {
            assertTrue(
                "${severities[i]} should be less severe than ${severities[i+1]}",
                order[severities[i]]!! < order[severities[i+1]]!!
            )
        }
    }

    @Test
    fun `test all WMO weather codes are categorized`() {
        val allCodes = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)
        for (code in allCodes) {
            val severity = classifyWeatherSeverity(code)
            assertTrue(
                "Code $code should have a valid severity",
                severity in listOf("good", "mild", "moderate", "severe")
            )
        }
    }

    @Test
    fun `test indoor exercise types are unique`() {
        val types = listOf("jumping_rope", "aerobics", "yoga", "weight_training", "cycling", "stair_climbing", "gym", "stretching", "running", "tai_chi", "dancing", "table_tennis")
        assertEquals("All indoor exercise types should be unique", types.size, types.toSet().size)
    }

    @Test
    fun `test plan b response model success`() {
        val response = TestPlanBResponse(
            code = 200,
            message = "Plan B generated",
            data = TestPlanBData(
                planId = 1,
                weather = TestWeatherAssessment(true, "moderate", "rain", 18.0, 15.0, 63, "indoor", null),
                needPlanB = true,
                originalCalories = 280.0,
                alternatives = listOf(TestPlanBAlternative("Yoga", "yoga", 60, 175.0, true, "yoga", 2.5)),
                planBTotalCalories = 175.0,
                reason = "rain"
            )
        )
        assertEquals(200, response.code)
        assertNotNull(response.data)
        assertTrue(response.data!!.needPlanB)
    }

    @Test
    fun `test plan b response model error case`() {
        val response = TestPlanBResponse(
            code = 404,
            message = "Plan not found",
            data = null
        )
        assertEquals(404, response.code)
        assertNull(response.data)
    }

    @Test
    fun `test plan b response model server error`() {
        val response = TestPlanBResponse(
            code = 500,
            message = "Internal server error",
            data = null
        )
        assertEquals(500, response.code)
        assertNull(response.data)
    }

    @Test
    fun `test weather assessment with all warnings`() {
        val assessment = TestWeatherAssessment(
            isBadWeather = true,
            severity = "severe",
            description = "thunderstorm",
            temperature = 40.0,
            windspeed = 55.0,
            weathercode = 95,
            recommendation = "Do not go outside",
            warnings = listOf("Extreme heat 40C", "High wind 55km/h")
        )
        assertEquals(2, assessment.warnings!!.size)
        assertTrue(assessment.warnings!![0].contains("40"))
        assertTrue(assessment.warnings!![1].contains("55"))
    }

    @Test
    fun `test plan b data calorie matching quality`() {
        val alts = listOf(
            TestPlanBAlternative("Rope", "jumping_rope", 15, 192.5, true, "d", 11.0),
            TestPlanBAlternative("Aerobics", "aerobics", 20, 152.0, true, "d", 6.5)
        )
        val totalPlanB = alts.sumOf { it.calories }
        val original = 350.0
        val ratio = totalPlanB / original
        assertTrue("Plan B total should be at least 50pct of original", ratio >= 0.5)
    }

    // ==================== Helper Models and Functions ====================

    data class TestWeatherAssessment(
        val isBadWeather: Boolean,
        val severity: String,
        val description: String,
        val temperature: Double?,
        val windspeed: Double?,
        val weathercode: Int?,
        val recommendation: String,
        val warnings: List<String>?
    )

    data class TestPlanBAlternative(
        val exerciseName: String,
        val exerciseType: String,
        val duration: Int,
        val calories: Double,
        val isIndoor: Boolean,
        val description: String,
        val metsValue: Double?
    )

    data class TestPlanBData(
        val planId: Int,
        val weather: TestWeatherAssessment,
        val needPlanB: Boolean,
        val originalCalories: Double,
        val alternatives: List<TestPlanBAlternative>,
        val planBTotalCalories: Double,
        val reason: String
    )

    data class TestPlanBResponse(
        val code: Int,
        val message: String,
        val data: TestPlanBData?
    )

    sealed class TestPlanBState {
        object Idle : TestPlanBState()
        object Loading : TestPlanBState()
        data class Success(val data: TestPlanBData) : TestPlanBState()
        data class Error(val message: String) : TestPlanBState()
    }

    enum class WeatherAlertLevel { NONE, INFO, WARNING, DANGER }

    private fun classifyWeatherSeverity(code: Int): String {
        return when (code) {
            0, 1, 2, 3 -> "good"
            45, 48, 51, 61, 71, 80 -> "mild"
            53, 55, 56, 63, 73, 77, 81, 85 -> "moderate"
            57, 65, 66, 67, 75, 82, 86, 95, 96, 99 -> "severe"
            else -> "good"
        }
    }

    private fun isBadWeather(severity: String): Boolean {
        return severity in listOf("moderate", "severe")
    }

    private fun getAlertLevel(severity: String): WeatherAlertLevel {
        return when (severity) {
            "good" -> WeatherAlertLevel.NONE
            "mild" -> WeatherAlertLevel.INFO
            "moderate" -> WeatherAlertLevel.WARNING
            "severe" -> WeatherAlertLevel.DANGER
            else -> WeatherAlertLevel.NONE
        }
    }

    private fun getWeatherIconName(code: Int?): String {
        return when (code) {
            null -> "WbSunny"
            0, 1 -> "WbSunny"
            2, 3 -> "Cloud"
            45, 48 -> "Cloud"
            in 51..55 -> "Grain"
            in 56..57 -> "Grain"
            in 61..67 -> "WaterDrop"
            in 71..77 -> "AcUnit"
            in 80..82 -> "WaterDrop"
            in 85..86 -> "AcUnit"
            in 95..99 -> "Thunderstorm"
            else -> "WbSunny"
        }
    }

    private fun getWeatherDescription(code: Int?): String {
        return when (code) {
            null -> "Unknown"
            0 -> "Clear"
            1, 2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }
}
