# Keep kotlinx.serialization serializers for the document model
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.sketchbook.app.**$$serializer { *; }
-keepclassmembers class com.sketchbook.app.** { *** Companion; }
-keepclasseswithmembers class com.sketchbook.app.** { kotlinx.serialization.KSerializer serializer(...); }
