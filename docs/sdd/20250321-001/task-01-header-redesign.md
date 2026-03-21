# Task 01: Header / Top Bar Redesign

**Parent:** SDD-20250321-001 — Gallery Grid Visual Redesign

## What You're Changing

The current top bar is a single row with Photos/Videos filter chips on the left and a "Filters" button on the right. There is no app title.

You will restructure this into two rows:
- **Row 1:** App title "Gallery Cleaner" on the left, "Filters" pill button on the right
- **Row 2:** Photos and Videos toggle buttons

## Before vs After

**Before:**
```
[ Photos chip ] [ Videos chip ]         [ Filters button ]
```

**After:**
```
Gallery Cleaner                         [ Filters ]
[ Photos ] [ Videos ]
```

## Step-by-Step Instructions

### Step 1: Add new colors

Open `app/src/main/res/values/colors.xml` and add these colors before the closing `</resources>` tag:

```xml
<!-- Header & Badge colors (SDD-20250321-001) -->
<color name="header_title_green">#1B5E20</color>
<color name="filter_btn_bg">#E0F2F1</color>
<color name="filter_btn_text">#00796B</color>
<color name="chip_selected_bg">#E0F2F1</color>
<color name="chip_selected_text">#00796B</color>
<color name="chip_unselected_border">#D1D5DB</color>
<color name="chip_unselected_text">#4B5563</color>
```

### Step 2: Restructure the top bar in activity_main.xml

Open `app/src/main/res/layout/activity_main.xml`.

Find the `topBar` LinearLayout (starts around line 23). Replace the entire `<LinearLayout android:id="@+id/topBar" ...>` block (including its children) with:

```xml
<!-- Top Bar: Title + Filters + Media Type Chips -->
<LinearLayout
    android:id="@+id/topBar"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingHorizontal="16dp"
    android:paddingTop="12dp"
    android:paddingBottom="12dp"
    android:elevation="2dp"
    android:background="?attr/colorSurface"
    app:layout_constraintTop_toBottomOf="@id/selectionToolbar"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent">

    <!-- Row 1: Title + Filters button -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:layout_marginBottom="12dp">

        <TextView
            android:id="@+id/appTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/app_name"
            android:textSize="24sp"
            android:textStyle="bold"
            android:textColor="@color/header_title_green" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnFilters"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="@string/filters"
            android:textColor="@color/filter_btn_text"
            app:backgroundTint="@color/filter_btn_bg"
            app:cornerRadius="18dp"
            app:icon="@null" />

    </LinearLayout>

    <!-- Row 2: Media Type Chips -->
    <com.google.android.material.chip.ChipGroup
        android:id="@+id/mediaTypeChipGroup"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:singleSelection="false">

        <com.google.android.material.chip.Chip
            android:id="@+id/chipPhotos"
            style="@style/Widget.Material3.Chip.Filter"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:text="@string/photos"
            android:checked="true" />

        <com.google.android.material.chip.Chip
            android:id="@+id/chipVideos"
            style="@style/Widget.Material3.Chip.Filter"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:text="@string/videos"
            android:checked="true" />

    </com.google.android.material.chip.ChipGroup>

</LinearLayout>
```

**Key things to notice:**
- The outer LinearLayout changed from `horizontal` to `vertical` orientation
- `paddingStart`/`paddingEnd` changed to `paddingHorizontal="16dp"`
- `elevation="2dp"` adds the subtle shadow
- The Filters button lost its icon (`app:icon="@null"`) and got a pill shape via `app:cornerRadius="18dp"`
- The chip height changed from `48dp` to `40dp`

### Step 3: Verify string resource exists

Open `app/src/main/res/values/strings.xml` and make sure `app_name` is defined. It should already exist as:
```xml
<string name="app_name">Gallery Cleaner</string>
```

If it says something else like "GalleryCleaner" (no space), update it to "Gallery Cleaner" with a space.

### Step 4: Check MainActivity.kt for broken references

Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

Search for any references to `binding.btnFilters`. The button ID stayed the same (`btnFilters`), so these should still work.

Search for `binding.mediaTypeChipGroup`, `binding.chipPhotos`, `binding.chipVideos` — these IDs also stayed the same, so no Kotlin changes should be needed.

### Step 5: Build and verify

Run:
```bash
./gradlew assembleDebug
```

If the build fails, check:
- Did you accidentally remove a view that's still referenced in `MainActivity.kt`?
- Are all XML tags properly closed?
- Did the constraint references break? (`filterSummary` still constrains to `topBar`)

### Step 6: Run tests

```bash
./gradlew testDebugUnitTest
```

Tests should pass since no logic changed — this is purely a layout change.

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/res/values/colors.xml` | Added 7 new color resources |
| `app/src/main/res/layout/activity_main.xml` | Restructured `topBar` to two-row vertical layout with title |

## Acceptance Criteria

- [ ] App title "Gallery Cleaner" is visible in dark green at the top
- [ ] "Filters" button appears as a teal pill to the right of the title
- [ ] Photos/Videos chips appear on a second row below the title
- [ ] A subtle shadow separates the header from the grid content
- [ ] Tapping Filters still opens the bottom sheet
- [ ] Tapping Photos/Videos chips still toggles filtering
- [ ] Build succeeds, all tests pass
