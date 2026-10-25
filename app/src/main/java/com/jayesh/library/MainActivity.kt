package com.jayesh.library

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class Book(val uri: String, val name: String, val ext: String)

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var root: LinearLayout
    private var dark = true
    private var allBooks = mutableListOf<Book>()
    private val purple = Color.rgb(133, 105, 235)
    private val pickCode = 81
    private val bg get() = if (dark) Color.rgb(18,18,24) else Color.rgb(247,246,252)
    private val panel get() = if (dark) Color.rgb(30,30,40) else Color.WHITE
    private val fg get() = if (dark) Color.rgb(244,243,250) else Color.rgb(30,29,39)
    private val muted get() = if (dark) Color.rgb(170,169,185) else Color.rgb(105,103,120)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("jayesh_library", MODE_PRIVATE)
        dark = prefs.getBoolean("dark", true)
        loadBooks()
        showHome()
    }

    private fun loadBooks() {
        allBooks.clear()
        prefs.getStringSet("books", emptySet())?.forEach { raw ->
            val p = raw.split("|", limit = 2)
            if (p.size == 2) {
                val uri = p[0]; val name = p[1]
                val ext = name.substringAfterLast('.', "").lowercase()
                allBooks.add(Book(uri, name, ext))
            }
        }
    }

    private fun saveBook(uri: Uri, name: String) {
        val set = prefs.getStringSet("books", emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add("${uri}|${name.replace("|", "_")}")
        prefs.edit().putStringSet("books", set).apply()
        loadBooks()
    }

    private fun base(title: String, subtitle: String? = null): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 10)
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
        val brand = TextView(this).apply {
            text = title; textSize = 25f; setTextColor(fg); typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        val theme = TextView(this).apply {
            text = if (dark) "☀" else "☾"; textSize = 24f; setTextColor(purple); gravity = Gravity.CENTER
            setPadding(12, 4, 12, 4)
            setOnClickListener { dark = !dark; prefs.edit().putBoolean("dark", dark).apply(); showHome() }
        }
        top.addView(theme)
        content.addView(top)
        if (subtitle != null) content.addView(TextView(this).apply {
            text = subtitle; textSize = 13f; setTextColor(muted); setPadding(0, 5, 0, 14)
        })
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        return content
    }

    private fun label(parent: LinearLayout, text: String) {
        parent.addView(TextView(this).apply {
            this.text = text; textSize = 17f; setTextColor(fg); typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 18, 0, 10)
        })
    }

    private fun button(text: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text; textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(16, 13, 16, 13); setBackgroundColor(purple); setOnClickListener { onClick() }
        }

    private fun showHome(filter: String = "All", query: String = "") {
        val content = base("Jayesh Library", "YOUR PERSONAL DIGITAL BOOKSHELF")
        val search = EditText(this).apply {
            hint = "Search books, comics, PDFs…"; textSize = 15f; setTextColor(fg); setHintTextColor(muted)
            setSingleLine(true); inputType = InputType.TYPE_CLASS_TEXT
            setPadding(14, 12, 14, 12); setBackgroundColor(panel)
            setText(query)
        }
        content.addView(search)
        search.setOnEditorActionListener { _, _, _ -> showHome(filter, search.text.toString()); true }
        val add = button("＋  Import books from device") {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "application/epub+zip", "application/zip", "application/x-cbz", "image/*", "text/plain"))
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(i, pickCode)
        }
        val addWrap = LinearLayout(this).apply { setPadding(0, 12, 0, 6); addView(add, LinearLayout.LayoutParams(-1,-2)) }
        content.addView(addWrap)
        label(content, "Browse your shelves")
        val cats = listOf("All", "PDF", "EPUB", "Comics", "Other")
        val catRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cats.forEach { c ->
            val b = TextView(this).apply {
                text = c; textSize = 12f; gravity = Gravity.CENTER; setTextColor(if (filter == c) Color.WHITE else fg)
                setPadding(10, 10, 10, 10); setBackgroundColor(if (filter == c) purple else panel)
                setOnClickListener { showHome(c, search.text.toString()) }
            }
            val lp = LinearLayout.LayoutParams(0, -2, 1f); lp.setMargins(2, 0, 2, 0); catRow.addView(b, lp)
        }
        content.addView(catRow)
        label(content, if (filter == "All") "Your bookshelf  ·  ${allBooks.size}" else "$filter shelf")
        val filtered = allBooks.filter {
            val matchesQuery = it.name.contains(query, ignoreCase = true)
            val matchesCategory = when (filter) {
                "PDF" -> it.ext == "pdf"
                "EPUB" -> it.ext == "epub"
                "Comics" -> it.ext in listOf("cbz", "cbr", "zip") || it.ext in listOf("jpg", "jpeg", "png", "webp")
                "Other" -> it.ext !in listOf("pdf", "epub", "cbz", "cbr", "zip", "jpg", "jpeg", "png", "webp")
                else -> true
            }
            matchesQuery && matchesCategory
        }
        if (filtered.isEmpty()) {
            content.addView(TextView(this).apply {
                text = if (allBooks.isEmpty()) "Your shelves are waiting.\nImport a PDF, EPUB, or comic to get started." else "No books match this search."
                textSize = 14f; setTextColor(muted); gravity = Gravity.CENTER; setPadding(12, 35, 12, 35)
            })
        } else {
            filtered.chunked(3).forEach { rowBooks ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP }
                rowBooks.forEach { book ->
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL; setPadding(8, 8, 8, 10); setBackgroundColor(panel)
                        setOnClickListener { openBook(book) }
                    }
                    val cover = TextView(this).apply {
                        text = when(book.ext) { "pdf" -> "PDF\n▤"; "epub" -> "EPUB\n▤"; "cbz","cbr","zip" -> "COMIC\n▧"; else -> "BOOK\n▤" }
                        textSize = 19f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                        typeface = Typeface.DEFAULT_BOLD; setBackgroundColor(coverColor(book.name))
                    }
                    card.addView(cover, LinearLayout.LayoutParams(-1, 128))
                    card.addView(TextView(this).apply {
                        text = book.name; textSize = 12f; setTextColor(fg); maxLines = 2
                        setPadding(2, 8, 2, 2)
                    })
                    val lp = LinearLayout.LayoutParams(0, -2, 1f); lp.setMargins(4, 4, 4, 8); row.addView(card, lp)
                }
                while (row.childCount < 3) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
                content.addView(row)
            }
        }
        content.addView(TextView(this).apply {
            text = "Files you import stay on your device. Jayesh Library does not upload them."
            textSize = 11f; setTextColor(muted); setPadding(0, 22, 0, 10)
        })
    }

    private fun coverColor(s: String): Int {
        val colors = listOf(Color.rgb(77,64,125), Color.rgb(37,91,105), Color.rgb(135,73,84), Color.rgb(74,92,64), Color.rgb(108,77,49))
        return colors[(s.hashCode() and Int.MAX_VALUE) % colors.size]
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickCode || resultCode != Activity.RESULT_OK || data == null) return
        val picked = mutableListOf<Uri>()
        data.clipData?.let { clip -> for (i in 0 until clip.itemCount) picked.add(clip.getItemAt(i).uri) }
        if (picked.isEmpty()) data.data?.let { picked.add(it) }
        picked.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            val name = queryName(uri) ?: "Book.${contentResolver.getType(uri)?.substringAfter('/') ?: "file"}"
            saveBook(uri, name)
        }
        showHome()
    }

    private fun queryName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    private fun openBook(book: Book) {
        when (book.ext) {
            "pdf" -> openPdf(book)
            "epub" -> openEpub(book)
            "cbz", "zip" -> openComicArchive(book)
            "txt", "md" -> openText(book)
            else -> Toast.makeText(this, "This format is saved in your shelf, but its reader is not available yet.", Toast.LENGTH_LONG).show()
        }
    }

    private fun readerShell(title: String): LinearLayout {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(12, 8, 12, 8) }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        bar.addView(TextView(this).apply { text = "‹  Library"; textSize = 16f; setTextColor(purple); setPadding(4, 12, 14, 12); setOnClickListener { showHome() } })
        bar.addView(TextView(this).apply { text = title; textSize = 14f; setTextColor(fg); maxLines = 1 }, LinearLayout.LayoutParams(0,-2,1f))
        page.addView(bar)
        setContentView(page)
        return page
    }

    private fun openPdf(book: Book) {
        try {
            val pfd = contentResolver.openFileDescriptor(Uri.parse(book.uri), "r") ?: return
            val renderer = PdfRenderer(pfd)
            var pageIndex = 0
            val page = readerShell(book.name)
            val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val image = ImageView(this).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER }
            val scroller = ScrollView(this)
            scroller.addView(image)
            page.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
            val prev = button("‹ Previous") { if (pageIndex > 0) { pageIndex--; renderPdf(renderer, pageIndex, image, controls) } }
            val next = button("Next ›") { if (pageIndex < renderer.pageCount-1) { pageIndex++; renderPdf(renderer, pageIndex, image, controls) } }
            controls.addView(prev, LinearLayout.LayoutParams(0,-2,1f))
            controls.addView(next, LinearLayout.LayoutParams(0,-2,1f))
            page.addView(controls)
            renderPdf(renderer, pageIndex, image, controls)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open this PDF.", Toast.LENGTH_LONG).show()
        }
    }

    private fun renderPdf(renderer: PdfRenderer, index: Int, image: ImageView, controls: LinearLayout) {
        try {
            renderer.openPage(index).use { p ->
                val w = resources.displayMetrics.widthPixels - 24
                val bmp = Bitmap.createBitmap(w, (w.toFloat() * p.height / p.width).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                image.setImageBitmap(bmp)
            }
            controls.getChildAt(0).isEnabled = index > 0
            controls.getChildAt(1).isEnabled = index < renderer.pageCount - 1
        } catch (_: Exception) {}
    }

    private fun openText(book: Book) {
        try {
            val text = contentResolver.openInputStream(Uri.parse(book.uri))!!.bufferedReader().use { it.readText() }
            showTextReader(book.name, text)
        } catch (_: Exception) { Toast.makeText(this, "Unable to open file.", Toast.LENGTH_SHORT).show() }
    }

    private fun openEpub(book: Book) {
        try {
            val tmp = File(cacheDir, "epub_${System.currentTimeMillis()}").apply { mkdirs() }
            contentResolver.openInputStream(Uri.parse(book.uri))!!.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && (entry.name.endsWith(".html", true) || entry.name.endsWith(".xhtml", true) || entry.name.endsWith(".htm", true))) {
                            val out = File(tmp, entry.name.substringAfterLast('/'))
                            FileOutputStream(out).use { zip.copyTo(it) }
                        }
                        entry = zip.nextEntry
                    }
                }
            }
            val html = tmp.listFiles()?.sortedBy { it.name }?.joinToString("<hr/>") { it.readText() } ?: "No readable chapter found."
            val web = WebView(this).apply {
                settings.javaScriptEnabled = false
                setBackgroundColor(bg)
                loadDataWithBaseURL(null, "<meta name='viewport' content='width=device-width, initial-scale=1'><body style='font:18px sans-serif;line-height:1.7;padding:18px;color:${if(dark) "#eeeeF5" else "#222"};background:${if(dark) "#121218" else "#f7f6fc"}'>$html</body>", "text/html", "UTF-8", null)
            }
            val page = readerShell(book.name)
            page.addView(web, LinearLayout.LayoutParams(-1,0,1f))
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to read this EPUB. Try importing a standard EPUB file.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openComicArchive(book: Book) {
        try {
            val tmp = File(cacheDir, "comic_${System.currentTimeMillis()}").apply { mkdirs() }
            contentResolver.openInputStream(Uri.parse(book.uri))!!.use { input ->
                ZipInputStream(input).use { zip ->
                    var e = zip.nextEntry
                    while (e != null) {
                        if (!e.isDirectory && e.name.substringAfterLast('.').lowercase() in listOf("jpg","jpeg","png","webp")) {
                            val out = File(tmp, e.name.substringAfterLast('/'))
                            FileOutputStream(out).use { zip.copyTo(it) }
                        }
                        e = zip.nextEntry
                    }
                }
            }
            val imgs = tmp.listFiles()?.sortedBy { it.name } ?: emptyList()
            if (imgs.isEmpty()) { Toast.makeText(this, "No comic images found in archive.", Toast.LENGTH_LONG).show(); return }
            var index = 0
            val page = readerShell(book.name)
            val image = ImageView(this).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER }
            page.addView(ScrollView(this).apply { addView(image) }, LinearLayout.LayoutParams(-1,0,1f))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(button("‹ Previous") { if(index>0) { index--; image.setImageBitmap(BitmapFactory.decodeFile(imgs[index].absolutePath)) } }, LinearLayout.LayoutParams(0,-2,1f))
            row.addView(button("Next ›") { if(index<imgs.lastIndex) { index++; image.setImageBitmap(BitmapFactory.decodeFile(imgs[index].absolutePath)) } }, LinearLayout.LayoutParams(0,-2,1f))
            page.addView(row)
            image.setImageBitmap(BitmapFactory.decodeFile(imgs[0].absolutePath))
        } catch (_: Exception) { Toast.makeText(this, "Unable to open this comic archive.", Toast.LENGTH_LONG).show() }
    }

    private fun showTextReader(title: String, text: String) {
        val page = readerShell(title)
        var size = 18f
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val body = TextView(this).apply { setTextColor(fg); textSize = size; setLineSpacing(8f, 1f); setTextIsSelectable(true); this.text = text }
        controls.addView(button("A−") { size = (size - 2).coerceAtLeast(12f); body.textSize = size }, LinearLayout.LayoutParams(0,-2,1f))
        controls.addView(button("A+") { size = (size + 2).coerceAtMost(34f); body.textSize = size }, LinearLayout.LayoutParams(0,-2,1f))
        page.addView(controls)
        page.addView(ScrollView(this).apply { setPadding(10, 12, 10, 12); addView(body) }, LinearLayout.LayoutParams(-1,0,1f))
    }
}
