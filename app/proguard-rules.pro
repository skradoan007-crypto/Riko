# VyRox Release R8 Keep Rules
-keepattributes *Annotation*, InnerClasses, Signature
-keep class com.rikodev.vyrox.core.model.** { *; }
-keep,includedescriptorclasses class com.rikodev.vyrox.core.model.**$$serializer { *; }
-keepclassmembers class com.rikodev.vyrox.core.model.** { *** Companion; }
-keepclasseswithmembers class com.rikodev.vyrox.core.model.** { kotlinx.serialization.KSerializer serializer(...); }
-dontwarn androidx.media3.**
