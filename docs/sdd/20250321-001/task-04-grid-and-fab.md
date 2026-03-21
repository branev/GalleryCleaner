# Task 04: Grid Styling + Extended Continue FAB

**Parent:** SDD-20250321-001 — Gallery Grid Visual Redesign

## What You're Changing

Two smaller changes bundled together:

1. **Grid styling** — Make the image grid edge-to-edge with tighter gaps and rounded corners on items
2. **Continue FAB** — Change from a small icon-only FAB to an extended FAB with "Continue" text

## Before vs After

**Grid — Before:**
```
|  8dp padding                    |
|  ┌────┐ 2dp ┌────┐ 2dp ┌────┐  |
|  │    │     │    │     │    │  |
|  │    │     │    │     │    │  |  ← square corners
|  └────┘     └────┘     └────┘  |
|  8dp padding                    |
```

**Grid — After:**
```
┌────┐1dp┌────┐1dp┌────┐
│    │   │    │   │    │
│    │   │    │   │    │  ← 4dp rounded corners
└────┘   └────┘   └────┘
```

**FAB — Before:** Small round button with ↓ icon only

**FAB — After:** Extended pill button with ↓ icon + "Continue" text, teal background

## Step-by-Step Instructions

### Part A: Grid Styling

#### Step A1: Update item_image.xml margins and corners

Open `app/src/main/res/layout/item_image.xml`.

**a)** Change the root ConstraintLayout margin from 2dp to 1dp:

Find:
```xml
android:layout_margin="2dp"
```
Change to:
```xml
android:layout_margin="1dp"
```

**b)** Add corner clipping to the root ConstraintLayout. Add these attributes:

```xml
android:background="@android:color/black"
android:clipChildren="true"
```

**c)** Add corner rounding to the ImageView. You have two options:

**Option 1 (recommended): Use ShapeableImageView from Material**

Replace `ImageView` with `com.google.android.material.imageview.ShapeableImageView` and add:
```xml
app:shapeAppearanceOverlay="@style/RoundedImageCorners"
```

Then add a style in `app/src/main/res/values/themes.xml`:
```xml
<style name="RoundedImageCorners" parent="">
    <item name="cornerSize">4dp</item>
</style>
```

**Option 2 (simpler): Use outline clipping**

Keep the regular `ImageView` but add a rounded background and clip it:
```xml
android:background="@drawable/grid_item_bg"
android:clipToOutline="true"
```

Create `app/src/main/res/drawable/grid_item_bg.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="4dp" />
    <solid android:color="#000000" />
</shape>
```

> **Which option to pick?** Option 2 is simpler and doesn't change the view type, which means fewer changes in the adapter. Go with Option 2 unless you have a reason to use ShapeableImageView.

> **Why does clipToOutline work?** When you set a background with rounded corners, Android creates an outline from that shape. `clipToOutline="true"` clips the view's content (the image) to that outline, giving you rounded corners without any custom drawing code.

#### Step A2: Remove RecyclerView padding

Open `app/src/main/res/layout/activity_main.xml`.

Find the RecyclerView (around line 100) and remove or change the padding:

Find:
```xml
android:padding="8dp"
```
Change to:
```xml
android:padding="0dp"
```

Or simply remove the `android:padding` line entirely. Keep `android:clipToPadding="false"` — it won't hurt anything with 0 padding.

### Part B: Extended Continue FAB

#### Step B1: Add FAB color

Open `app/src/main/res/values/colors.xml` and add:

```xml
<!-- Continue FAB (SDD-20250321-001) -->
<color name="fab_continue_bg">#057A70</color>
```

#### Step B2: Replace FAB in activity_main.xml

Find the `fabContinue` FloatingActionButton (around line 160). Replace it with:

```xml
<!-- Continue Extended FAB (scroll to first unviewed item) -->
<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
    android:id="@+id/fabContinue"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_margin="24dp"
    android:contentDescription="@string/continue_reviewing"
    android:text="@string/continue_reviewing"
    android:textColor="@android:color/white"
    android:visibility="gone"
    app:backgroundTint="@color/fab_continue_bg"
    app:icon="@drawable/ic_arrow_downward"
    app:iconTint="@android:color/white"
    app:layout_constraintBottom_toTopOf="@id/selectionActionBar"
    app:layout_constraintEnd_toEndOf="parent" />
```

**What changed:**
- `FloatingActionButton` → `ExtendedFloatingActionButton`
- Added `android:text` with the "Continue" label
- Margin: 16dp → 24dp
- Added explicit `app:backgroundTint` and text/icon colors
- `app:srcCompat` → `app:icon` (ExtendedFAB uses `icon` attribute, not `srcCompat`)

#### Step B3: Update MainActivity.kt type reference

Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

Search for `fabContinue`. The binding type will automatically change from `FloatingActionButton` to `ExtendedFloatingActionButton` since ViewBinding generates types from the XML.

However, if there's any code that explicitly casts or types it as `FloatingActionButton`, update it to `ExtendedFloatingActionButton`:

```kotlin
// If you see this import:
import com.google.android.material.floatingactionbutton.FloatingActionButton
// Change to (or add alongside):
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
```

Most code should work without changes because `ExtendedFloatingActionButton` extends `MaterialButton`, and common operations like `visibility`, `setOnClickListener`, `isEnabled`, and `alpha` are inherited.

Check for any `show()`/`hide()` calls — `ExtendedFloatingActionButton` has these too, so they should still work.

### Step C: Build and test

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Common issues:
- **Type mismatch on fabContinue**: If code uses `FloatingActionButton` type explicitly, change it to `ExtendedFloatingActionButton`
- **Grid item bg drawable not found**: Make sure you created the file in `res/drawable/`, not `res/drawable-v24/` or similar

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/res/layout/item_image.xml` | Reduced margin to 1dp, added corner rounding |
| `app/src/main/res/layout/activity_main.xml` | Removed RecyclerView padding, replaced FAB with ExtendedFAB |
| `app/src/main/res/drawable/grid_item_bg.xml` | **New file** — rounded rectangle for item clipping |
| `app/src/main/res/values/colors.xml` | Added `fab_continue_bg` color |
| `app/src/main/java/.../MainActivity.kt` | Updated FAB type reference (if needed) |

## Acceptance Criteria

- [ ] Grid items have no outer padding — the grid starts at the screen edge
- [ ] Grid items have ~1dp gaps between them
- [ ] Grid item thumbnails have 4dp rounded corners
- [ ] Continue FAB shows "Continue" text next to the arrow icon
- [ ] Continue FAB has a teal background with white text/icon
- [ ] Continue FAB still scrolls to the first unviewed item when tapped
- [ ] Continue FAB still hides/shows at the correct times
- [ ] Build succeeds, all tests pass
