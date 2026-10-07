# Remove verbose/debug/info logging from release builds (works because the build
# uses proguard-android-optimize.txt). Warnings and errors are kept.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}