package org.linwg.http;

import android.util.Log;

import org.linwg.retrofitdynamicbaseurl.App;
import org.linwg.retrofitdynamicbaseurl.Constants;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class RetrofitBaseUrlInterceptor implements Interceptor {

    public RetrofitBaseUrlInterceptor() {
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String configHost = request.header("ConfigHost");
        if (configHost != null && configHost.length() > 0) {
            String host = App.hostMap.get(configHost);
            if (host != null && host.startsWith("http")) {
                String url = request.url().url().toString();
                url = url.replace(Constants.BASE_URL, host);
                request = request.newBuilder().url(url).removeHeader("ConfigHost").build();
            }
        }
        Log.i("UrlInterceptor", "url = " + request.url());
        return chain.proceed(request);
    }
}
