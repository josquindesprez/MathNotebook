# Navigation Compose type-safe routes (kotlinx.serialization)
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class it.lectio.bibbia.**$$serializer { *; }
-keepclassmembers class it.lectio.bibbia.** {
    *** Companion;
}
-keepclasseswithmembers class it.lectio.bibbia.** {
    kotlinx.serialization.KSerializer serializer(...);
}
