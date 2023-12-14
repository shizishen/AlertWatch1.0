package com.longdo.mjpegview.server;

import static android.content.ContentValues.TAG;

import static com.longdo.mjpegview.MainActivity.NOTIFICATION_ID;

import android.Manifest;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.longdo.mjpegview.Constants;
import com.longdo.mjpegview.MainActivity;
import com.longdo.mjpegview.R;
import com.longdo.mjpegview.utils.MjpegDecoder;
import com.longdo.mjpegviewer.MjpegView;
import com.longdo.mjpegviewer.MjpegViewError;
import com.longdo.mjpegviewer.MjpegViewStateChangeListener;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class DelayedMessageService extends IntentService {

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_Waring = "com.longdo.mjpegview.server.action.Waring";
    public static final String ACTION_BAZ = "com.longdo.mjpegview.server.action.BAZ";

    // TODO: Rename parameters
    public static final String EXTRA_PARAM1 = "com.longdo.mjpegview.server.extra.PARAM1";
    public static final String EXTRA_PARAM2 = "com.longdo.mjpegview.server.extra.PARAM2";
    private static final int PERMISSION_REQUEST_CODE = 123; // 声明权限请求代码

    private Module mModule = null;
    private static String[] mClasses;
    private MjpegView view1;
    private boolean isRunning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int INTERVAL = 5000; // 5 seconds


    public DelayedMessageService() {
        super("MyIntentService");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public DelayedMessageService(String name) {
        super(name);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化模型和分类标签
        try {
            mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "video_classification.ptl"));
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("classes_ch.txt")));
            String line;
            List<String> classes = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                classes.add(line);
            }
            mClasses = new String[classes.size()];
            classes.toArray(mClasses);
        } catch (IOException e) {
            Log.e(TAG, "Error reading model file", e);
            stopSelf(); // 停止Service

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "channel_id",
                    "Channel Name",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "onHandleIntent: 后台监控已启动!!!");
            //使用系统Toast
            Toast.makeText(getApplicationContext(), "后台监控已启动!!!", Toast.LENGTH_LONG).show();
            final String url = intent.getStringExtra(EXTRA_PARAM2);
            handleActionFoo(url);
        }

    }


    private void handleActionFoo(String url) {
        MjpegDecoder mjpegDecoder = new MjpegDecoder();
        mjpegDecoder.setUrl(url);
        mjpegDecoder.setStateChangeListener(new MjpegViewStateChangeListener() {
            @Override
            public void onStreamDownloadStart() {
            }

            @Override
            public void onStreamDownloadStop() {
            }

            @Override
            public void onServerConnected() {
            }

            @Override
            public void onMeasurementChanged(Rect rect) {
            }

            @Override
            public void onNewFrame(Bitmap image) {
                if (image != null) {
                    Log.e(TAG, "onNewFrame: image != null!!!!!!!!!!!!!!!!!!!");
                } else {
                    Log.e(TAG, "onNewFrame: image = null!!!!!!!!!!!!!!!!!!!");
                }

                final Pair<Integer[], Long> pair = getResult(image);
                final Integer[] scoresIdx = pair.first;
                String tops[] = new String[Constants.TOP_COUNT];
                for (int j = 0; j < Constants.TOP_COUNT; j++)
                    tops[j] = mClasses[scoresIdx[j]];

                //判断类型,并进行处理
                warningInfo(tops);

                final String result = String.join(", ", tops);
                final long inferenceTime = pair.second;
                final int finalI = 0;
            }

            @Override
            public void onError(MjpegViewError error) {
            }
        });
        mjpegDecoder.start();
    }

    private void warningInfo(String tops[]) {
        String[] targetChars = {
                "摔角",
                "抽烟",
                "打拳的人（拳击）",
                "踢腿",
                "瑜伽",
                "潜水"
        };
        List<String> foundChars = new ArrayList<>();
        boolean isCharPresent = false; // 标记是否找到目标字符
        for (String targetChar : targetChars) {

            for (String item : tops) {
                if (item.equals(targetChar)) {
                    isCharPresent = true;
                    foundChars.add(targetChar);
                    break; // 找到目标字符，提前结束循环
                }
            }
        }
        if (isCharPresent) {
            //触发警报后,保存图片到本地
            if (foundChars != null) {

                // 将List<String> foundChars转换为逗号分隔的字符串
                StringBuilder stringBuilder = new StringBuilder();
                for (String foundChar : foundChars) {
                    stringBuilder.append(foundChar).append(", ");
                }
                String text = stringBuilder.toString();
                if (text.length() > 0) {
                    //notificationShow(text);
                    // 去除末尾的逗号和空格
                    text = text.substring(0, text.length() - 2);
                }


                //通知
                notificationShow(text);



                Log.e(TAG, "warningInfo:" + foundChars);
            }
        } else {
            // tops数组中不包含目标字符
            // 在这里执行其他操作
            // ...
        }
    }
    private void notificationShow(String text) {
        Log.e(TAG, "warningInfo: 部署通知" );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("警告:")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(new long[]{0, 1000, 1000, 1000}); // 设置振动模式


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        Log.e(TAG, "warningInfo: 权限判断" );
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.e(TAG, "warningInfo: 不允许通知" );
            return;
        }
        Log.e(TAG, "warningInfo: 发出通知" );

        notificationManager.notify(0, builder.build());
        //点击通知的响应
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

    }
    private Pair<Integer[], Long> getResult(Bitmap bitmap) {

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(Constants.MODEL_INPUT_SIZE);
//
//        // extract 4 frames for each second of the video and pack them to a float buffer to be converted to the model input tensor
//        for (int i = 0; i < Constants.COUNT_OF_FRAMES_PER_INFERENCE; i++) {
//            long timeUs = 1000 * (fromMs + (int) ((toMs - fromMs) * i / (Constants.COUNT_OF_FRAMES_PER_INFERENCE - 1.)));
//            Bitmap bitmap = mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        float ratio = Math.min(bitmap.getWidth(), bitmap.getHeight()) / (float) Constants.TARGET_VIDEO_SIZE;
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() / ratio), (int) (bitmap.getHeight() / ratio), true);
        Bitmap centerCroppedBitmap = Bitmap.createBitmap(resizedBitmap,
                resizedBitmap.getWidth() > resizedBitmap.getHeight() ? (resizedBitmap.getWidth() - resizedBitmap.getHeight()) / 2 : 0,
                resizedBitmap.getHeight() > resizedBitmap.getWidth() ? (resizedBitmap.getHeight() - resizedBitmap.getWidth()) / 2 : 0,
                Constants.TARGET_VIDEO_SIZE, Constants.TARGET_VIDEO_SIZE);

        TensorImageUtils.bitmapToFloatBuffer(centerCroppedBitmap, 0, 0,
                Constants.TARGET_VIDEO_SIZE, Constants.TARGET_VIDEO_SIZE, Constants.MEAN_RGB, Constants.STD_RGB, inTensorBuffer,
                (Constants.COUNT_OF_FRAMES_PER_INFERENCE - 1) * 1 * Constants.TARGET_VIDEO_SIZE * Constants.TARGET_VIDEO_SIZE);


        Tensor inputTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 3, Constants.COUNT_OF_FRAMES_PER_INFERENCE, 160, 160});

        final long startTime = SystemClock.elapsedRealtime();
        Tensor outputTensor = mModule.forward(IValue.from(inputTensor)).toTensor();
        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;

        final float[] scores = outputTensor.getDataAsFloatArray();
        Integer scoresIdx[] = new Integer[scores.length];
        for (int i = 0; i < scores.length; i++)
            scoresIdx[i] = i;

        Arrays.sort(scoresIdx, new Comparator<Integer>() {
            @Override
            public int compare(final Integer o1, final Integer o2) {
                return Float.compare(scores[o2], scores[o1]);
            }
        });

        return new Pair<>(scoresIdx, inferenceTime);
    }


}