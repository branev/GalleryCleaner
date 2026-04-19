# Task 01: Code Review Follow-ups

**Parent:** SDD-20260419-003 — Code Review Follow-ups

## What You're Changing

Six small quality fixes from a code-review pass. No user-visible
change. Four files touched.

## Step-by-Step Instructions

### Step 1 — Coroutine migration + explicit try/catch in `MediaInfoBottomSheetFragment.kt`

Add imports (next to existing androidx imports):

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
```

Replace the whole `loadExtraMetadata` function:

```kotlin
private fun loadExtraMetadata(uri: Uri, fallbackDate: Long) {
    viewLifecycleOwner.lifecycleScope.launch {
        val result = withContext(Dispatchers.IO) {
            queryMediaStore(uri)
        }
        // Back on main thread — scope is tied to the view; if the
        // view is gone, we never get here.
        val resolutionText = if (result.width > 0 && result.height > 0) {
            "${result.width} × ${result.height}"
        } else {
            getString(R.string.info_unknown)
        }
        val capturedSec =
            if (result.dateTakenMillis > 0) result.dateTakenMillis / 1000L
            else fallbackDate
        resolutionValueView?.text = resolutionText
        capturedValueView?.text = formatDate(capturedSec)
    }
}

private data class ExtraMetadata(
    val width: Int,
    val height: Int,
    val dateTakenMillis: Long,
)

private fun queryMediaStore(uri: Uri): ExtraMetadata {
    val ctx = context ?: return ExtraMetadata(0, 0, 0L)
    val projection = buildList {
        add(MediaStore.MediaColumns.WIDTH)
        add(MediaStore.MediaColumns.HEIGHT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.MediaColumns.DATE_TAKEN)
        }
    }.toTypedArray()

    var width = 0
    var height = 0
    var dateTakenMillis = 0L

    try {
        ctx.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val wIdx = c.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val hIdx = c.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                if (wIdx >= 0 && !c.isNull(wIdx)) width = c.getInt(wIdx)
                if (hIdx >= 0 && !c.isNull(hIdx)) height = c.getInt(hIdx)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val dtIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                    if (dtIdx >= 0 && !c.isNull(dtIdx)) {
                        dateTakenMillis = c.getLong(dtIdx)
                    }
                }
            }
        }
    } catch (_: SecurityException) {
        // Caller lacks permission — return zeros, UI shows "—".
    } catch (_: IllegalArgumentException) {
        // Malformed URI / unknown column — same fallback.
    }
    return ExtraMetadata(width, height, dateTakenMillis)
}
```

Delete the old `Thread { ... }` and the `activity?.runOnUiThread { if
(_binding != null) ... }` block. `lifecycleScope` handles both
concerns.

### Step 2 — Coroutine migration + explicit try/catch in `MediaViewerActivity.kt`

Add imports (next to existing androidx imports):

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
```

Also add `import android.util.Log` if not already there.

Replace `loadVideoPoster` and `extractVideoPoster`:

```kotlin
private fun loadVideoPoster(uri: Uri) {
    lifecycleScope.launch {
        val bitmap = withContext(Dispatchers.IO) { extractVideoPoster(uri) }
            ?: return@launch
        // Back on main; scope is activity-bound, so no isFinishing check.
        binding.imageView.setImageBitmap(bitmap)
    }
}

private fun extractVideoPoster(uri: Uri): android.graphics.Bitmap? {
    // Fast path: MediaStore cached thumbnail (API 29+).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val thumb = contentResolver.loadThumbnail(uri, Size(1024, 1024), null)
            if (thumb != null) return thumb
        } catch (_: IOException) {
            // Fall through to MMR.
        } catch (_: SecurityException) {
            return null  // Caller lacks access; MMR won't help.
        }
    }

    // Fallback: MediaMetadataRetriever.
    var retriever: MediaMetadataRetriever? = null
    return try {
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)
        retriever.frameAtTime
            ?: retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST)
    } catch (e: IllegalArgumentException) {
        Log.w("MediaViewer", "poster: MMR rejected URI $uri", e)
        null
    } catch (e: RuntimeException) {
        // MMR can throw undocumented RuntimeExceptions on malformed
        // media. Re-throw CancellationException so the coroutine can
        // be cancelled cleanly when the activity dies mid-load.
        if (e is CancellationException) throw e
        Log.w("MediaViewer", "poster: MMR failed for $uri", e)
        null
    } finally {
        try { retriever?.release() } catch (_: Exception) {}
    }
}
```

Remove `import kotlin.runCatching` if it's the only user (unlikely —
it's a stdlib function, no import needed). Add imports if missing:
`import java.io.IOException`.

### Step 3 — `days!!` fix in `GalleryViewModel.kt`

Find the `when` around line 483. The current code:

```kotlin
else -> Pair(now - (dateRange.preset.days!! * 24 * 60 * 60L), now)
```

Replace with:

```kotlin
else -> {
    val days = dateRange.preset.days ?: return Pair(0L, now)
    Pair(now - (days * 24 * 60 * 60L), now)
}
```

The `?:` fallback means: if somehow `days` is null in this branch
(can't happen today but future enum additions might), return
`ALL_TIME`-equivalent bounds. Compiler proves non-null inside the
branch.

### Step 4 — `scrollListener!!` fix in `FastScrollHelper.kt`

Find the block around line 42. Current:

```kotlin
scrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrolled(...) { ... }
}
recyclerView.addOnScrollListener(scrollListener!!)
```

Replace with:

```kotlin
val listener = object : RecyclerView.OnScrollListener() {
    override fun onScrolled(...) { ... }
}
recyclerView.addOnScrollListener(listener)
scrollListener = listener
```

Compiler sees `listener` as non-null throughout.

### Step 5 — Build + lint

```bash
./gradlew --stop
./gradlew assembleDebug testDebugUnitTest lint
```

All green. The coroutines imports are already on the classpath via
`androidx.lifecycle:lifecycle-runtime-ktx`.

### Step 6 — On-device spot check

```bash
./gradlew installDebug
```

1. Tap a photo → Info sheet → Resolution + Captured populate after a
   brief delay. Identical to before.
2. Tap several videos in a row — posters still appear cleanly.
3. Open a video, press back *before* the first frame renders. Open a
   different video immediately. No crash, no stale poster from the
   previous one.
4. Fast-scroller still drags normally.
5. Date-range filter still clamps correctly when you pick `Last 7
   days`, `Last 30 days`, etc.

### Step 7 — Mark done + commit

1. Flip every `[ ]` to `[x]` on `requirement.md`.
2. Move the folder `docs/sdd/todo/20260419-003-code-review-followups/`
   → `docs/sdd/done/20260419-003-code-review-followups/`.
3. Update `STATUS.md` — row to Completed, tasks 1/1, summary:
   `Thread→lifecycleScope migration in viewer/info sheet, runCatching→explicit try/catch, days!! + scrollListener!! force-unwraps removed, Log.w re-added on MMR failures.`
4. **Atomic commits:** implementation + doc-done.
