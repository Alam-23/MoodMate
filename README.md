# MoodMate - Aplikasi Chatbot Empatik dengan AI

MoodMate adalah aplikasi Android yang menggunakan kecerdasan buatan Gemini 2.5 untuk memberikan dukungan emosional melalui percakapan yang empatik. Aplikasi ini menganalisis mood pengguna dan memberikan insight yang berguna untuk kesehatan mental.

## ✨ Fitur Utama

### 1. Chat Empatik 💬
- Chatbot AI yang mendengarkan dengan empati
- Respons yang hangat dan mendukung
- Analisis mood otomatis dari percakapan
- Interface chat yang modern dan nyaman

### 2. Mood Tracker 📊
- Grafik tren mood harian
- Distribusi mood dalam bentuk pie chart
- Insight dan rekomendasi berdasarkan pola mood
- Analisis mood otomatis dari chat

### 3. Journal Pribadi 📝
- Catat perasaan dan pengalaman harian
- Mood indicator untuk setiap entry
- Riwayat jurnal yang lengkap
- UI yang clean dan mudah digunakan

### 4. Pengaturan & Konfigurasi ⚙️
- Setup API Key Gemini
- Informasi aplikasi
- Kebijakan privasi
- Tema dark mode

## 🚀 Setup dan Instalasi

### Prasyarat
1. Android Studio (versi terbaru)
2. SDK Android minimum API level 21
3. API Key Gemini 2.5 dari Google AI Studio

### Langkah Instalasi

1. **Clone atau download project ini**
   ```bash
   git clone <repository-url>
   cd MoodMate
   ```

2. **Buka project di Android Studio**
   - File → Open → Pilih folder MoodMate

3. **Sync project dengan Gradle**
   - Tunggu proses sync selesai
   - Pastikan semua dependencies ter-download

4. **Build dan jalankan aplikasi**
   - Pilih device/emulator
   - Klik Run (Shift+F10)

### Setup API Key Gemini

1. **Dapatkan API Key:**
   - Kunjungi [Google AI Studio](https://makersuite.google.com/app/apikey)
   - Login dengan akun Google
   - Buat API Key baru
   - Copy API Key yang dihasilkan

2. **Setup di aplikasi:**
   - Buka aplikasi MoodMate
   - Pergi ke tab "Settings"
   - Tap "API Key Gemini"
   - Masukkan API Key Anda
   - Tap "Simpan"

## 📱 Cara Penggunaan

### 1. Chat dengan AI
- Buka tab "Chat"
- Mulai percakapan dengan menceritakan perasaan Anda
- AI akan merespons dengan empati dan memberikan dukungan
- Mood Anda akan dianalisis secara otomatis

### 2. Lihat Analisis Mood
- Buka tab "Mood"
- Lihat grafik tren mood 7 hari terakhir
- Cek distribusi mood dalam pie chart
- Baca insight dan rekomendasi

### 3. Tulis Jurnal
- Buka tab "Journal"
- Tap tombol "+" untuk menambah entry baru
- Tulis judul dan isi jurnal
- Mood akan terdeteksi otomatis dari isi jurnal

### 4. Kelola Pengaturan
- Buka tab "Settings"
- Atur API Key Gemini
- Lihat informasi aplikasi
- Baca kebijakan privasi

## 🏗️ Arsitektur Aplikasi

### Teknologi yang Digunakan
- **Bahasa:** Java
- **UI Framework:** Android Native (XML layouts)
- **Database:** SQLite (Room ORM)
- **HTTP Client:** OkHttp3
- **JSON Parser:** Gson
- **Charts:** MPAndroidChart
- **AI API:** Google Gemini 2.5 Flash

### Struktur Project
```
app/src/main/java/com/example/moodmate/
├── MainActivity.java              # Activity utama dengan bottom navigation
├── fragments/                     # Fragment untuk setiap tab
│   ├── ChatFragment.java         # Fragment chat dengan AI
│   ├── MoodFragment.java         # Fragment analisis mood
│   ├── JournalFragment.java      # Fragment jurnal pribadi
│   └── SettingsFragment.java     # Fragment pengaturan
├── adapters/                     # RecyclerView adapters
│   ├── ChatAdapter.java          # Adapter untuk chat messages
│   └── JournalAdapter.java       # Adapter untuk journal entries
├── models/                       # Data models
│   ├── ChatMessage.java          # Model untuk chat message
│   ├── JournalEntry.java         # Model untuk journal entry
│   └── MoodEntry.java            # Model untuk mood data
├── services/                     # Services dan utilities
│   └── GeminiAIService.java      # Service untuk komunikasi dengan Gemini AI
└── database/                     # Database helper
    └── DatabaseHelper.java       # SQLite database helper
```

## 🎨 Design dan UI

### Color Scheme
- **Primary:** #7C4DFF (Purple)
- **Background:** #2D2D2D (Dark Gray)
- **Surface:** #3A3A3A (Light Gray)
- **Text Primary:** #FFFFFF (White)
- **Text Secondary:** #B0B0B0 (Light Gray)

### Icons dan Assets
- Material Design Icons
- Custom chat bubbles
- Mood emoji indicators
- Gradient backgrounds

## 🔒 Privasi dan Keamanan

- **Data Lokal:** Semua chat dan jurnal disimpan lokal di device
- **Enkripsi:** Komunikasi dengan API menggunakan HTTPS
- **API Key:** Disimpan dengan aman di SharedPreferences
- **No Cloud Storage:** Tidak ada data yang dikirim ke server eksternal
- **Anonymous:** Tidak ada data pribadi yang dikumpulkan

## 📋 TODO dan Pengembangan

### Fitur yang Akan Datang
- [ ] Notifikasi reminder untuk journaling
- [ ] Export data jurnal ke PDF
- [ ] Mood prediction berdasarkan pola
- [ ] Integration dengan Google Calendar
- [ ] Voice-to-text untuk chat
- [ ] Tema custom dan personalisasi
- [ ] Backup dan restore data
- [ ] Widget untuk mood tracking

### Bugs dan Perbaikan
- [ ] Optimasi performa chat adapter
- [ ] Handling error network yang lebih baik
- [ ] Loading states untuk semua operasi
- [ ] Input validation yang lebih ketat

## 🤝 Kontribusi

Kontribusi sangat diterima! Silakan:
1. Fork repository ini
2. Buat branch feature baru
3. Commit perubahan Anda
4. Push ke branch
5. Buat Pull Request

## 📄 Lisensi

Project ini menggunakan lisensi MIT. Lihat file [LICENSE](LICENSE) untuk detail.

## 📞 Support

Jika Anda mengalami masalah atau memiliki pertanyaan:
- Buat issue di GitHub repository
- Email ke: support@moodmate.app
- Dokumentasi: [Wiki](https://github.com/username/moodmate/wiki)

## 🙏 Acknowledgments

- Google AI untuk Gemini API
- Material Design untuk design guidelines
- MPAndroidChart untuk library charting
- Community Android Developer Indonesia

---

**MoodMate** - Your Empathetic AI Companion for Mental Wellness 💜