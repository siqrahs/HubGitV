# Kotlin/Compose
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.hubgitv.client.** {
    *** Companion;
}
-keepclasseswithmembers class com.hubgitv.client.** {
    kotlinx.serialization.KSerializer serializer(...);
}
