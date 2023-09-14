package com.example.securevideosdksample.downloadService;

import static com.example.securevideosdksample.downloadService.DownloadService.CANCEL;
import static com.example.securevideosdksample.downloadService.DownloadService.CANCEL_ALL;
import static com.example.securevideosdksample.downloadService.DownloadService.DOWNLOAD_SERVICE_ID;
import static com.example.securevideosdksample.downloadService.DownloadService.PAUSE;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;


public abstract class CustomIntentService extends Service {
    private final String TAG = getClass().getSimpleName();
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private String mName;
    private boolean mRedelivery;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj);
            stopSelf(msg.arg1);
        }
    }

    public CustomIntentService(String name) {
        super();
        mName = name;
    }

    public void setIntentRedelivery(boolean enabled) {
        mRedelivery = enabled;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case PAUSE:
                    try {
                        if (!pauseDownload(intent.getStringExtra(DOWNLOAD_SERVICE_ID)))
                            mServiceHandler.removeMessages(intent.getIntExtra(DOWNLOAD_SERVICE_ID, -1));
                        stopSelf(startId);
                    } catch (Exception e) {
                        Log.e(TAG, "Error while PAUSE download: ", e);
                    }
                    break;
                case CANCEL:
                    try {
                        if (!cancelDownload(intent.getStringExtra(DOWNLOAD_SERVICE_ID)))
                            mServiceHandler.removeMessages(intent.getIntExtra(DOWNLOAD_SERVICE_ID, -1));
                    } catch (Exception e) {
                        Log.e(TAG, "Error while CANCEL download: ", e);
                    }
                    break;
                case CANCEL_ALL:
                    try {
                        cancelAllDownload();
                        mServiceHandler.removeCallbacksAndMessages(null);
                    } catch (Exception e) {
                        Log.e(TAG, "Error while CANCEL_ALL download", e);
                    }
                    break;
            }
        } else {
            if (intent != null)
                msg.what = Integer.parseInt(intent.getStringExtra(DOWNLOAD_SERVICE_ID));
            Log.d(TAG, "Message: " + msg);
            Log.d(TAG, "Download service id: " + DOWNLOAD_SERVICE_ID);
            Log.d(TAG, "Download service intent action: " + intent.getAction());
            mServiceHandler.sendMessage(msg);
            //onHandleIntent(intent);
        }
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @WorkerThread
    protected abstract void onHandleIntent(@Nullable Intent intent);


    protected abstract boolean cancelDownload(String downloadServiceId);

    protected abstract void cancelAllDownload();

    protected abstract boolean pauseDownload(String downloadServiceId);


}
