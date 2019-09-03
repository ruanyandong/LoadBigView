package com.example.loadbigimage;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;

/**
 * Android长图加载
 *     1、不能一次把整张图片加载到内存，分部分加载
 *     2、内存复用
 * 或者使用WebView来加载，但是容易内存泄漏
 */
public class MainActivity extends AppCompatActivity {

    NeBigView bigView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bigView = findViewById(R.id.bigView);

        InputStream is = null;
        try {
            is = getAssets().open("big.jpg");
            bigView.setImage(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
