// Top-level build file — plugin declarations only.
// Submodule build files apply the plugins they need.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
}
