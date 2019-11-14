package org.linwg.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.linwg.retrofitdynamicbaseurl.Constants;

import java.lang.reflect.Modifier;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by wengui on 2016/8/17.
 */
public class HttpUtil {
    private Retrofit retrofit;

    private HttpUtil() {
        OkHttpClient okHttpClient = OKHttpUtil.getUtil().getOkHttpClient();
        Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC).
                setDateFormat("yyyy-MM-dd HH:mm:ss").
                setExclusionStrategies(new SpecificClassExclusionStrategy(null, null)).create();
        retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(Constants.BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    private static class Holder {
        static HttpUtil util = new HttpUtil();
    }

    public static HttpUtil getUtil() {
        return Holder.util;
    }

    public <T> T getService(Class<T> clazz) {
        return retrofit.create(clazz);
    }

}
