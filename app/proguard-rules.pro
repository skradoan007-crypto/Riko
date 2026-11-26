# Keep JNI bridge methods called from libnexmesh_native.so
-keep class com.rikodev.sync.nexmesh.core.nativeengine.NativeEngineBridge { *; }
-keepclassmembers class * {
    native <methods>;
}
# Keep kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.rikodev.sync.nexmesh.core.protocol.model.** { *; }
