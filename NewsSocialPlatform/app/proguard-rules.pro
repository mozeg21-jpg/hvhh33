# ═══════════════════════════════════════════════════════════════════
# Akhbari (com.news.kimo) – ProGuard / R8 Rules
# ═══════════════════════════════════════════════════════════════════

# ─── Default Android Rules ──────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ─── Keep Application Class ─────────────────────────────────────
-keep public class com.news.kimo.** {
    public protected *;
}

# ─── Model Classes (all in com.news.kimo.models) ────────────────
-keep class com.news.kimo.models.** { *; }
-keepclassmembers class com.news.kimo.models.** { *; }

# ─── Gson ───────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# Keep any class that has @SerializedName annotation
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
    <init>(...);
}

# ─── Firebase ───────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Database model objects
-keepclassmembers class com.news.kimo.models.** {
    <init>(com.google.firebase.database.DataSnapshot);
}

# ─── Glide ──────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}
-keep class com.bumptech.glide.load.resource.bitmap.VideoDecoder {
    *** decode(...);
}
-keep class com.bumptech.glide.load.resource.bitmap.HardwareConfigState {
    *** isHardwareConfigured(...);
}

# ─── Retrofit ───────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**

# ─── ExoPlayer ──────────────────────────────────────────────────
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# ─── Lottie ─────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ─── Room ───────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ─── DataStore ──────────────────────────────────────────────────
-keepclassmembers class * extends androidx.datastore.preferences.core.protobuf.GeneratedMessageLite {
    <fields>;
}

# ─── JitPack / ImagePicker ──────────────────────────────────────
-keep class com.github.dhaval2404.imagepicker.** { *; }
-dontwarn com.github.dhaval2404.imagepicker.**

# ─── Shimmer ────────────────────────────────────────────────────
-keep class com.facebook.shimmer.** { *; }

# ─── WorkManager ────────────────────────────────────────────────
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ─── Remove logging in release ──────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
