# Mini Book Library

[![CI](https://github.com/SultanZhalifa/MiniBookLibrary/actions/workflows/android.yml/badge.svg)](https://github.com/SultanZhalifa/MiniBookLibrary/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/minSdk-21-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/targetSdk-34-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Material](https://img.shields.io/badge/Material-3-757575?logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![APK](https://img.shields.io/badge/Download-APK-success?logo=android&logoColor=white)](#download)

> A modern, offline-first Android app for tracking your personal book collection — built with Kotlin, MVVM, Room, Coroutines, and Material 3.

Mini Book Library is a portfolio-grade Android application that lets you sign in, build a personal library, track reading progress, and export your collection. It's designed to demonstrate clean architecture, reactive data flow, and a polished Material 3 user interface.

---

## ✨ Features

### Library management
- 📖 **Add, edit, and delete** books with cover images, author, year, category, and personal notes
- 🔍 **Real-time search** across title, author, and category
- 🎯 **Sort and filter** by title, date added, rating, or reading status — preferences persist across launches
- ⬅️ **Swipe-to-delete** with one-tap **Undo** via Snackbar

### Reading tracker
- 📚 Three-state reading status: **Want to Read**, **Currently Reading**, **Finished**
- 📊 **Page progress tracking** with a progress bar for "Currently Reading" books
- ⭐ **5-star rating** per book — sortable, surfaced on the dashboard average

### Dashboard
- 👋 Personalized greeting based on time of day
- 📈 Live stats: total books, average rating, status breakdown (with colored progress bars)
- 🔁 Recently added carousel with horizontal scrolling

### Cover images
- 🖼 Pick a cover from gallery via the modern **Photo Picker** (no permissions needed on Android 13+)
- 💾 Cover bytes copied to internal storage so they survive app restarts and backups
- ⚡ Image loading via [Coil](https://coil-kt.github.io/coil/)

### ISBN auto-fill
- 🔎 Type an ISBN-10 or ISBN-13 on the Add/Edit screen and tap **Auto-fill** — the app calls the public **Google Books API** and pre-populates title, author, year, category, and page count.
- 🛡 Doesn't overwrite anything you've already typed — your local input always wins.
- 📦 Zero new dependencies: uses platform `HttpURLConnection` + `org.json`. No OkHttp, no Retrofit.
- ⏱ 8s timeout so a flaky connection never hangs the UI.

### Data export & backup
- 📄 **Export to PDF** using the platform `PdfDocument` API — no third-party PDF dependency
- 💾 **JSON backup** of your full library, savable to Downloads
- ♻️ **Restore from JSON** with conflict-free re-import (dataset is appended, not overwritten)

### Account & UX
- 🔐 Login & registration with **salted SHA-256 hashing** (passwords never stored in plaintext)
- 🌗 **Theme toggle**: System / Light / Dark, with full Material 3 color tokens
- 🚀 **3-slide onboarding** (ViewPager2 with dot indicators) shown only on first launch
- 🛡 Session persistence with `SharedPreferences` — open the app and pick up where you left off

---

## 🏗 Architecture

The app follows clean **MVVM + Repository** architecture with unidirectional data flow:

```
┌────────────────────────────────────────────────────────────────────┐
│                                UI                                  │
│  Onboarding · Auth · Dashboard · BookList · Detail · Edit · Settings│
│                       (Activities + Fragments)                     │
└──────────────────────────────┬─────────────────────────────────────┘
                               │ collects StateFlow
                               ▼
┌────────────────────────────────────────────────────────────────────┐
│                            ViewModels                              │
│  AuthVM · DashboardVM · BookListVM · BookDetailVM · AddEditVM ·     │
│                            SettingsVM                              │
└──────────────────────────────┬─────────────────────────────────────┘
                               │ suspend / Flow
                               ▼
┌────────────────────────────────────────────────────────────────────┐
│                          Repositories                              │
│              UserRepository  ·  BookRepository                     │
└──────────────────────────────┬─────────────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
       ┌──────────────────┐          ┌────────────────────┐
       │   Room Database  │          │ PreferencesManager │
       │  (UserDao,       │          │  (theme, session,  │
       │   BookDao)       │          │   sort/filter)     │
       └──────────────────┘          └────────────────────┘
```

**Why this shape:**
- **Single source of truth** — Room emits `Flow<List<BookEntity>>`. The list, dashboard, and counters subscribe independently.
- **Repository hides storage details** — switching from Room to a remote API later wouldn't change a single ViewModel.
- **State is derived, not pushed** — every UI screen consumes a `StateFlow<UiState>`. Combined with `repeatOnLifecycle`, the UI auto-pauses and resumes with the lifecycle.
- **Hand-rolled DI via `ServiceLocator`** — Hilt would be overkill for one module; a tiny container is more interview-friendly to read.

---

## 🧰 Tech stack

| Layer            | Technology |
| ---------------- | ---------- |
| Language         | Kotlin 2.0 |
| Architecture     | MVVM + Repository, single-Activity + Navigation Component |
| Async            | Kotlin Coroutines + Flow / StateFlow |
| Database         | Room 2.6 (KSP) |
| UI               | Material 3, ViewBinding, ConstraintLayout, ViewPager2 |
| Image loading    | Coil 2.7 |
| Navigation       | Jetpack Navigation Component (Safe Args) |
| Networking       | Platform `HttpURLConnection` + `org.json` (Google Books API) |
| PDF export       | Android `PdfDocument` (platform) |
| Backup format    | Hand-rolled JSON via `org.json` |
| Testing          | JUnit 4, MockK, Turbine, kotlinx-coroutines-test, Room testing |
| CI               | GitHub Actions (build + unit tests + APK artifact) |

---

## 📁 Module structure

```
app/src/main/java/com/example/minibooklibrary/
├── MiniBookLibraryApp.kt            # Application entry — applies persisted theme
├── di/
│   └── ServiceLocator.kt            # Hand-rolled DI container
├── domain/
│   ├── ReadingStatus.kt             # Want / Reading / Finished enum
│   ├── SortOrder.kt                 # Title/date/rating sorts
│   └── StatusFilter.kt              # Library list filter
├── data/
│   ├── local/
│   │   ├── BookDatabase.kt          # Room database singleton
│   │   ├── dao/{UserDao, BookDao}   # Data access objects (suspend + Flow)
│   │   └── entity/{User, Book}      # @Entity-annotated tables
│   ├── preferences/
│   │   └── PreferencesManager.kt    # Typed wrapper around SharedPreferences
│   ├── remote/
│   │   └── GoogleBooksService.kt    # ISBN lookup via Google Books, parser separated for tests
│   └── repository/
│       ├── UserRepository.kt        # Auth + session
│       ├── BookRepository.kt        # CRUD + reactive reads
│       └── AuthResult.kt            # Sealed result type
├── ui/
│   ├── RouterActivity.kt            # First-screen routing decision
│   ├── onboarding/                  # 3-slide ViewPager2 onboarding
│   ├── auth/                        # AuthActivity + Login/Register fragments
│   ├── main/MainActivity.kt         # Single-Activity host with bottom nav
│   ├── dashboard/                   # Home screen with stats + recent rail
│   ├── books/
│   │   ├── list/                    # Library tab + RecyclerView adapter
│   │   ├── detail/                  # Book detail screen
│   │   └── edit/                    # Add/Edit form
│   ├── settings/                    # Theme, backup/restore, PDF export
│   └── common/ViewModelFactory.kt   # Builds ViewModels from ServiceLocator
└── util/
    ├── PasswordHasher.kt            # Salted SHA-256
    ├── ImageStorage.kt              # Copies cover URIs into internal storage
    ├── PdfExporter.kt               # Renders the library to a PDF
    ├── BackupManager.kt             # JSON export / import
    └── Extensions.kt                # Toast, view-visibility, date formatting
```

Tests live under `app/src/test/java/com/example/minibooklibrary/` mirroring the same package structure.

---

## 🧪 Testing

Run the unit test suite:

```bash
./gradlew test
```

What's covered:
- `PasswordHasherTest` — determinism, salt-isolation, verify success/failure
- `EnumStorageTest` — guards `ReadingStatus`, `SortOrder`, `StatusFilter` storage values from accidental rename/reorder
- `UserRepositoryTest` — registration validation, duplicate-username, hash-not-plaintext, login success/failure paths, session persistence
- `BookRepositoryTest` — timestamp stamping on insert/update, dao forwarding
- `GoogleBooksParserTest` — ISBN-lookup JSON parsing (happy path, missing fields, multiple authors, taxonomy splitting, year extraction) plus service-level network and validation paths
- `AuthViewModelTest` — login state transitions, register validation, error consumption, logout event
- `BookListViewModelTest` — search filters, status filters, A-Z sort, sort/filter persistence, swipe-delete + restore
- `AddEditBookViewModelTest` — form validation rules (title, year range, page progress), persistence of new books, edit hydration, ISBN auto-fill (preserves user input, surfaces failures, rejects too-short input)

---

## 🚀 Getting started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK Platform 34

### Clone and run

```bash
git clone https://github.com/SultanZhalifa/MiniBookLibrary.git
cd MiniBookLibrary
./gradlew assembleDebug
```

Then open the project in Android Studio and run on any device/emulator with API 21+.

### First launch flow

1. **Onboarding** — three slides introducing core features (skippable)
2. **Register** an account, then **sign in**
3. Land on the **Dashboard** with empty stats
4. Tap **Add a book** to populate your library
5. From **Settings**, try Light/Dark theme, **Export to PDF**, or **Backup to JSON**

---

## 📸 Screenshots

> Add real device screenshots to `screenshots/` and they'll show up here. The filenames below are the expected slots.

| Onboarding | Login | Dashboard |
| :---: | :---: | :---: |
| ![Onboarding](screenshots/01_onboarding.png) | ![Login](screenshots/02_login.png) | ![Dashboard](screenshots/03_dashboard.png) |

| Library | Book detail | Add / Edit |
| :---: | :---: | :---: |
| ![Library](screenshots/04_library.png) | ![Detail](screenshots/05_detail.png) | ![Edit](screenshots/06_edit.png) |

| Settings | Dark mode | PDF export |
| :---: | :---: | :---: |
| ![Settings](screenshots/07_settings.png) | ![Dark mode](screenshots/08_dark_mode.png) | ![PDF](screenshots/09_pdf.png) |

---

## 🗺 Roadmap

- [x] ISBN auto-fill via Google Books API
- [ ] **Barcode scanner** for ISBN (ML Kit) — feeds the existing auto-fill flow
- [ ] Cloud sync via Firebase or a self-hosted backend
- [ ] Reading streak / analytics charts (MPAndroidChart)
- [ ] Book recommendations based on rating history
- [ ] Multi-user library sharing
- [ ] Widget for "Currently Reading"

---

## 📥 Download

Pre-built APKs will be attached to GitHub Releases. To produce your own:

```bash
./gradlew assembleRelease
# APK at: app/build/outputs/apk/release/app-release.apk
```

For an unsigned debug build:

```bash
./gradlew assembleDebug
```

---

## 🙋 About

Built by **Sultan Zhalifunnas Musyaffa** as a portfolio piece showcasing modern Android development practices. The original Java prototype has been fully rewritten in Kotlin with a clean MVVM architecture, Room migration, and a Material 3 redesign.

If this project helped you, ⭐ the repo or reach out — feedback is always welcome.

---

## 📄 License

```
MIT License — see LICENSE for details.
```
