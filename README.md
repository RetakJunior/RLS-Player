# RLS Player — Minimalist Yerel Müzik Oynatıcı

RLS Player, Android cihazlardaki yerel ses dosyaları için tasarlanmış; ağ izni istemeyen, takip veya reklam içermeyen **offline-first** bir müzik oynatıcıdır. Gece laciverti (`#010736`) ve sıcak krem (`#FCF1D0`) renk paleti, zarif tipografisi ve AndroidX Media3 tabanlı güçlü ses motoruyla sakin ve premium bir deneyim sunar.

---

## Temel Özellikler

- **Geniş Format Desteği:** Android MediaStore üzerinden `mp3`, `m4a`, `aac`, `flac`, `wav`, `ogg`, `opus` ve cihaz sağlayıcısının desteklediği tüm yerel ses formatlarını tarar.
- **Kapsamlı Kütüphane Görünümleri:**
  - **Şarkılar:** Anlık filtreleme, arama ve sıralama seçenekleri (A → Z, Z → A, Sanatçı, Albüm, Eklenme Tarihi, Süre).
  - **Albümler:** İki sütunlu şık kapak ızgarası ve albüm bazında hızlı oynatma.
  - **Sanatçılar:** Sanatçı bazında parçaları gruplama ve tek dokunuşla çalma.
  - **Klasörler:** Cihaz dosya sistemindeki fiziksel dizin hiyerarşisine göre müzik listeleme.
  - **Kitaplık:** Kullanıcı tanımlı çalma listeleri, tek dokunuşla favorileme ve dinleme geçmişi.
- **Media3 & ExoPlayer Altyapısı:** `MediaSessionService` sayesinde ekran kapalıyken, kilit ekranında ve bildirim panelinde kesintisiz arka plan oynatma.
- **Ses Odağı (Audio Focus) ve Güvenlik:** Telefon aramalarında otomatik duraklatma, kulaklık bağlantısı koptuğunda (`Audio Becoming Noisy`) anında durma.
- **Akıllı Uyku Zamanlayıcısı:** 10, 15, 30, 45, 60 dakika veya "Şarkı bitiminde dur" seçenekleri.
- **Gizlilik ve Pil Tasarrufu:** Sıfır internet izni (`INTERNET` izni yoktur), arka planda gereksiz uyanıklık ve pil tüketimi bulunmaz.

---

## Mimari Yapı

Proje, Google'ın önerdiği **Modern Android Architecture (Clean Architecture + MVVM)** prensiplerine göre yapılandırılmıştır:

```
com.rls.player
├── core
│   ├── designsystem   # Night & Cream teması, Inter font ailesi, Material 3 bileşenleri
│   ├── data           # MediaStore scanner, Room DAO ve Entity'leri, DataStore ayarları
│   ├── domain         # Müzik modelleri, repository arayüzleri, UseCase iş mantığı
│   └── player         # Media3 ExoPlayer, MediaSessionService ve Uyku Zamanlayıcısı
├── feature
│   ├── app            # Single-Activity Compose Scaffold, Tab bar, MiniPlayer & FullPlayer
│   ├── library        # Kütüphane sekmeleri, arama ve sıralama mantığı
│   ├── player         # MediaController tabanlı oynatıcı ViewModel'i
│   └── settings       # Kullanıcı tercihleri ve yeniden tarama ekranı
└── di                 # Dagger Hilt bağımlılık enjeksiyon modülleri
```

---

## Kurulum ve Çalıştırma

### Gereksinimler

- JDK 17
- Android SDK Platform 35 & Build-Tools
- Android Studio Ladybug veya daha yenisi

### Komut Satırından Derleme

Debug APK oluşturmak için:

```bash
./gradlew :app:assembleDebug
```

Release paketi (AAB) oluşturmak için:

```bash
./gradlew :app:bundleRelease
```

---

## İzin Yönetimi

RLS Player yalnızca cihazınızdaki müzik dosyalarını okumak için gerekli minimum izinleri talep eder:

- **Android 13+ (API 33 ve üzeri):** `android.permission.READ_MEDIA_AUDIO`
- **Eski Android sürümleri (API 24 - 32):** `android.permission.READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`)
- **Bildirimler:** `android.permission.POST_NOTIFICATIONS` (Medya kontrollerinin bildirim çubuğunda görünmesi için)
- **Arka Plan Servisi:** `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`

---

## Lisans ve Haklar

- Bu uygulama bağımsız bir açık kaynak projesidir. Samsung Music veya üçüncü taraf markalara ait hiçbir tescilli kod veya görsel varlık içermez.
- Arayüzde kullanılan **Inter** yazı tipi, SIL Open Font License (OFL) kapsamında sunulmaktadır.
