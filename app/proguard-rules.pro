########################################
# 🚀 Papa Scan - ProGuard Rules
########################################

# ==== ROOM (Base de datos local) ====
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao public interface *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ==== VIEWMODEL y LIVEDATA ====
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ==== RETROFIT y OKHTTP ====
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# ==== GSON ====
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==== GLIDE ====
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { *; }

# ==== TENSORFLOW LITE ====
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# ==== OTRAS LIBRERÍAS ====
-dontwarn com.squareup.picasso.**
-dontwarn com.airbnb.lottie.**
-dontwarn com.mikhaellopez.circularprogressbar.**
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

########################################
# ✅ Fin de reglas Papa Scan
########################################
