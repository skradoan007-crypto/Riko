-keep class com.rikodev.music.android.** { *; }
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Extra safety margin for minifying the debug build too — these libraries
# ship their own consumer rules via AAR metadata, but keeping this
# explicit avoids surprises from reflection-based lookups.
-keep class dagger.hilt.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
