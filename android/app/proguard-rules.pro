# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclass * extends android.webkit.WebView {
#   public <init>(android.content.Context);
#   public <init>(android.content.Context, android.util.AttributeSet);
#   public <init>(android.content.Context, android.util.AttributeSet, int);
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn org.codehaus.mojo.**
-dontwarn org.apache.maven.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coil
-dontwarn coil.**
-dontwarn okio.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode, it should be enough in most cases.
-if class * extends retrofit2.Call
-keep,allowobfuscation,allowshrinking interface retrofit2.Call

-if class * extends retrofit2.Response
-keep,allowobfuscation,allowshrinking class retrofit2.Response
