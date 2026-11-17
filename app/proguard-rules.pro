# Room, DataStore, Navigation and WorkManager ship their own consumer rules.
# Keep enum names stable: they are stored as text.
-keepclassmembers enum com.macrow.app.** { *; }

# ML Kit finds its parts by class name from the manifest (component registrars) and by reflection.
# Without these rules the release build crashes the moment the barcode scanner opens.
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
