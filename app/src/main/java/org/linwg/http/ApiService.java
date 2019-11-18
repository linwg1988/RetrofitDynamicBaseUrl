package org.linwg.http;

import org.linwg.retrofitdynamicbaseurl.Constants;
import org.linwg.retrofitdynamicbaseurl.DynamicIgnore;
import org.linwg.retrofitdynamicbaseurl.ServiceHeader;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

@ServiceHeader("ConfigHost:" + Constants.SINA_HOST_KEY)
public interface ApiService {

    @GET("/users")
    Call<ResponseBody> methodA();

    @Headers("Test:testHeader")
    @GET("/users")
    Call<ResponseBody> methodB();

    @Headers({"Test1:testHeader1", "Test2:testHeader2"})
    @GET("/users")
    Call<ResponseBody> methodC();

    @DynamicIgnore
    @GET("/users")
    Call<ResponseBody> methodD();
}
