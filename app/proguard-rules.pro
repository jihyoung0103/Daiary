# Add project specific ProGuard rules here.

# Kotlin coroutines — volatile fields required after R8 minification
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep annotation metadata (used by Firebase Firestore reflection)
-keepattributes Signature
-keepattributes *Annotation*
