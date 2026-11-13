# ProGuard rules for the launcher app
# Keep the app package mostly intact for launch and UI classes
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'o'

-keep public class com.shreenivas.tvhome.MainActivity

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullParameter(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkParameterIsNotNull(...);
}

-dontwarn kotlin.**
-dontwarn kotlinx.**
