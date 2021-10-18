package com.shencoder.javacv_facedetectdemo;

import android.content.Context;

import androidx.multidex.MultiDexApplication;

import xcrash.XCrash;

/**
 * @author ShenBen
 * @date 2021/7/14 10:19
 * @email 714081644@qq.com
 */
public class App extends MultiDexApplication {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        XCrash.InitParameters parameters = new XCrash.InitParameters();
        parameters.setAppVersion(BuildConfig.VERSION_NAME);
        parameters.setLogDir(getExternalFilesDir("crash_log").getAbsolutePath());
        parameters.setJavaRethrow(false);
        parameters.setAnrRethrow(false);
        parameters.setNativeRethrow(false);
        XCrash.init(this, parameters);
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }
}
