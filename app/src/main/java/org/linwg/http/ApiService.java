package org.linwg.http;

import org.linwg.retrofitdynamicbaseurl.Constants;
import org.linwg.retrofitdynamicbaseurl.ServiceHeader;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

@ServiceHeader("ConfigHost:" + Constants.BAIDU_HOST_KEY)
public interface ApiService {

    @GET("/users")
    Observable<String> methodA(@Query("since") int lastIdQueried, @Query("per_page") int perPage);
}
