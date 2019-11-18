package org.linwg.retrofitdynamicbaseurl;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.linwg.http.ApiService;
import org.linwg.http.HttpUtil;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    int index = -1;

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView tvMethodDesc = findViewById(R.id.tvMethodDesc);
        final TextView tvMethodResult = findViewById(R.id.tvMethodResult);
        final EditText etHost = findViewById(R.id.etHost);
        etHost.setText(Constants.BAIDU_HOST_VALUE);
        findViewById(R.id.tvDoRequest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Call<ResponseBody> call = null;
                if (index == -1) {
                    toast("Please select request.");
                } else if (index == 0) {
                    call = HttpUtil.getUtil().getService(ApiService.class).methodA();
                } else if (index == 1) {
                    call = HttpUtil.getUtil().getService(ApiService.class).methodB();
                } else if (index == 2) {
                    call = HttpUtil.getUtil().getService(ApiService.class).methodC();
                } else if (index == 3) {
                    call = HttpUtil.getUtil().getService(ApiService.class).methodD();
                }
                String s = etHost.getText().toString();
                if (!s.startsWith("http://") && !s.startsWith("https://")) {
                    toast("error host");
                    return;
                }
                App.hostMap.put(Constants.SINA_HOST_KEY, s);
                if (call != null) {
                    doCall(call, tvMethodResult);
                }
            }
        });
        findViewById(R.id.tvRequestA).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpannableStringBuilder sb = new SpannableStringBuilder("image resource");
                Drawable drawable = getResources().getDrawable(R.mipmap.a);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                sb.setSpan(new ImageSpan(drawable), sb.length() - 14, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvMethodDesc.setText(sb);
                index = 0;
            }
        });

        findViewById(R.id.tvRequestB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpannableStringBuilder sb = new SpannableStringBuilder("image resource");
                Drawable drawable = getResources().getDrawable(R.mipmap.b);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                sb.setSpan(new ImageSpan(drawable), sb.length() - 14, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvMethodDesc.setText(sb);
                index = 1;
            }
        });

        findViewById(R.id.tvRequestC).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpannableStringBuilder sb = new SpannableStringBuilder("image resource");
                Drawable drawable = getResources().getDrawable(R.mipmap.c);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                sb.setSpan(new ImageSpan(drawable), sb.length() - 14, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvMethodDesc.setText(sb);
                index = 2;
            }
        });
        findViewById(R.id.tvRequestD).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpannableStringBuilder sb = new SpannableStringBuilder("image resource");
                Drawable drawable = getResources().getDrawable(R.mipmap.d);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                sb.setSpan(new ImageSpan(drawable), sb.length() - 14, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvMethodDesc.setText(sb);
                index = 3;
            }
        });
        findViewById(R.id.tvRequestA).performClick();
    }

    private void doCall(Call<ResponseBody> call, final TextView tvMethodResult) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                tvMethodResult.setText("Request result :\n");
                tvMethodResult.append("Request original url :" + call.request().url().toString() + "\n");
                tvMethodResult.append("Request original headers :" + call.request().headers() + "\n");
                tvMethodResult.append("Response :" + response.toString() + "\n");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                tvMethodResult.setText("Request result:\n");
                tvMethodResult.append("Request original url:" + call.request().url().toString() + "\n");
                tvMethodResult.append("Error:" + t.getMessage());
            }
        });
    }
}
