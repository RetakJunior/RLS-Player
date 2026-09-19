# RLS Player — Kalite Güvence ve Test Kontrol Listesi (QA Checklist)

Bu kontrol listesi, RLS Player'ın üretim ortamı dağıtımı (Play Store / APK Release) öncesinde doğrulanması gereken kritik fonksiyonel ve donanımsal test senaryolarını içerir.

---

### 1. İlk Kurulum ve İzin Akışı

- [ ] **Android 13+ (API 33+) İzin Testi:** Uygulama açılışında `READ_MEDIA_AUDIO` izni istenir, izin verildiğinde MediaStore taraması otomatik başlar.
- [ ] **Eski Android Sürümleri (API 24-32) İzin Testi:** Yalnızca `READ_EXTERNAL_STORAGE` izni talep edilir.
- [ ] **İzin Reddi (Permission Denied):** İzin reddedildiğinde uygulama çökmez; "Müziğin, cihazında kalır" açıklayıcı boş ekranı ve izin verme butonu gösterilir.
- [ ] **Bildirim İzni (POST_NOTIFICATIONS):** Android 13+ bildirim izni istendiğinde medya kontrollerinin bildirim çekmecesine düşmesi doğrulanır.

---

### 2. Kütüphane Taraması ve Performans

- [ ] **Boş Kütüphane Testi:** Cihazda hiçbir ses dosyası yokken "Müzik Bulunamadı" boş durum ekranı gösterilir, hata fırlatılmaz.
- [ ] **Büyük Kütüphane Testi (5000+ Parça):** MediaStore sorgusu arka planda (`Dispatchers.IO`) parçalı işlenir; `LazyColumn` scroll sırasında 60/120 FPS akıcılık korunur.
- [ ] **Bozuk / Sıfır Süreli Medya Testi:** Süresi 5 saniyeden kısa ses efektleri, zil sesleri veya bozuk dosyalar filtrelenir; eksik metadata durumunda "Bilinmeyen Parça/Sanatçı" güvenli şekilde basılır.
- [ ] **Yeniden Tarama (Rescan):** Müzik çalarken Ayarlar menüsünden yeniden tarama başlatıldığında çalan parça kesilmez, yeni dosyalar Room veritabanına eklenir.

---

### 3. Oynatma ve Ses Deneyimi

- [ ] **Şarkı Başlatma:** Herhangi bir parçaya dokunulduğunda kuyruk listesi ExoPlayer'a yüklenir, ilk parça anında çalmaya başlar ve mini player belirir.
- [ ] **Mini Player Kontrolleri:** Oynat/duraklat ve sonraki parça butonları anında tepki verir; mini bara dokunulduğunda tam ekran player açılır.
- [ ] **Tam Ekran Kontroller:** Seek bar ileri/geri kaydırma, önceki/sonraki parça, karıştırma (shuffle) ve tekrar (OFF/ALL/ONE) modları doğru çalışır.
- [ ] **Kuyruk Bottom Sheet:** Çalma sırasındaki parçalar listelenir, mevcut parça belirgindir.

---

### 4. Arka Plan ve Sistem Entegrasyonu

- [ ] **Arka Plan Çalma:** Uygulama ana ekrana atıldığında veya ekran kilitlendiğinde müzik kesilmeden çalmaya devam eder (`MediaSessionService`).
- [ ] **Bildirim & Kilit Ekranı:** MediaStyle bildiriminde parça adı, sanatçı, albüm kapağı ve medya butonları (önceki, oynat/duraklat, sonraki) çalışır.
- [ ] **Kulaklık Çıkartma (Audio Becoming Noisy):** Kulaklık kablosu veya Bluetooth bağlantısı kesildiğinde ses hoparlörden aniden patlamaz; ExoPlayer otomatik duraklar.
- [ ] **Donanım & Bluetooth Medya Tuşları:** Kulaklık üzerindeki oynat/duraklat ve parça atlama butonları doğru komutları iletir.
- [ ] **Ses Odağı (Audio Focus):** Telefon araması geldiğinde müzik duraklar; arama bittiğinde kaldığı yerden devam eder. Başka bir uygulama (örn. navigasyon) ses çaldığında ses kısılır (ducking).

---

### 5. Koleksiyon ve Ayarlar

- [ ] **Favori İşlemleri:** Kalp ikonuna dokunulduğunda parça favorilere eklenir; Kitaplık sekmesindeki Favoriler listesinde hemen görünür.
- [ ] **Çalma Listesi Yönetimi:** Yeni çalma listesi oluşturma, listeleri görüntüleme ve silme işlemleri Room veritabanında kalıcı olarak saklanır.
- [ ] **Son Çalınanlar:** Oynatılan şarkılar geçmiş tablosuna kaydedilir ve Kitaplık sekmesinden tekrar çalınabilir.
- [ ] **Uyku Zamanlayıcısı:** 15 dk veya "Şarkı bitiminde dur" seçildiğinde süre dolduğunda oynatma nazikçe duraklatılır; arka plan servis kaynakları serbest bırakılır.
- [ ] **Animasyonları Azalt (Reduced Motion):** Tercih DataStore üzerinde saklanır ve uygulama yeniden başlatıldığında korunur.

---

### 6. Güvenlik ve Gizlilik

- [ ] **Sıfır Ağ Trafiği:** Uygulama hiçbir HTTP/HTTPS isteği yapmaz; logcat üzerinden harici IP iletişimi olmadığı doğrulanır.
