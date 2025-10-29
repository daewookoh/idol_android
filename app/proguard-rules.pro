# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================
# SNS Login SDK ProGuard Rules
# ============================================================

# Line SDK - Keep all Line SDK classes and data binding
-keep class com.linecorp.linesdk.** { *; }
-dontwarn com.linecorp.linesdk.**

# Kakao SDK - Keep all Kakao SDK classes
-keep class com.kakao.sdk.** { *; }
-dontwarn com.kakao.sdk.**

# Facebook SDK - Keep all Facebook SDK classes
-keep class com.facebook.** { *; }
-dontwarn com.facebook.**

# Google Sign-In - Keep all Google auth classes
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**