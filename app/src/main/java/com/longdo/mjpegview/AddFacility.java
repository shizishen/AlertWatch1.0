package com.longdo.mjpegview;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddFacility extends AppCompatActivity {
    private String cameraText1;
    private List<String> cameraText_list = new ArrayList<>();

    private Button saveButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_facility);



        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initData();
                if (isEditTextEmpty()) {
                    Toast.makeText(AddFacility.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    saveTextToFile(cameraText1, "text1.txt");
                    Toast.makeText(AddFacility.this, "url saved", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AddFacility.this, MainActivity.class);
                    intent.putExtra("key",cameraText1 );
                    startActivity(intent);

                }
            }
        });

    }
    private void saveTextToFile(String text, String fileName) {
        try {
            FileOutputStream fileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            fileOutputStream.write(text.getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void  initData() {


        EditText editText1 = findViewById(R.id.switchToggle1_edit);
        cameraText1 = editText1.getText().toString();  // 获取EditText中的文本
    }
    private boolean isEditTextEmpty() {
        EditText editText1 = findViewById(R.id.switchToggle1_edit);
        return editText1.getText().toString().isEmpty();
    }
}