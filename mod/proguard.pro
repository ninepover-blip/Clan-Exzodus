-dontshrink
-dontoptimize
-dontwarn **

-adaptclassstrings
-adaptresourcefilecontents **.json,**.accesswidener
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod,Exceptions
-renamesourcefileattribute Source
-keepattributes SourceFile,LineNumberTable

# Fabric discovers this class from fabric.mod.json.
-keep public class tech.onetap.Onetap { public *; }

# Mixin names and annotated members are resolved from the mixin configuration.
-keep class tech.onetap.mixin.** { *; }
-keep @interface org.spongepowered.asm.mixin.**
-keep @org.spongepowered.asm.mixin.Mixin class * { *; }

# Event handlers and module metadata are annotation/reflection driven.
-keep @interface com.google.common.eventbus.Subscribe
-keepclassmembers class * {
    @com.google.common.eventbus.Subscribe <methods>;
}
-keep @interface tech.onetap.module.ModuleInformation
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static final ** *;
}
-keepclasseswithmembers,allowobfuscation class * {
    @tech.onetap.module.ModuleInformation <fields>;
}

# Preserve JNI/serialization contracts and bundled third-party libraries.
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}
-keep class !tech.onetap.** { *; }

-printmapping build/proguard-mapping.txt
-printseeds build/proguard-seeds.txt
