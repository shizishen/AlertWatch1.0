package com.longdo.mjpegview;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.longdo.mjpegview.server.DelayedMessageService;
import com.longdo.mjpegviewer.MjpegView;
import com.longdo.mjpegviewer.MjpegViewError;
import com.longdo.mjpegviewer.MjpegViewStateChangeListener;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private MjpegView view1;
    private TextView mTextView;
    private Module mModule = null;
    private static String[] mClasses;
    private List<String> mResults = new ArrayList<>();
    private boolean isRun = false;
    private boolean isOpenBackServer = false;
    String url = "https://app.punyapat.me/mjpeg-server/mjpeg";
    public static  final int NOTIFICATION_ID = 5453;
    private static final int PERMISSION_REQUEST_CODE = 123; // 可以选择任何整数值作为请求代码

    private Handler mHandler = new Handler();
    private TextView warnText;

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Your Notification Channel Name";
            String description = "Your Notification Channel Description";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("your_channel_id", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 创建通知渠道
        createNotificationChannel();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


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
            finish();
        }

        setContentView(R.layout.activity_main);


        Intent intent = getIntent();
        String receivedData = intent.getStringExtra("key");
        TextView ipText = findViewById(R.id.ipText);
        if (receivedData != null) {
            url = receivedData;
            ipText.setText("正在监控ip:"+receivedData);
        }

        //后台开关监控
        SwitchCompat switchCompat = findViewById(R.id.switchServer);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 处理开关状态的变化
                if (isChecked) {
                    // 开关打开
                    isOpenBackServer = true;
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);

                    }else{
                        Toast.makeText(MainActivity.this, "后台服务已打开,通知权限已开启", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    // 开关关闭
                    isOpenBackServer = false;
                    Toast.makeText(MainActivity.this, "后台服务已关闭", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button btnCamera = findViewById(R.id.test_camera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LiveVideoClassificationActivity.class);
                startActivity(intent);

            }
        });

        Button btnHowToUse = findViewById(R.id.how_use);
        btnHowToUse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,HowUse.class);
                startActivity(intent);
            }
        });

        Button btnAddFac = findViewById(R.id.add_fac);
        btnAddFac.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,AddFacility.class);
                url = intent.getStringExtra("key");
                startActivity(intent);
            }
        });

        mTextView = findViewById(R.id.textView);
        warnText = findViewById(R.id.warnText);
        //imageView = findViewById(R.id.imageView);

        new Thread(new Runnable() {
            @Override
            public void run() {
                view1 = findViewById(R.id.mjpegview1);
                view1.setAdjustHeight(true);
                view1.setAdjustWidth(true);
                view1.setMode(MjpegView.MODE_FIT_WIDTH);
                //view.setMsecWaitAfterReadImageError(1000);
                //view1.setUrl("http://192.168.56.47:8080/cam.mjpeg");
                //view1.setUrl("https://app.punyapat.me/mjpeg-server/mjpeg");


                view1.setUrl(url);
                view1.setRecycleBitmap(false);
                view1.setStateChangeListener(new MjpegViewStateChangeListener() {

                    @Override
                    public void onStreamDownloadStart() {
                        Log.d("StateChangeListener", "onStreamDownloadStart");
                    }

                    @Override
                    public void onStreamDownloadStop() {
                        Log.d("StateChangeListener", "onStreamDownloadStop");
                    }

                    @Override
                    public void onServerConnected() {
                        Log.d("StateChangeListener", "onServerConnected");
                    }

                    @Override
                    public void onMeasurementChanged(Rect rect) {
                        Log.d("StateChangeListener", "onMeasurementChanged");
                    }

                    @Override
                    public void onNewFrame(Bitmap image) {
                        final Pair<Integer[], Long> pair = getResult(image);
                        final Integer[] scoresIdx = pair.first;
                        String tops[] = new String[Constants.TOP_COUNT];
                        for (int j = 0; j < Constants.TOP_COUNT; j++)
                                    tops[j] = mClasses[scoresIdx[j]];

                        //判断类型,并进行处理
                        warningInfo(tops, image);
                        final String result = String.join(", ", tops);
                        final long inferenceTime = pair.second;
                        final int finalI = 0;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //imageView.setImageBitmap(image);
                                mTextView.setVisibility(View.VISIBLE);
                                mTextView.setText(String.format("%ds: %s - %dms", finalI + 1, result, inferenceTime));
                            }
                        });
                        mResults.add(result);
                    }


                    @Override
                    public void onError(MjpegViewError error) {

                    }

                });

            }
        }).start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        view1.startStream();
        super.onResume();
        // 停止后台服务

            Intent intent = new Intent(this, DelayedMessageService.class);
            intent.putExtra(DelayedMessageService.EXTRA_PARAM2, url);
            intent.setAction(DelayedMessageService.ACTION_Waring);
            stopService(intent);

    }

    @Override
    protected void onPause() {
        view1.stopStream();
        if (isOpenBackServer){
            // 启动后台服务
            Intent intent = new Intent(this, DelayedMessageService.class);
            intent.setAction(DelayedMessageService.ACTION_Waring);
            intent.putExtra(DelayedMessageService.EXTRA_PARAM2, url);
            startService(intent);
        }

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //Settings
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        view1.stopStream();
        super.onStop();
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

    private void warningInfo(String tops[], Bitmap image) {
        String[] targetChars = {
                "摔角",
                "打拳的人（拳击）",
                "踢腿"
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
                    // 去除末尾的逗号和空格
                    text = text.substring(0, text.length() - 2);
                }
                //通知
                // 启动通知定时器
                //startNotificationTimer();
                //notificationShow(text);
                Log.e(TAG, "warningInfo:" + foundChars);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        warnText.setText("警告内容:"+foundChars);
                    }
                });
            }

        } else {
            // tops数组中不包含目标字符
            // 在这里执行其他操作
            // ...
        }
    }

    public static String[] getClasses() {
        return mClasses;
    }
}
