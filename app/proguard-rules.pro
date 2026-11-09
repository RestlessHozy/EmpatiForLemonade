# Keep JSON and domain/research models intact during R8 optimization
-keep class org.json.** { *; }
-keep class com.example.empati.data.** { *; }
-keepnames class com.example.empati.** { *; }
-dontwarn org.json.**
