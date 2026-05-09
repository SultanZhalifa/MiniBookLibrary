# Mini Book Library - ProGuard rules

# Preserve generics, annotations and inner classes used by libraries via reflection.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Keep stack traces meaningful for crash reporting.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Navigation Component
-keep class androidx.navigation.** { *; }

# Material Components
-keep class com.google.android.material.** { *; }

# Domain models used by JSON backup/restore
-keep class com.example.minibooklibrary.data.local.entity.** { *; }
-keep class com.example.minibooklibrary.domain.** { *; }

# Coil
-keep class coil.** { *; }
-dontwarn coil.**
