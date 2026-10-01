#!/usr/bin/env python3
"""Unduh dependensi Compose/AndroidX untuk build HubGitV.

AndroidX artifact >= 1.7 sudah KMP: nama file memakai suffix `-android`
(Contoh ui-android-1.9.0.aar) dan memuat classes.jar di dalam AAR.
Script ini mencoba kandidat nama secara berurutan dan memverifikasi isi AAR
sebelum disimpan, supaya file korup tidak ikut terpakai.
"""
import os
import subprocess
import sys
import time
import zipfile

G = "https://dl.google.com/dl/android/maven2"
M = "https://repo1.maven.org/maven2"

AAR = [
    ("androidx.compose.ui", "ui", "1.9.0"),
    ("androidx.compose.ui", "ui-graphics", "1.9.0"),
    ("androidx.compose.ui", "ui-text", "1.9.0"),
    ("androidx.compose.ui", "ui-unit", "1.9.0"),
    ("androidx.compose.ui", "ui-util", "1.9.0"),
    ("androidx.compose.ui", "ui-geometry", "1.9.0"),
    ("androidx.compose.foundation", "foundation", "1.9.0"),
    ("androidx.compose.foundation", "foundation-layout", "1.9.0"),
    ("androidx.compose.material3", "material3", "1.3.2"),
    ("androidx.compose.runtime", "runtime", "1.9.0"),
    ("androidx.compose.runtime", "runtime-saveable", "1.9.0"),
    ("androidx.activity", "activity-compose", "1.10.1"),
    ("androidx.activity", "activity-ktx", "1.10.1"),
    ("androidx.lifecycle", "lifecycle-viewmodel-compose", "2.9.0"),
    ("androidx.lifecycle", "lifecycle-runtime-ktx", "2.9.0"),
    ("androidx.lifecycle", "lifecycle-viewmodel-ktx", "2.9.0"),
    ("androidx.lifecycle", "lifecycle-runtime-compose", "2.9.0"),
    ("androidx.lifecycle", "lifecycle-viewmodel", "2.9.0"),
    ("androidx.lifecycle", "lifecycle-runtime", "2.9.0"),
    ("androidx.annotation", "annotation-experimental", "1.5.1"),
    ("androidx.collection", "collection", "1.5.0"),
    ("androidx.savedstate", "savedstate", "1.2.1"),
    ("androidx.core", "core-ktx", "1.16.0"),
    ("androidx.security", "security-crypto", "1.1.0-alpha06"),
]

JAR = [
    ("androidx.annotation", "annotation", "1.9.1", "jar"),
    ("org.jetbrains.kotlinx", "kotlinx-coroutines-android", "1.10.2", "jar"),
    ("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.10.2", "jar"),
    ("org.jetbrains.kotlinx", "kotlinx-serialization-json-jvm", "1.8.1", "jar"),
]


def valid_aar(path):
    try:
        with zipfile.ZipFile(path) as z:
            names = z.namelist()
            if any(n == "classes.jar" for n in names):
                return True
            # AAR KMP pernah menaruh classes di 'classes.jar' persis; cek apa pun
            return any(n.endswith("classes.jar") for n in names)
    except zipfile.BadZipFile:
        return False


def valid_jar(path):
    try:
        with zipfile.ZipFile(path) as z:
            return any(n.endswith(".class") for n in z.namelist())
    except zipfile.BadZipFile:
        return False


def fetch(url, dest, check):
    for attempt in range(4):
        res = subprocess.run(
            ["curl", "-sL", "--connect-timeout", "15", "-o", dest + ".tmp", "-w", "%{http_code}", url],
            capture_output=True, text=True,
        )
        code = res.stdout.strip()[-3:]
        if code == "200" and os.path.exists(dest + ".tmp") and check(dest + ".tmp"):
            os.replace(dest + ".tmp", dest)
            return os.path.getsize(dest)
        if os.path.exists(dest + ".tmp"):
            os.remove(dest + ".tmp")
        time.sleep(1.5 * (attempt + 1))
    return None


def main():
    libs = os.path.join(os.path.dirname(os.path.abspath(__file__)), "libs")
    os.makedirs(libs, exist_ok=True)
    failed = []
    for group, art, ver, ext in [(g, a, v, "aar") for g, a, v in AAR] + JAR:
        base = G if group.startswith("androidx") else M
        path = group.replace(".", "/")
        checker = valid_aar if ext == "aar" else valid_jar
        names = [f"{art}-android-{ver}.{ext}", f"{art}-{ver}.{ext}"]
        # buang kandidat duplikat dan file basi dari percobaan sebelumnya
        for stale in os.listdir(libs):
            if stale.startswith(art + "-") and stale not in names:
                try:
                    if not checker(os.path.join(libs, stale)):
                        os.remove(os.path.join(libs, stale))
                        print(f"  - buang korup {stale}")
                except Exception:
                    pass
        done = None
        for cand in names:
            dest = os.path.join(libs, cand)
            if os.path.exists(dest) and checker(dest):
                done = os.path.getsize(dest)
                print(f"  = {cand} {done}")
                break
            size = fetch(f"{base}/{path}/{art}/{ver}/{cand}", dest, checker)
            if size:
                done = size
                print(f"  + {cand} {size}")
                break
        if done is None:
            failed.append(f"{group}:{art}:{ver}")
    if failed:
        print("GAGAL: " + ", ".join(failed), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
