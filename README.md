<p align="center">
  <img src="https://img.shields.io/badge/📄-EasyPDFKit-blue?style=for-the-badge&labelColor=000" alt="EasyPDFKit - Android PDF Viewer Library" />
</p>

<p align="center">
  <a href="https://jitpack.io/#vishalroy000/EasyPDFKit"><img src="https://jitpack.io/v/vishalroy000/EasyPDFKit.svg" alt="JitPack" /></a>
  <img src="https://img.shields.io/badge/API-21%2B-brightgreen.svg" alt="Android API 21+" />
  <img src="https://img.shields.io/badge/Kotlin-100%25-7F52FF.svg" alt="Kotlin" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" />
  <img src="https://img.shields.io/badge/Pdfium-Native%20C++-orange.svg" alt="Pdfium Native Rendering" />
</p>

<h3 align="center">Android PDF Viewer Library — Lightweight, Fast & Easy to Integrate</h3>

<p align="center">
A modern, open-source <b>PDF viewer library for Android</b> built on native Pdfium rendering engine with Kotlin Coroutines.<br/>
Drop-in replacement for the abandoned <code>AndroidPdfViewer (barteksc)</code> and <code>android-pdf-viewer</code>.<br/>
Display PDF files in your Android app with <b>just 3 lines of code</b>.
</p>

<p align="center">
  <b>android pdf viewer • pdf reader sdk • pdf library android • kotlin pdf viewer • pdfium android</b>
</p>

---

## Why EasyPDFKit?

Looking for a **PDF viewer for Android** that actually works? Most Android PDF libraries are either abandoned (like barteksc/AndroidPdfViewer), bloated with unnecessary features, or crash on large documents. EasyPDFKit is built from scratch to solve these problems:

- **Just works** — display any PDF document with 3 lines of Kotlin code, zero configuration needed
- **Never crashes** — multi-resolution rendering with bounded memory; tested on 1000+ page PDF documents without OOM
- **Stays lightweight** — zero forced dependencies beyond Pdfium native; keeps your APK size small
- **Modern stack** — Kotlin-first, Coroutines, Lifecycle-aware, AndroidX, minSdk 21
- **Actively maintained** — unlike abandoned alternatives like barteksc, AndroidPdfViewer, or MuPDF viewer

### EasyPDFKit vs Other Android PDF Libraries

| Feature | EasyPDFKit | barteksc/AndroidPdfViewer | MuPDF | WebView |
|---------|-----------|--------------------------|-------|---------|
| **Maintained** | ✅ Active | ❌ Abandoned (2019) | ⚠️ Complex | ✅ |
| **Kotlin-first** | ✅ | ❌ Java | ❌ C/Java | N/A |
| **Memory safe** | ✅ LRU cache | ⚠️ OOM on large PDFs | ✅ | ⚠️ |
| **Night mode** | ✅ Built-in | ❌ Manual | ❌ | ❌ |
| **Password PDFs** | ✅ | ✅ | ✅ | ❌ |
| **APK size impact** | ~3 MB | ~3 MB | ~15 MB | 0 MB |
| **Setup complexity** | 3 lines | 5 lines | Complex | Simple |
| **Zoom & gestures** | ✅ Smooth | ⚠️ Basic | ✅ | ⚠️ |
| **URL loading** | ✅ Built-in | ❌ Manual | ❌ | ✅ |

---

## Features

| Category | What you get |
|----------|-------------|
| **PDF Rendering** | Vertical continuous scroll, fit-width layout, multi-resolution bitmap rendering (1x/2x/4x) using native Pdfium engine |
| **Touch Gestures** | Pinch-to-zoom, double-tap zoom, fling with inertia, smooth zoom animations, configurable zoom range |
| **Memory Management** | LRU bitmap cache (1/6 of heap), automatic bitmap recycling, no OutOfMemoryError on large PDFs |
| **PDF Sources** | Load from `File`, `Uri`, `Asset`, `ByteArray`, `URL` — including Firebase Storage download URLs, Google Drive links, S3 pre-signed URLs |
| **Network Loading** | Built-in HTTP downloader with disk cache + progress callback, or plug in OkHttp/Ktor/Retrofit |
| **PDF Security** | Open password-protected / encrypted PDF files with retry callback |
| **Reading Modes** | Night mode (dark mode / color inversion), configurable page spacing, optional page snapping |
| **Lifecycle** | Auto-releases native resources on view detach, coroutine-scoped background rendering |
| **Compatibility** | minSdk 21 (Android 5.0+, 99% devices), Kotlin & Java support, ViewBinding ready, Jetpack Compose compatible via AndroidView |

---

## Installation

### Step 1 — Add JitPack repository

Open your **project-level** `settings.gradle.kts` and add JitPack inside `repositories`:

```kotlin
// settings.gradle.kts (Project level)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }   // ← Add this line
    }
}
```

<details>
<summary>📎 Using Groovy? (settings.gradle)</summary>

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```
</details>

### Step 2 — Add the dependency

Open your **app-level** `build.gradle.kts` and add:

```kotlin
// build.gradle.kts (App module)
dependencies {
 implementation("com.github.vishalroy000:EasyPDFKit:v1.0.0")
}
```

<details>
<summary>📎 Using Groovy? (build.gradle)</summary>

```groovy
dependencies {
   implementation("com.github.vishalroy000.EasyPDFKit:easypdfkit:v1.0.1")
}
```
</details>

### Step 3 — Add Internet permission (only if loading PDF from URL)

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
```

### Step 4 — Sync & Build

Click **"Sync Now"** in Android Studio → Done! ✅

---

## Quick Start — Display a PDF in 3 Lines

### 1. Add EasyPdfView to your XML layout

```xml
<!-- res/layout/activity_pdf.xml -->
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.easypdfkit.EasyPdfView
        android:id="@+id/pdfView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</FrameLayout>
```

### 2. Load a PDF file in your Activity or Fragment

```kotlin
// That's it — 3 lines to display any PDF!
binding.pdfView.fromAsset("sample.pdf")
    .onLoad { pageCount -> Log.d("PDF", "Loaded $pageCount pages") }
    .load()
```

> **Note:** Place your PDF file in `app/src/main/assets/` folder.

---

## Load PDF from Different Sources

### From Assets folder

```kotlin
binding.pdfView.fromAsset("sample.pdf")
    .onLoad { pageCount -> /* PDF loaded successfully */ }
    .onError { error -> /* handle error */ }
    .load()
```

### From URL (Firebase Storage, S3, any HTTP/HTTPS link)

```kotlin
// Works with any direct PDF URL — Firebase Storage, AWS S3, Google Drive, your own server
binding.pdfView.fromUrl("https://firebasestorage.googleapis.com/v0/b/.../sample.pdf?alt=media")
    .onLoad { pageCount ->
        binding.toolbar.subtitle = "$pageCount pages"
    }
    .onPageChange { page, total ->
        binding.pageIndicator.text = "${page + 1} / $total"
    }
    .onError { error ->
        Toast.makeText(this, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
    }
    .load()
```

### From File (internal storage, downloads, external storage)

```kotlin
val pdfFile = File(context.filesDir, "downloaded_report.pdf")
binding.pdfView.fromFile(pdfFile)
    .onLoad { /* PDF ready */ }
    .load()
```

### From Uri (SAF File Picker / content:// / FileProvider)

```kotlin
// Use with Android's built-in file picker
val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    uri?.let {
        binding.pdfView.fromUri(it)
            .onLoad { pages -> /* ready */ }
            .load()
    }
}

// Launch the PDF file picker
pickPdf.launch("application/pdf")
```

### From ByteArray (database, network response, in-memory PDF)

```kotlin
val pdfBytes: ByteArray = // from Room database, Retrofit response, etc.
binding.pdfView.fromBytes(pdfBytes)
    .load()
```

---

## Open Password-Protected PDF Files

EasyPDFKit handles encrypted and password-protected PDF documents:

```kotlin
binding.pdfView.fromFile(encryptedFile)
    .onPasswordRequired { retry ->
        // Show your own password dialog
        val editText = EditText(this).apply {
            hint = "Enter PDF password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(48, 32, 48, 16)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("🔐 Password Required")
            .setMessage("This PDF is password protected.")
            .setView(editText)
            .setPositiveButton("Unlock") { _, _ ->
                retry(editText.text.toString())  // retry with entered password
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }
    .onError { error ->
        Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
    }
    .load()
```

If you already know the password:

```kotlin
binding.pdfView.fromFile(encryptedFile)
    .password("my_secret_password")
    .load()
```

---

## Night Mode / Dark Mode for PDF Reading

```kotlin
// Enable dark mode at load time
binding.pdfView.fromAsset("sample.pdf")
    .nightMode(true)
    .load()

// Toggle dark mode at runtime — no reload needed!
var isNight = false

binding.btnNightMode.setOnClickListener {
    isNight = !isNight
    binding.pdfView.setNightMode(isNight)
}
```

---

## All Configuration Options

```kotlin
binding.pdfView.fromUrl("https://example.com/doc.pdf")

    // ── Document options
    .password("optional_password")     // Pre-set password for encrypted PDFs

    // ── Display options
    .nightMode(false)                  // Dark mode / inverted colors for reading
    .pageSpacingDp(12)                 // Gap between pages in dp (default: 8dp)
    .snapToPage(true)                  // Snap to nearest page after scroll ends

    // ── Zoom options
    .zoomRange(1f, 8f)                 // Min and max zoom levels (default: 1f to 6f)
    .doubleTapZoom(3f)                 // Zoom level on double-tap (default: 2.5f)

    // ── Event listeners
    .onLoad { pageCount ->             // Called when PDF document is loaded
        Log.d("PDF", "Total pages: $pageCount")
    }
    .onPageChange { page, total ->     // Called when visible page changes (0-based)
        indicator.text = "${page + 1} / $total"
    }
    .onZoomChange { zoom ->            // Called when zoom level changes
        Log.d("PDF", "Zoom: ${zoom}x")
    }
    .onTap { x, y ->                   // Called on single tap (return true to consume)
        toggleToolbar()
        true
    }
    .onPasswordRequired { retry ->     // Called when PDF needs a password
        showPasswordDialog { pw -> retry(pw) }
    }
    .onError { throwable ->            // Called on any error
        showError(throwable.message)
    }

    // ── Start loading the PDF
    .load()
```

---

## Programmatic Navigation & Control

```kotlin
// ── Jump to a specific page (0-based index)
binding.pdfView.jumpTo(0)                       // Instant jump to first page
binding.pdfView.jumpTo(9, animated = true)      // Smooth scroll to page 10

// ── Read current state
val currentPage: Int = binding.pdfView.currentPage   // Currently visible page (0-based)
val totalPages: Int = binding.pdfView.pageCount       // Total number of pages
val isReady: Boolean = binding.pdfView.isLoaded       // Is PDF loaded and ready?

// ── Change zoom level programmatically
binding.pdfView.setZoom(2.5f)

// ── Toggle night mode
binding.pdfView.setNightMode(true)

// ── Release resources (auto-called when view is detached from window)
binding.pdfView.recycle()
```

---

## Custom PDF Downloader (OkHttp / Ktor / Retrofit)

The built-in downloader uses `HttpURLConnection` with disk caching — zero extra dependencies. To use OkHttp instead:

```kotlin
// 1. Create your custom downloader
val okHttpDownloader = PdfDownloader { context, url ->
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    val response = client.newCall(request).execute()
    
    val cacheFile = File(context.cacheDir, "pdf_${url.hashCode()}.pdf")
    cacheFile.outputStream().use { output ->
        response.body!!.byteStream().use { input -> input.copyTo(output) }
    }
    cacheFile  // Return the downloaded File
}

// 2. Use it with fromSource()
binding.pdfView.fromSource(
        PdfSource.FromUrl("https://example.com/doc.pdf", okHttpDownloader)
    )
    .onLoad { /* ready */ }
    .load()
```

### PDF Download Progress Listener

```kotlin
// Set before calling .load()
PdfDownloader.progressListener = { bytesRead, totalBytes ->
    val percent = if (totalBytes > 0) (bytesRead * 100 / totalBytes).toInt() else -1
    runOnUiThread {
        progressBar.progress = percent
        progressText.text = "$percent%"
    }
}

binding.pdfView.fromUrl("https://example.com/large_doc.pdf")
    .load()
```

---

## Use with Jetpack Compose

```kotlin
@Composable
fun PdfScreen(pdfUrl: String) {
    AndroidView(
        factory = { context ->
            EasyPdfView(context).apply {
                fromUrl(pdfUrl)
                    .nightMode(false)
                    .onLoad { /* ready */ }
                    .load()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

---

## Complete Activity Example (Copy-Paste Ready)

### XML Layout — `res/layout/activity_pdf_viewer.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.easypdfkit.EasyPdfView
        android:id="@+id/pdfView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/bottomBar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <TextView
        android:id="@+id/pageIndicator"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:background="#CC333333"
        android:paddingHorizontal="16dp"
        android:paddingVertical="6dp"
        android:text="Loading..."
        android:textColor="@android:color/white"
        android:textSize="13sp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <LinearLayout
        android:id="@+id/bottomBar"
        android:layout_width="0dp"
        android:layout_height="56dp"
        android:background="?android:colorBackground"
        android:elevation="8dp"
        android:gravity="center"
        android:orientation="horizontal"
        android:paddingHorizontal="16dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent">

        <Button
            android:id="@+id/btnPrev"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="← Prev" />

        <Button
            android:id="@+id/btnNight"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginHorizontal="8dp"
            android:text="🌙 Night" />

        <Button
            android:id="@+id/btnNext"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Next →" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

### Kotlin Activity — `PdfViewerActivity.kt`

```kotlin
class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var nightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pdfUrl = intent.getStringExtra("pdf_url")
        val pdfAsset = intent.getStringExtra("pdf_asset")

        val configurator = when {
            pdfUrl != null -> binding.pdfView.fromUrl(pdfUrl)
            pdfAsset != null -> binding.pdfView.fromAsset(pdfAsset)
            else -> {
                Toast.makeText(this, "No PDF source", Toast.LENGTH_SHORT).show()
                return
            }
        }

        configurator
            .pageSpacingDp(8)
            .onLoad { pages -> binding.pageIndicator.text = "1 / $pages" }
            .onPageChange { page, total -> binding.pageIndicator.text = "${page + 1} / $total" }
            .onTap { _, _ ->
                val bar = binding.bottomBar
                bar.visibility = if (bar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                true
            }
            .onPasswordRequired { retry ->
                val input = EditText(this).apply {
                    hint = "Password"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setPadding(48, 32, 48, 16)
                }
                MaterialAlertDialogBuilder(this)
                    .setTitle("Password Required")
                    .setView(input)
                    .setPositiveButton("Unlock") { _, _ -> retry(input.text.toString()) }
                    .setNegativeButton("Cancel") { _, _ -> finish() }
                    .show()
            }
            .onError { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() }
            .load()

        binding.btnPrev.setOnClickListener {
            binding.pdfView.jumpTo(binding.pdfView.currentPage - 1, animated = true)
        }
        binding.btnNext.setOnClickListener {
            binding.pdfView.jumpTo(binding.pdfView.currentPage + 1, animated = true)
        }
        binding.btnNight.setOnClickListener {
            nightMode = !nightMode
            binding.pdfView.setNightMode(nightMode)
            binding.btnNight.text = if (nightMode) "☀️ Light" else "🌙 Night"
        }
    }
}
```

### Launch from anywhere in your app

```kotlin
startActivity(Intent(this, PdfViewerActivity::class.java).apply {
    putExtra("pdf_url", "https://example.com/document.pdf")
    // OR: putExtra("pdf_asset", "sample.pdf")
})
```

---

## Java Compatibility

EasyPDFKit works in Java Android projects too:

```java
EasyPdfView pdfView = findViewById(R.id.pdfView);

pdfView.fromAsset("sample.pdf")
    .nightMode(false)
    .pageSpacingDp(8)
    .onLoad(pageCount -> {
        Log.d("PDF", "Loaded " + pageCount + " pages");
    })
    .onPageChange((page, total) -> {
        pageIndicator.setText((page + 1) + " / " + total);
    })
    .onError(error -> {
        Log.e("PDF", "Load failed", error);
    })
    .load();
```

---

## API Reference

### EasyPdfView — Public Methods

| Method | Description |
|--------|-------------|
| `fromFile(File)` | Load PDF from a local file |
| `fromUri(Uri)` | Load PDF from content:// or file:// URI |
| `fromAsset(String)` | Load PDF from app's assets folder |
| `fromBytes(ByteArray)` | Load PDF from raw byte array |
| `fromUrl(String)` | Download and display PDF from any HTTP/HTTPS URL |
| `fromSource(PdfSource)` | Load PDF from any custom PdfSource |
| `jumpTo(page, animated)` | Navigate to specific page (0-based index) |
| `setNightMode(Boolean)` | Toggle dark reading mode at runtime |
| `setZoom(Float)` | Set zoom level programmatically |
| `recycle()` | Release native resources manually |

### Configurator — Builder Options

| Method | Default | Description |
|--------|---------|-------------|
| `.password(String?)` | `null` | Pre-set password for encrypted PDF |
| `.nightMode(Boolean)` | `false` | Enable dark mode / color inversion |
| `.pageSpacingDp(Int)` | `8` | Gap between PDF pages in dp |
| `.snapToPage(Boolean)` | `false` | Snap to nearest page after scrolling |
| `.zoomRange(min, max)` | `1f, 6f` | Allowed zoom range |
| `.doubleTapZoom(Float)` | `2.5f` | Double-tap zoom target level |
| `.onLoad { }` | — | PDF document loaded callback |
| `.onPageChange { }` | — | Page changed callback (0-based) |
| `.onZoomChange { }` | — | Zoom level changed callback |
| `.onTap { }` | — | Single tap callback |
| `.onPasswordRequired { }` | — | Password needed callback |
| `.onError { }` | — | Error callback |
| `.load()` | — | **Start loading** (must be called last) |

### Read-Only Properties

| Property | Type | Description |
|----------|------|-------------|
| `currentPage` | `Int` | Currently visible page (0-based) |
| `pageCount` | `Int` | Total number of pages in PDF |
| `isLoaded` | `Boolean` | Whether PDF document is loaded and ready |

---

## Project Architecture

```
EasyPDFKit/
│
├── easypdfkit/                          # 📦 Library module (published artifact)
│   └── src/main/java/com/easypdfkit/
│       ├── EasyPdfView.kt               # Main custom view — public API
│       ├── core/
│       │   └── PdfEngine.kt             # Pdfium native wrapper (thread-confined)
│       ├── cache/
│       │   └── PageBitmapCache.kt       # LRU bitmap cache (1/6 heap budget)
│       ├── source/
│       │   └── PdfSource.kt             # Sealed class for all PDF input types
│       ├── listeners/
│       │   └── Callbacks.kt             # Event listener interfaces
│       └── util/
│           └── PdfDownloader.kt         # Pluggable HTTP downloader
│
├── sample/                              # 📱 Demo application
│   └── src/main/
│       ├── java/.../MainActivity.kt     # Demo: Asset/URL/Picker/Night mode
│       └── res/layout/activity_main.xml
│
├── build.gradle.kts                     # Root build config
├── settings.gradle.kts                  # Module includes
├── gradle.properties                    # Version & publishing
├── jitpack.yml                          # JitPack build config
├── LICENSE                              # Apache 2.0
└── README.md
```

### How the PDF Rendering Engine Works

1. **Open** — Pdfium opens the PDF via file descriptor (supports password decryption natively)
2. **Layout** — Each page is scaled to fit the view width at zoom=1x; page positions are precomputed
3. **Render** — Visible pages (±1 prefetch) are rendered at zoom-bucketed resolution:
   - zoom ≤ 1.25x → render at 1x width
   - zoom ≤ 2.5x → render at 2x width
   - zoom > 2.5x → render at 4x width (bitmap-scaled beyond)
4. **Cache** — Rendered bitmaps stored in LRU cache sized at 1/6 of max heap
5. **Draw** — Canvas draws cached bitmaps with optional night-mode color filter
6. **Recycle** — On eviction or view detach, bitmaps are recycled and native resources freed

> All Pdfium calls run on a single-threaded coroutine dispatcher because Pdfium is **not thread-safe**.

---

## Roadmap

| Version | Status | Features |
|---------|--------|----------|
| v1.0 | ✅ Released | Core PDF viewer, zoom, gestures, night mode, password support, URL loading, LRU cache |
| v1.1 | 🔜 Next | PDF text search & selection, horizontal scroll mode, region-based hi-res rendering |
| v1.2 | 📋 Planned | Thumbnail sidebar, PDF bookmarks with persistence, dual-page tablet mode |
| v2.0 | 📋 Planned | PDF annotations module (highlight, draw, notes) as separate optional artifact |

---

## Requirements

- Android Studio Arctic Fox or later
- Kotlin 1.9+
- minSdk 21 (Android 5.0 Lollipop)
- targetSdk 34
- JDK 17

---

## Troubleshooting

**Build error: "Could not find com.github.vishalroy000.EasyPDFKit:easypdfkit"**
→ Make sure `maven { url = uri("https://jitpack.io") }` is in your `settings.gradle.kts` repositories block (not just `build.gradle.kts`). Then click Sync.

**Blank white screen when loading PDF**
→ PDF might be downloading. Add `.onError { }` listener to check for network errors. For URL loading, ensure `INTERNET` permission is in your AndroidManifest.xml.

**OutOfMemoryError on very large PDFs**
→ EasyPDFKit handles this automatically with LRU caching. If you still see issues, reduce max zoom: `.zoomRange(1f, 3f)`

**Password dialog not showing for encrypted PDF**
→ Make sure you set `.onPasswordRequired { retry -> }` before calling `.load()`

**PDF not rendering after screen rotation**
→ EasyPDFKit handles orientation changes automatically. If using in a Fragment, ensure the view is not recreated unnecessarily.

---

## Contributing

Contributions are welcome! Here's how:

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/text-search`
3. **Commit** your changes: `git commit -m 'feat: add text search'`
4. **Push** to the branch: `git push origin feature/text-search`
5. **Open** a Pull Request

Please follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.

---

## Keywords

`android pdf viewer` `pdf reader android` `pdf library android kotlin` `android pdf sdk` `pdfium android` `pdf view android` `display pdf android` `open pdf android app` `pdf renderer android` `android pdf viewer library open source` `barteksc alternative` `AndroidPdfViewer replacement` `kotlin pdf viewer` `android pdf custom view` `pdf viewer jetpack compose` `firebase pdf viewer android` `password protected pdf android` `night mode pdf reader` `android pdf zoom pinch` `pdf viewer recyclerview`

---

## License

```
Copyright 2025 Vishal Roy

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

<p align="center">
  <b>Made with ❤️ for the Android developer community</b><br/><br/>
  <a href="https://github.com/vishalroy000/EasyPDFKit">⭐ Star this repo</a> if you find it useful!<br/>
  Found a bug? <a href="https://github.com/vishalroy000/EasyPDFKit/issues">Open an issue</a>
</p>
