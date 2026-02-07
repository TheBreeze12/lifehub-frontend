package com.example.lifehub

import android.app.Application
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer

/**
 * LifeHub Application类
 *
 * 在Application层完成高德地图SDK的隐私合规初始化。
 * 从2021年底开始，高德地图SDK要求在初始化之前必须调用隐私合规接口，
 * 否则在部分厂商手机（如vivo、华为）上会导致SDK初始化失败并崩溃。
 *
 * 必须在 AndroidManifest.xml 中注册：
 * <application android:name=".LifeHubApplication" ...>
 */
class LifeHubApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 高德地图隐私合规设置（必须在任何地图/定位API调用之前）
        // 参数说明：
        //   updatePrivacyShow(context, isContains, isShow)
        //     isContains: 隐私政策中是否包含高德SDK相关说明
        //     isShow: 是否已向用户展示过隐私政策
        //   updatePrivacyAgree(context, isAgree)
        //     isAgree: 用户是否同意隐私政策
        initAmapPrivacy()
    }

    /**
     * 初始化高德地图隐私合规
     *
     * 注意：生产环境中应在用户明确同意隐私政策后再调用agree接口。
     * 当前为开发/比赛环境，默认同意以确保SDK正常工作。
     */
    private fun initAmapPrivacy() {
        try {
            // 定位SDK隐私合规
            AMapLocationClient.updatePrivacyShow(this, true, true)
            AMapLocationClient.updatePrivacyAgree(this, true)

            // 地图SDK隐私合规
            MapsInitializer.updatePrivacyShow(this, true, true)
            MapsInitializer.updatePrivacyAgree(this, true)
        } catch (e: Exception) {
            // 隐私合规API调用失败不应阻塞应用启动
            // 在部分环境下（如单元测试）可能找不到native库
            e.printStackTrace()
        }
    }
}
