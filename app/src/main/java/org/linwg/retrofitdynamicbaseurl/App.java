package org.linwg.retrofitdynamicbaseurl;

import android.app.Application;

import java.util.HashMap;
import java.util.Map;

public class App extends Application {

    public static Map<String, String> hostMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        hostMap.put(Constants.BAIDU_HOST_KEY, Constants.BAIDU_HOST_VALUE);
        hostMap.put(Constants.SINA_HOST_KEY, Constants.SINA_HOST_VALUE);
    }
}
