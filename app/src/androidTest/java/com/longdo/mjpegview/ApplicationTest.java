package com.longdo.mjpegview;

import android.app.Application;
import android.content.Context;
//import android.test.ApplicationTestCase;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
public class ApplicationTest {

    @Test
    public void testSomething() {
        // 使用 InstrumentationRegistry 访问应用程序上下文
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // 在这里编写你的测试逻辑
        // ...

        // 断言你的测试结果
        // ...
    }
}