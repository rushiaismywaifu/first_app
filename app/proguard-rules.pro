# Project specific ProGuard / R8 rules
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep model classes and all their fields for Gson serialization
-keep class com.webhook.sender.model.** { *; }
-keepclassmembers class com.webhook.sender.model.** { *; }

# Keep ViewBinding and Kotlin metadata
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
    public static *** bind(...);
}

# Gson specific rules
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keepclasseswithmembers,allowshrinking,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
