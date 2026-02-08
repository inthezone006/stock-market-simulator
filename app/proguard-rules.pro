# Firebase Rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Retrofit & Gson Rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep interface retrofit2.** { *; }

# Fix for ClassCastException in Retrofit generic types
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve our API interfaces and data models
-keep class com.rahul.stocksim.data.FinnhubApi { *; }
-keep class com.rahul.stocksim.data.Finnhub* { *; }
-keep class com.rahul.stocksim.model.** { *; }

# Coil Rules
-keep class coil.** { *; }

# Navigation Rules
-keep class androidx.navigation.** { *; }
