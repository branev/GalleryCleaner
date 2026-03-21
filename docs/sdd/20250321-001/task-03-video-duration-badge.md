# Task 03: Video Duration Badge

**Parent:** SDD-20250321-001 — Gallery Grid Visual Redesign

## What You're Changing

Currently, video items show a white play-circle icon (24dp) at the top-left corner. There is no duration information.

You will replace this with a small dark badge showing the play icon and duration text (e.g., "▶ 0:45").

This task has two parts:
1. **Data:** Query video duration from MediaStore and add it to `MediaItem`
2. **UI:** Replace the icon-only indicator with a duration badge

## Before vs After

**Before:**
```
┌──────────────┐
│ ▶(circle)    │
│              │
│              │
│  [WhatsApp]  │
└──────────────┘
```

**After:**
```
┌──────────────┐
│ [▶ 0:45]     │
│              │
│              │
│  [WhatsApp]  │
└──────────────┘
```

## Step-by-Step Instructions

### Step 1: Add the duration field to MediaItem

Open `app/src/main/java/com/example/gallerycleaner/MediaItem.kt`.

Find the `MediaItem` data class and add a `duration` field:

```kotlin
data class MediaItem(
    val uri: Uri,
    val displayName: String?,
    val bucket: String?,
    val relativePathOrData: String?,
    val source: SourceType,
    val mediaType: MediaType,
    val dateAdded: Long = 0L,
    val size: Long = 0L,
    val duration: Long = 0L  // Video duration in milliseconds, 0 for photos
)
```

> **Why milliseconds?** That's what MediaStore returns for `DURATION`. We'll format it to `M:SS` in the adapter.

### Step 2: Query duration from MediaStore

Open `app/src/main/java/com/example/gallerycleaner/GalleryViewModel.kt`.

Find the `queryMedia()` method. You need to:

**a)** Add `DURATION` to the video projection. Find where `MediaStore.Video.Media` is queried. Look for the projection array for videos and add `MediaStore.Video.VideoColumns.DURATION`.

**b)** Read the duration value from the cursor. Find where `MediaItem` is constructed for video items and pass the duration.

The exact code depends on how `queryMedia()` is structured, but the key parts are:

For the **projection** (the columns you ask MediaStore to return):
```kotlin
// Add this to the video projection array:
MediaStore.Video.VideoColumns.DURATION
```

For **reading the cursor**:
```kotlin
// After getting other columns from the cursor:
val durationMs = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION))
```

For **constructing the MediaItem**:
```kotlin
MediaItem(
    // ... existing fields ...
    duration = durationMs  // Add this
)
```

> **Important:** Only video queries have the DURATION column. For image queries, leave `duration = 0L` (the default).

### Step 3: Create the video duration badge drawable

Create a new file `app/src/main/res/drawable/video_badge_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="4dp" />
    <solid android:color="#99000000" />
</shape>
```

> This is a slightly rounded rectangle with 60% black background — enough contrast to read white text on any thumbnail.

### Step 4: Create a small play icon

Create a new file `app/src/main/res/drawable/ic_play_small.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="10dp"
    android:height="10dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M8,5v14l11,-7z" />
</vector>
```

> This is just a simple white play triangle, no circle border. It's sized at 10dp to fit inside the small badge.

### Step 5: Replace the video indicator in item_image.xml

Open `app/src/main/res/layout/item_image.xml`.

Find the `videoIndicator` ImageView (around line 59). Replace it with:

```xml
<!-- Video duration badge (top-left) -->
<LinearLayout
    android:id="@+id/videoDurationBadge"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    android:background="@drawable/video_badge_bg"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingHorizontal="6dp"
    android:paddingVertical="2dp"
    android:visibility="gone"
    app:layout_constraintStart_toStartOf="@id/imageView"
    app:layout_constraintTop_toTopOf="@id/imageView">

    <ImageView
        android:layout_width="10dp"
        android:layout_height="10dp"
        android:importantForAccessibility="no"
        android:src="@drawable/ic_play_small" />

    <TextView
        android:id="@+id/videoDuration"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="4dp"
        android:textColor="@android:color/white"
        android:textSize="10sp"
        android:textStyle="bold" />

</LinearLayout>
```

> **Note:** The old ID was `videoIndicator`. The new ID is `videoDurationBadge`. You must update all references in Kotlin code.

### Step 6: Update ImageAdapter.kt

Open `app/src/main/java/com/example/gallerycleaner/ImageAdapter.kt`.

**a)** Add a helper method to format duration. Add this inside the `companion object` block at the bottom:

```kotlin
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
```

**b)** Update the `bind()` method. Find the section that shows/hides `videoIndicator` (around line 111). Replace it with:

```kotlin
// Show video duration badge for videos
if (item.mediaType == MediaType.VIDEO) {
    binding.videoDurationBadge.visibility = View.VISIBLE
    binding.videoDuration.text = formatDuration(item.duration)
} else {
    binding.videoDurationBadge.visibility = View.GONE
}
```

**c)** Search the entire file for any other references to `videoIndicator` and replace with `videoDurationBadge`. There shouldn't be any outside of `bind()`, but double-check.

### Step 7: Update MainActivity.kt references (if any)

Search `MainActivity.kt` for `videoIndicator`. If there are any references, update them to `videoDurationBadge`. There likely aren't any since the adapter handles this view.

### Step 8: Build and verify

```bash
./gradlew assembleDebug
```

Common issues:
- **Build error about `videoIndicator` not found**: You still have a reference to the old ID somewhere. Search for `videoIndicator` across all Kotlin files.
- **MediaStore DURATION column not found**: Make sure you only added it to the **video** query projection, not the image query.

### Step 9: Run tests

```bash
./gradlew testDebugUnitTest
```

If tests create `MediaItem` instances, they may fail because of the new `duration` parameter. Since it has a default value (`0L`), existing test code should still compile. If any test explicitly lists all parameters, add `duration = 0L`.

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/java/.../MediaItem.kt` | Added `duration: Long = 0L` field |
| `app/src/main/java/.../GalleryViewModel.kt` | Query `DURATION` column for videos |
| `app/src/main/res/drawable/video_badge_bg.xml` | **New file** — dark rounded rectangle background |
| `app/src/main/res/drawable/ic_play_small.xml` | **New file** — small white play triangle |
| `app/src/main/res/layout/item_image.xml` | Replaced `videoIndicator` with `videoDurationBadge` container |
| `app/src/main/java/.../ImageAdapter.kt` | Format and display duration, updated view references |

## Acceptance Criteria

- [ ] Video items show a dark badge at the top-left with "▶ 0:45" style text
- [ ] Photo items do NOT show the video badge
- [ ] Duration is formatted as `M:SS` (e.g., "0:45", "1:30", "12:05")
- [ ] The play icon is a small white triangle (not the old circle icon)
- [ ] The badge background is semi-transparent dark with rounded corners
- [ ] Existing tests still pass
- [ ] Build succeeds
