# Task 01: Delete Flow Improvements

**Parent:** SDD-20260321-001 — Delete Flow Improvements

## What You're Changing

Three changes to the delete experience:
1. Remove the custom confirmation dialog (Android 11+ already has a system dialog)
2. Show live file size in the selection action bar
3. After successful delete, show a full-screen celebration overlay with "Done! X items deleted" and an 8-second undo snackbar

## Step-by-Step Instructions

### Step 1: Add new colors and strings

Open `app/src/main/res/values/colors.xml` and add:

```xml
<!-- Delete success overlay (SDD-20260321-001) -->
<color name="overlay_bg">#99000000</color>
<color name="success_checkmark">#34D399</color>
<color name="snackbar_bg">#2C2C2E</color>
<color name="snackbar_undo_bg">#2DD4BF</color>
<color name="snackbar_undo_text">#134E4A</color>
```

Open `app/src/main/res/values/strings.xml` and add:

```xml
<!-- Delete success overlay (SDD-20260321-001) -->
<string name="delete_success_title">Done! %1$d items deleted</string>
<string name="space_saved">%1$s of space saved</string>
```

### Step 2: Create the large checkmark drawable

Create `app/src/main/res/drawable/ic_check_large.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="120dp"
    android:height="120dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M20,6L9,17L4,12"
        android:strokeWidth="2.5"
        android:strokeColor="@color/success_checkmark"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="@android:color/transparent" />
</vector>
```

### Step 3: Add the success overlay layout to activity_main.xml

Open `app/src/main/res/layout/activity_main.xml`.

Add this **just before** the closing `</androidx.constraintlayout.widget.ConstraintLayout>` tag (it must be on top of everything):

```xml
<!-- Delete Success Overlay -->
<FrameLayout
    android:id="@+id/deleteSuccessOverlay"
    android:layout_width="0dp"
    android:layout_height="0dp"
    android:background="@color/overlay_bg"
    android:clickable="true"
    android:focusable="true"
    android:visibility="gone"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent">

    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:gravity="center"
        android:orientation="vertical">

        <ImageView
            android:id="@+id/successCheckmark"
            android:layout_width="120dp"
            android:layout_height="120dp"
            android:src="@drawable/ic_check_large"
            android:importantForAccessibility="no" />

        <TextView
            android:id="@+id/successTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:textColor="@android:color/white"
            android:textSize="28sp"
            android:textStyle="bold"
            android:gravity="center" />

    </LinearLayout>

</FrameLayout>
```

### Step 4: Add file size TextView to the selection action bar

In the same file, find the selection action bar's inner `LinearLayout`. Replace the empty spacer `View` (the one with `layout_weight="1"`) with:

```xml
<TextView
    android:id="@+id/selectionSize"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_weight="1"
    android:gravity="center"
    android:textSize="14sp"
    android:textStyle="bold"
    android:textColor="?attr/colorOnSurface" />
```

### Step 5: Update MainActivity.kt — Remove custom dialog for Android 11+

Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

Find `handleDelete()` and replace it with:

```kotlin
private fun handleDelete() {
    val selectedItems = viewModel.getSelectedItems()
    if (selectedItems.isEmpty()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+ — skip our dialog, go straight to system trash dialog
        performTrash(selectedItems)
    } else {
        // Pre-Android 11 — keep confirmation since delete is irreversible (no system trash)
        val state = viewModel.uiState.value
        val hiddenCount = if (state is GalleryUiState.Selection) state.hiddenSelectedCount else 0
        showTrashConfirmationDialog(selectedItems.size, hiddenCount) {
            performTrash(selectedItems)
        }
    }
}
```

### Step 6: Update MainActivity.kt — Show file size in selection bar

Find the `is GalleryUiState.Selection ->` block in `renderState()`. Add this line after the toolbar title is set:

```kotlin
// Show total size of selected items
binding.selectionSize.text = Formatter.formatFileSize(this, viewModel.getSelectedItemsTotalSize())
```

> **Note:** Use `android.text.format.Formatter.formatFileSize()` — the platform utility handles locale, units, and edge cases. No need for a custom formatter.

### Step 7: Update MainActivity.kt — Success overlay + 8-second undo snackbar

Replace `showTrashSuccessSnackbar()` with:

```kotlin
private var successSnackbar: Snackbar? = null

private fun showTrashSuccessSnackbar(count: Int, totalSize: Long, trashedUris: Set<Uri>) {
    // Show success overlay
    binding.deleteSuccessOverlay.visibility = View.VISIBLE
    binding.successTitle.text = getString(R.string.delete_success_title, count)

    // Animate: fade in overlay, scale up checkmark
    binding.deleteSuccessOverlay.alpha = 0f
    binding.deleteSuccessOverlay.animate()
        .alpha(1f)
        .setDuration(300)
        .start()

    binding.successCheckmark.scaleX = 0f
    binding.successCheckmark.scaleY = 0f
    binding.successCheckmark.animate()
        .scaleX(1f).scaleY(1f)
        .setDuration(400)
        .setInterpolator(OvershootInterpolator())
        .start()

    // Show snackbar with 8-second custom duration
    // Material Components 1.4+ accepts any positive int as milliseconds
    val message = getString(R.string.space_saved,
        Formatter.formatFileSize(this, totalSize))
    val snackbar = Snackbar.make(binding.root, message, 8000)
        .setAction(getString(R.string.undo)) {
            dismissSuccessOverlay()
            performRestore(trashedUris)
        }

    snackbar.addCallback(object : Snackbar.Callback() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            if (event != DISMISS_EVENT_ACTION) {
                dismissSuccessOverlay()
            }
        }
    })

    successSnackbar = snackbar
    snackbar.show()

    // Dismiss overlay on tap
    binding.deleteSuccessOverlay.setOnClickListener {
        snackbar.dismiss()
    }
}

private fun dismissSuccessOverlay() {
    // Cancel any running animations before dismissing
    binding.deleteSuccessOverlay.animate().cancel()
    binding.successCheckmark.animate().cancel()

    binding.deleteSuccessOverlay.animate()
        .alpha(0f)
        .setDuration(200)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.deleteSuccessOverlay.visibility = View.GONE
                binding.deleteSuccessOverlay.animate().setListener(null)
            }
        })
        .start()

    successSnackbar = null
}
```

> **Notes on best practices used:**
> - `Snackbar.make(view, message, 8000)` — Material Components 1.4+ treats any positive int as milliseconds. No need for `LENGTH_INDEFINITE` + `setDuration()` hack.
> - `Formatter.formatFileSize(context, bytes)` — Android's built-in formatter instead of a custom `formatFileSize()`. Handles locale-appropriate formatting.
> - `AnimatorListenerAdapter` — proper lifecycle-aware animation listener instead of `withEndAction()` which can't be cancelled.
> - `animate().cancel()` — cancel running animations before starting new ones to avoid race conditions.
> - `successSnackbar` field — keeps a reference for cleanup (e.g., in `onDestroy`).

### Step 8: Build and verify

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

**Test each scenario:**
1. Select items → Delete → system dialog appears (no custom dialog first on Android 11+)
2. Confirm in system dialog → overlay appears with "Done! X items deleted", checkmark animates
3. Snackbar shows "X.X MB of space saved" with Undo for 8 seconds
4. Wait 8 seconds → overlay fades, items gone
5. OR tap Undo → overlay fades, items restored
6. OR tap overlay → dismisses immediately
7. Selection bar shows file size between Select All and Delete buttons
8. On pre-Android 11 (if testable): custom dialog still appears

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/res/values/colors.xml` | Added overlay and snackbar colors |
| `app/src/main/res/values/strings.xml` | Added overlay text strings |
| `app/src/main/res/drawable/ic_check_large.xml` | **New file** — large checkmark stroke icon |
| `app/src/main/res/layout/activity_main.xml` | Added overlay FrameLayout, replaced spacer with size TextView |
| `app/src/main/java/.../MainActivity.kt` | Removed dialog for API 30+, added overlay, size display, 8s snackbar |

## Acceptance Criteria

- [ ] No custom dialog on Android 11+ (system dialog only)
- [ ] Custom dialog still appears on pre-Android 11
- [ ] Selection bar shows live file size (e.g., "1.2 GB")
- [ ] After delete: overlay fades in with animated checkmark + "Done! X items deleted"
- [ ] Snackbar shows "X.X MB of space saved" with Undo button
- [ ] Snackbar stays for 8 seconds
- [ ] Tapping Undo dismisses overlay and restores items
- [ ] Tapping overlay dismisses everything
- [ ] Build succeeds, all tests pass
