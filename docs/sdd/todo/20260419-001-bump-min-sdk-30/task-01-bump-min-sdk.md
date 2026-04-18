# Task 01: Bump minSdk to 30 and drop compat shims

**Parent:** SDD-20260419-001 — Bump minSdk to 30

## What You're Changing

Raise the app's minimum Android version from 8.0 (API 26) to 11 (API 30)
and delete the code we only needed to support the older floor:
`CustomTypefaceSpan`, two `WindowCompat.getInsetsController` calls, a
handful of `Build.VERSION.SDK_INT` branches whose "else" arms are now
unreachable, and the README minimum-version line.

No features gained. No features lost. Fewer lines of code, one fewer
class.

## Before vs After

| Thing | Before | After |
|---|---|---|
| `minSdk` | 26 (Android 8.0) | **30 (Android 11)** |
| Custom span class | `CustomTypefaceSpan` (MetricAffectingSpan subclass) | deleted — use `TypefaceSpan(typeface)` directly |
| Filter sheet status-bar flip | `WindowCompat.getInsetsController(w, w.decorView).isAppearanceLightStatusBars = false` | `w.insetsController?.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS)` |
| MediaStore URIs (PHOTO/VIDEO) | `if (SDK_INT >= 29) getContentUri(VOLUME_EXTERNAL) else EXTERNAL_CONTENT_URI` | always `getContentUri(VOLUME_EXTERNAL)` |
| Projection column | `if (SDK_INT >= 29) RELATIVE_PATH else DATA (deprecated)` | always `RELATIVE_PATH` |
| Trash flow | `if (SDK_INT >= R) systemTrash else legacyDelete` | always `systemTrash` (`createTrashRequest`) |
| Restore flow | `if (SDK_INT >= R) systemUntrash else // nothing` | always `systemUntrash` |

## Prerequisites

- `./gradlew --stop` before you start.
- A device or emulator running Android 11+ for the smoke test (API 30
  or higher).

## Step-by-Step Instructions

### Step 1 — Bump `minSdk` in the Gradle config

Open `app/build.gradle.kts`. In the `defaultConfig` block:

```kotlin
minSdk = 26
```

Change to:

```kotlin
minSdk = 30
```

Nothing else in this file changes. `compileSdk` and `targetSdk` stay
at 35.

### Step 2 — Delete `CustomTypefaceSpan.kt`

From the project root:

```bash
rm app/src/main/java/com/example/gallerycleaner/CustomTypefaceSpan.kt
```

The class exists solely to avoid `TypefaceSpan(Typeface)`, which is
API 28+. At minSdk 30 it's always available.

### Step 3 — Use `TypefaceSpan(typeface)` directly in the filter sheet

Open `FilterBottomSheetFragment.kt`. In `sourceChipLabel`, replace:

```kotlin
spannable.setSpan(
    CustomTypefaceSpan(monoTypeface),
    start, text.length,
    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
)
```

with:

```kotlin
spannable.setSpan(
    android.text.style.TypefaceSpan(monoTypeface),
    start, text.length,
    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
)
```

> Inline the fully-qualified name or add an import — your call. There's
> also `androidx.core.content.res.ResourcesCompat` at the top; leave
> that, it's fine.

### Step 4 — Drop `WindowCompat` from the filter sheet

Same file, `onStart()` and `onDestroyView()`:

**Remove** the import at the top:

```kotlin
import androidx.core.view.WindowCompat
```

**Add** this import:

```kotlin
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
```

**Replace** the `onStart()` body:

```kotlin
override fun onStart() {
    super.onStart()
    // Light status-bar icons while the sheet's dark scrim is showing.
    dialog?.window?.insetsController?.setSystemBarsAppearance(
        0,
        APPEARANCE_LIGHT_STATUS_BARS
    )
}
```

**Replace** the `onDestroyView()` body (keeping the existing
`super.onDestroyView()` and `_binding = null`):

```kotlin
override fun onDestroyView() {
    // Restore dark status-bar icons for the main activity.
    requireActivity().window.insetsController?.setSystemBarsAppearance(
        APPEARANCE_LIGHT_STATUS_BARS,
        APPEARANCE_LIGHT_STATUS_BARS
    )
    super.onDestroyView()
    _binding = null
}
```

### Step 5 — Collapse `SDK_INT >= 29` branches in `GalleryViewModel`

Open `GalleryViewModel.kt`. Find `queryMedia` (around line 357).

**Replace** the `when` block assigning `collection`:

```kotlin
val collection = when (mediaType) {
    MediaType.PHOTO -> if (Build.VERSION.SDK_INT >= 29) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    MediaType.VIDEO -> if (Build.VERSION.SDK_INT >= 29) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }
}
```

with:

```kotlin
val collection = when (mediaType) {
    MediaType.PHOTO -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    MediaType.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
}
```

A few lines down, replace:

```kotlin
if (Build.VERSION.SDK_INT >= 29) {
    projection += MediaStore.MediaColumns.RELATIVE_PATH
} else {
    @Suppress("DEPRECATION")
    projection += MediaStore.MediaColumns.DATA
}
```

with:

```kotlin
projection += MediaStore.MediaColumns.RELATIVE_PATH
```

Inside the `cursor?.use` block, replace:

```kotlin
val pathCol = if (Build.VERSION.SDK_INT >= 29) {
    cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
} else {
    @Suppress("DEPRECATION")
    cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
}
```

with:

```kotlin
val pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
```

After these three edits, the `@Suppress("DEPRECATION")` annotations in
this function should be gone. If you see `android.provider.MediaStore`
imports that are no longer referenced, Android Studio's
"Optimize Imports" (Ctrl+Alt+O) will clean them up.

### Step 6 — Collapse `SDK_INT >= R` branches in `MainActivity`

Open `MainActivity.kt`. Three spots.

**6a. `handleDelete()`** (around line 766):

```kotlin
private fun handleDelete() {
    val selectedItems = viewModel.getSelectedItems()
    if (selectedItems.isEmpty()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+ — skip custom dialog, system trash dialog provides confirmation
        performTrash(selectedItems)
    } else {
        // Pre-Android 11 — keep confirmation since delete is irreversible
        val state = viewModel.uiState.value
        val hiddenCount = if (state is GalleryUiState.Selection) state.hiddenSelectedCount else 0
        showTrashConfirmationDialog(selectedItems.size, hiddenCount) {
            performTrash(selectedItems)
        }
    }
}
```

Replace with:

```kotlin
private fun handleDelete() {
    val selectedItems = viewModel.getSelectedItems()
    if (selectedItems.isEmpty()) return
    // Android 11+ — system trash dialog provides confirmation; no custom dialog needed.
    performTrash(selectedItems)
}
```

You can also **delete** `showTrashConfirmationDialog(...)` entirely
(the fallback method — search the file to confirm no one else calls it)
and the `MaterialAlertDialogBuilder` import if it's now unused.

**6b. `performTrash()`** (around line 800):

The whole `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { ... }
else { legacy delete }` becomes just the modern branch:

```kotlin
private fun performTrash(uris: Set<Uri>) {
    pendingTrashUris = uris
    pendingTrashSize = viewModel.getSelectedItemsTotalSize()
    isRestoreOperation = false

    val pendingIntent = MediaStore.createTrashRequest(contentResolver, uris.toList(), true)
    val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
    trashRequestLauncher.launch(request)
}
```

Delete the whole legacy `else` block (the `for (uri in uris)` loop with
direct `contentResolver.delete`). It's unreachable at minSdk 30.

**6c. `performRestore()`** (around line 837):

```kotlin
private fun performRestore(uris: Set<Uri>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        pendingTrashUris = uris
        isRestoreOperation = true
        // createTrashRequest with isTrashed=false restores items
        val pendingIntent = MediaStore.createTrashRequest(contentResolver, uris.toList(), false)
        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        trashRequestLauncher.launch(request)
    }
    // Pre-Android 11 doesn't have system trash, so no restore is possible
}
```

Replace with:

```kotlin
private fun performRestore(uris: Set<Uri>) {
    pendingTrashUris = uris
    isRestoreOperation = true
    // createTrashRequest with isTrashed=false restores items
    val pendingIntent = MediaStore.createTrashRequest(contentResolver, uris.toList(), false)
    val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
    trashRequestLauncher.launch(request)
}
```

**Do NOT change** the two `SDK_INT >= 33` branches later in the file
(`hasReadPermission`, `requestPermissions` area). API 33 is above our
new floor of 30; those branches are still live.

### Step 7 — Remove unused imports and dead code

Quick cleanup pass:

- In `GalleryViewModel.kt`, `android.os.Build` may no longer be
  referenced (the `SDK_INT >= 33` check in `MainActivity.kt` is where
  `Build` still lives). Let Android Studio report unused imports.
- In `MainActivity.kt`, `MaterialAlertDialogBuilder` may be unused if
  you deleted `showTrashConfirmationDialog`. Remove the import.
- No need to keep any `@Suppress("DEPRECATION")` annotations that were
  guarding the now-dead `MediaStore.MediaColumns.DATA` path.

Run **Code → Optimize Imports** (Ctrl+Alt+O) on the three changed
Kotlin files to sweep up automatically.

### Step 8 — Update the README

Open `README.md`. Find the "Requirements" section:

```markdown
- Android 8.0 (API 26) or higher
```

Replace with:

```markdown
- Android 11 (API 30) or higher
```

### Step 9 — Build, test, lint

```bash
./gradlew --stop
./gradlew clean assembleDebug testDebugUnitTest lint
```

Expected: `BUILD SUCCESSFUL`, no lint errors. The two pre-existing
`statusBarColor` deprecation warnings are unrelated.

Likely failure modes:
- `Unresolved reference: CustomTypefaceSpan` → Step 3 missed the inline
  replacement in `sourceChipLabel`.
- `Unresolved reference: Build` in a file where you deleted all uses →
  remove the `import android.os.Build` line.
- `Unresolved reference: showTrashConfirmationDialog` → you deleted
  the method but forgot a caller (unlikely, but check the file).

### Step 10 — Verify the manifest floor

Confirm the APK advertises the new minimum:

```bash
./gradlew :app:processDebugMainManifest
grep minSdkVersion app/build/intermediates/merged_manifest/debug/AndroidManifest.xml
```

Expected:
```
android:minSdkVersion="30"
```

### Step 11 — Smoke test on-device

Install on an Android 11+ device (or emulator). Run through:

1. Launch the app, grant media permissions, grid loads.
2. Tap Filters → sheet opens, status bar icons go light, Cancel
   dismisses, status bar goes dark again.
3. Open the filter sheet → source chips show `Name  N` with mono
   counts (regression check on the TypefaceSpan swap).
4. Long-press a tile → selection bar appears. Tap Delete → system
   trash dialog appears (no more custom confirmation). Confirm →
   items go to trash, undo snackbar shows.
5. Tap the media viewer → verify it still opens and navigates.
6. Close and re-launch — no crashes.

## Definition of Done

- [ ] `minSdk = 30` in `app/build.gradle.kts`
- [ ] `CustomTypefaceSpan.kt` deleted
- [ ] No `WindowCompat.getInsetsController` calls anywhere in `app/src/`
- [ ] No `Build.VERSION.SDK_INT >= 29` or `>= Build.VERSION_CODES.R`
      branches left (use `grep -rn "SDK_INT >= 29\|VERSION_CODES.R"
      app/src/`)
- [ ] README requirements line reflects Android 11
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [ ] Smoke test (Step 11) passes
- [ ] PR titled `SDD-20260419-001 — Bump minSdk to 30`

## Known gotchas

- **`WindowCompat.setDecorFitsSystemWindows`** in `MainActivity.kt`
  and `MediaViewerActivity.kt` **stays**. The compat wrapper compiles
  to the same call at minSdk 30, and switching to
  `window.setDecorFitsSystemWindows(false)` directly is churn without
  benefit. Leave it.
- **`ViewCompat.setOnApplyWindowInsetsListener`** stays for the same
  reason plus ergonomics — it uses `WindowInsetsCompat`, which exposes
  `Type.systemBars()` / `Type.displayCutout()` accessors the raw
  platform API doesn't.
- **`ContextCompat.checkSelfPermission`** stays. The permission code
  still needs the `SDK_INT >= 33` branching for READ_MEDIA_* vs
  READ_EXTERNAL_STORAGE, and the compat wrapper does no harm.
- **Don't touch the 33+ checks.** If you see `SDK_INT >= 33` or
  `>= Build.VERSION_CODES.TIRAMISU`, those are for the new media
  permission scheme and are still required above our new floor.
- **Emulator on API 29 or below** won't run the APK after this bump —
  the device-selector dropdown in Android Studio will grey them out.
  Create an API 30+ AVD if you don't have one.
