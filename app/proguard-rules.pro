# Firebase Rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Crashlytics
-keep public class com.google.firebase.crashlytics.** { *; }
-keepattributes SourceFile,LineNumberTable

# Retrofit & Gson Rules
-keepattributes Signature, Metadata, *Annotation*
-keepattributes EnclosingMethod, InnerClasses
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class com.google.gson.** { *; }

# Fix for ClassCastException in Retrofit suspend functions
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve our API interfaces and data models
-keep interface com.rahul.stocksim.data.FinnhubApi { *; }
-keep class com.rahul.stocksim.data.Finnhub** { *; }
-keep class com.rahul.stocksim.model.** { *; }

# Kotlin Coroutines
-keep class kotlin.coroutines.Continuation { *; }

# Coil Rules
-keep class coil.** { *; }

# Navigation Rules
-keep class androidx.navigation.** { *; }

# Room & WorkManager (Fix for "Failed to create an instance of class androidx.work.impl.WorkDatabase")
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>(...);
}
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-dontwarn androidx.work.impl.WorkDatabase_Impl

# Startup / DataStore / Protobuf potential fixes
-keep class androidx.startup.** { *; }
-keep class androidx.datastore.** { *; }
-keep class * extends androidx.startup.Initializer

# Proguard fix for potential adflow NPE (transitive/removed dependency)
-dontwarn com.adflow.**
-keep class com.adflow.** { *; }
