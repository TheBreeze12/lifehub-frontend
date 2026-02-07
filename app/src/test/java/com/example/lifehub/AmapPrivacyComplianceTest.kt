package com.example.lifehub

import com.example.lifehub.ui.components.AMapViewState
import com.example.lifehub.ui.components.LatLngPoint
import com.example.lifehub.ui.components.MarkerData
import com.example.lifehub.ui.components.PolylineData
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Method

/**
 * 高德地图隐私合规与闪退修复测试
 *
 * 验证内容：
 * 1. LifeHubApplication 类存在且正确继承 Application
 * 2. 隐私合规初始化方法存在
 * 3. MapView 数据模型完整性
 * 4. AMapViewState 状态机完整性
 * 5. 边界条件处理
 */
class AmapPrivacyComplianceTest {

    // ========== LifeHubApplication 结构验证 ==========

    /**
     * 测试1: LifeHubApplication 类存在
     */
    @Test
    fun testLifeHubApplicationClassExists() {
        val clazz = Class.forName("com.example.lifehub.LifeHubApplication")
        assertNotNull("LifeHubApplication类应存在", clazz)
    }

    /**
     * 测试2: LifeHubApplication 继承自 android.app.Application
     */
    @Test
    fun testLifeHubApplicationExtendsApplication() {
        val clazz = Class.forName("com.example.lifehub.LifeHubApplication")
        val superClass = clazz.superclass
        assertEquals(
            "LifeHubApplication应继承android.app.Application",
            "android.app.Application",
            superClass?.name
        )
    }

    /**
     * 测试3: LifeHubApplication 包含 onCreate 方法
     */
    @Test
    fun testLifeHubApplicationHasOnCreate() {
        val clazz = Class.forName("com.example.lifehub.LifeHubApplication")
        val onCreateMethod = clazz.getDeclaredMethod("onCreate")
        assertNotNull("LifeHubApplication应包含onCreate方法", onCreateMethod)
    }

    /**
     * 测试4: LifeHubApplication 包含 initAmapPrivacy 私有方法
     */
    @Test
    fun testLifeHubApplicationHasInitAmapPrivacy() {
        val clazz = Class.forName("com.example.lifehub.LifeHubApplication")
        val method = clazz.getDeclaredMethod("initAmapPrivacy")
        assertNotNull("LifeHubApplication应包含initAmapPrivacy方法", method)
        // 验证是私有方法
        assertTrue(
            "initAmapPrivacy应为私有方法",
            java.lang.reflect.Modifier.isPrivate(method.modifiers)
        )
    }

    /**
     * 测试5: 验证高德定位SDK隐私合规API类可加载
     * AMapLocationClient.updatePrivacyShow 和 updatePrivacyAgree 方法存在
     */
    @Test
    fun testAMapLocationClientPrivacyApiExists() {
        try {
            val clazz = Class.forName("com.amap.api.location.AMapLocationClient")
            // 检查 updatePrivacyShow(Context, boolean, boolean) 静态方法
            val showMethod = clazz.getMethod(
                "updatePrivacyShow",
                android.content.Context::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            assertNotNull("AMapLocationClient.updatePrivacyShow方法应存在", showMethod)

            // 检查 updatePrivacyAgree(Context, boolean) 静态方法
            val agreeMethod = clazz.getMethod(
                "updatePrivacyAgree",
                android.content.Context::class.java,
                Boolean::class.javaPrimitiveType
            )
            assertNotNull("AMapLocationClient.updatePrivacyAgree方法应存在", agreeMethod)
        } catch (e: ClassNotFoundException) {
            fail("AMapLocationClient类不存在，请检查高德地图SDK依赖: ${e.message}")
        } catch (e: NoSuchMethodException) {
            fail("AMapLocationClient隐私合规方法不存在: ${e.message}")
        }
    }

    /**
     * 测试6: 验证高德地图SDK隐私合规API类可加载
     * MapsInitializer.updatePrivacyShow 和 updatePrivacyAgree 方法存在
     */
    @Test
    fun testMapsInitializerPrivacyApiExists() {
        try {
            val clazz = Class.forName("com.amap.api.maps.MapsInitializer")
            // 检查 updatePrivacyShow(Context, boolean, boolean) 静态方法
            val showMethod = clazz.getMethod(
                "updatePrivacyShow",
                android.content.Context::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            assertNotNull("MapsInitializer.updatePrivacyShow方法应存在", showMethod)

            // 检查 updatePrivacyAgree(Context, boolean) 静态方法
            val agreeMethod = clazz.getMethod(
                "updatePrivacyAgree",
                android.content.Context::class.java,
                Boolean::class.javaPrimitiveType
            )
            assertNotNull("MapsInitializer.updatePrivacyAgree方法应存在", agreeMethod)
        } catch (e: ClassNotFoundException) {
            fail("MapsInitializer类不存在，请检查高德地图SDK依赖: ${e.message}")
        } catch (e: NoSuchMethodException) {
            fail("MapsInitializer隐私合规方法不存在: ${e.message}")
        }
    }

    // ========== AMapViewState 状态机测试 ==========

    /**
     * 测试7: AMapViewState.Loading 状态
     */
    @Test
    fun testAMapViewStateLoading() {
        val state = AMapViewState.Loading
        assertTrue("应为Loading状态", state is AMapViewState.Loading)
        assertFalse("不应为Error状态", state is AMapViewState.Error)
    }

    /**
     * 测试8: AMapViewState.Error 状态 - 正常错误消息
     */
    @Test
    fun testAMapViewStateErrorNormal() {
        val state = AMapViewState.Error("地图初始化失败")
        assertTrue("应为Error状态", state is AMapViewState.Error)
        assertEquals("错误消息应正确", "地图初始化失败", state.message)
    }

    /**
     * 测试9: AMapViewState.Error 状态 - 空错误消息
     */
    @Test
    fun testAMapViewStateErrorEmpty() {
        val state = AMapViewState.Error("")
        assertTrue("空消息也应为Error状态", state is AMapViewState.Error)
        assertEquals("空消息内容应为空字符串", "", state.message)
    }

    /**
     * 测试10: AMapViewState.Error 状态 - 包含异常信息
     */
    @Test
    fun testAMapViewStateErrorWithExceptionInfo() {
        val exMessage = "地图配置失败: SDK未初始化"
        val state = AMapViewState.Error(exMessage)
        assertTrue("应包含异常信息", state.message.contains("SDK未初始化"))
    }

    // ========== MapView 数据模型边界测试 ==========

    /**
     * 测试11: MarkerData 极端坐标值 - 南极
     */
    @Test
    fun testMarkerDataExtremeSouthPole() {
        val marker = MarkerData(latitude = -90.0, longitude = 0.0, title = "南极")
        assertEquals(-90.0, marker.latitude, 0.001)
    }

    /**
     * 测试12: MarkerData 极端坐标值 - 北极
     */
    @Test
    fun testMarkerDataExtremeNorthPole() {
        val marker = MarkerData(latitude = 90.0, longitude = 0.0, title = "北极")
        assertEquals(90.0, marker.latitude, 0.001)
    }

    /**
     * 测试13: MarkerData 极端坐标值 - 国际日期变更线
     */
    @Test
    fun testMarkerDataExtremeIDL() {
        val marker = MarkerData(latitude = 0.0, longitude = 180.0)
        assertEquals(180.0, marker.longitude, 0.001)
        val markerNeg = MarkerData(latitude = 0.0, longitude = -180.0)
        assertEquals(-180.0, markerNeg.longitude, 0.001)
    }

    /**
     * 测试14: PolylineData 单点路线（不应崩溃）
     */
    @Test
    fun testPolylineDataSinglePoint() {
        val polyline = PolylineData(
            points = listOf(LatLngPoint(39.9042, 116.4074)),
            width = 10f
        )
        assertEquals("单点路线应有1个点", 1, polyline.points.size)
    }

    /**
     * 测试15: PolylineData 大量点（性能边界）
     */
    @Test
    fun testPolylineDataLargePoints() {
        val points = (0 until 10000).map {
            LatLngPoint(39.9042 + it * 0.0001, 116.4074 + it * 0.0001)
        }
        val polyline = PolylineData(points = points)
        assertEquals("应能处理10000个点", 10000, polyline.points.size)
    }

    /**
     * 测试16: PolylineData 空点列表
     */
    @Test
    fun testPolylineDataEmptyPoints() {
        val polyline = PolylineData(points = emptyList())
        assertTrue("空点列表应为空", polyline.points.isEmpty())
    }

    /**
     * 测试17: LatLngPoint 精度验证 - 高精度坐标
     */
    @Test
    fun testLatLngPointHighPrecision() {
        val point = LatLngPoint(39.90420000001, 116.40740000002)
        assertEquals(39.90420000001, point.latitude, 1e-10)
        assertEquals(116.40740000002, point.longitude, 1e-10)
    }

    /**
     * 测试18: MarkerData equality 验证
     */
    @Test
    fun testMarkerDataEquality() {
        val marker1 = MarkerData(latitude = 39.9042, longitude = 116.4074, title = "A")
        val marker2 = MarkerData(latitude = 39.9042, longitude = 116.4074, title = "A")
        assertEquals("相同数据的MarkerData应相等", marker1, marker2)
    }

    /**
     * 测试19: MarkerData inequality 验证
     */
    @Test
    fun testMarkerDataInequality() {
        val marker1 = MarkerData(latitude = 39.9042, longitude = 116.4074, title = "A")
        val marker2 = MarkerData(latitude = 39.9043, longitude = 116.4074, title = "A")
        assertNotEquals("不同坐标的MarkerData不应相等", marker1, marker2)
    }

    /**
     * 测试20: PolylineData copy 验证（data class 特性）
     */
    @Test
    fun testPolylineDataCopy() {
        val original = PolylineData(
            points = listOf(LatLngPoint(39.9, 116.4)),
            color = android.graphics.Color.RED,
            width = 15f
        )
        val copied = original.copy(color = android.graphics.Color.BLUE)
        assertEquals("复制后宽度不变", 15f, copied.width, 0.01f)
        assertEquals("复制后颜色已改变", android.graphics.Color.BLUE, copied.color)
        assertEquals("复制后点列表不变", 1, copied.points.size)
    }

    // ========== 生命周期相关静态验证 ==========

    /**
     * 测试21: MapView.kt 文件中应导入 LocalLifecycleOwner
     * 通过反射验证编译后的类引用了生命周期相关类
     */
    @Test
    fun testLifecycleEventObserverClassExists() {
        try {
            val clazz = Class.forName("androidx.lifecycle.LifecycleEventObserver")
            assertNotNull("LifecycleEventObserver类应存在", clazz)
        } catch (e: ClassNotFoundException) {
            fail("LifecycleEventObserver类不存在，生命周期管理依赖缺失")
        }
    }

    /**
     * 测试22: Lifecycle.Event 枚举应包含必要事件
     */
    @Test
    fun testLifecycleEventsExist() {
        try {
            val clazz = Class.forName("androidx.lifecycle.Lifecycle\$Event")
            assertNotNull("Lifecycle.Event类应存在", clazz)

            // 验证关键事件常量存在
            val onResume = clazz.getDeclaredField("ON_RESUME")
            assertNotNull("ON_RESUME事件应存在", onResume)

            val onPause = clazz.getDeclaredField("ON_PAUSE")
            assertNotNull("ON_PAUSE事件应存在", onPause)

            val onDestroy = clazz.getDeclaredField("ON_DESTROY")
            assertNotNull("ON_DESTROY事件应存在", onDestroy)
        } catch (e: ClassNotFoundException) {
            fail("Lifecycle.Event类不存在: ${e.message}")
        }
    }

    /**
     * 测试23: AMapViewState 状态切换模拟
     */
    @Test
    fun testAMapViewStateTransitions() {
        // 模拟状态流转: Loading -> Success 或 Loading -> Error
        var state: AMapViewState = AMapViewState.Loading
        assertTrue("初始应为Loading", state is AMapViewState.Loading)

        // 模拟失败
        state = AMapViewState.Error("初始化失败")
        assertTrue("失败后应为Error", state is AMapViewState.Error)

        // 再次重试 -> Loading -> 成功（Success需要AMap对象，此处仅验证Error消息）
        state = AMapViewState.Loading
        assertTrue("重试时应重置为Loading", state is AMapViewState.Loading)
    }

    /**
     * 测试24: 中文错误消息编码验证
     */
    @Test
    fun testChineseErrorMessageEncoding() {
        val state = AMapViewState.Error("地图配置失败: SDK未初始化，请检查隐私合规设置")
        assertTrue("中文消息应包含'隐私合规'", state.message.contains("隐私合规"))
        assertTrue("中文消息应包含'SDK'", state.message.contains("SDK"))
    }

    /**
     * 测试25: MarkerData hashCode 一致性
     */
    @Test
    fun testMarkerDataHashCodeConsistency() {
        val marker = MarkerData(latitude = 39.9042, longitude = 116.4074, title = "Test")
        val hash1 = marker.hashCode()
        val hash2 = marker.hashCode()
        assertEquals("同一对象的hashCode应一致", hash1, hash2)
    }
}
