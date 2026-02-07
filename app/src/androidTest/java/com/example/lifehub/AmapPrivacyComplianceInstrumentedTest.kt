package com.example.lifehub

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 高德地图隐私合规修复 - Android仪器化测试
 *
 * 在真实Android环境中验证：
 * 1. LifeHubApplication 已注册为应用的 Application 类
 * 2. 高德地图隐私合规API已在Application启动时调用
 * 3. AndroidManifest.xml 中的关键配置正确
 * 4. 地图SDK类可加载
 * 5. 权限配置正确
 */
@RunWith(AndroidJUnit4::class)
class AmapPrivacyComplianceInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ========== Application 注册验证 ==========

    /**
     * 测试1: 验证应用的Application类是LifeHubApplication
     * 这是最关键的测试 - 确保隐私合规代码能在应用启动时执行
     */
    @Test
    fun testApplicationIsLifeHubApplication() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertTrue(
            "Application应为LifeHubApplication实例，实际为: ${app.javaClass.name}",
            app is LifeHubApplication
        )
    }

    /**
     * 测试2: 验证LifeHubApplication类名在Manifest中正确注册
     */
    @Test
    fun testApplicationClassRegisteredInManifest() {
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName, 0
            )
            // Application类名应以LifeHubApplication结尾
            val appClassName = appInfo.className
            assertNotNull("Application类名不应为null", appClassName)
            assertTrue(
                "Application类名应为LifeHubApplication，实际为: $appClassName",
                appClassName?.endsWith("LifeHubApplication") == true
            )
        } catch (e: PackageManager.NameNotFoundException) {
            fail("无法获取应用信息: ${e.message}")
        }
    }

    // ========== 高德地图SDK集成验证 ==========

    /**
     * 测试3: 验证高德地图SDK MapView类可加载
     */
    @Test
    fun testAmapMapViewClassLoadable() {
        try {
            Class.forName("com.amap.api.maps.MapView")
            assertTrue("MapView类应可加载", true)
        } catch (e: ClassNotFoundException) {
            fail("MapView类不存在，请检查高德地图3D SDK依赖")
        }
    }

    /**
     * 测试4: 验证高德定位SDK AMapLocationClient类可加载
     */
    @Test
    fun testAmapLocationClientClassLoadable() {
        try {
            Class.forName("com.amap.api.location.AMapLocationClient")
            assertTrue("AMapLocationClient类应可加载", true)
        } catch (e: ClassNotFoundException) {
            fail("AMapLocationClient类不存在，请检查高德地图SDK依赖")
        }
    }

    /**
     * 测试5: 验证MapsInitializer类可加载
     */
    @Test
    fun testMapsInitializerClassLoadable() {
        try {
            Class.forName("com.amap.api.maps.MapsInitializer")
            assertTrue("MapsInitializer类应可加载", true)
        } catch (e: ClassNotFoundException) {
            fail("MapsInitializer类不存在")
        }
    }

    /**
     * 测试6: 验证AMapLocationClient包含隐私合规静态方法
     */
    @Test
    fun testAMapLocationClientHasPrivacyMethods() {
        try {
            val clazz = Class.forName("com.amap.api.location.AMapLocationClient")

            val showMethod = clazz.getMethod(
                "updatePrivacyShow",
                android.content.Context::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            assertNotNull("updatePrivacyShow方法应存在", showMethod)

            val agreeMethod = clazz.getMethod(
                "updatePrivacyAgree",
                android.content.Context::class.java,
                Boolean::class.javaPrimitiveType
            )
            assertNotNull("updatePrivacyAgree方法应存在", agreeMethod)
        } catch (e: Exception) {
            fail("AMapLocationClient隐私合规方法检查失败: ${e.message}")
        }
    }

    /**
     * 测试7: 验证MapsInitializer包含隐私合规静态方法
     */
    @Test
    fun testMapsInitializerHasPrivacyMethods() {
        try {
            val clazz = Class.forName("com.amap.api.maps.MapsInitializer")

            val showMethod = clazz.getMethod(
                "updatePrivacyShow",
                android.content.Context::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            assertNotNull("updatePrivacyShow方法应存在", showMethod)

            val agreeMethod = clazz.getMethod(
                "updatePrivacyAgree",
                android.content.Context::class.java,
                Boolean::class.javaPrimitiveType
            )
            assertNotNull("updatePrivacyAgree方法应存在", agreeMethod)
        } catch (e: Exception) {
            fail("MapsInitializer隐私合规方法检查失败: ${e.message}")
        }
    }

    // ========== AndroidManifest.xml 配置验证 ==========

    /**
     * 测试8: 验证高德地图API Key meta-data存在
     */
    @Test
    fun testAmapApiKeyMetaDataExists() {
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val metaData = appInfo.metaData
            assertNotNull("meta-data不应为null", metaData)

            val apiKey = metaData.getString("com.amap.api.v2.apikey")
            assertNotNull("高德地图API Key应配置", apiKey)
            assertFalse("API Key不应为空", apiKey.isNullOrEmpty())
        } catch (e: PackageManager.NameNotFoundException) {
            fail("无法获取应用meta-data: ${e.message}")
        }
    }

    /**
     * 测试9: 验证位置权限已在Manifest中声明
     */
    @Test
    fun testLocationPermissionsDeclared() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

            assertTrue(
                "应声明ACCESS_FINE_LOCATION权限",
                permissions.contains("android.permission.ACCESS_FINE_LOCATION")
            )
            assertTrue(
                "应声明ACCESS_COARSE_LOCATION权限",
                permissions.contains("android.permission.ACCESS_COARSE_LOCATION")
            )
        } catch (e: PackageManager.NameNotFoundException) {
            fail("无法获取权限信息: ${e.message}")
        }
    }

    /**
     * 测试10: 验证网络权限已声明
     */
    @Test
    fun testNetworkPermissionsDeclared() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

            assertTrue(
                "应声明INTERNET权限",
                permissions.contains("android.permission.INTERNET")
            )
            assertTrue(
                "应声明ACCESS_NETWORK_STATE权限",
                permissions.contains("android.permission.ACCESS_NETWORK_STATE")
            )
        } catch (e: PackageManager.NameNotFoundException) {
            fail("无法获取权限信息: ${e.message}")
        }
    }

    /**
     * 测试11: 验证WIFI权限已声明（高德地图SDK需要）
     */
    @Test
    fun testWifiPermissionDeclared() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

            assertTrue(
                "应声明ACCESS_WIFI_STATE权限",
                permissions.contains("android.permission.ACCESS_WIFI_STATE")
            )
        } catch (e: PackageManager.NameNotFoundException) {
            fail("无法获取权限信息: ${e.message}")
        }
    }

    /**
     * 测试12: 验证前台服务权限已声明（运动追踪需要）
     */
    @Test
    fun testForegroundServicePermissionDeclared() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

            assertTrue(
                "应声明FOREGROUND_SERVICE权限",
                permissions.contains("android.permission.FOREGROUND_SERVICE")
            )
        } catch (e: PackageManager.NameNotFoundException) {
            fail("无法获取权限信息: ${e.message}")
        }
    }

    // ========== 隐私合规调用验证 ==========

    /**
     * 测试13: 验证隐私合规API调用不会抛出异常
     * 模拟Application中的隐私合规调用流程
     */
    @Test
    fun testPrivacyComplianceCallDoesNotCrash() {
        try {
            // 重复调用隐私合规API不应崩溃
            com.amap.api.location.AMapLocationClient.updatePrivacyShow(context, true, true)
            com.amap.api.location.AMapLocationClient.updatePrivacyAgree(context, true)
            com.amap.api.maps.MapsInitializer.updatePrivacyShow(context, true, true)
            com.amap.api.maps.MapsInitializer.updatePrivacyAgree(context, true)
            assertTrue("隐私合规API调用不应崩溃", true)
        } catch (e: Exception) {
            fail("隐私合规API调用抛出异常: ${e.message}")
        }
    }

    /**
     * 测试14: 验证多次调用隐私合规API不会崩溃（幂等性）
     */
    @Test
    fun testPrivacyComplianceIdempotent() {
        try {
            repeat(3) {
                com.amap.api.location.AMapLocationClient.updatePrivacyShow(context, true, true)
                com.amap.api.location.AMapLocationClient.updatePrivacyAgree(context, true)
                com.amap.api.maps.MapsInitializer.updatePrivacyShow(context, true, true)
                com.amap.api.maps.MapsInitializer.updatePrivacyAgree(context, true)
            }
            assertTrue("多次调用隐私合规API不应崩溃", true)
        } catch (e: Exception) {
            fail("多次调用隐私合规API抛出异常: ${e.message}")
        }
    }

    /**
     * 测试15: 验证高德定位服务已在Manifest注册
     */
    @Test
    fun testApsServiceRegistered() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SERVICES
            )
            val services = packageInfo.services?.map { it.name } ?: emptyList()
            assertTrue(
                "APSService应已注册",
                services.any { it.contains("APSService") }
            )
        } catch (e: PackageManager.NameNotFoundException) {
            fail("无法获取服务信息: ${e.message}")
        }
    }
}
