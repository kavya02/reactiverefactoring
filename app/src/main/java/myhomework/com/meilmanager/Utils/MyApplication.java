package myhomework.com.meilmanager.Utils;

import android.app.Application;

import myhomework.com.meilmanager.ServerAPI;

/**
 * Created by root on 25.07.2016.
 */
public class MyApplication extends Application {
    ServerAPI serverAPI;

    @Override
    public void onCreate() {
        super.onCreate();

        serverAPI = ServerAPI.getInstance(this);
    }

}