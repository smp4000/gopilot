# Moshi
-keep class de.stationpilot.gopilot.data.api.** { *; }
-keepclassmembers class de.stationpilot.gopilot.data.api.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
