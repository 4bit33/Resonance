# Readable crash traces.
-keepattributes SourceFile,LineNumberTable

# yt-dlp import (youtubedl-android): Jackson maps JSON onto its classes by
# reflection, and its updater unpacks archives with commons-compress.
-keep class com.yausername.** { *; }
-keep class org.apache.commons.compress.archivers.zip.** { *; }
