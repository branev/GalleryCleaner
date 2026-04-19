# Task 01: Media Info Bottom Sheet

**Parent:** SDD-20260418-014 — Media Info Bottom Sheet

## What You're Changing

The `Info` button in the media viewer currently shows a
`Coming soon` snackbar (stubbed in SDD-013). This task wires a real
bottom sheet that slides up over the viewer, showing the file's
metadata (size, duration, resolution, capture date, source, path)
and a single `Locate in source folder` action.

## Before vs After

| Thing | Before | After |
|---|---|---|
| Info button behavior | Snackbar `Coming soon` | Opens `MediaInfoBottomSheetFragment` |
| Viewer intent extras | uri, mediaType, displayName, dateAdded, size, source | + `EXTRA_DURATION`, `EXTRA_PATH` |
| Metadata surface | None | Full details list with async resolution + capture-date load |

## Prerequisites

- SDD-002 (tokens, mono font) and SDD-013 (viewer action bar)
  merged.
- `./gradlew --stop` before you start.
- Files to understand first:
  - [MediaItem.kt](../../../../app/src/main/java/com/example/gallerycleaner/MediaItem.kt) —
    has `duration` and `relativePathOrData` already.
  - [FilterBottomSheetFragment.kt onStart/onDestroyView](../../../../app/src/main/java/com/example/gallerycleaner/FilterBottomSheetFragment.kt) —
    status-bar flip pattern to mirror.
  - [MediaViewerActivity.kt setupActionBar](../../../../app/src/main/java/com/example/gallerycleaner/MediaViewerActivity.kt) —
    where the Info button handler lives.

## Step-by-Step Instructions

### Step 1 — Drawables

**`res/drawable/ic_folder.xml`** (24dp):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M10,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2h-8l-2,-2z"
        android:fillColor="#FFFFFF" />
</vector>
```

**`res/drawable/ic_play_badge.xml`** — a 16dp circular black-tint
badge with a white play triangle inside, used as a video overlay on
the 48dp thumbnail:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="oval">
            <solid android:color="#40000000" />
            <size android:width="16dp" android:height="16dp" />
        </shape>
    </item>
    <item android:gravity="center">
        <vector
            android:width="8dp"
            android:height="8dp"
            android:viewportWidth="24"
            android:viewportHeight="24">
            <path
                android:pathData="M7,5 L7,19 L19,12 Z"
                android:fillColor="#FFFFFF" />
        </vector>
    </item>
</layer-list>
```

**`res/drawable/media_info_thumb_bg.xml`** — used for the `outlineProvider`
clip on the thumbnail ImageView:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/surface_alt" />
    <corners android:radius="10dp" />
</shape>
```

### Step 2 — Strings

Add to `res/values/strings.xml` (anywhere sensible — after the
viewer block is fine):

```xml
<!-- Media info sheet (SDD-20260418-014) -->
<string name="info_size">Size</string>
<string name="info_duration">Duration</string>
<string name="info_resolution">Resolution</string>
<string name="info_captured">Captured</string>
<string name="info_source">Source</string>
<string name="info_path">Path</string>
<string name="info_locate_folder">Locate in source folder</string>
<string name="info_media_type_and_source">%1$s · %2$s</string>
<string name="info_photo">Photo</string>
<string name="info_video">Video</string>
<string name="info_unknown">—</string>
<string name="info_locate_failed">Can\'t open folder on this device</string>
```

Leave `viewer_info_coming_soon` alone for now — it's harmless if no
one references it; we can cull it after the viewer integration lands.

### Step 3 — Layout `bottom_sheet_media_info.xml`

Create `res/layout/bottom_sheet_media_info.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@color/surface"
    android:paddingBottom="16dp">

    <!-- Drag handle -->
    <View
        android:layout_width="36dp"
        android:layout_height="4dp"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="8dp"
        android:layout_marginBottom="4dp"
        android:background="@drawable/bottom_sheet_handle" />

    <!-- Header row: thumbnail + filename + subtitle -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="20dp"
        android:paddingTop="16dp"
        android:paddingBottom="12dp">

        <FrameLayout
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:background="@drawable/media_info_thumb_bg"
            android:clipToOutline="true">

            <ImageView
                android:id="@+id/infoThumb"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:scaleType="centerCrop"
                android:importantForAccessibility="no" />

            <ImageView
                android:id="@+id/infoThumbVideoBadge"
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:layout_gravity="bottom|end"
                android:layout_margin="2dp"
                android:src="@drawable/ic_play_badge"
                android:visibility="gone"
                android:importantForAccessibility="no" />
        </FrameLayout>

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="12dp"
            android:orientation="vertical">

            <TextView
                android:id="@+id/infoFilename"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="@color/ink"
                android:textSize="15sp"
                android:textStyle="bold"
                android:maxLines="1"
                android:ellipsize="middle"
                tools:text="IMG_20260412_142217.jpg" />

            <TextView
                android:id="@+id/infoSubtitle"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="2dp"
                android:textColor="@color/ink3"
                android:textSize="12sp"
                android:maxLines="1"
                tools:text="Photo · Viber" />
        </LinearLayout>
    </LinearLayout>

    <!-- Details list populated in MediaInfoBottomSheetFragment -->
    <LinearLayout
        android:id="@+id/infoDetailsContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical" />

    <!-- Locate in source folder action -->
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnLocateFolder"
        style="@style/Widget.Material3.Button.OutlinedButton"
        android:layout_width="match_parent"
        android:layout_height="44dp"
        android:layout_marginHorizontal="20dp"
        android:layout_marginTop="16dp"
        android:insetTop="0dp"
        android:insetBottom="0dp"
        android:text="@string/info_locate_folder"
        android:textColor="@color/ink"
        app:icon="@drawable/ic_folder"
        app:iconTint="@color/ink"
        app:iconGravity="textStart"
        app:cornerRadius="22dp"
        app:strokeColor="@color/line_strong"
        app:strokeWidth="1dp" />

</LinearLayout>
```

### Step 4 — Fragment `MediaInfoBottomSheetFragment.kt`

Create `app/src/main/java/com/example/gallerycleaner/MediaInfoBottomSheetFragment.kt`:

```kotlin
package com.example.gallerycleaner

import android.content.ContentUris
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import coil.load
import coil.request.videoFrameMillis
import com.example.gallerycleaner.databinding.BottomSheetMediaInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaInfoBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMediaInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetMediaInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            args.getParcelable(ARG_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION") args.getParcelable(ARG_URI) as? Uri
        } ?: run { dismiss(); return }

        val displayName = args.getString(ARG_DISPLAY_NAME).orEmpty()
        val mediaType = MediaType.values()[args.getInt(ARG_MEDIA_TYPE, 0)]
        val source = SourceType.values()[args.getInt(ARG_SOURCE, SourceType.OTHER.ordinal)]
        val size = args.getLong(ARG_SIZE, 0L)
        val dateAdded = args.getLong(ARG_DATE_ADDED, 0L)
        val duration = args.getLong(ARG_DURATION, 0L)
        val path = args.getString(ARG_PATH).orEmpty()

        setupHeader(uri, displayName, mediaType, source)
        setupDetails(mediaType, size, duration, dateAdded, source, path)
        setupLocateFolder(uri)

        loadExtraMetadata(uri, dateAdded)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { w ->
            WindowCompat.getInsetsController(w, w.decorView)
                .isAppearanceLightStatusBars = false
        }
    }

    override fun onDestroyView() {
        requireActivity().window.let { w ->
            WindowCompat.getInsetsController(w, w.decorView)
                .isAppearanceLightStatusBars = true
        }
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeader(uri: Uri, displayName: String, mediaType: MediaType, source: SourceType) {
        binding.infoFilename.text = displayName
        val typeLabel = if (mediaType == MediaType.VIDEO) {
            getString(R.string.info_video)
        } else {
            getString(R.string.info_photo)
        }
        binding.infoSubtitle.text =
            getString(R.string.info_media_type_and_source, typeLabel, source.label)

        binding.infoThumb.load(uri) {
            if (mediaType == MediaType.VIDEO) videoFrameMillis(0L)
            crossfade(true)
        }
        binding.infoThumbVideoBadge.visibility =
            if (mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE
    }

    private fun setupDetails(
        mediaType: MediaType,
        size: Long,
        duration: Long,
        dateAdded: Long,
        source: SourceType,
        path: String,
    ) {
        binding.infoDetailsContainer.removeAllViews()

        val mono: Typeface? = ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono)

        addRow(R.string.info_size, Formatter.formatFileSize(requireContext(), size))
        if (mediaType == MediaType.VIDEO) {
            addRow(R.string.info_duration, formatMs(duration), typeface = mono)
        }
        // Resolution populated async in loadExtraMetadata; start with "—".
        resolutionValueView = addRow(
            R.string.info_resolution,
            getString(R.string.info_unknown),
            typeface = mono,
        )
        // Captured; replaced by DATE_TAKEN if present.
        capturedValueView = addRow(R.string.info_captured, formatDate(dateAdded))
        addRow(R.string.info_source, source.label)
        addRow(R.string.info_path, path.ifEmpty { getString(R.string.info_unknown) },
            typeface = mono, isLast = true)
    }

    private var resolutionValueView: TextView? = null
    private var capturedValueView: TextView? = null

    private fun addRow(
        labelRes: Int,
        value: String,
        typeface: Typeface? = null,
        isLast: Boolean = false,
    ): TextView {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }
        val label = TextView(ctx).apply {
            setText(labelRes)
            textSize = 13f
            setTextColor(ContextCompat.getColor(ctx, R.color.ink3))
            layoutParams = LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val valueView = TextView(ctx).apply {
            text = value
            textSize = 13f
            setTextColor(ContextCompat.getColor(ctx, R.color.ink))
            if (typeface != null) this.typeface = typeface
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setTextIsSelectable(true)  // useful for path
        }
        row.addView(label)
        row.addView(valueView)
        binding.infoDetailsContainer.addView(row)

        if (!isLast) {
            val divider = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                )
                setBackgroundColor(ContextCompat.getColor(ctx, R.color.line))
            }
            binding.infoDetailsContainer.addView(divider)
        }
        return valueView
    }

    private fun setupLocateFolder(uri: Uri) {
        binding.btnLocateFolder.setOnClickListener {
            val ctx = requireContext()
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, ctx.contentResolver.getType(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(viewIntent, getString(R.string.info_locate_folder))
            try {
                startActivity(chooser)
            } catch (_: Exception) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.info_locate_failed),
                    Snackbar.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun loadExtraMetadata(uri: Uri, fallbackDate: Long) {
        Thread {
            val ctx = context ?: return@Thread
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

            runCatching {
                ctx.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        width = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH))
                        height = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val idx = c.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                            if (idx >= 0 && !c.isNull(idx)) {
                                dateTakenMillis = c.getLong(idx)
                            }
                        }
                    }
                }
            }

            val resolutionText = if (width > 0 && height > 0) {
                "$width × $height"
            } else {
                getString(R.string.info_unknown)
            }
            val capturedSec = if (dateTakenMillis > 0) dateTakenMillis / 1000L else fallbackDate

            requireActivity().runOnUiThread {
                if (_binding != null) {
                    resolutionValueView?.text = resolutionText
                    capturedValueView?.text = formatDate(capturedSec)
                }
            }
        }.start()
    }

    private fun formatDate(unixSeconds: Long): String {
        if (unixSeconds <= 0) return getString(R.string.info_unknown)
        val date = Date(unixSeconds * 1000L)
        return SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(date)
    }

    private fun formatMs(ms: Long): String {
        if (ms <= 0) return getString(R.string.info_unknown)
        val totalSec = ms / 1000
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "MediaInfoBottomSheet"

        private const val ARG_URI = "arg_uri"
        private const val ARG_DISPLAY_NAME = "arg_display_name"
        private const val ARG_MEDIA_TYPE = "arg_media_type"
        private const val ARG_SOURCE = "arg_source"
        private const val ARG_SIZE = "arg_size"
        private const val ARG_DATE_ADDED = "arg_date_added"
        private const val ARG_DURATION = "arg_duration"
        private const val ARG_PATH = "arg_path"

        fun newInstance(
            uri: Uri,
            displayName: String?,
            mediaType: MediaType,
            source: SourceType,
            size: Long,
            dateAdded: Long,
            duration: Long,
            path: String?,
        ) = MediaInfoBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_URI, uri)
                putString(ARG_DISPLAY_NAME, displayName)
                putInt(ARG_MEDIA_TYPE, mediaType.ordinal)
                putInt(ARG_SOURCE, source.ordinal)
                putLong(ARG_SIZE, size)
                putLong(ARG_DATE_ADDED, dateAdded)
                putLong(ARG_DURATION, duration)
                putString(ARG_PATH, path)
            }
        }
    }
}
```

### Step 5 — Extend the viewer intent contract

In `MediaViewerActivity.kt` (companion object), add two new extras:

```kotlin
const val EXTRA_DURATION = "extra_duration"
const val EXTRA_PATH = "extra_path"
```

Read them in `onCreate` near the other extras:

```kotlin
val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
val path = intent.getStringExtra(EXTRA_PATH).orEmpty()
```

Store them on the activity (or in a property) so the Info button
handler can pass them.

### Step 6 — Wire the Info button

Replace the snackbar stub in `setupActionBar`:

```kotlin
binding.btnOverlayInfo.setOnClickListener {
    MediaInfoBottomSheetFragment.newInstance(
        uri = uri,
        displayName = displayName,
        mediaType = if (isVideo) MediaType.VIDEO else MediaType.PHOTO,
        source = if (sourceOrdinal in SourceType.values().indices) {
            SourceType.values()[sourceOrdinal]
        } else SourceType.OTHER,
        size = size,
        dateAdded = dateAdded,
        duration = duration,
        path = path,
    ).show(supportFragmentManager, MediaInfoBottomSheetFragment.TAG)
}
```

Note: you'll need `displayName`, `sourceOrdinal`, `size`,
`dateAdded`, `duration`, `path` accessible where the click handler
runs. Easiest fix: store them as `lateinit var` / `private var`
properties on the activity populated in `onCreate`, or pass them
into `setupActionBar(...)` as parameters.

### Step 7 — Pass the new extras from `MainActivity.openMediaViewer`

```kotlin
private fun openMediaViewer(item: MediaItem) {
    val intent = Intent(this, MediaViewerActivity::class.java).apply {
        putExtra(MediaViewerActivity.EXTRA_URI, item.uri)
        putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, item.mediaType.ordinal)
        putExtra(MediaViewerActivity.EXTRA_DISPLAY_NAME, item.displayName)
        putExtra(MediaViewerActivity.EXTRA_DATE_ADDED, item.dateAdded)
        putExtra(MediaViewerActivity.EXTRA_SIZE, item.size)
        putExtra(MediaViewerActivity.EXTRA_SOURCE, item.source.ordinal)
        putExtra(MediaViewerActivity.EXTRA_DURATION, item.duration)
        putExtra(MediaViewerActivity.EXTRA_PATH, item.relativePathOrData)
    }
    mediaViewerLauncher.launch(intent)
}
```

### Step 8 — Build + lint

```bash
./gradlew --stop
./gradlew clean assembleDebug testDebugUnitTest lint
```

All three green. If lint flags `viewer_info_coming_soon` as unused,
delete it from `strings.xml` and rebuild.

### Step 9 — On-device smoke

```bash
./gradlew installDebug
```

Test matrix:

1. **Tap a photo → Info** → sheet slides up. Header shows the photo
   thumb with filename + `Photo · {Source}` subtitle. No play badge
   on the thumbnail.
2. Details rows: Size, Resolution, Captured, Source, Path. **No
   Duration row for photos.**
3. Resolution shows `—` briefly, then `W × H` in mono after ~50ms.
4. Captured date shows capture time (photos from camera have `DATE_TAKEN`;
   downloaded media fall back to `dateAdded`).
5. Path is in mono and wraps; long-press to select.
6. **Tap a video → Info** → thumbnail shows the first frame with a
   small play glyph in the bottom-right corner; subtitle is
   `Video · {Source}`; Duration row appears with `0:48` in mono.
7. **Tap `Locate in source folder`** → chooser opens with available
   apps (Files / Photos / etc). On a device with none, a snackbar
   explains.
8. **Swipe down / tap scrim / system back** → sheet dismisses,
   returns to viewer in its previous state (paused at same frame
   if video was paused).
9. Status bar icons go light while sheet is open, revert on dismiss.

### Step 10 — Mark done + commit

1. Flip `[ ]` → `[x]` on `requirement.md` acceptance list.
2. Move `docs/sdd/todo/20260418-014-info-sheet/` →
   `docs/sdd/done/20260418-014-info-sheet/`.
3. Update `docs/sdd/STATUS.md` — move the row from Pending to
   Completed, tasks `1/1`, one-line summary:
   `New MediaInfoBottomSheetFragment with async Resolution/Captured load, Locate-in-folder chooser, status-bar flip. Final umbrella SDD — 001 can now close.`
4. **Atomic commits:** implementation + doc-done.

Since this is the last umbrella SDD, also consider a separate final
commit that closes `SDD-20260418-001` itself — tick its umbrella
acceptance items and move it to `done/`. That's a judgment call
based on whether the user wants a clear "visual redesign complete"
marker.
