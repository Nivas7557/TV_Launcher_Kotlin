-optimizationpasses 9
-allowaccessmodification
-mergeinterfacesaggressively
-repackageclasses ''

-keep public class com.shreenivas.tvhome.MainActivity
-keep public class com.shreenivas.tvhome.NightNudgeReceiver

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkParameterIsNotNull(...);
    public static void throwUninitializedPropertyAccessException(...);
}

-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
    public static int w(...);
}

-dontwarn kotlin.**
-dontwarn kotlinx.**
