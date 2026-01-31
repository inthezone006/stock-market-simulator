# Firebase Rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Retrofit & Gson Rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Coil Rules
-keep class coil.** { *; }

# Navigation Rules
-keep class androidx.navigation.** { *; }

# Data Models
-keep class com.rahul.stocksim.model.** { *; }
-keep class com.rahul.stocksim.data.** { *; }