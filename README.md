# HubGitV

Klien GitHub untuk Android, ditulis Kotlin + Jetpack Compose, **tanpa Gradle**.

## Kenapa tanpa Gradle

Perangkat target tidak punya Android SDK/Gradle daemon yang layak, jadi proses
build disederhanakan menjadi satu tahap: `kotlinc` → `d8` → `aapt2` → zip → `apksigner`.
Semua alat diambil dari build-tools resmi dan library Compose/AndroidX dari Maven.

## Isi repo

| Path | Isi |
| --- | --- |
| `src/com/hubgitv/client/` | Kode Kotlin (API, model, ViewModel, Compose UI) |
| `res/values/strings.xml` | Nama aplikasi |
| `AndroidManifest.xml` | Manifest minimal (INTERNET + activity utama) |
| `build.sh` | Build APK satu tahap |
| `fetch_deps.py` | Unduh dependensi Compose/AndroidX ke `libs/` |
| `refetch.sh` | Shell equivalent dari `fetch_deps.py` |

`libs/`, `build/`, dan `keystore.jks` tidak ikut di-commit; jalankan `fetch_deps.py`
untuk mengunduh ulang dependensinya.

## Build

```sh
python3 fetch_deps.py   # sekali saja, mengisi libs/
bash build.sh           # menghasilkan HubGitV.apk
```

`build.sh` butuh variabel `HARNESS_SCRATCH` dan `PREFIX` yang menunjuk toolchain
Linux dan Android SDK build-tools (d8, apksigner, android.jar, kotlinc).

## Signing

`build.sh` membuat `keystore.jks` otomatis (password `androidx`, alias `app`)
kalau belum ada, sehingga APK debug bisa langsung di-install. Untuk rilis,
ganti dengan keystore milik sendiri beserta password yang benar.

## Fitur

Login dengan personal access token, dashboard profil, daftar repository (milik
sendiri dan user lain), detail repository, branch, commit, penjelajahan file dan
isi file, README, issues (daftar, detail, komentar, label, buat baru), pencarian
issue, releases, notifications, dan profile.

## Lisensi

AGPL-3.0, lihat `LICENSE`.
