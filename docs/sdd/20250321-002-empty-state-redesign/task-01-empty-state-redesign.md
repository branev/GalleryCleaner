# Task 01: Empty State UI Redesign

**Parent:** SDD-20250321-002 — Empty State Screen Redesign

## What You're Changing

The empty state screens currently show a raw icon with small text. You will update them to show:
- The icon inside a circular gray container
- Larger, bolder typography
- A "Reset Filters" tonal pill button

## Before vs After

**Before:**
```
        [filter_list icon, 60% opacity]

           No items found
    Try adjusting your filters
        or date range
```

**After:**
```
          ┌─────────┐
          │  ( 🔍✕ ) │  ← gray circle with search_off icon
          └─────────┘

        No items found

  Try adjusting your filters or
  date range to see more photos

       [ Reset Filters ]  ← teal tonal pill button
```

## Step-by-Step Instructions

### Step 1: Add new colors

Open `app/src/main/res/values/colors.xml` and add:

```xml
<!-- Empty state colors (SDD-20250321-002) -->
<color name="empty_state_icon_bg">#E5E5EA</color>
<color name="empty_state_icon_color">#8E8E93</color>
<color name="empty_state_subtitle_color">#8E8E93</color>
<color name="empty_state_reset_btn_bg">#1A008080</color>
<color name="empty_state_reset_btn_text">#008080</color>
```

### Step 2: Add new strings

Open `app/src/main/res/values/strings.xml` and add:

```xml
<!-- Empty state (SDD-20250321-002) -->
<string name="reset_filters">Reset Filters</string>
```

Also update the existing subtitle string to match the design:
```xml
<string name="adjust_filters_hint">Try adjusting your filters or date range to see more photos</string>
```

### Step 3: Create the icon circle background drawable

Create `app/src/main/res/drawable/empty_state_icon_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/empty_state_icon_bg" />
    <size android:width="96dp" android:height="96dp" />
</shape>
```

### Step 4: Create the search_off icon

Create `app/src/main/res/drawable/ic_search_off.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/empty_state_icon_color"
        android:pathData="M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z" />
    <path
        android:fillColor="@color/empty_state_icon_color"
        android:pathData="M3.29,3.29L2.22,4.36l2.87,2.87C4.73,8.04 4.5,8.75 4.5,9.5c0,2.76 2.24,5 5,5 0.75,0 1.46,-0.17 2.1,-0.47l2.87,2.87 1.06,-1.06L3.29,3.29z" />
</vector>
```

> **Note:** This is a simplified search_off icon. If you want the exact Material Symbol, download it from [Google Fonts Material Symbols](https://fonts.google.com/icons?selected=Material+Symbols+Outlined:search_off) as a 24dp SVG and convert it to Android vector drawable using Android Studio: **File > New > Vector Asset > Local File**.

### Step 5: Update the empty state layout in activity_main.xml

Open `app/src/main/res/layout/activity_main.xml`.

Find the `emptyStateContainer` LinearLayout (around line 214). Replace it with:

```xml
<!-- Empty State Container -->
<LinearLayout
    android:id="@+id/emptyStateContainer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:gravity="center"
    android:paddingHorizontal="32dp"
    android:paddingVertical="32dp"
    android:visibility="gone"
    app:layout_constraintTop_toBottomOf="@id/filterLoadingIndicator"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent">

    <!-- Icon in circular background -->
    <FrameLayout
        android:layout_width="96dp"
        android:layout_height="96dp"
        android:background="@drawable/empty_state_icon_bg">

        <ImageView
            android:id="@+id/emptyIcon"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:layout_gravity="center"
            android:importantForAccessibility="no"
            android:src="@drawable/ic_search_off" />

    </FrameLayout>

    <!-- Title -->
    <TextView
        android:id="@+id/emptyTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:textSize="22sp"
        android:textStyle="bold"
        android:textColor="?attr/colorOnSurface" />

    <!-- Subtitle -->
    <TextView
        android:id="@+id/emptySubtitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:maxWidth="240dp"
        android:textSize="17sp"
        android:textColor="@color/empty_state_subtitle_color"
        android:gravity="center"
        android:lineSpacingMultiplier="1.3" />

    <!-- Reset Filters Button -->
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnResetFilters"
        style="@style/Widget.Material3.Button.TextButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="32dp"
        android:text="@string/reset_filters"
        android:textColor="@color/empty_state_reset_btn_text"
        android:textStyle="bold"
        android:paddingHorizontal="32dp"
        android:paddingVertical="12dp"
        app:backgroundTint="@color/empty_state_reset_btn_bg"
        app:cornerRadius="999dp"
        android:visibility="gone" />

</LinearLayout>
```

**Key differences from the old layout:**
- Icon is now inside a `FrameLayout` (the gray circle) at 48dp, no alpha
- Title bumped to 22sp
- Subtitle bumped to 17sp with `maxWidth="240dp"` and `lineSpacingMultiplier="1.3"`
- New `btnResetFilters` MaterialButton (tonal pill, hidden by default)
- Margin between icon and title increased to 24dp
- Margin before button is 32dp

### Step 6: Update MainActivity.kt — empty state rendering

Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

**a)** Remove the empty state container click handler. Find `setupEmptyStateClickHandler()` and remove the click listener setup (or the entire method if it only does this). Also remove the call to it from `onCreate` or wherever it's called.

**b)** Add a click handler for the new Reset button. In `onCreate` (or wherever other button handlers are set up), add:

```kotlin
binding.btnResetFilters.setOnClickListener {
    viewModel.resetFilters()
}
```

**c)** Update each empty state rendering block to use the new icon and show/hide the Reset button:

For **`GalleryUiState.Empty`** (no media at all):
```kotlin
binding.emptyIcon.setImageResource(R.drawable.ic_search_off)
binding.emptyTitle.text = state.message
binding.emptySubtitle.visibility = View.GONE
binding.btnResetFilters.visibility = View.GONE
```

For **`GalleryUiState.NoFiltersSelected`**:
```kotlin
binding.emptyIcon.setImageResource(R.drawable.ic_search_off)
binding.emptyTitle.text = getString(R.string.no_filters_selected)
binding.emptySubtitle.text = getString(R.string.select_categories_hint)
binding.emptySubtitle.visibility = View.VISIBLE
binding.btnResetFilters.visibility = View.VISIBLE
```

For **`GalleryUiState.NoMatchingItems`**:
```kotlin
binding.emptyIcon.setImageResource(R.drawable.ic_search_off)
binding.emptyTitle.text = getString(R.string.no_matching_items)
binding.emptySubtitle.text = getString(R.string.adjust_filters_hint)
binding.emptySubtitle.visibility = View.VISIBLE
binding.btnResetFilters.visibility = View.VISIBLE
```

For **`showError()`**:
```kotlin
binding.emptyIcon.setImageResource(R.drawable.ic_search_off)
binding.emptyTitle.text = message
binding.emptySubtitle.visibility = View.GONE
binding.btnResetFilters.visibility = View.GONE
```

### Step 7: Add resetFilters() to GalleryViewModel

Open `app/src/main/java/com/example/gallerycleaner/GalleryViewModel.kt`.

Check if a `resetFilters()` method already exists. If not, add one that resets all filters to defaults:

```kotlin
fun resetFilters() {
    filterPreferences.saveSelectedSources(SourceType.values().toSet())
    filterPreferences.saveSelectedMediaTypes(MediaType.values().toSet())
    filterPreferences.saveDateRange(DateRange.DEFAULT)
    filterPreferences.saveSortOption(SortOption.DATE_DESC)
}
```

> **Note:** Check `FilterPreferences` for exact method names — they may differ slightly.

### Step 8: Build and verify

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Test each empty state:
1. Deselect all sources via Filters → should show NoFiltersSelected with Reset button
2. Select a source that has no items → should show NoMatchingItems with Reset button
3. Tap "Reset Filters" → should restore all defaults and show items again

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/res/values/colors.xml` | Added 5 empty state colors |
| `app/src/main/res/values/strings.xml` | Added "Reset Filters" string, updated subtitle |
| `app/src/main/res/drawable/empty_state_icon_bg.xml` | **New file** — gray oval background |
| `app/src/main/res/drawable/ic_search_off.xml` | **New file** — search_off icon |
| `app/src/main/res/layout/activity_main.xml` | Restructured empty state with circular icon, larger text, Reset button |
| `app/src/main/java/.../MainActivity.kt` | Updated rendering, added Reset button handler, removed container click |
| `app/src/main/java/.../GalleryViewModel.kt` | Added `resetFilters()` method |

## Status: COMPLETE

## Acceptance Criteria

- [x] Empty state icon appears inside a gray circle (96dp)
- [x] Icon is `search_off` (48dp, gray), no opacity reduction
- [x] Title is 22sp bold
- [x] Subtitle is 17sp gray with max width ~240dp
- [x] "Reset Filters" pill button appears for NoFiltersSelected and NoMatchingItems
- [x] "Reset Filters" button does NOT appear for the generic Empty state
- [x] Tapping "Reset Filters" restores all filter defaults and shows items
- [x] Build succeeds, all tests pass
