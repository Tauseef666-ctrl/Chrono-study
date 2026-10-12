# Keep Room generated implementations.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data model classes used by serialization/backup.
-keep class com.chronostudy.app.data.backup.** { *; }
-keep class com.chronostudy.app.data.local.entity.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
