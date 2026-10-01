set -uo pipefail
G=https://dl.google.com/dl/android/maven2
LIBS="$(cd "$(dirname "$0")" && pwd)/libs"
LIST="androidx.compose.ui|ui|1.9.0
androidx.compose.ui|ui-graphics|1.9.0
androidx.compose.ui|ui-text|1.9.0
androidx.compose.ui|ui-unit|1.9.0
androidx.compose.ui|ui-util|1.9.0
androidx.compose.ui|ui-geometry|1.9.0
androidx.compose.foundation|foundation|1.9.0
androidx.compose.foundation|foundation-layout|1.9.0
androidx.compose.material3|material3|1.3.2
androidx.compose.runtime|runtime|1.9.0
androidx.compose.runtime|runtime-saveable|1.9.0
androidx.activity|activity-compose|1.10.1
androidx.activity|activity-ktx|1.10.1
androidx.lifecycle|lifecycle-viewmodel-compose|2.9.0
androidx.lifecycle|lifecycle-runtime-ktx|2.9.0
androidx.lifecycle|lifecycle-viewmodel-ktx|2.9.0
androidx.lifecycle|lifecycle-runtime-compose|2.9.0
androidx.annotation|annotation-experimental|1.5.1
androidx.collection|collection|1.5.0
androidx.savedstate|savedstate|1.2.1
androidx.core|core-ktx|1.16.0
androidx.security|security-crypto|1.1.0-alpha06"
get() { # url dest
  curl -sfL --retry 4 --retry-delay 2 -o "$2.tmp" "$1" || return 1
  unzip -l "$2.tmp" >/dev/null 2>&1 || { rm -f "$2.tmp"; return 1; }
  grep -q classes.jar <(unzip -l "$2.tmp") || { rm -f "$2.tmp"; return 1; }
  [ "$(stat -c%s "$2.tmp")" -gt 20000 ] || { rm -f "$2.tmp"; return 1; }
  mv "$2.tmp" "$2"; return 0
}
echo "$LIST" | while IFS='|' read -r g a v; do
  path=$(echo "$g" | tr '.' '/')
  done=0
  for cand in "$a-android-$v" "$a-$v"; do
    t="$LIBS/$cand.aar"
    if get "$G/$path/$a/$v/$cand.aar" "$t"; then
      echo "ok $cand.aar $(stat -c%s "$t")"; rm -f "$LIBS/$a-$v.aar"; done=1; break
    fi
  done
  [ "$done" = 1 ] || echo "FAIL $a-$v"
done
