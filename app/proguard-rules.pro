# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/perth/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
# 保留你的com.longdo.mjpegview包中的类
-keep class com.longdo.mjpegview.** {*;}

# 保留与你的TensorFlow Lite模型和PyTorch相关的类
-keep class org.pytorch.** {*;}
-keep class org.pytorch.torchvision.** {*;}

# 保留在AndroidManifest.xml中使用的任何类
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# 保留实现Android接口的任何类
-keep public class * implements android.os.Parcelable
-keep public class * implements android.os.Parcelable$Creator
-keep public class * implements android.os.Parcelable$Creator$lt*$gt
-keep public class * implements android.view.View$OnClickListener
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# 保留具有Bundle参数的构造函数的任何类
-keepclasseswithmembers class * {
    public <init>(android.os.Bundle);
}

# 保留PyTorch的本地库
-keep class org.pytorch.LiteModuleLoader { *; }
-keep class org.pytorch.PyTorchAndroid { *; }

# 保留你的通知通道和服务类
-keep class com.longdo.mjpegview.MainActivity { *; }
-keep class com.longdo.mjpegview.server.DelayedMessageService { *; }

# 保留你在代码中使用的常量和字段
-keepclassmembers class com.longdo.mjpegview.Constants {
    public static <fields>;
}
