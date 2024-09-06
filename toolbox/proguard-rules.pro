# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-optimizationpasses 5                               # 指定代码的压缩级别
-dontusemixedcaseclassnames                         # 混淆时不会产生形形色色的类名
-dontskipnonpubliclibraryclasses                    # 指定不去忽略非公共的库类
-dontskipnonpubliclibraryclassmembers               # 指定不去忽略包可见的库类的成员
-dontpreverify                                      # 不预校验
-ignorewarnings                                     # 屏蔽警告
-verbose                                            # 混淆时记录日志
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*    #优化
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions, SourceFile, LineNumberTable, LocalVariableTable, LocalVariableTypeTable

#代码优化选项，不加该行会将没有用到的类删除
-dontshrink
-obfuscationdictionary dictionary_rules.txt
-classobfuscationdictionary dictionary_rules.txt
-dontwarn androidx.annotation.Keep
-keep @androidx.annotation.Keep class **{
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
#保留混淆枚举中的values()和valueOf()方法
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

#不混淆实现android.os.Parcelable的类
-keep class * implements android.os.Parcelable
#Parcelable实现类中的CREATOR字段是绝对不能改变的，包括大小写
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

#R文件中的所有记录资源id的静态字段
-keepclassmembers class **.R$* {
    public static <fields>;
}

# The support library contains references to newer platform versions.
# Dont warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
#忽略support包因为版本兼容产生的警告
-dontwarn android.support.**
-dontwarn androidx.**

#--->jni native function
-keepclassmembers class * {
    native <methods>;
}

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# keep自定义view的get/set方法
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

# keep继续自Activity中所有包含public void *(android.view.View)签名的方法，如onClick
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

#保护所有类中的TAG字段
-keepclassmembers class * {
    static final java.lang.String TAG;
}

-keepnames class * implements java.io.Serializable

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

##--> protect library class
-keep public class **Util*{
    public <methods>;
}

# protect Writer/Dispatcher
-keep public class com.threshold.toolbox.**er*{
    public <methods>;
}

-keep public class com.threshold.toolbox.*$*er{
    public <methods>;
}

-keepclasseswithmembernames public class com.threshold.permissions.*{
    public *;
}

-keepclasseswithmembernames public class com.threshold.toolbox.log.**{
    public *;
}

# protect inner class fields and methods in jni class.
-keepclasseswithmembernames class com.threshold.jni.**Jni$* {
     <fields>;
     public <methods>;
}

-keep class com.threshold.toolbox.MessageDispatcher{
    public <methods>;
}

-keep class com.threshold.toolbox.ByteRingBuffer{
    public <methods>;
}

-keep class com.threshold.toolbox.RingBuffer{
    public <methods>;
}

-keep class com.threshold.toolbox.BitConverter{
    public <methods>;
}

-keep class com.threshold.toolbox.FileWriter{
    public <methods>;
}

-keep class com.threshold.toolbox.AsyncFileWriter{
    public <methods>;
}

-keep class com.threshold.toolbox.toasty.Toasty{
    public <methods>;
}

-keep class com.threshold.toolbox.toasty.Toasty$Config{
    public <methods>;
}

-keepclasseswithmembernames class com.threshold.toolbox.log.LogTag{
    public <methods>;
}
-keepclasseswithmembernames class com.threshold.toolbox.log.LogPriority{
    public <methods>;
}

-keep class com.threshold.toolbox.log.ILog{
    public <methods>;
}
-keepclasseswithmembernames class * implements com.threshold.toolbox.log.ILog{
    public <methods>;
}
-keepclasseswithmembernames class * extends com.threshold.toolbox.log.AbsLogger{
    public <methods>;
}
-keepclasseswithmembernames class com.threshold.toolbox.log.LoggerConfig{
     public *;
}
-keepclasseswithmembernames class com.threshold.toolbox.log.LoggerFactory{
     public *;
}
-keepclasseswithmembernames class com.threshold.toolbox.log.LoggerFactory$LogStrategy{
    public <methods>;
}

-keep class com.threshold.toolbox.log.Printer{
    public <methods>;
}

-keepclasseswithmembernames class * implements com.threshold.toolbox.log.Printer{
    public <methods>;
}

-keepclasseswithmembernames class com.threshold.toolbox.log.SLog{
    public <methods>;
}

