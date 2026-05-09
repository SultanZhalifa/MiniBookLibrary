# Mini Book Library

[![CI](https://github.com/SultanZhalifa/MiniBookLibrary/actions/workflows/android.yml/badge.svg)](https://github.com/SultanZhalifa/MiniBookLibrary/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/minSdk-21-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/targetSdk-34-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An offline-first Android app for keeping track of the books I read. Sign in, add books with covers and ratings, mark them as want-to-read / reading / finished, and export the whole library to PDF or JSON when you need to.

I started this as a small Java + raw SQLite project for class. After it worked, I rewrote it from scratch in Kotlin with MVVM, Room, Coroutines, and Material 3 so I had something I could actually point to in interviews.

## Features

Library:

- Add, edit, and delete books with title, author, year, category, cover image, and personal notes.
- Live search across title, author, and category as you type.
- Sort by title, date added, or rating. Filter by reading status. Both choices stick across launches.
- Swipe a row to delete with an Undo snackbar so accidental swipes don't lose data.

Reading tracker:

- Three reading states: Want to Read, Currently Reading, Finished.
- Page progress (current / total) for books you're actively reading, with a progress bar.
- 1 to 5 star rating per book, sortable, also rolled up into a dashboard average.

Dashboard:

- Greeting that changes with time of day.
- Total count, average rating, and a status breakdown with colored progress bars.
- Recently added books in a horizontal rail at the bottom.

Cover images:

- Pick from gallery using the platform Photo Picker, which doesn't need any runtime permission on Android 13+.
- The picked bytes are copied into internal storage so the cover survives reinstalls and Auto Backup.
- Image loading via Coil.

ISBN auto-fill:

- Type a 10 or 13 digit ISBN on the Add/Edit screen and tap Auto-fill. The app calls the public Google Books API and fills in title, author, year, category, and page count.
- It only fills fields you haven't typed in yet. Anything you've already entered is left alone.
- No new dependencies for this. It uses `HttpURLConnection` and `org.json` which are both on the platform.
- 8 second timeout so a flaky connection doesn't sit and spin.

Export and backup:

- Export the whole library to PDF using the platform `PdfDocument` API. Saves to Downloads.
- JSON backup with the same approach. Pick the file, open it, mail it to yourself.
- Restore appends imported books rather than replacing the existing ones, so you can merge backups without losing anything.

Account and UX:

- Login and registration with salted SHA-256 password hashing. Plaintext is never persisted.
- Theme picker: Follow system, Light, or Dark. Uses Material 3 color tokens so both modes look right.
- Three-slide onboarding on first launch, ViewPager2 with dot indicators. Skippable.
- Session is held in `SharedPreferences` so the app opens straight to the dashboard once you've signed in.

## Architecture

Single-activity for the post-login app, plus separate activities for onboarding and auth so each lives in its own navigation graph. State flows in one direction: Room emits data, the ViewModel shapes it into a `StateFlow<UiState>`, the Fragment collects it.

```
UI (Activities + Fragments)
  collects StateFlow
       |
ViewModels
  AuthViewModel, DashboardViewModel, BookListViewModel,
  BookDetailViewModel, AddEditBookViewModel, SettingsViewModel
       |
  suspend / Flow
       |
Repositories
  UserRepository, BookRepository
       |
       +-- Room (UserDao, BookDao)
       +-- PreferencesManager (theme, session, last sort/filter)
       +-- GoogleBooksService (ISBN lookup)
```

A few things worth calling out:

- Room is the source of truth. The dashboard, library list, and counters each subscribe to their own `Flow` from the same DAOs and re-render independently. Nothing is pushed; everything is derived.
- The Repository layer is the only thing that knows about Room. If I ever swap to a remote API, no ViewModel needs to change.
- DI is a hand-rolled `ServiceLocator`. Hilt would have been fine but felt heavy for one module, and a small container is easier for someone to read in 30 seconds.
- The `GoogleBooksParser` is split off from the service so I can hit it with JSON fixtures in a JVM unit test without touching the network.

## Tech stack

| Layer | What |
| --- | --- |
| Language | Kotlin 2.0 |
| Architecture | MVVM with Repository, single-Activity + Navigation Component |
| Async | Kotlin Coroutines + Flow / StateFlow |
| Database | Room 2.6 (KSP) |
| UI | Material 3, ViewBinding, ConstraintLayout, ViewPager2 |
| Image loading | Coil 2.7 |
| Networking | `HttpURLConnection` + `org.json` (no extra deps) |
| PDF export | Android `PdfDocument` |
| Tests | JUnit 4, MockK, Turbine, kotlinx-coroutines-test, Room testing |
| CI | GitHub Actions (build, unit tests, debug APK artifact) |

## Project layout

```
app/src/main/java/com/example/minibooklibrary/
  MiniBookLibraryApp.kt           Application class, applies persisted theme on startup
  di/ServiceLocator.kt            Hand-rolled DI container
  domain/
    ReadingStatus.kt              Want / Reading / Finished
    SortOrder.kt                  Sort options
    StatusFilter.kt               Library filter
    BookLookup.kt                 Result type for ISBN auto-fill
  data/
    local/
      BookDatabase.kt             Room database singleton
      dao/{UserDao, BookDao}      Suspend functions + Flow reads
      entity/{User, Book}         @Entity tables
    preferences/PreferencesManager.kt   Typed SharedPreferences wrapper
    remote/GoogleBooksService.kt        Google Books client + parser
    repository/{User, Book}Repository   Suspend writes, reactive reads
  ui/
    RouterActivity.kt             Decides where the launcher should go
    onboarding/                   ViewPager2 onboarding
    auth/                         Login + Register fragments
    main/MainActivity.kt          Bottom-nav host
    dashboard/                    Home screen
    books/{list, detail, edit}    Library, detail, add/edit
    settings/                     Theme, backup/restore, PDF export
    common/ViewModelFactory.kt    Builds ViewModels from the ServiceLocator
  util/
    PasswordHasher.kt             Salted SHA-256
    ImageStorage.kt               Copies cover URIs into internal storage
    PdfExporter.kt                PdfDocument-based exporter
    BackupManager.kt              JSON export / import
    Extensions.kt                 Small helpers

app/src/test/...                  Mirrors the same package structure
```

## Running it

You'll need:

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (or the JBR that ships with Android Studio)
- An emulator or device on API 21 or higher

Then:

```bash
git clone https://github.com/SultanZhalifa/MiniBookLibrary.git
cd MiniBookLibrary
./gradlew assembleDebug
```

Open the project in Android Studio and hit Run. On first launch you'll go through onboarding, register an account, and land on an empty dashboard. Tap Add a book to start filling it in. The Settings tab has the theme toggle, PDF export, and backup buttons.

## Tests

```bash
./gradlew testDebugUnitTest
```

The suite has 51 tests across these classes:

- `PasswordHasherTest` covers determinism, salt isolation, and verify success/failure.
- `EnumStorageTest` pins the persisted string values for `ReadingStatus`, `SortOrder`, and `StatusFilter` so a future rename can't silently break existing user data.
- `UserRepositoryTest` covers register validation, duplicate username handling, password hashing (never plaintext), login success and failure, and session persistence.
- `BookRepositoryTest` checks timestamp stamping on insert/update and dao forwarding.
- `GoogleBooksParserTest` walks the JSON parser through happy path, missing fields, multiple authors, taxonomy splitting (Google returns nested categories like `Fiction / Romance`), and year extraction. Plus service-level paths for short ISBNs and null HTTP responses.
- `AuthViewModelTest` covers login state transitions, register validation, error consumption, and the logout event.
- `BookListViewModelTest` covers search, status filter, A-Z sort, sort/filter persistence, swipe-delete, and Undo restore.
- `AddEditBookViewModelTest` covers form validation (title, year range, page progress), saving new books, edit hydration, and ISBN auto-fill including the rule that user-typed input is never overwritten.

## Screenshots

Screenshots live in `screenshots/`. The README references the filenames below; replace the placeholders by running the app, capturing each screen, and saving the PNGs at the same paths.

| Onboarding | Login | Dashboard |
| :---: | :---: | :---: |
| ![Onboarding](screenshots/01_onboarding.png) | ![Login](screenshots/02_login.png) | ![Dashboard](screenshots/03_dashboard.png) |

| Library | Book detail | Add / Edit |
| :---: | :---: | :---: |
| ![Library](screenshots/04_library.png) | ![Detail](screenshots/05_detail.png) | ![Edit](screenshots/06_edit.png) |

| Settings | Dark mode | PDF export |
| :---: | :---: | :---: |
| ![Settings](screenshots/07_settings.png) | ![Dark mode](screenshots/08_dark_mode.png) | ![PDF](screenshots/09_pdf.png) |

See `screenshots/README.md` for the recommended capture process.

## Roadmap

Things I'd like to add next, roughly in priority order:

- Barcode scanner for ISBN using ML Kit, feeding the existing auto-fill flow.
- A "Currently Reading" home-screen widget.
- Reading streaks and a small analytics chart.
- Book recommendations based on rating history.
- Cloud sync (probably Firebase, since it's the lowest-friction option).

## Download

CI uploads a debug APK as a workflow artifact on every push to a tracked branch, so the easiest way to grab a build is from the latest run on the [Actions tab](https://github.com/SultanZhalifa/MiniBookLibrary/actions). For a release build:

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

This is unsigned by default. Set up a signing config in `app/build.gradle.kts` if you want to install on a real device long-term.

## Notes on a few decisions

These are the calls I had to think about more than once while building this:

- **Hand-rolled JSON instead of kotlinx-serialization or Moshi.** The schemas are tiny (book entity, backup file, Google Books volumeInfo) and the dependency cost felt larger than the benefit. The parser tests use the same `org.json` impl that runs on the device, added explicitly as a `testImplementation` so unit tests don't get the stub jar's no-op version.
- **Plain `HttpURLConnection` instead of OkHttp.** One unauthenticated GET endpoint doesn't justify another transitive dependency. The `HttpClient` interface in `GoogleBooksService` makes the network layer trivial to fake in tests.
- **`ServiceLocator` instead of Hilt.** I weighed both. For a single-module app where I wanted readers to follow the dependency graph by clicking through the source rather than reading codegen, a 60-line `object` won.
- **Reactive read, suspend write.** Repositories return cold `Flow` for reads (Room handles invalidation) and `suspend` for writes. ViewModels collect with `repeatOnLifecycle` so the UI doesn't waste work in the background.
- **Salted SHA-256 instead of bcrypt or Argon2.** The realistic threat is somebody pulling the SQLite file off a non-rooted phone. Salted SHA-256 defeats rainbow tables, which covers that. A real production app should use a memory-hard KDF.
- **Schema export off for now.** Room's schema export emits CI-time JSON snapshots that are useful when you ship migrations. I've left it off until v2 of the schema, with a comment in `BookDatabase.kt` explaining when to flip it back on.

## License

MIT. See [LICENSE](LICENSE).
