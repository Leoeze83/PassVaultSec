# PassVaultSec Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.passvaultsec.app.data.local.entity.** { *; }
-keep class com.passvaultsec.app.domain.model.** { *; }
