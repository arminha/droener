package ch.scs.droener;

import android.app.Application;
import android.util.Log;

import com.parrot.arsdk.ARSDK;

public class DroenerApp extends Application {
    private static final String TAG = "DroenerApp";

    @Override
    public void onCreate() {
        super.onCreate();
        ARSDK.loadSDKLibs();
        Log.d(TAG, "Loaded native libraries");
    }
}
