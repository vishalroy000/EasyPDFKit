package com.easypdfkit.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.easypdfkit.sample.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var nightModeOn = false

    // SAF file picker
    private val pickPdf = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> loadFromUri(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // ── Load from assets (put any sample.pdf in sample/src/main/assets/)
        b.btnAsset.setOnClickListener { loadFromAsset() }

        // ── Load from URL
        b.btnUrl.setOnClickListener { showUrlDialog() }

        // ── Pick PDF from device
        b.btnPicker.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
            }
            pickPdf.launch(intent)
        }

        // ── Night mode toggle
        b.btnNight.setOnClickListener {
            nightModeOn = !nightModeOn
            b.pdfView.setNightMode(nightModeOn)
            b.btnNight.text = if (nightModeOn) "☀️" else "🌙"
        }

        // Auto-load asset on launch
        loadFromAsset()
    }

    // ──────────────────────────── loaders

    private fun loadFromAsset() {
        b.pdfView.fromAsset("sample.pdf")
            .nightMode(nightModeOn)
            .pageSpacingDp(8)
            .onLoad { pages ->
                b.pageIndicator.text = "1 / $pages"
                toast("Loaded $pages pages from assets")
            }
            .onPageChange { page, count ->
                b.pageIndicator.text = "${page + 1} / $count"
            }
            .onPasswordRequired { retry ->
                showPasswordDialog { pw -> retry(pw) }
            }
            .onError { t ->
                Log.e("EasyPDFKit", "Error", t)
                toast("Error: ${t.message}")
            }
            .load()
    }

    private fun loadFromUri(uri: android.net.Uri) {
        b.pdfView.fromUri(uri)
            .nightMode(nightModeOn)
            .onLoad { pages ->
                b.pageIndicator.text = "1 / $pages"
                toast("Loaded $pages pages")
            }
            .onPageChange { page, count ->
                b.pageIndicator.text = "${page + 1} / $count"
            }
            .onPasswordRequired { retry ->
                showPasswordDialog { pw -> retry(pw) }
            }
            .onError { t ->
                Log.e("EasyPDFKit", "Error", t)
                toast("Error: ${t.message}")
            }
            .load()
    }

    private fun loadFromUrl(url: String) {
        toast("Downloading...")
        b.pdfView.fromUrl(url)
            .nightMode(nightModeOn)
            .onLoad { pages ->
                b.pageIndicator.text = "1 / $pages"
                toast("Loaded $pages pages from URL")
            }
            .onPageChange { page, count ->
                b.pageIndicator.text = "${page + 1} / $count"
            }
            .onError { t ->
                Log.e("EasyPDFKit", "Error", t)
                toast("Download/load error: ${t.message}")
            }
            .load()
    }

    // ──────────────────────────── dialogs

    private fun showUrlDialog() {
        val input = TextInputEditText(this).apply {
            hint = "https://example.com/doc.pdf"
            setPadding(48, 32, 48, 16)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Load from URL")
            .setView(input)
            .setPositiveButton("Load") { _, _ ->
                val url = input.text?.toString()?.trim()
                if (!url.isNullOrBlank()) loadFromUrl(url)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPasswordDialog(onSubmit: (String) -> Unit) {
        val input = TextInputEditText(this).apply {
            hint = "Enter password"
            setPadding(48, 32, 48, 16)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Password Protected PDF")
            .setMessage("This document requires a password.")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                val pw = input.text?.toString() ?: ""
                onSubmit(pw)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
