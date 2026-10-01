# HubGitV

Klien GitHub untuk Android, ditulis Kotlin + Jetpack Compose.

Login memakai Personal Access Token (PAT). Layar login jadi tempat pertama
untuk menempel token, menampilkan/menyembunyikan karakter token, membuka
halaman pembuatan token GitHub, dan penjelasan scope yang dibutuhkan.

## Fitur

- Login PAT, token disimpan terenkripsi di perangkat (EncryptedSharedPreferences, AES256-GCM)
- Dashboard profil dengan shortcut ke semua menu
- Daftar repo (milik sendiri, collaborator, org), dengan filter lokal
- Detail repo: bahasa, branch, topics, README, tombol bintang
- Penjelajahan file dan isi file (base64 dari Contents API)
- Issues: daftar dengan filter open/closed/all, detail, komentar, label, buat issue baru
- Pencarian issue lintas repo
- Commits, releases, notifications, profil publik user
- Tampilan gelap mengikuti palet GitHub

## Struktur

Layout Gradle standar:

| Path | Isi |
| --- | --- |
| `app/src/main/java/com/hubgitv/client/` | Kode Kotlin (Models, GitHubApi, TokenStore, AppViewModel, MainActivity) |
| `app/src/main/java/com/hubgitv/client/ui/` | Compose screens (Login, Repo, Files/Issues/lainnya, komponen bersama) |
| `app/src/main/res/values/` | Nama aplikasi dan tema |
| `app/src/main/AndroidManifest.xml` | Manifest (INTERNET + activity utama) |
| `gradle/libs.versions.toml` | Version catalog (AGP, Kotlin, Compose BOM, library) |
| `.github/workflows/build.yml` | Build APK di GitHub Actions |

## Build di GitHub Actions

Push ke `main` (atau master) memicu workflow `Build Android APK`:

- Output APK ada di artifact `HubGitV-APK`
- Manual: tab Actions → `Build Android APK` → Run workflow → pilih `debug` atau `release`

Build lokal dengan Gradle:

```sh
./gradlew assembleDebug
```

## Build tanpa Gradle (opsional, untuk perangkat tanpa SDK penuh)

`build.sh` membangun APK satu tahap: `kotlinc` → `d8` → `aapt2` → zip → `apksigner`.

```sh
python3 fetch_deps.py   # mengunduh dependensi Compose/AndroidX ke libs/
bash build.sh           # menghasilkan HubGitV.apk
```

`build.sh` butuh `HARNESS_SCRATCH` dan `PREFIX` yang menunjuk toolchain Linux
dan Android SDK build-tools (d8, apksigner, android.jar, kotlinc). Build ini
hanya dipakai untuk uji di perangkat; build resmi lewat Gradle.

## Signing

`build.sh` membuat `keystore.jks` otomatis (password `androidx`, alias `app`)
kalau belum ada. Untuk rilis, buat `release.properties` di root:

```properties
storeFile=keystore.jks
storePassword=...
keyAlias=app
keyPassword=...
```

`app/build.gradle.kts` memakainya hanya kalau file itu ada dan lengkap.

## Lisensi

AGPL-3.0, lihat `LICENSE`.
