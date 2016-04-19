package com.glacier.camera;

import android.app.Application;

import com.umeng.analytics.MobclickAgent;

/**
 * Created by shy on 2016/4/19.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MobclickAgent.setDebugMode(true);
    }
}
