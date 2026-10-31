# Add project specific ProGuard rules here.
# Preserve Room entities, DAOs, and telemetry data models for R8 minification.
-keep class com.growtotype.fpsmeterandhud.model.** { *; }
-keep class com.growtotype.fpsmeterandhud.db.** { *; }
-keepnames class com.growtotype.fpsmeterandhud.overlay.FpsOverlayService
-keepnames class com.growtotype.fpsmeterandhud.tile.FpsTileService
-dontwarn sun.misc.Unsafe
