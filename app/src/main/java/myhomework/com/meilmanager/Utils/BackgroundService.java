package myhomework.com.meilmanager.Utils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;

import myhomework.com.meilmanager.Crypto;
import myhomework.com.meilmanager.ServerAPI;

/**
 * Created by root on 25.07.2016.
 */
public class BackgroundService extends Service {
    private ServerAPI serverAPI;
    private SharedPreferences preferences;

    public BackgroundService(SharedPreferences prefs) {
        preferences = prefs;
    }

    @Override
    public void onCreate() {
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
