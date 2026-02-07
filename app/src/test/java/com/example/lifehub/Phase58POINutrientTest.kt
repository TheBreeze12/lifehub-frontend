package com.example.lifehub

import com.example.lifehub.ui.components.ExercisePOI
import com.example.lifehub.ui.components.POIType
import com.example.lifehub.ui.components.generateExercisePOIs
import com.example.lifehub.ui.components.filterPOIsByType
import com.example.lifehub.ui.components.getPOITypeLabel
import com.example.lifehub.ui.components.getPOITypeIcon
import com.example.lifehub.ui.components.NutrientType
import com.example.lifehub.ui.components.NutrientDisplayItem
import com.example.lifehub.ui.components.getDefaultNutrientTypes
import com.example.lifehub.ui.components.getAllNutrientTypes
import com.example.lifehub.ui.components.getExtendedNutrientTypes
import com.example.lifehub.ui.components.estimateExtendedNutrients
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 58: 运动POI展示 + 营养素自定义展示 - 前端单元测试
 *
 * 测试内容：
 * 1. POI数据模型创建与属性验证
 * 2. POI类型枚举完整性
 * 3. POI生成逻辑（基于坐标生成周边POI）
 * 4. POI按类型筛选
 * 5. POI标签与图标映射
 * 6. 营养素类型枚举完整性
 * 7. 默认/扩展营养素列表
 * 8. 营养素估算逻辑
 * 9. 营养素展示项数据模型
 * 10. 边界情况测试
 */
class Phase58POINutrientTest {

    // ============================================================
    // 1. ExercisePOI 数据模型测试
    // ============================================================

    @Test
    fun `test ExercisePOI creation with all fields`() {
        val poi = ExercisePOI(
            id = "poi_1",
            name = "朝阳公园",
            type = POIType.PARK,
            latitude = 39.9342,
            longitude = 116.4740,
            description = "大型城市公园，适合跑步和散步",
            distance = 500.0
        )

        assertEquals("poi_1", poi.id)
        assertEquals("朝阳公园", poi.name)
        assertEquals(POIType.PARK, poi.type)
        assertEquals(39.9342, poi.latitude, 0.0001)
        assertEquals(116.4740, poi.longitude, 0.0001)
        assertEquals("大型城市公园，适合跑步和散步", poi.description)
        assertEquals(500.0, poi.distance!!, 0.1)
    }

    @Test
    fun `test ExercisePOI creation with default values`() {
        val poi = ExercisePOI(
            id = "poi_2",
            name = "测试POI",
            type = POIType.TRAIL,
            latitude = 39.9,
            longitude = 116.4
        )

        assertEquals("poi_2", poi.id)
        assertEquals(POIType.TRAIL, poi.type)
        assertNull(poi.description)
        assertNull(poi.distance)
    }

    @Test
    fun `test ExercisePOI equality`() {
        val poi1 = ExercisePOI("1", "公园A", POIType.PARK, 39.9, 116.4)
        val poi2 = ExercisePOI("1", "公园A", POIType.PARK, 39.9, 116.4)
        val poi3 = ExercisePOI("2", "公园B", POIType.PARK, 39.9, 116.4)

        assertEquals(poi1, poi2)
        assertNotEquals(poi1, poi3)
    }

    // ============================================================
    // 2. POIType 枚举测试
    // ============================================================

    @Test
    fun `test POIType has all expected types`() {
        val types = POIType.values()
        assertTrue(types.contains(POIType.PARK))
        assertTrue(types.contains(POIType.TRAIL))
        assertTrue(types.contains(POIType.FITNESS_AREA))
        assertTrue(types.contains(POIType.GYM))
        assertTrue(types.contains(POIType.SPORTS_FIELD))
        assertEquals(5, types.size)
    }

    @Test
    fun `test getPOITypeLabel returns correct Chinese labels`() {
        assertEquals("公园", getPOITypeLabel(POIType.PARK))
        assertEquals("步道", getPOITypeLabel(POIType.TRAIL))
        assertEquals("健身区", getPOITypeLabel(POIType.FITNESS_AREA))
        assertEquals("健身房", getPOITypeLabel(POIType.GYM))
        assertEquals("运动场", getPOITypeLabel(POIType.SPORTS_FIELD))
    }

    @Test
    fun `test getPOITypeIcon returns non-null for all types`() {
        POIType.values().forEach { type ->
            assertNotNull("Icon should not be null for $type", getPOITypeIcon(type))
        }
    }

    // ============================================================
    // 3. POI生成逻辑测试
    // ============================================================

    @Test
    fun `test generateExercisePOIs returns non-empty list`() {
        val pois = generateExercisePOIs(39.9042, 116.4074)
        assertTrue("Should generate at least 1 POI", pois.isNotEmpty())
    }

    @Test
    fun `test generateExercisePOIs generates POIs near given coordinates`() {
        val centerLat = 39.9042
        val centerLng = 116.4074
        val pois = generateExercisePOIs(centerLat, centerLng)

        pois.forEach { poi ->
            // POI应该在中心点附近（约2km范围内，即约0.02度）
            val latDiff = Math.abs(poi.latitude - centerLat)
            val lngDiff = Math.abs(poi.longitude - centerLng)
            assertTrue(
                "POI ${poi.name} should be within ~2km: latDiff=$latDiff, lngDiff=$lngDiff",
                latDiff < 0.025 && lngDiff < 0.025
            )
        }
    }

    @Test
    fun `test generateExercisePOIs has unique IDs`() {
        val pois = generateExercisePOIs(39.9042, 116.4074)
        val ids = pois.map { it.id }
        assertEquals("All POI IDs should be unique", ids.size, ids.distinct().size)
    }

    @Test
    fun `test generateExercisePOIs covers multiple types`() {
        val pois = generateExercisePOIs(39.9042, 116.4074)
        val types = pois.map { it.type }.distinct()
        assertTrue("Should have at least 2 different POI types", types.size >= 2)
    }

    @Test
    fun `test generateExercisePOIs with different coordinates`() {
        val pois1 = generateExercisePOIs(39.9042, 116.4074)
        val pois2 = generateExercisePOIs(31.2304, 121.4737) // 上海

        // 两个不同位置生成的POI坐标应不同
        assertNotEquals(
            pois1.firstOrNull()?.latitude,
            pois2.firstOrNull()?.latitude
        )
    }

    @Test
    fun `test generateExercisePOIs with edge coordinates`() {
        // 测试极端坐标值
        val pois1 = generateExercisePOIs(0.0, 0.0)
        assertTrue("Should generate POIs even at origin", pois1.isNotEmpty())

        val pois2 = generateExercisePOIs(89.0, 179.0)
        assertTrue("Should generate POIs at extreme coordinates", pois2.isNotEmpty())
    }

    // ============================================================
    // 4. POI筛选测试
    // ============================================================

    @Test
    fun `test filterPOIsByType with specific type`() {
        val pois = listOf(
            ExercisePOI("1", "公园A", POIType.PARK, 39.9, 116.4),
            ExercisePOI("2", "步道B", POIType.TRAIL, 39.9, 116.4),
            ExercisePOI("3", "公园C", POIType.PARK, 39.9, 116.4),
            ExercisePOI("4", "健身房D", POIType.GYM, 39.9, 116.4)
        )

        val parks = filterPOIsByType(pois, POIType.PARK)
        assertEquals(2, parks.size)
        assertTrue(parks.all { it.type == POIType.PARK })
    }

    @Test
    fun `test filterPOIsByType with null type returns all`() {
        val pois = listOf(
            ExercisePOI("1", "公园A", POIType.PARK, 39.9, 116.4),
            ExercisePOI("2", "步道B", POIType.TRAIL, 39.9, 116.4)
        )

        val all = filterPOIsByType(pois, null)
        assertEquals(2, all.size)
    }

    @Test
    fun `test filterPOIsByType with no matching type`() {
        val pois = listOf(
            ExercisePOI("1", "公园A", POIType.PARK, 39.9, 116.4),
            ExercisePOI("2", "公园B", POIType.PARK, 39.9, 116.4)
        )

        val gyms = filterPOIsByType(pois, POIType.GYM)
        assertTrue(gyms.isEmpty())
    }

    @Test
    fun `test filterPOIsByType with empty list`() {
        val result = filterPOIsByType(emptyList(), POIType.PARK)
        assertTrue(result.isEmpty())
    }

    // ============================================================
    // 5. NutrientType 枚举测试
    // ============================================================

    @Test
    fun `test NutrientType has all expected types`() {
        val types = NutrientType.values()
        assertTrue(types.contains(NutrientType.PROTEIN))
        assertTrue(types.contains(NutrientType.FAT))
        assertTrue(types.contains(NutrientType.CARBS))
        assertTrue(types.contains(NutrientType.DIETARY_FIBER))
        assertTrue(types.contains(NutrientType.SODIUM))
        assertTrue(types.contains(NutrientType.VITAMIN_A))
        assertTrue(types.contains(NutrientType.VITAMIN_C))
        assertTrue(types.contains(NutrientType.CALCIUM))
        assertTrue(types.contains(NutrientType.IRON))
        assertTrue(types.contains(NutrientType.POTASSIUM))
        assertTrue("Should have at least 10 nutrient types", types.size >= 10)
    }

    @Test
    fun `test NutrientType label is non-empty for all types`() {
        NutrientType.values().forEach { type ->
            assertTrue(
                "Label for $type should not be empty",
                type.label.isNotBlank()
            )
        }
    }

    @Test
    fun `test NutrientType unit is non-empty for all types`() {
        NutrientType.values().forEach { type ->
            assertTrue(
                "Unit for $type should not be empty",
                type.unit.isNotBlank()
            )
        }
    }

    // ============================================================
    // 6. 默认/扩展营养素列表测试
    // ============================================================

    @Test
    fun `test getDefaultNutrientTypes contains core nutrients`() {
        val defaults = getDefaultNutrientTypes()
        assertTrue(defaults.contains(NutrientType.PROTEIN))
        assertTrue(defaults.contains(NutrientType.FAT))
        assertTrue(defaults.contains(NutrientType.CARBS))
        assertEquals(3, defaults.size)
    }

    @Test
    fun `test getExtendedNutrientTypes contains additional nutrients`() {
        val extended = getExtendedNutrientTypes()
        assertTrue(extended.contains(NutrientType.DIETARY_FIBER))
        assertTrue(extended.contains(NutrientType.SODIUM))
        assertTrue(extended.contains(NutrientType.VITAMIN_A))
        assertTrue(extended.contains(NutrientType.VITAMIN_C))
        assertTrue(extended.contains(NutrientType.CALCIUM))
        assertTrue(extended.contains(NutrientType.IRON))
        assertTrue("Extended should have at least 6 types", extended.size >= 6)
    }

    @Test
    fun `test getAllNutrientTypes is superset of default and extended`() {
        val all = getAllNutrientTypes()
        val defaults = getDefaultNutrientTypes()
        val extended = getExtendedNutrientTypes()

        assertTrue(all.containsAll(defaults))
        assertTrue(all.containsAll(extended))
        assertEquals(defaults.size + extended.size, all.size)
    }

    @Test
    fun `test no overlap between default and extended`() {
        val defaults = getDefaultNutrientTypes().toSet()
        val extended = getExtendedNutrientTypes().toSet()
        val overlap = defaults.intersect(extended)
        assertTrue("Default and extended should not overlap", overlap.isEmpty())
    }

    // ============================================================
    // 7. NutrientDisplayItem 数据模型测试
    // ============================================================

    @Test
    fun `test NutrientDisplayItem creation`() {
        val item = NutrientDisplayItem(
            type = NutrientType.DIETARY_FIBER,
            value = 3.5f,
            dailyRecommended = 25.0f
        )

        assertEquals(NutrientType.DIETARY_FIBER, item.type)
        assertEquals(3.5f, item.value, 0.01f)
        assertEquals(25.0f, item.dailyRecommended, 0.01f)
    }

    @Test
    fun `test NutrientDisplayItem percentage calculation`() {
        val item = NutrientDisplayItem(
            type = NutrientType.CALCIUM,
            value = 200.0f,
            dailyRecommended = 800.0f
        )

        val percentage = item.value / item.dailyRecommended
        assertEquals(0.25f, percentage, 0.01f)
    }

    @Test
    fun `test NutrientDisplayItem with zero daily recommended`() {
        val item = NutrientDisplayItem(
            type = NutrientType.SODIUM,
            value = 500.0f,
            dailyRecommended = 0.0f
        )
        assertEquals(0.0f, item.dailyRecommended, 0.01f)
    }

    // ============================================================
    // 8. 营养素估算逻辑测试
    // ============================================================

    @Test
    fun `test estimateExtendedNutrients returns items for all selected types`() {
        val selectedTypes = listOf(
            NutrientType.DIETARY_FIBER,
            NutrientType.SODIUM,
            NutrientType.CALCIUM
        )
        val items = estimateExtendedNutrients(
            foodName = "番茄炒蛋",
            calories = 150,
            protein = 10.5f,
            fat = 8.2f,
            carbs = 6.3f,
            selectedTypes = selectedTypes
        )

        assertEquals(3, items.size)
        assertTrue(items.any { it.type == NutrientType.DIETARY_FIBER })
        assertTrue(items.any { it.type == NutrientType.SODIUM })
        assertTrue(items.any { it.type == NutrientType.CALCIUM })
    }

    @Test
    fun `test estimateExtendedNutrients returns non-negative values`() {
        val allExtended = getExtendedNutrientTypes()
        val items = estimateExtendedNutrients(
            foodName = "红烧肉",
            calories = 350,
            protein = 20.0f,
            fat = 25.0f,
            carbs = 8.0f,
            selectedTypes = allExtended
        )

        items.forEach { item ->
            assertTrue(
                "Value for ${item.type} should be >= 0, got ${item.value}",
                item.value >= 0f
            )
            assertTrue(
                "Daily recommended for ${item.type} should be > 0",
                item.dailyRecommended > 0f
            )
        }
    }

    @Test
    fun `test estimateExtendedNutrients with empty selected types`() {
        val items = estimateExtendedNutrients(
            foodName = "白粥",
            calories = 50,
            protein = 1.0f,
            fat = 0.2f,
            carbs = 10.0f,
            selectedTypes = emptyList()
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `test estimateExtendedNutrients with vegetable food`() {
        val items = estimateExtendedNutrients(
            foodName = "凉拌黄瓜",
            calories = 30,
            protein = 1.0f,
            fat = 0.5f,
            carbs = 5.0f,
            selectedTypes = listOf(NutrientType.DIETARY_FIBER, NutrientType.VITAMIN_C)
        )

        assertEquals(2, items.size)
        // 蔬菜类食物应该有一定的膳食纤维
        val fiberItem = items.find { it.type == NutrientType.DIETARY_FIBER }
        assertNotNull(fiberItem)
        assertTrue("Vegetable should have some fiber", fiberItem!!.value > 0f)
    }

    @Test
    fun `test estimateExtendedNutrients with meat food`() {
        val items = estimateExtendedNutrients(
            foodName = "红烧排骨",
            calories = 400,
            protein = 25.0f,
            fat = 30.0f,
            carbs = 5.0f,
            selectedTypes = listOf(NutrientType.IRON, NutrientType.SODIUM)
        )

        assertEquals(2, items.size)
        // 肉类食物应该有铁
        val ironItem = items.find { it.type == NutrientType.IRON }
        assertNotNull(ironItem)
        assertTrue("Meat should have iron", ironItem!!.value > 0f)
    }

    @Test
    fun `test estimateExtendedNutrients with zero calories`() {
        val items = estimateExtendedNutrients(
            foodName = "水",
            calories = 0,
            protein = 0f,
            fat = 0f,
            carbs = 0f,
            selectedTypes = listOf(NutrientType.SODIUM, NutrientType.CALCIUM)
        )

        assertEquals(2, items.size)
        items.forEach { item ->
            assertTrue("Zero calorie food should have low nutrients", item.value >= 0f)
        }
    }

    // ============================================================
    // 9. 边界情况测试
    // ============================================================

    @Test
    fun `test POI with very long name`() {
        val longName = "A".repeat(200)
        val poi = ExercisePOI("1", longName, POIType.PARK, 39.9, 116.4)
        assertEquals(longName, poi.name)
    }

    @Test
    fun `test POI with empty name`() {
        val poi = ExercisePOI("1", "", POIType.PARK, 39.9, 116.4)
        assertEquals("", poi.name)
    }

    @Test
    fun `test NutrientType labels are Chinese`() {
        // 验证所有标签都包含中文字符
        val chineseRegex = Regex("[\\u4e00-\\u9fa5]")
        NutrientType.values().forEach { type ->
            assertTrue(
                "Label '${type.label}' for $type should contain Chinese characters",
                chineseRegex.containsMatchIn(type.label)
            )
        }
    }

    @Test
    fun `test NutrientDisplayItem with high values`() {
        val item = NutrientDisplayItem(
            type = NutrientType.SODIUM,
            value = 5000.0f,
            dailyRecommended = 2000.0f
        )
        assertTrue("Value can exceed daily recommended", item.value > item.dailyRecommended)
    }

    @Test
    fun `test multiple POI generation calls are consistent`() {
        val pois1 = generateExercisePOIs(39.9042, 116.4074)
        val pois2 = generateExercisePOIs(39.9042, 116.4074)
        
        // 相同坐标应生成相同数量的POI
        assertEquals(pois1.size, pois2.size)
    }

    @Test
    fun `test all nutrient types have valid daily recommended`() {
        val items = estimateExtendedNutrients(
            foodName = "测试食物",
            calories = 200,
            protein = 15.0f,
            fat = 10.0f,
            carbs = 20.0f,
            selectedTypes = getExtendedNutrientTypes()
        )

        items.forEach { item ->
            assertTrue(
                "Daily recommended for ${item.type} should be positive",
                item.dailyRecommended > 0f
            )
        }
    }
}
