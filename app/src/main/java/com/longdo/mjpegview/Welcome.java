package com.longdo.mjpegview;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Welcome extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000; // 延迟时间，单位：毫秒

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // 从 assets 文件夹加载字体文件
        Typeface typeface = Typeface.createFromAsset(getAssets(), "Lobster-Regular.ttf");
        TextView text = findViewById(R.id.app);
        text.setTypeface(typeface);



        Button btnStart = findViewById(R.id.start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在这里启动 MainActivity
                Intent intent = new Intent(Welcome.this, MainActivity.class);
                startActivity(intent);

            }
        });













    }
}