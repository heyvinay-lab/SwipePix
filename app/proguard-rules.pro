# Proguard rules for SwipePix

# Hilt
-dontwarn dagger.hilt.**
-dontwarn in.heyvinay.swipepix.Hilt_*
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Coil
-dontwarn coil3.**

# Keep data classes used with MediaStore
-keep class in.heyvinay.swipepix.data.model.** { *; }

# Compose
-dontwarn androidx.compose.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Keep all serializable classes, companion objects, and generated serializers
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** serializer(...);
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class * implements kotlinx.serialization.KSerializer {
    <init>(...);
    *;
}
-keep class * extends kotlinx.serialization.internal.GeneratedSerializer {
    *;
}
-keepclassmembers class * implements kotlinx.serialization.internal.GeneratedSerializer {
    *;
}

# Keep SwipePix navigation routes explicitly
-keep class in.heyvinay.swipepix.ui.navigation.** { *; }
-keepclassmembers class in.heyvinay.swipepix.ui.navigation.** { *; }

# Room Database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class in.heyvinay.swipepix.data.local.** { *; }
-keepclassmembers class in.heyvinay.swipepix.data.local.** { *; }

# UI State and presentation models
-keep class in.heyvinay.swipepix.ui.cleanup.** { *; }
-keepclassmembers class in.heyvinay.swipepix.ui.cleanup.** { *; }
-keep class in.heyvinay.swipepix.ui.albums.** { *; }
-keepclassmembers class in.heyvinay.swipepix.ui.albums.** { *; }

