# Task 01: Media Viewer Redesign

**Parent:** SDD-20260418-013 — Media Viewer

## What You're Changing

The fullscreen viewer (what you see when you tap a thumbnail in the
grid) grows from a passive preview into the **reviewer loop**'s
second half. After this task:

- The subtitle metadata gets the source appended (`Apr 12, 2026 · 8.71
  MB · Viber`) and shrinks to 11sp 55% white.
- A new bottom action bar shows three buttons — **Info · Keep ·
  Delete** — always visible when the chrome is.
- **Keep** marks the item as viewed (so it dims in the grid) and
  closes the viewer with a haptic tick. **Delete** closes the viewer
  and routes back to `MainActivity.performTrash()`, which fires the
  redesigned delete-success card with its own Undo ring.
- Videos gain a custom bottom player row (play/pause + accent SeekBar
  + mono time labels) replacing the stock Android `MediaController`
  which fades into bright photo content.
- The center 72dp play/pause overlay goes away — the control lives in
  the new row now.
- `MainActivity` starts the viewer via a result-launcher so it can
  react to Keep / Delete.

## Before vs After

| Thing | Before | After |
|---|---|---|
| Subtitle metadata | `date · size` (12sp 70% white) | `date · size · source` (11sp 55% white) |
| Bottom action bar | None | `Info · Keep · Delete`, 10dp radius, icon-only |
| Keep action | *nothing* | Marks viewed + closes viewer + haptic tick |
| Delete action | *nothing* | Routes to `performTrash` in MainActivity |
| Video controls | System `MediaController` | Custom row: play/pause + accent SeekBar + mono `0:12 / 0:48` |
| Center play/pause | 72dp glyph in overlay | moved into the new row |
| Start viewer | `startActivity(intent)` | `launcher.launch(intent)` + result routing |

## Prerequisites

- SDD-002 merged. `accent`, `danger`, `surface`, mono font token
  available.
- SDD-010 merged. `performTrash` exists in MainActivity and wires the
  redesigned delete-success card.
- `./gradlew --stop` before you start.
- Files to understand first:
  - [activity_media_viewer.xml](../../../../app/src/main/res/layout/activity_media_viewer.xml) — current layout
  - [MediaViewerActivity.kt](../../../../app/src/main/java/com/example/gallerycleaner/MediaViewerActivity.kt) — current activity
  - [MainActivity.kt:739-748](../../../../app/src/main/java/com/example/gallerycleaner/MainActivity.kt#L739-L748) — `openMediaViewer`
  - [GalleryViewModel.kt:325-346](../../../../app/src/main/java/com/example/gallerycleaner/GalleryViewModel.kt#L325-L346) — `markAsViewed`

## Step-by-Step Instructions

### Step 1 — Button background drawables

Create `res/drawable/viewer_btn_bg.xml` (10% white, 10dp radius):

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#1AFFFFFF" />
    <corners android:radius="10dp" />
</shape>
```

Create `res/drawable/viewer_btn_bg_danger.xml` (danger fill, 10dp):

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/danger" />
    <corners android:radius="10dp" />
</shape>
```

### Step 2 — SeekBar styling for video scrubber

Create `res/drawable/viewer_seekbar_thumb.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/accent" />
    <size android:width="14dp" android:height="14dp" />
</shape>
```

Create `res/drawable/viewer_seekbar_progress.xml` (layer-list with
track + accent progress):

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@android:id/background">
        <shape android:shape="rectangle">
            <size android:height="3dp" />
            <solid android:color="#40FFFFFF" />
            <corners android:radius="2dp" />
        </shape>
    </item>
    <item android:id="@android:id/progress">
        <clip>
            <shape android:shape="rectangle">
                <size android:height="3dp" />
                <solid android:color="@color/accent" />
                <corners android:radius="2dp" />
            </shape>
        </clip>
    </item>
</layer-list>
```

### Step 3 — Verify icons + add strings

Verify the following drawables exist (grep or Glob). They should —
they're used elsewhere:

- `ic_info` — if missing, add a 24dp info vector in white (just a
  circled `i`). Claude can synthesize one.
- `ic_check` — used on the delete-success card; reuse.
- `ic_delete` — used in selection mode; reuse.
- `ic_play_circle`, `ic_pause_circle` — already exist for the center
  overlay. Will reuse tinted white.

Add to `res/values/strings.xml` (anywhere sensible):

```xml
<!-- Media viewer (SDD-20260418-013) -->
<string name="viewer_keep">Keep</string>
<string name="viewer_delete">Delete</string>
<string name="viewer_info">Info</string>
<string name="viewer_info_coming_soon">Info sheet coming soon</string>
<string name="viewer_delete_failed">Could not delete item</string>
```

### Step 4 — Rebuild `activity_media_viewer.xml`

Replace the whole file. Key structural moves:
- Drop the `videoControlsOverlay` FrameLayout + 72dp center play/pause.
- Add a `videoPlayerRow` (LinearLayout) constrained just above the
  bottom action bar, visible only during video playback.
- Add a `bottomActionBar` (LinearLayout) constrained to `parent`'s
  bottom, with three equal-weight buttons.

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/black">

    <!-- Image viewer -->
    <ImageView
        android:id="@+id/imageView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="fitCenter"
        android:contentDescription="@string/media_preview"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Video player container -->
    <FrameLayout
        android:id="@+id/videoContainer"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <VideoView
            android:id="@+id/videoView"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center" />
    </FrameLayout>

    <!-- Loading indicator -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:indeterminate="true"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Top info bar -->
    <LinearLayout
        android:id="@+id/topInfoBar"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:background="#CC000000"
        android:paddingVertical="10dp"
        android:paddingStart="8dp"
        android:paddingEnd="16dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageButton
            android:id="@+id/btnClose"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_close"
            android:contentDescription="@string/exit_selection"
            app:tint="@android:color/white" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:layout_marginStart="4dp">

            <TextView
                android:id="@+id/viewerTitle"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="@android:color/white"
                android:textSize="15sp"
                android:textStyle="bold"
                android:maxLines="1"
                android:ellipsize="middle" />

            <TextView
                android:id="@+id/viewerSubtitle"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="#8CFFFFFF"
                android:textSize="11sp"
                android:maxLines="1" />
        </LinearLayout>
    </LinearLayout>

    <!-- Video player row (visible only for videos) -->
    <LinearLayout
        android:id="@+id/videoPlayerRow"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:background="#CC000000"
        android:paddingHorizontal="12dp"
        android:paddingVertical="8dp"
        android:visibility="gone"
        app:layout_constraintBottom_toTopOf="@id/bottomActionBar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageButton
            android:id="@+id/btnPlayPause"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_play_circle"
            android:contentDescription="@string/play_pause" />

        <TextView
            android:id="@+id/videoTimeCurrent"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="4dp"
            android:textColor="#B3FFFFFF"
            android:textSize="11sp"
            android:fontFamily="@font/jetbrains_mono"
            android:text="0:00" />

        <SeekBar
            android:id="@+id/videoSeekBar"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginHorizontal="8dp"
            android:progressDrawable="@drawable/viewer_seekbar_progress"
            android:thumb="@drawable/viewer_seekbar_thumb"
            android:splitTrack="false" />

        <TextView
            android:id="@+id/videoTimeTotal"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="#B3FFFFFF"
            android:textSize="11sp"
            android:fontFamily="@font/jetbrains_mono"
            android:text="0:00" />
    </LinearLayout>

    <!-- Bottom action bar: Info · Keep · Delete -->
    <LinearLayout
        android:id="@+id/bottomActionBar"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:background="#CC000000"
        android:paddingHorizontal="12dp"
        android:paddingTop="8dp"
        android:paddingBottom="12dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <FrameLayout
            android:id="@+id/btnOverlayInfo"
            android:layout_width="0dp"
            android:layout_height="48dp"
            android:layout_weight="1"
            android:background="@drawable/viewer_btn_bg"
            android:clickable="true"
            android:focusable="true"
            android:foreground="?attr/selectableItemBackground">

            <ImageView
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:layout_gravity="center"
                android:src="@drawable/ic_info"
                app:tint="@android:color/white"
                android:contentDescription="@string/viewer_info" />
        </FrameLayout>

        <Space
            android:layout_width="8dp"
            android:layout_height="1dp" />

        <FrameLayout
            android:id="@+id/btnOverlayKeep"
            android:layout_width="0dp"
            android:layout_height="48dp"
            android:layout_weight="1"
            android:background="@drawable/viewer_btn_bg"
            android:clickable="true"
            android:focusable="true"
            android:foreground="?attr/selectableItemBackground">

            <ImageView
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:layout_gravity="center"
                android:src="@drawable/ic_check"
                app:tint="@android:color/white"
                android:contentDescription="@string/viewer_keep" />
        </FrameLayout>

        <Space
            android:layout_width="8dp"
            android:layout_height="1dp" />

        <FrameLayout
            android:id="@+id/btnOverlayDelete"
            android:layout_width="0dp"
            android:layout_height="48dp"
            android:layout_weight="1"
            android:background="@drawable/viewer_btn_bg_danger"
            android:clickable="true"
            android:focusable="true"
            android:foreground="?attr/selectableItemBackground">

            <ImageView
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:layout_gravity="center"
                android:src="@drawable/ic_delete"
                app:tint="@android:color/white"
                android:contentDescription="@string/viewer_delete" />
        </FrameLayout>
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

### Step 5 — Rewrite `MediaViewerActivity.kt`

Replace the file. Key moves:
- Add `EXTRA_SOURCE` to companion object.
- Drop `MediaController` import and use.
- Add `EXTRA_ACTION` result contract.
- Build custom video controls (SeekBar + time labels + play/pause).
- Wire three bottom buttons: Info (stub snackbar), Keep (haptic +
  result), Delete (result).

```kotlin
package com.example.gallerycleaner

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.example.gallerycleaner.databinding.ActivityMediaViewerBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaViewerBinding
    private var isVideo = false
    private var isPlaying = false
    private var itemUri: Uri? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            if (binding.videoView.isPlaying) {
                val pos = binding.videoView.currentPosition
                binding.videoSeekBar.progress = pos
                binding.videoTimeCurrent.text = formatMs(pos.toLong())
            }
            progressHandler.postDelayed(this, 250L)
        }
    }

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_MEDIA_TYPE = "extra_media_type"
        const val EXTRA_DISPLAY_NAME = "extra_display_name"
        const val EXTRA_DATE_ADDED = "extra_date_added"
        const val EXTRA_SIZE = "extra_size"
        const val EXTRA_SOURCE = "extra_source"

        const val EXTRA_ACTION = "extra_action"
        const val ACTION_KEPT = "kept"
        const val ACTION_DELETE = "delete"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val originalPaddingTop = binding.topInfoBar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.topInfoBar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val topInset = maxOf(systemBars.top, cutout.top)
            view.setPadding(view.paddingLeft, originalPaddingTop + topInset, view.paddingRight, view.paddingBottom)
            insets
        }

        val originalActionBarPaddingBottom = binding.bottomActionBar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionBar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight,
                originalActionBarPaddingBottom + systemBars.bottom)
            insets
        }

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_URI)
        }
        val mediaTypeOrdinal = intent.getIntExtra(EXTRA_MEDIA_TYPE, 0)
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        val dateAdded = intent.getLongExtra(EXTRA_DATE_ADDED, 0L)
        val size = intent.getLongExtra(EXTRA_SIZE, 0L)
        val sourceOrdinal = intent.getIntExtra(EXTRA_SOURCE, -1)

        if (uri == null) {
            finish()
            return
        }
        itemUri = uri
        isVideo = mediaTypeOrdinal == MediaType.VIDEO.ordinal

        setupToolbar(displayName, dateAdded, size, sourceOrdinal)
        setupActionBar(uri)

        if (isVideo) {
            setupVideoPlayer(uri)
        } else {
            setupImageViewer(uri)
        }
    }

    private fun setupToolbar(displayName: String, dateAdded: Long, size: Long, sourceOrdinal: Int) {
        binding.viewerTitle.text = displayName

        val parts = mutableListOf<String>()
        if (dateAdded > 0) {
            val date = Date(dateAdded * 1000L)
            parts.add(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date))
        }
        if (size > 0) {
            parts.add(Formatter.formatFileSize(this, size))
        }
        if (sourceOrdinal in 0 until SourceType.values().size) {
            parts.add(SourceType.values()[sourceOrdinal].label)
        }
        binding.viewerSubtitle.text = parts.joinToString(" · ")

        binding.btnClose.setOnClickListener { finish() }
    }

    private fun setupActionBar(uri: Uri) {
        binding.btnOverlayInfo.setOnClickListener {
            Snackbar.make(binding.root, R.string.viewer_info_coming_soon, Snackbar.LENGTH_SHORT).show()
        }
        binding.btnOverlayKeep.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            val result = Intent().apply {
                putExtra(EXTRA_ACTION, ACTION_KEPT)
                putExtra(EXTRA_URI, uri)
            }
            setResult(RESULT_OK, result)
            finish()
        }
        binding.btnOverlayDelete.setOnClickListener {
            val result = Intent().apply {
                putExtra(EXTRA_ACTION, ACTION_DELETE)
                putExtra(EXTRA_URI, uri)
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }

    private fun setupImageViewer(uri: Uri) {
        binding.imageView.visibility = View.VISIBLE
        binding.videoContainer.visibility = View.GONE
        binding.videoPlayerRow.visibility = View.GONE

        binding.imageView.load(uri) {
            crossfade(true)
            listener(
                onStart = { binding.progressBar.visibility = View.VISIBLE },
                onSuccess = { _, _ -> binding.progressBar.visibility = View.GONE },
                onError = { _, _ -> binding.progressBar.visibility = View.GONE },
            )
        }

        binding.imageView.setOnClickListener { toggleChrome() }
    }

    private fun setupVideoPlayer(uri: Uri) {
        binding.imageView.visibility = View.GONE
        binding.videoContainer.visibility = View.VISIBLE
        binding.videoPlayerRow.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE

        binding.videoView.setVideoURI(uri)

        binding.videoView.setOnPreparedListener { mp ->
            binding.progressBar.visibility = View.GONE
            mp.isLooping = false
            scaleVideoToFit(mp.videoWidth, mp.videoHeight)
            val duration = binding.videoView.duration
            binding.videoSeekBar.max = duration
            binding.videoTimeTotal.text = formatMs(duration.toLong())
            updatePlayPauseIcon()
        }

        binding.videoView.setOnCompletionListener {
            isPlaying = false
            updatePlayPauseIcon()
            progressHandler.removeCallbacks(progressUpdater)
        }

        binding.videoView.setOnErrorListener { _, _, _ ->
            binding.progressBar.visibility = View.GONE
            true
        }

        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.videoContainer.setOnClickListener { toggleChrome() }

        binding.videoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.videoView.seekTo(progress)
                    binding.videoTimeCurrent.text = formatMs(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun togglePlayPause() {
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
            isPlaying = false
            progressHandler.removeCallbacks(progressUpdater)
        } else {
            binding.videoView.start()
            isPlaying = true
            progressHandler.post(progressUpdater)
        }
        updatePlayPauseIcon()
    }

    private fun updatePlayPauseIcon() {
        if (isPlaying || binding.videoView.isPlaying) {
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause_circle)
        } else {
            binding.btnPlayPause.setImageResource(R.drawable.ic_play_circle)
        }
    }

    private fun scaleVideoToFit(videoWidth: Int, videoHeight: Int) {
        val containerWidth = binding.videoContainer.width
        val containerHeight = binding.videoContainer.height

        if (videoWidth > 0 && videoHeight > 0 && containerWidth > 0 && containerHeight > 0) {
            val videoAspect = videoWidth.toFloat() / videoHeight
            val containerAspect = containerWidth.toFloat() / containerHeight

            val layoutParams = binding.videoView.layoutParams
            if (videoAspect > containerAspect) {
                layoutParams.width = containerWidth
                layoutParams.height = (containerWidth / videoAspect).toInt()
            } else {
                layoutParams.height = containerHeight
                layoutParams.width = (containerHeight * videoAspect).toInt()
            }
            binding.videoView.layoutParams = layoutParams
        }
    }

    private fun toggleChrome() {
        val visible = binding.topInfoBar.visibility == View.VISIBLE
        val nextVis = if (visible) View.GONE else View.VISIBLE
        binding.topInfoBar.visibility = nextVis
        binding.bottomActionBar.visibility = nextVis
        if (isVideo) binding.videoPlayerRow.visibility = nextVis
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }

    override fun onPause() {
        super.onPause()
        if (isVideo && binding.videoView.isPlaying) {
            binding.videoView.pause()
            isPlaying = false
            progressHandler.removeCallbacks(progressUpdater)
            updatePlayPauseIcon()
        }
    }

    override fun onDestroy() {
        progressHandler.removeCallbacks(progressUpdater)
        super.onDestroy()
        if (isVideo) binding.videoView.stopPlayback()
    }
}
```

### Step 6 — Wire `MainActivity` for result routing

Find `openMediaViewer` (around line 739) and the field declarations
near the top of the class.

**Add the launcher field** (alongside other `registerForActivityResult`
launchers, or near `hintManager` / `hintPreferences`):

```kotlin
private val mediaViewerLauncher = registerForActivityResult(
    androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode != RESULT_OK) return@registerForActivityResult
    val data = result.data ?: return@registerForActivityResult
    val action = data.getStringExtra(MediaViewerActivity.EXTRA_ACTION) ?: return@registerForActivityResult
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        data.getParcelableExtra(MediaViewerActivity.EXTRA_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        data.getParcelableExtra(MediaViewerActivity.EXTRA_URI) as? Uri
    } ?: return@registerForActivityResult

    when (action) {
        MediaViewerActivity.ACTION_KEPT -> viewModel.markAsViewed(uri)
        MediaViewerActivity.ACTION_DELETE -> performTrash(setOf(uri))
    }
}
```

**Replace `openMediaViewer`**:

```kotlin
private fun openMediaViewer(item: MediaItem) {
    val intent = Intent(this, MediaViewerActivity::class.java).apply {
        putExtra(MediaViewerActivity.EXTRA_URI, item.uri)
        putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, item.mediaType.ordinal)
        putExtra(MediaViewerActivity.EXTRA_DISPLAY_NAME, item.displayName)
        putExtra(MediaViewerActivity.EXTRA_DATE_ADDED, item.dateAdded)
        putExtra(MediaViewerActivity.EXTRA_SIZE, item.size)
        putExtra(MediaViewerActivity.EXTRA_SOURCE, item.source.ordinal)
    }
    mediaViewerLauncher.launch(intent)
}
```

> The `SourceType` enum has a `label` field used elsewhere; its
> `ordinal` serialises cleanly across the intent. If `MediaItem.source`
> isn't the exact field name, grep for `source:` in `MediaItem.kt` and
> use whatever's there.

### Step 7 — Sanity grep

```bash
grep -n "MediaController" app/src/main/java/com/example/gallerycleaner/MediaViewerActivity.kt
grep -n "videoControlsOverlay" app/src/main/java app/src/main/res
grep -n "startActivity(intent)" app/src/main/java/com/example/gallerycleaner/MainActivity.kt | grep -i viewer
```

Expected: no matches for any. (The third grep should miss because
we swapped `startActivity` for `launcher.launch`.)

### Step 8 — Build + test + lint

```bash
./gradlew --stop
./gradlew clean assembleDebug testDebugUnitTest lint
```

All green. The compiler will complain if `MediaItem.source` is named
differently — fix the reference in step 6.

### Step 9 — On-device smoke

```bash
./gradlew installDebug
```

Test matrix:

1. **Tap a photo** → viewer opens. Header shows filename + `date ·
   size · source`. Bottom shows 3 buttons. No video row.
2. **Tap `×`** → back to grid, photo not dimmed (kept = not set).
3. **Reopen same photo → tap Keep** → haptic tick, viewer closes,
   photo is dimmed/desaturated in the grid.
4. **Tap a different photo → tap Delete** → viewer closes; delete
   success card pops with freed size + Undo ring. Undo restores.
5. **Tap Info** → snackbar "Info sheet coming soon" appears. No
   crash, viewer stays open.
6. **Tap a video** → video row appears at the bottom edge. Play
   button starts playback, SeekBar fills in accent, time labels
   tick in mono. Drag the SeekBar → video seeks.
7. **Tap once on video** → chrome hides (top + video row + action
   bar). Tap again → chrome returns.
8. **Rotate to landscape during video** → video still plays, chrome
   still toggles. (Nothing guaranteed, just check nothing crashes.)

### Step 10 — Mark done + commit

1. Flip `[ ]` → `[x]` on requirement.md's acceptance list.
2. Move the folder `docs/sdd/todo/20260418-013-media-viewer/` →
   `docs/sdd/done/20260418-013-media-viewer/`.
3. Update `docs/sdd/STATUS.md` — row moves to Completed,
   Tasks 1/1, summary:
   `Metadata + source in chrome, Info·Keep·Delete bottom bar, custom accent video player row, result-launcher routing to markAsViewed / performTrash. Info stubs snackbar pending SDD-014.`
4. **Atomic commits:** implementation + doc-done.
