# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.flow.** {
    volatile <fields>;
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.* <fields>;
}

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Compose
-keepclassmembers class * extends androidx.compose.ui.node.ModifierNodeElement {
    <fields>;
    <init>(...);
}

# Eclipse JGit - Complete package coverage
-keep class org.eclipse.jgit.api.** { *; }
-keep class org.eclipse.jgit.lib.** { *; }
-keep class org.eclipse.jgit.transport.** { *; }
-keep class org.eclipse.jgit.errors.** { *; }
-keep class org.eclipse.jgit.revwalk.** { *; }
-keep class org.eclipse.jgit.treewalk.** { *; }
-keep class org.eclipse.jgit.diff.** { *; }
-keep class org.eclipse.jgit.merge.** { *; }
-keep class org.eclipse.jgit.storage.** { *; }
-keep class org.eclipse.jgit.util.** { *; }
-keep class org.eclipse.jgit.submodule.** { *; }
-keep class org.eclipse.jgit.ignore.** { *; }
-keep class org.eclipse.jgit.internal.** { *; }
-keep class org.eclipse.jgit.pgm.** { *; }

# JGit SSH JSCH
-keep class org.eclipse.jgit.transport.ssh.jsch.** { *; }

# Apache MINA SSHD (used by JGit SSH)
-keep class org.apache.sshd.** { *; }
-dontwarn org.apache.sshd.**

# BouncyCastle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# JSch (for SSH)
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# Keep model classes
-keep class jamgmilk.fuwagit.domain.model.** { *; }
-keep class jamgmilk.fuwagit.data.local.credential.** { *; }

# Keep missing classes for R8
-keep class com.google.errorprone.annotations.** { *; }
-keep class javax.management.** { *; }
-keep class org.ietf.jgss.** { *; }

# Don't warn about missing platform classes
-dontwarn com.google.errorprone.annotations.**
-dontwarn java.lang.ProcessHandle
-dontwarn java.lang.management.ManagementFactory
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
