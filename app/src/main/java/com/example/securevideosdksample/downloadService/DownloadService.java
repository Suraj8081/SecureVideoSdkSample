package com.example.securevideosdksample.downloadService;

import static java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.securevideosdksample.ApplicationClass;
import com.example.securevideosdksample.R;
import com.example.securevideosdksample.room.CareerwillDatabase;
import com.example.securevideosdksample.room.table.DownloadVideoTable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;


public class DownloadService extends CustomIntentService {

    public static String action = "";

    public static final String VIDEO_DOWNLOAD_ACTION = "video_download_service_receiver";
    public static final String VIDEO_DOWNLOAD_PROGRESS = "video_download_progress";

    public static final String CURRENT_PERCENT_DOWNLOADED = "current_percentage";
    public static final String VIDEOID = "videoId";
    public static final String MESSAGE = "message";
    public static final String RESULT = "result";


    private boolean onPauseCalled = false;
    public static final int ONTASK = 1090;

    public static final int VIDEO_DOWNLOAD_CANCELLED = 3;
    public static final int VIDEO_DOWNLOAD_RESUMED = 4;
    public static final int VIDEO_DOWNLOAD_STARTED = 6;
    public static final int NOT_ENOUGH_MEMORY = -198;
    public static final int VIDEO_FILE_EXIST = -101;
    public static final int EXCEPTION_OCCURRED = -110;
    public static final int NOT_AVAILABLE_ON_SERVER = -111;
    public static final int VIDEO_DOWNLOAD_SUCCESSFUL = 1;
    public static final int VIDEO_DOWNLOAD_PAUSED = 2;

    public static final String DOWNLOAD_SERVICE_ID = "download_service_id";
    public static final String URL = "url";
    public static final String CANCEL = "cancel";
    public static final String CANCEL_ALL = "cancelAll";
    public static final String PAUSE = "pause";
    public static boolean isServiceRunning = false;
    public static final String FILEDOWNLOADSTATUS = "resume_status";

    NotificationManager mNotificationManager;

    public static final String DOWNLOADED_VIDEOS = "/.Videos/";
    public static final String DOWNLOADING_VIDEOS = "/.processing/";
    private String TAG = getClass().getSimpleName();


    public static final String VIDEONAME = "videoName";
    private String filePath = "";
    public static String courseId = "";
    private String videoName = "";

    private File downloadingFile;
    public static String videoId;
    private String url;


    private long total;
    private OutputStream fileOutput;
    private InputStream inputStream;

    private int percentage, prevPercentage;

    private String lengthInMb;
    private long lengthLong;
    private String originalFileLengthString = "";
    private String userId = "";

    private CareerwillDatabase sanskritiDatabase;

    public DownloadService() {
        super("DownloadService");
    }

    NotificationCompat.Builder mBuilder = null;

    protected void onHandleIntent(Intent intent) {
        isServiceRunning = true;
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.cancelAll();
        }

        videoName = intent.getStringExtra(VIDEONAME);
        videoId = intent.getStringExtra(DOWNLOAD_SERVICE_ID);
        url = intent.getStringExtra(URL);
        userId = intent.getStringExtra("userId");
        filePath = intent.getStringExtra("filePath");
        courseId = intent.getStringExtra("course_id");


        try {
            mBuilder = new NotificationCompat.Builder(getApplicationContext(), getPackageName()).setContentTitle(videoName).setOngoing(true).setAutoCancel(true).setSmallIcon(android.R.drawable.stat_sys_download).setStyle(new NotificationCompat.BigTextStyle().bigText("")).setPriority(Notification.PRIORITY_MAX).setContentText("Preparing...").setTicker("Preparing...");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = "MyApp_download";
                NotificationChannel channel = new NotificationChannel(channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setSound(null, null);
                channel.enableLights(false);
                channel.enableVibration(false);
                if (mNotificationManager != null) {
                    mNotificationManager.createNotificationChannel(channel);
                }
                mBuilder.setChannelId(channelId);
            }

            mBuilder.setProgress(100, 0, true);
            startForeground(11111, mBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sanskritiDatabase = CareerwillDatabase.Companion.getInstance(ApplicationClass.Companion.getInstance());
        }


        String videoStatus = intent.getStringExtra("status");
        if (sanskritiDatabase != null) {
            DownloadVideoTable downloadVideoTable = sanskritiDatabase.downloadDao().getVideo(videoId, "0", userId, courseId);
            if (downloadVideoTable != null && !downloadVideoTable.getVideoStatus().equalsIgnoreCase("Downloading Pause")) {
                if (videoStatus != null) {
                    sanskritiDatabase.downloadDao().updateStatusProgress(videoId, "0", "Downloading Running", userId, courseId);
                    publishResults("", VIDEO_DOWNLOAD_STARTED, "");
                }
            }
        }
        action = "";

        boolean isResumed = intent.getBooleanExtra(FILEDOWNLOADSTATUS, false);

        final File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + DOWNLOADED_VIDEOS + filePath + ".mp4");
        downloadingFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + DOWNLOADING_VIDEOS + filePath + ".mp4");


        if (sanskritiDatabase != null) {
            DownloadVideoTable downloadVideoTable = sanskritiDatabase.downloadDao().getVideo(videoId, "0", userId, courseId);
            if (downloadVideoTable != null && !downloadVideoTable.getVideoStatus().equalsIgnoreCase("Downloading Pause")) {

                HttpsURLConnection conn = null;
                try {
                    java.net.URL urlObj = new URL(url);
                    if (!file.exists()) {

                        conn = (HttpsURLConnection) urlObj.openConnection();
                        conn.setRequestProperty("Connection", "keep-alive");
                        conn.setReadTimeout(Integer.MAX_VALUE);
                        total = 0;
                        if (isResumed) {
                            if (sanskritiDatabase.downloadDao().isRecordExistsUserId(videoId, "0", userId, courseId)) {
                                total = downloadingFile.length();
                            } else {
                                total = 0;
                            }
                            conn.setRequestProperty("Range", "bytes=" + (total) + "-");
                        }

                        conn.connect();

                        int responseCode = conn.getResponseCode();
                        final String fileLengthStr = conn.getHeaderField("content-length");

                        if ((responseCode == HttpsURLConnection.HTTP_OK || responseCode == HttpsURLConnection.HTTP_PARTIAL) && fileLengthStr != null && !fileLengthStr.equalsIgnoreCase("null") && !"0".equalsIgnoreCase(fileLengthStr)) {


                            if (isResumed) {
                                originalFileLengthString = downloadVideoTable.getOriginalFileLengthString();
                                lengthLong = Long.parseLong(originalFileLengthString);
                            } else {
                                originalFileLengthString = fileLengthStr;
                                lengthLong = Long.parseLong(fileLengthStr);
                            }

                            lengthInMb = fileSize(lengthLong);
                            if (isMemoryAvailable(lengthLong)) {
                                String base64 = url;

                                Objects.requireNonNull(file.getParentFile()).mkdirs();

                                if (!Objects.requireNonNull(downloadingFile.getParentFile()).exists())
                                    downloadingFile.getParentFile().mkdirs();
                                if (!downloadingFile.exists())
                                    downloadingFile.createNewFile();

                                if (isResumed) {
                                    publishResults(downloadingFile.getAbsolutePath(), VIDEO_DOWNLOAD_RESUMED, "");
                                } else {
                                    publishResults("", VIDEO_DOWNLOAD_STARTED, "");
                                    sanskritiDatabase.downloadDao().updateVideoFileLength(videoId, originalFileLengthString, total, lengthInMb, percentage, "0", "Downloading Running", base64, courseId);


                                }

                                fileOutput = new FileOutputStream(downloadingFile, isResumed);
                                inputStream = conn.getInputStream();

                                byte[] buffer = new byte[4096];
                                int bufferLength;
                                percentage = 0;
                                prevPercentage = 0;
                                try {

                                    while ((bufferLength = inputStream.read(buffer)) > 0) {
                                        switch (action) {
                                            case "":
                                                total += bufferLength;
                                                fileOutput.write(buffer, 0, bufferLength);


                                                percentage = (int) ((total * 100) / lengthLong);
                                                if (percentage - prevPercentage >= 1) {
                                                    String progressText = fileSize(total) + "/" + lengthInMb;
                                                    mBuilder.setProgress(100, percentage, false);
                                                    mBuilder.setContentText(progressText);
                                                    sanskritiDatabase.downloadDao().updateVideoFileLength(videoId, originalFileLengthString, total, lengthInMb, percentage, "0", "Downloading Running", base64, courseId);

                                                    mNotificationManager.notify(11111, mBuilder.build());
                                                    publishDownloadProgress(videoId, progressText, percentage);
                                                }
                                                prevPercentage = percentage;
                                                break;
                                            case PAUSE:
                                                if (!onPauseCalled) {
                                                    sanskritiDatabase.downloadDao().updateVideoFileLength(videoId, originalFileLengthString, total, lengthInMb, percentage, "0", "Downloading Pause", base64, courseId);

                                                    fileOutput.close();
                                                    inputStream.close();

                                                    if (mNotificationManager != null) {
                                                        mNotificationManager.cancel(11111);
                                                    }
                                                    notifyDownloadState(videoName, "Download has been paused by user");
                                                }
                                                publishResults(downloadingFile.getAbsolutePath(), VIDEO_DOWNLOAD_PAUSED, "");
                                                onPauseCalled = false;
                                                return;
                                            case CANCEL:
                                                fileOutput.flush();
                                                fileOutput.close();
                                                inputStream.close();

                                                if (downloadingFile.exists())
                                                    downloadingFile.delete();

                                                if (mNotificationManager != null) {
                                                    mNotificationManager.cancel(11111);
                                                }
                                                notifyDownloadState(videoName, "Download is cancelled by user");
                                                publishResults(downloadingFile.getAbsolutePath(), VIDEO_DOWNLOAD_CANCELLED, "");
                                                return;
                                            case CANCEL_ALL:
                                                fileOutput.flush();
                                                fileOutput.close();
                                                inputStream.close();

                                                if (downloadingFile.exists()) {
                                                    downloadingFile.delete();
                                                }

                                                if (mNotificationManager != null) {
                                                    mNotificationManager.cancel(11111);
                                                }
                                                notifyDownloadState(videoName, "Download is cancelled because user logged out");

                                                publishResults(downloadingFile.getAbsolutePath(), VIDEO_DOWNLOAD_CANCELLED, "");
                                                return;
                                        }
                                    }
                                    fileOutput.flush();
                                    fileOutput.close();
                                    inputStream.close();

                                    downloadingFile.renameTo(file);
                                    if (mNotificationManager != null) {
                                        mNotificationManager.cancel(11111);
                                    }
                                    publishResults(file.getAbsolutePath(), videoId, VIDEO_DOWNLOAD_SUCCESSFUL, "success");
                                } catch (Exception e) {
                                    if (e instanceof SSLException) {
                                        //Pause if no internet
                                        try {
                                            fileOutput.flush();
                                            fileOutput.close();
                                            inputStream.close();
                                            if (mNotificationManager != null) {
                                                mNotificationManager.cancel(11111);
                                            }
                                            notifyDownloadState(videoName, "Internet Issue. Download is not completed Please retry!!!");
                                            publishResults(downloadingFile.getAbsolutePath(), VIDEO_DOWNLOAD_PAUSED, "");

                                        } catch (IOException ex) {
                                            Log.e(TAG, "Exception no net: " + ex);
                                            notifyDownloadState(videoName, "Connection lost!!!. Please make sure network availability.");
                                        }
                                    } else {
                                        if (e instanceof SocketException) {

                                            if (fileOutput != null) {
                                                fileOutput.flush();
                                                fileOutput.close();
                                            }

                                            if (inputStream != null)
                                                inputStream.close();
                                            if (mNotificationManager != null) {
                                                mNotificationManager.cancel(11111);
                                            }

                                            notifyDownloadState(videoName, "Internet Issue. Download is not completed Please retry!!!");
                                            publishResults(downloadingFile.getAbsolutePath(), VIDEO_DOWNLOAD_PAUSED, "");

                                        } else {
                                            fileOutput.flush();
                                            fileOutput.close();
                                            inputStream.close();
                                            if (downloadingFile.exists())
                                                downloadingFile.delete();

                                            if (mNotificationManager != null) {
                                                mNotificationManager.cancel(11111);
                                            }
                                            notifyDownloadState(videoName, "Something went wrong. Download is not completed Please retry!!!");
                                        }
                                        publishResults(downloadingFile.getAbsolutePath(), videoId, EXCEPTION_OCCURRED, e.getMessage());
                                        Log.e(TAG, e.getMessage());

                                    }
                                }
                            } else {
                                if (downloadingFile.exists())
                                    downloadingFile.delete();
                                notifyDownloadState(videoName, "Not Enough Memory Space");
                                publishResults(downloadingFile.getAbsolutePath(), videoId, NOT_ENOUGH_MEMORY, "Not Enough Memory Space");
                            }

                        } else {
                            if (downloadingFile.exists())
                                downloadingFile.delete();

                            if (responseCode == HTTP_NOT_FOUND) {
                                notifyDownloadState(videoId, "Video not found!!!");
                            } else if (responseCode != HttpsURLConnection.HTTP_OK)
                                notifyDownloadState(videoId, getErrorMessage(responseCode));
                            publishResults(downloadingFile.getAbsolutePath(), videoId, NOT_AVAILABLE_ON_SERVER, getErrorMessage(responseCode));
                        }
                    } else {
                        file.delete();
                        notifyDownloadState(videoName, "Video is already Downloaded.");
                        publishResults(file.getAbsolutePath(), videoId, VIDEO_FILE_EXIST, "Video is already Downloaded.");
                    }


                } catch (Exception e) {
                    if (e instanceof ConnectException) {
                        //Pause if no internet
                        try {

                            if (fileOutput != null) {
                                fileOutput.flush();
                                fileOutput.close();
                            }

                            if (inputStream != null)
                                inputStream.close();
                            if (mNotificationManager != null) {
                                mNotificationManager.cancel(11111);
                            }
                            notifyDownloadState(videoName, "Download has been paused by user");
                            publishResults(downloadingFile.getAbsolutePath(), VIDEO_DOWNLOAD_PAUSED, "");
                        } catch (IOException ex) {
                            Log.e(TAG, "Exception no net: " + ex);
                            notifyDownloadState(videoName, "Connection lost!!!. Please make sure network availability.");
                        }
                    }
                    if (e instanceof SocketException) {
                        try {

                            if (fileOutput != null) {
                                fileOutput.flush();
                                fileOutput.close();
                            }
                            if (inputStream != null)
                                inputStream.close();
                            if (mNotificationManager != null) {
                                mNotificationManager.cancel(11111);
                            }
                            if (conn != null) {
                                conn.disconnect();
                            }
                        } catch (IOException ex) {
                            Log.e(TAG, "Exception no net: " + ex);
                            notifyDownloadState(videoName, "Connection lost!!!. Please make sure network availability.");
                        }
                    } else if (e instanceof IOException) {
                        if (e instanceof UnknownHostException) {
                            try {
                                if (fileOutput != null) {
                                    fileOutput.flush();
                                    fileOutput.close();
                                }

                                if (inputStream != null)
                                    inputStream.close();
                                if (mNotificationManager != null) {
                                    mNotificationManager.cancel(11111);
                                }
                                notifyDownloadState(videoName, "Internet Issue. Download is Pause!!!");
                                DownloadVideoTable downloadVideoTable1 = sanskritiDatabase.downloadDao().getVideo(videoId, "0", userId, courseId);
                                if (downloadVideoTable1 == null || downloadVideoTable1.getTotalDownloadLocale() == 0) {
                                    if (downloadingFile.exists())
                                        downloadingFile.delete();

                                    String base64 = url;
                                    sanskritiDatabase.downloadDao().updateVideoFileLength(videoId, downloadVideoTable1 != null ? downloadVideoTable1.getOriginalFileLengthString() : "0", downloadVideoTable1 != null ? downloadVideoTable1.getTotalDownloadLocale() : 0, downloadVideoTable1 != null ? downloadVideoTable1.getLengthInMb() : "", downloadVideoTable1 != null ? downloadVideoTable1.getPercentage() : 0, "0", "Downloading Pause", base64, courseId);

                                    publishResults(downloadingFile.getAbsolutePath(), 2021, "");
                                }

                            } catch (Exception ee) {
                                ee.printStackTrace();
                            }


                        } else {
                            notifyDownloadState(videoName, "Internet Issue. Download is not completed Please retry!!!");
                            publishResults(downloadingFile.getAbsolutePath(), VIDEO_DOWNLOAD_PAUSED, "");
                            if (downloadingFile.exists())
                                downloadingFile.delete();
                        }

                    } else {
                        notifyDownloadState(videoName, "Couldn't save video. Not able to create file.");
                        publishResults(downloadingFile.getAbsolutePath(), videoId, EXCEPTION_OCCURRED, e.getMessage());
                        if (downloadingFile.exists())
                            downloadingFile.delete();
                    }
                    Log.e(TAG, "Download error", e);
                } finally {
                    Log.d(TAG, "Downloading process finished");
                    if (conn != null) {
                        try {
                            if (fileOutput != null) {
                                fileOutput.flush();
                                fileOutput.close();
                            }
                            if (inputStream != null)
                                inputStream.close();
                            conn.disconnect();
                            // conn.getErrorStream().close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (isResumed) {
                if (downloadingFile.exists())
                    downloadingFile.delete();

                if (mNotificationManager != null) {
                    mNotificationManager.cancel(11111);
                }
                notifyDownloadState(videoName, "Download is cancelled by user");
                publishResults(downloadingFile.getAbsolutePath(), VIDEO_DOWNLOAD_CANCELLED, "");
            } else {
                if (downloadingFile.exists())
                    downloadingFile.delete();

                if (mNotificationManager != null) {
                    mNotificationManager.cancel(11111);
                }
                notifyDownloadState(videoName, "Download is cancelled by user");
                publishResults(downloadingFile.getAbsolutePath(), VIDEO_DOWNLOAD_CANCELLED, "");
            }
        }


    }


    @Override
    protected boolean cancelDownload(String videoId) {
        if (DownloadService.videoId.equals(videoId)) {
            action = CANCEL;
            return true;
        }
        return false;
    }

    protected void cancelAllDownload() {
        action = CANCEL_ALL;

    }

    protected boolean pauseDownload(String videoId) {
        if (this.videoId.equals(videoId)) {
            action = PAUSE;
            return true;
        }
        return false;
    }


    private void notifyDownloadState(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int icon = message != null && !message.isEmpty() && message.contains("Successfully") ? R.mipmap.ic_launcher : android.R.drawable.ic_dialog_alert;
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, getPackageName()).setSmallIcon(icon).setColor(getResources().getColor(R.color.colorPrimary)).setColorized(true).setStyle(new NotificationCompat.BigTextStyle().bigText(message)).setContentTitle(title).setContentText(message).setPriority(Notification.PRIORITY_MAX).setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = getResources().getString(R.string.app_name) + "download";
            NotificationChannel channel = new NotificationChannel(channelId, getResources().getString(R.string.app_name) + "Video Download", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder.setChannelId(channelId);
        }

        if (notificationManager != null) {
            notificationManager.notify(NotificationID.getID(), notificationBuilder.build());
        }
    }

    static class NotificationID {
        private final static AtomicInteger c = new AtomicInteger(0);

        public static int getID() {
            return c.incrementAndGet();
        }
    }


    private boolean isMemoryAvailable(final long fileSize) {
        long availableSize = getAvailableExternalMemorySize();
        return availableSize > fileSize;
    }

    private String fileSize(double length) {
        DecimalFormat format = new DecimalFormat("#.##");
        String convertedSizeStr;
        if (length > 1024 * 1024) {
            convertedSizeStr = format.format(length / (1024 * 1024)) + " MB";
        } else if (length > 1024) {
            convertedSizeStr = format.format(length / 1024) + " KB";
        } else {
            convertedSizeStr = format.format(length) + " B";
        }
        return convertedSizeStr;

    }


    private long getAvailableExternalMemorySize() {
        File path = getExternalFilesDir(null);
        StatFs stat = new StatFs(path.getPath());
        long blockSize, availableBlocks;
        blockSize = stat.getBlockSizeLong();
        availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    private void publishDownloadProgress(String videoId, String progressText, int currentProgress) {
        Intent intent = new Intent("video_download_progress");
        intent.putExtra(VIDEOID, videoId);
        intent.putExtra("course_id", courseId);
        intent.putExtra(CURRENT_PERCENT_DOWNLOADED, currentProgress);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(intent);
    }

    private void publishResults(String outputPath, String resId, int result, String message) {
        if (result != VIDEO_DOWNLOAD_RESUMED && result != VIDEO_DOWNLOAD_STARTED)
            stopForeground(true);
        if (VIDEO_DOWNLOAD_SUCCESSFUL == result) {
            sanskritiDatabase.downloadDao().updateVideoStatus(videoId, "1", "Downloaded", percentage, "", courseId);

        } else if (result == EXCEPTION_OCCURRED || result == VIDEO_DOWNLOAD_CANCELLED || result == NOT_AVAILABLE_ON_SERVER || result == NOT_ENOUGH_MEMORY) {
            sanskritiDatabase.downloadDao().deleteVideo(videoId, userId, courseId);
        } else if (VIDEO_FILE_EXIST == result) {

        } else if (result == VIDEO_DOWNLOAD_PAUSED) {
            String base64 = url;

            sanskritiDatabase.downloadDao().updateVideoFileLength(videoId, originalFileLengthString, total, lengthInMb, percentage, "0", "Downloading Pause", base64, courseId);
        } else if (result == ONTASK) {
            CareerwillDatabase.destroyInstance();
        }

        Intent intent = new Intent(VIDEO_DOWNLOAD_ACTION);
        intent.putExtra("outputPath", outputPath);
        intent.putExtra(RESULT, result);
        intent.putExtra("course_id", courseId);
        intent.putExtra(MESSAGE, message);
        intent.putExtra(VIDEOID, videoId);
        intent.putExtra("percentage", percentage);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(intent);
        Log.e("myLog END", "" + "service END");


    }

    private void publishResults(String outputPath, int result, String message) {
        publishResults(outputPath, "", result, message);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //Called when app is removed form recent panel

        //stopSelf();
        onPauseCalled = true;
        action = PAUSE;
        try {
            if (isServiceRunning) {
                if (sanskritiDatabase != null) {

                    String base64 = url;
                    AsyncTask.execute(() -> {
                        sanskritiDatabase.downloadDao().updateVideoFileLength(videoId, originalFileLengthString, total, lengthInMb, percentage, "0", "Downloading Pause", base64, courseId);
                        for (DownloadVideoTable download : sanskritiDatabase.downloadDao().getAllVideos(userId, courseId)) {
                            if (download.getVideoStatus().equalsIgnoreCase("Downloading Running") && download.getPercentage() > 0) {
                                sanskritiDatabase.downloadDao().updateVideoStatus(download.getVideoId(), "Downloading Pause", userId, courseId);
                            }
                            if (download.getVideoStatus().equalsIgnoreCase("Downloading Running") && download.getPercentage() == 0) {
                                sanskritiDatabase.downloadDao().deleteVideo(download.getVideoId(), userId, courseId);
                            }
                            try {
                                if (fileOutput != null) {
                                    fileOutput.flush();
                                    fileOutput.close();
                                }
                                if (inputStream != null)
                                    inputStream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                        }
                    });

                }
            }

            if (mNotificationManager != null) {
                mNotificationManager.cancel(11111);
            }
            notifyDownloadState(videoName, "Download has been paused by user");
            publishResults(downloadingFile.getAbsolutePath(), ONTASK, "");


            isServiceRunning = false;
            videoId = "";
            courseId = "";
            onPauseCalled = false;

        } catch (Exception e) {
            Log.e(TAG, "onTaskRemoved: ", e);
        }//   FirebaseCrashlytics.getInstance().recordException(e);
    }


    @Override
    public void onDestroy() {
        isServiceRunning = false;
        videoId = "";
        courseId = "";
        super.onDestroy();
    }


    public String getErrorMessage(int responseCode) {
        String message = "";
        switch (responseCode) {

            case HTTP_CLIENT_TIMEOUT:
            case -1:
                message = "Problem while connecting! please check your Internet connection.";
                break;

            case HTTP_UNAVAILABLE:
                message = "Server under maintenance! please try after some time.";
                break;

            case HTTP_GATEWAY_TIMEOUT:
            case HTTP_INTERNAL_ERROR:
                message = "Server error! please try after some time.";
                break;

            case HTTP_UNAUTHORIZED:
            case HTTP_NOT_FOUND:
                message = "Session timed out. Kindly login again.";
                break;
            case -2:
                message = "Something went wrong! Please try again.";
                break;
            case -3:
                message = "Sorry, we are facing some problems regarding payment.\nPlease try again later.";
                break;
            default:
                message = "Something went wrong, please try again after some time.";
                break;
        }
        return message;
    }

}