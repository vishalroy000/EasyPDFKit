<p align="center">
  <img src="https://img.shields.io/badge/📄-EasyPDFKit-blue?style=for-the-badge&labelColor=000" alt="EasyPDFKit" />
</p>

<p align="center">
  <a href="https://jitpack.io/#vishalroy000/EasyPDFKit"><img src="https://jitpack.io/v/vishalroy000/EasyPDFKit.svg" alt="JitPack" /></a>
  <img src="https://img.shields.io/badge/API-21%2B-brightgreen.svg" alt="API 21+" />
  <img src="https://img.shields.io/badge/Kotlin-100%25-7F52FF.svg" alt="Kotlin" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" />
  <img src="https://img.shields.io/badge/Pdfium-Native%20C++-orange.svg" alt="Pdfium" />
</p>

<h3 align="center">A modern, lightweight PDF viewer library for Android</h3>

<p align="center">
Built on native Pdfium rendering engine with Kotlin Coroutines.<br/>
Drop-in replacement for the abandoned <code>AndroidPdfViewer (barteksc)</code>.
</p>

<p align="center"><b>One dependency. Three lines of code. Any PDF.</b></p>

---

## Why EasyPDFKit?

Most Android PDF libraries are either abandoned, bloated, or crash on large documents. EasyPDFKit is built from scratch with three goals:

- **Just works** — display any PDF with 3 lines of code, no configuration needed
- **Never crashes** — multi-resolution rendering with bounded memory; tested on 1000+ page documents
- **Stay lightweight** — zero forced dependencies beyond Pdfium native; your APK stays small

---

## Features

| Category | What you get |
|----------|-------------|
| **Rendering** | Vertical continuous scroll, fit-width layout, multi-resolution bitmap rendering (1x/2x/4x) |
| **Gestures** | Pinch-to-zoom, double-tap zoom, fling with inertia, smooth zoom animations |
| **Memory** | LRU bitmap cache (1/6 of heap), automatic bitmap recycling, no OOM on large docs |
| **Sources** | `File`, `Uri`, `Asset`, `ByteArray`, `URL` — including Firebase Storage download URLs |
| **Network** | Built-in HTTP downloader with disk cache + progress callback, or plug in OkHttp/Ktor |
| **Security** | Password-protected PDF support with retry callback |
| **Display** | Night mode (color inversion), configurable page spacing, optional page snapping |
| **Lifecycle** | Auto-releases native resources on view detach, coroutine-scoped rendering |
| **Compatibility** | minSdk 21 (99%+ devices), Kotlin + Java support, ViewBinding ready |

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
    implementation 'com.github.vishalroy000:EasyPDFKit:v1.0.0'
}
```
</details>

### Step 3 — Add Internet permission (only if loading from URL)

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
```

### Step 4 — Sync & Build

Click **"Sync Now"** in Android Studio → Done! ✅

---

## Quick Start — 3 Lines to Display a PDF

### 1. Add the view to your XML layout

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

### 2. Load a PDF in your Activity/Fragment

```kotlin
// That's it — 3 lines!
binding.pdfView.fromAsset("sample.pdf")
    .onLoad { pageCount -> Log.d("PDF", "Loaded $pageCount pages") }
    .load()
```

> **Note:** Place your PDF file in `app/src/main/assets/` folder.

---

## Loading from Different Sources

### From Asset

```kotlin
binding.pdfView.fromAsset("sample.pdf")
    .onLoad { pageCount -> /* document ready */ }
    .onError { error -> /* handle error */ }
    .load()
```

### From URL (Firebase Storage, direct links, any HTTP URL)

```kotlin
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

### From File

```kotlin
val pdfFile = File(context.filesDir, "downloaded_report.pdf")
binding.pdfView.fromFile(pdfFile)
    .onLoad { /* ready */ }
    .load()
```

### From Uri (SAF File Picker / content:// / FileProvider)

```kotlin
// Use with ActivityResultContracts.StartActivityForResult or GetContent
val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    uri?.let {
        binding.pdfView.fromUri(it)
            .onLoad { pages -> /* ready */ }
            .load()
    }
}

// Launch picker
pickPdf.launch("application/pdf")
```

### From ByteArray

```kotlin
val pdfBytes: ByteArray = // ... from network, database, etc.
binding.pdfView.fromBytes(pdfBytes)
    .load()
```

---

## Password-Protected PDFs

EasyPDFKit handles encrypted PDFs with a simple retry callback pattern:

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
                retry(editText.text.toString())  // ← retry with password
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }
    .onError { error ->
        // Wrong password or other errors land here
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

## Night Mode (Dark Reading)

```kotlin
// Enable at load time
binding.pdfView.fromAsset("sample.pdf")
    .nightMode(true)
    .load()

// Toggle at runtime (no reload needed)
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
    .nightMode(false)                  // Inverted colors for dark reading
    .pageSpacingDp(12)                 // Gap between pages in dp (default: 8dp)
    .snapToPage(true)                  // Snap to nearest page after scroll ends

    // ── Zoom options
    .zoomRange(1f, 8f)                 // Min and max zoom levels (default: 1f to 6f)
    .doubleTapZoom(3f)                 // Zoom level on double-tap (default: 2.5f)

    // ── Event listeners
    .onLoad { pageCount ->             // Called when document is loaded
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

    // ── Start loading
    .load()
```

---

## Programmatic Navigation & Control

```kotlin
// ── Jump to a specific page (0-based index)
binding.pdfView.jumpTo(0)                       // Jump to first page (instant)
binding.pdfView.jumpTo(9, animated = true)      // Smooth scroll to page 10

// ── Read current state
val currentPage: Int = binding.pdfView.currentPage   // Currently visible page (0-based)
val totalPages: Int = binding.pdfView.pageCount       // Total page count
val isReady: Boolean = binding.pdfView.isLoaded       // Document loaded and ready?

// ── Change zoom programmatically
binding.pdfView.setZoom(2.5f)                   // Set zoom to 2.5x

// ── Night mode
binding.pdfView.setNightMode(true)              // Enable night mode
binding.pdfView.setNightMode(false)             // Disable night mode

// ── Release resources (auto-called when view is detached from window)
binding.pdfView.recycle()
```

---

## Custom Downloader (OkHttp / Ktor / Firebase)

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

### Download Progress Listener

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

## Complete Activity Example (Copy-Paste Ready)

### XML Layout — `res/layout/activity_pdf_viewer.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- PDF Viewer -->
    <com.easypdfkit.EasyPdfView
        android:id="@+id/pdfView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/bottomBar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Page indicator -->
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

    <!-- Bottom bar -->
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

### Activity — `PdfViewerActivity.kt`

```kotlin
package com.yourapp.ui

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yourapp.databinding.ActivityPdfViewerBinding

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var nightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get PDF source from intent
        val pdfUrl = intent.getStringExtra("pdf_url")
        val pdfAsset = intent.getStringExtra("pdf_asset")

        // Load PDF
        val configurator = when {
            pdfUrl != null -> binding.pdfView.fromUrl(pdfUrl)
            pdfAsset != null -> binding.pdfView.fromAsset(pdfAsset)
            else -> {
                Toast.makeText(this, "No PDF source provided", Toast.LENGTH_SHORT).show()
                return
            }
        }

        configurator
            .pageSpacingDp(8)
            .onLoad { pages ->
                binding.pageIndicator.text = "1 / $pages"
            }
            .onPageChange { page, total ->
                binding.pageIndicator.text = "${page + 1} / $total"
            }
            .onZoomChange { zoom ->
                // Optional: show zoom level
            }
            .onTap { _, _ ->
                // Toggle bottom bar visibility on tap
                val bar = binding.bottomBar
                bar.visibility = if (bar.visibility == android.view.View.VISIBLE)
                    android.view.View.GONE else android.view.View.VISIBLE
                true
            }
            .onPasswordRequired { retry ->
                val input = EditText(this).apply {
                    hint = "Enter password"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setPadding(48, 32, 48, 16)
                }
                MaterialAlertDialogBuilder(this)
                    .setTitle("Password Required")
                    .setView(input)
                    .setPositiveButton("Unlock") { _, _ -> retry(input.text.toString()) }
                    .setNegativeButton("Cancel") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
            .onError { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
            .load()

        // ── Button controls
        binding.btnPrev.setOnClickListener {
            val target = (binding.pdfView.currentPage - 1).coerceAtLeast(0)
            binding.pdfView.jumpTo(target, animated = true)
        }

        binding.btnNext.setOnClickListener {
            val target = (binding.pdfView.currentPage + 1).coerceAtMost(binding.pdfView.pageCount - 1)
            binding.pdfView.jumpTo(target, animated = true)
        }

        binding.btnNight.setOnClickListener {
            nightMode = !nightMode
            binding.pdfView.setNightMode(nightMode)
            binding.btnNight.text = if (nightMode) "☀️ Light" else "🌙 Night"
        }
    }
}
```

### Launch it from anywhere

```kotlin
// From another activity
val intent = Intent(this, PdfViewerActivity::class.java).apply {
    putExtra("pdf_url", "https://example.com/document.pdf")
    // OR
    putExtra("pdf_asset", "sample.pdf")
}
startActivity(intent)
```

---

## Java Compatibility

EasyPDFKit works with Java projects too:

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

### EasyPdfView

| Method | Description |
|--------|-------------|
| `fromFile(File)` | Load PDF from a local file |
| `fromUri(Uri)` | Load from content:// or file:// URI |
| `fromAsset(String)` | Load from app's assets folder |
| `fromBytes(ByteArray)` | Load from raw bytes |
| `fromUrl(String)` | Download and load from HTTP/HTTPS URL |
| `fromSource(PdfSource)` | Load from any custom PdfSource |
| `jumpTo(page, animated)` | Navigate to page (0-based) |
| `setNightMode(Boolean)` | Toggle night mode at runtime |
| `setZoom(Float)` | Set zoom level programmatically |
| `recycle()` | Release native resources manually |

### Configurator (Builder)

| Method | Default | Description |
|--------|---------|-------------|
| `.password(String?)` | `null` | Pre-set document password |
| `.nightMode(Boolean)` | `false` | Enable color inversion |
| `.pageSpacingDp(Int)` | `8` | Gap between pages in dp |
| `.snapToPage(Boolean)` | `false` | Snap to nearest page after scroll |
| `.zoomRange(min, max)` | `1f, 6f` | Allowed zoom range |
| `.doubleTapZoom(Float)` | `2.5f` | Double-tap zoom target |
| `.onLoad { }` | — | Document loaded callback |
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
| `pageCount` | `Int` | Total number of pages |
| `isLoaded` | `Boolean` | Whether document is loaded and ready |

---

## Project Architecture

```
EasyPDFKit/
│
├── easypdfkit/                          # 📦 Library module (publishable artifact)
│   └── src/main/java/com/easypdfkit/
│       ├── EasyPdfView.kt               # Main custom view — public API surface
│       ├── core/
│       │   └── PdfEngine.kt             # Pdfium native wrapper (thread-confined)
│       ├── cache/
│       │   └── PageBitmapCache.kt       # LRU bitmap cache (1/6 heap budget)
│       ├── source/
│       │   └── PdfSource.kt             # Sealed class for all input types
│       ├── listeners/
│       │   └── Callbacks.kt             # Event listener interfaces
│       └── util/
│           └── PdfDownloader.kt         # Pluggable HTTP downloader
│
├── sample/                              # 📱 Demo application
│   └── src/main/
│       ├── java/.../MainActivity.kt     # Sample with Asset/URL/Picker/Night mode
│       └── res/layout/activity_main.xml
│
├── build.gradle.kts                     # Root build config
├── settings.gradle.kts                  # Module includes
├── gradle.properties                    # Version & publishing coordinates
├── jitpack.yml                          # JitPack build config
├── LICENSE                              # Apache 2.0
└── README.md                            # This file
```

### How the Rendering Engine Works

1. **Open** — Pdfium opens the PDF via file descriptor (supports password decryption natively)
2. **Layout** — Each page is scaled to fit the view width at zoom=1x; page tops are precomputed
3. **Render** — Visible pages (±1 for prefetch) are rendered at a zoom-bucketed resolution:
   - zoom ≤ 1.25x → render at 1x width
   - zoom ≤ 2.5x → render at 2x width  
   - zoom > 2.5x → render at 4x width (bitmap-scaled beyond that)
4. **Cache** — Rendered bitmaps go into an LRU cache sized at 1/6 of max heap
5. **Draw** — Canvas draws cached bitmaps with optional night-mode color filter
6. **Recycle** — On eviction or view detach, bitmaps are recycled and native resources freed

> All Pdfium calls run on a single-threaded coroutine dispatcher because Pdfium is **not thread-safe**.

---

## Roadmap

| Version | Status | Features |
|---------|--------|----------|
| v1.0 | ✅ Released | Core viewer, zoom, gestures, night mode, password, URL loading, LRU cache |
| v1.1 | 🔜 Next | Text search & selection, horizontal scroll mode, region-based hi-res rendering |
| v1.2 | 📋 Planned | Thumbnail sidebar, bookmarks with persistence, dual-page tablet mode |
| v2.0 | 📋 Planned | Annotations module (highlight, draw, notes) as separate optional artifact |

---

## Requirements

- Android Studio Arctic Fox or later
- Kotlin 1.9+
- minSdk 21 (Android 5.0 Lollipop)
- targetSdk 34
- JDK 17

---

## Troubleshooting

**Build error: "Failed to resolve com.github.vishalroy000:EasyPDFKit"**  
→ Make sure `maven { url = uri("https://jitpack.io") }` is in your `settings.gradle.kts` (not just `build.gradle.kts`)

**Blank white screen when loading**  
→ PDF might be downloading. Add `.onError { }` to check for network errors. For URL loading, ensure `INTERNET` permission is in your manifest.

**OOM on very large PDFs**  
→ EasyPDFKit handles this automatically. If you still see issues, try reducing max zoom: `.zoomRange(1f, 3f)`

**Password dialog not showing**  
→ Make sure you set `.onPasswordRequired { retry -> }` before `.load()`

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
  <b>Made with ❤️ for the Android developer community</b><br/>
  <a href="https://github.com/vishalroy000/EasyPDFKit">⭐ Star this repo</a> if you find it useful!
</p>
