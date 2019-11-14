package org.linwg.http;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Created by wengui on 2016/10/11.
 */

public final class OKHttpUtil {
    private static boolean hasInit;

    private static class Holder {
        static OKHttpUtil util = new OKHttpUtil();
    }

    private OkHttpClient okHttpClient;

    public static OKHttpUtil getUtil() {
        if (Holder.util == null) {
            throw new RuntimeException("OKHttpUtil has not init!");
        }
        return Holder.util;
    }

    private OKHttpUtil() {
        RetrofitBaseUrlInterceptor interceptor = new RetrofitBaseUrlInterceptor();
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .retryOnConnectionFailure(true);
        okHttpClient = builder.connectTimeout(60, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).writeTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public static class ResponseProgressInterceptor implements Interceptor {

        private final ProgressResponseListener progressListener;

        public ResponseProgressInterceptor(ProgressResponseListener l) {
            this.progressListener = l;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            //拦截
            Response originalResponse = chain.proceed(chain.request());
            //包装响应体并返回
            return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
        }
    }

    public static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressResponseListener progressListener;
        private BufferedSource bufferedSource;


        ProgressResponseBody(ResponseBody responseBody, ProgressResponseListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }


        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }


        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }


        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytes = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytes += bytesRead != -1 ? bytesRead : 0;
                    if (null != progressListener) {
                        progressListener.onProgressChanged(totalBytes, contentLength(), (float) totalBytes / contentLength());
                    }
                    return bytesRead;
                }
            };
        }
    }

    public interface ProgressResponseListener {
        /**
         * @param numBytes
         * @param totalBytes
         * @param percent
         */
        void onProgressChanged(long numBytes, long totalBytes, float percent);
    }

    public static abstract class AbstractProgressResponseListener implements ProgressResponseListener {
        boolean started;
        long lastRefreshTime = 0L;
        long lastBytesWritten = 0L;
        int minTime = 250;//最小回调时间100ms，避免频繁回调

        /**
         * 进度发生了改变，如果numBytes，totalBytes，percent都为-1，则表示总大小获取不到
         *
         * @param numBytes   已读/写大小
         * @param totalBytes 总大小
         * @param percent    百分比
         */
        @Override
        public final void onProgressChanged(long numBytes, long totalBytes, float percent) {
            if (!started) {
                onProgressStart(totalBytes);
                started = true;
            }
            if (numBytes == -1 && totalBytes == -1 && percent == -1) {
                onProgressChanged(-1, -1, -1, -1);
                return;
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRefreshTime >= minTime || numBytes == totalBytes || percent >= 1F) {
                long intervalTime = (currentTime - lastRefreshTime);
                if (intervalTime == 0) {
                    intervalTime += 1;
                }
                long updateBytes = numBytes - lastBytesWritten;
                final long networkSpeed = updateBytes / intervalTime;
                onProgressChanged(numBytes, totalBytes, percent, networkSpeed);
                lastRefreshTime = System.currentTimeMillis();
                lastBytesWritten = numBytes;
            }
            if (numBytes == totalBytes || percent >= 1F) {
                onProgressFinish();
            }
        }

        /**
         * 进度发生了改变，如果numBytes，totalBytes，loanAmountRate，speed都为-1，则表示总大小获取不到
         *
         * @param numBytes   已读/写大小
         * @param totalBytes 总大小
         * @param percent    百分比
         * @param speed      速度 bytes/ms
         */
        public abstract void onProgressChanged(long numBytes, long totalBytes, float percent, float speed);

        /**
         * 进度开始
         *
         * @param totalBytes 总大小
         */
        public void onProgressStart(long totalBytes) {

        }

        /**
         * 进度结束
         */
        public void onProgressFinish() {

        }
    }

    /**
     * 请求体回调实现类，用于UI层回调
     * User:lizhangqu(513163535@qq.com)
     * Date:2015-09-02
     * Time: 22:34
     */
    public static abstract class UIProgressResponseListener extends AbstractProgressResponseListener {
        private Handler mHandler;

        private static final int WHAT_START = 0x01;
        private static final int WHAT_UPDATE = 0x02;
        private static final int WHAT_FINISH = 0x03;
        private static final String CURRENT_BYTES = "numBytes";
        private static final String TOTAL_BYTES = "totalBytes";
        private static final String PERCENT = "loanAmountRate";
        private static final String SPEED = "speed";

        public UIProgressResponseListener() {

        }

        private void ensureHandler() {
            if (mHandler != null) {
                return;
            }
            synchronized (UIProgressResponseListener.class) {
                if (mHandler == null) {
                    mHandler = new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            if (msg == null) {
                                return;
                            }
                            switch (msg.what) {
                                case WHAT_START:
                                    Bundle startData = msg.getData();
                                    if (startData == null) {
                                        return;
                                    }
                                    onUIProgressStart(startData.getLong(TOTAL_BYTES));
                                    break;
                                case WHAT_UPDATE:
                                    Bundle updateData = msg.getData();
                                    if (updateData == null) {
                                        return;
                                    }
                                    long numBytes = updateData.getLong(CURRENT_BYTES);
                                    long totalBytes = updateData.getLong(TOTAL_BYTES);
                                    float percent = updateData.getFloat(PERCENT);
                                    float speed = updateData.getFloat(SPEED);
                                    onUIProgressChanged(numBytes, totalBytes, percent, speed);
                                    break;
                                case WHAT_FINISH:
                                    onUIProgressFinish();
                                    break;
                                default:
                                    break;

                            }
                        }
                    };
                }
            }
        }

        /**
         * 进度发生了改变，如果numBytes，totalBytes，loanAmountRate，speed都为-1，则表示总大小获取不到
         *
         * @param numBytes   已读/写大小
         * @param totalBytes 总大小
         * @param percent    百分比
         * @param speed      速度 bytes/ms
         */
        @Override
        public final void onProgressChanged(long numBytes, long totalBytes, float percent, float speed) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                onUIProgressChanged(numBytes, totalBytes, percent, speed);
                return;
            }
            ensureHandler();
            Message message = mHandler.obtainMessage();
            message.what = WHAT_UPDATE;
            Bundle data = new Bundle();
            data.putLong(CURRENT_BYTES, numBytes);
            data.putLong(TOTAL_BYTES, totalBytes);
            data.putFloat(PERCENT, percent);
            data.putFloat(SPEED, speed);
            message.setData(data);
            mHandler.sendMessage(message);
        }


        /**
         * 进度开始
         *
         * @param totalBytes 总大小
         */
        @Override
        public final void onProgressStart(long totalBytes) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                onUIProgressStart(totalBytes);
                return;
            }
            ensureHandler();
            Message message = mHandler.obtainMessage();
            message.what = WHAT_START;
            Bundle data = new Bundle();
            data.putLong(TOTAL_BYTES, totalBytes);
            message.setData(data);
            mHandler.sendMessage(message);
        }

        /**
         * 进度结束
         */
        @Override
        public final void onProgressFinish() {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                onUIProgressFinish();
                return;
            }
            ensureHandler();
            Message message = mHandler.obtainMessage();
            message.what = WHAT_FINISH;
            mHandler.sendMessage(message);
        }

        /**
         * 进度发生了改变，如果numBytes，totalBytes，loanAmountRate，speed都为-1，则表示总大小获取不到
         *
         * @param numBytes   已读/写大小
         * @param totalBytes 总大小
         * @param percent    百分比
         * @param speed      速度 bytes/ms
         */
        public abstract void onUIProgressChanged(long numBytes, long totalBytes, float percent, float speed);


        /**
         * 进度开始
         *
         * @param totalBytes 总大小
         */
        public void onUIProgressStart(long totalBytes) {

        }

        /**
         * 进度结束
         */
        public void onUIProgressFinish() {

        }
    }
}
