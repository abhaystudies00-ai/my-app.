# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep class com.screentimetracker.data.model.** { *; }
-keep class com.github.mikephil.charting.** { *; }
