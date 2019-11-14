package org.linwg.retrofitdynamicbaseurl;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.linwg.http.ApiService;
import org.linwg.http.HttpUtil;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tvRequestB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpUtil.getUtil().getService(ApiService.class).methodA(1,10).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(String s) {
                        Log.i("HttpUtil", s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("HttpUtil", e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
            }
        });
    }
}
