# Task 02: Source Badge Redesign

**Parent:** SDD-20250321-001 — Gallery Grid Visual Redesign

## What You're Changing

The source badge (e.g., "WhatsApp", "Camera") at the bottom-left of each grid item currently has a single fixed style: white text on a dark semi-transparent background.

You will:
1. Change the badge shape to a pill (fully rounded)
2. Make the badge smaller and closer to the edge
3. Make the badge color depend on **viewed state** — dark teal when fresh, light/white when viewed

## Before vs After

**Before (all items):**
- Background: `#66000000` (40% black), 12dp corners
- Text: white, 12sp
- Margin: 8dp from edges

**After (unviewed items):**
- Background: `#166A62` (dark teal), fully rounded pill
- Text: white, 11sp
- Margin: 4dp from edges

**After (viewed/dimmed items):**
- Background: `#D9FFFFFF` (85% white), fully rounded pill
- Text: `#1F2937` (dark gray), 11sp
- Margin: 4dp from edges

## Step-by-Step Instructions

### Step 1: Add new badge colors

Open `app/src/main/res/values/colors.xml` and add (if not already added by Task 01):

```xml
<!-- Badge colors (SDD-20250321-001) -->
<color name="badge_unviewed_bg">#166A62</color>
<color name="badge_unviewed_text">#FFFFFF</color>
<color name="badge_viewed_bg">#D9FFFFFF</color>
<color name="badge_viewed_text">#1F2937</color>
```

### Step 2: Create badge background drawables

Create two new drawable files.

**File: `app/src/main/res/drawable/badge_bg_unviewed.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="999dp" />
    <solid android:color="@color/badge_unviewed_bg" />
</shape>
```

**File: `app/src/main/res/drawable/badge_bg_viewed.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="999dp" />
    <solid android:color="@color/badge_viewed_bg" />
</shape>
```

> **Why 999dp?** Android uses the radius as a maximum — setting it very high makes the shape a perfect pill/capsule regardless of the view's height. This is the standard trick for pill shapes in XML.

### Step 3: Update the badge in item_image.xml

Open `app/src/main/res/layout/item_image.xml`.

Find the `sourceBadge` TextView (around line 72). Replace it with:

```xml
<!-- Source badge (bottom-left) -->
<TextView
    android:id="@+id/sourceBadge"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    android:background="@drawable/badge_bg_unviewed"
    android:ellipsize="end"
    android:maxLines="1"
    android:paddingStart="8dp"
    android:paddingEnd="8dp"
    android:paddingTop="2dp"
    android:paddingBottom="2dp"
    android:textColor="@color/badge_unviewed_text"
    android:textSize="11sp"
    app:layout_constraintBottom_toBottomOf="@id/imageView"
    app:layout_constraintStart_toStartOf="@id/imageView" />
```

**What changed from the original:**
- `android:layout_margin`: 8dp → 4dp
- `android:background`: `@drawable/label_bg` → `@drawable/badge_bg_unviewed`
- `android:textColor`: `@android:color/white` → `@color/badge_unviewed_text`
- `android:textSize`: 12sp → 11sp
- `android:paddingTop`/`Bottom`: 4dp → 2dp

### Step 4: Update ImageAdapter.kt to swap badge style based on viewed state

Open `app/src/main/java/com/example/gallerycleaner/ImageAdapter.kt`.

Find the `updateViewedVisuals` method (around line 121). It currently only changes the root alpha. Update it to also change the badge style:

```kotlin
fun updateViewedVisuals(item: MediaItem) {
    val isViewed = item.uri in viewedItems
    // Reduce opacity for viewed items (only when not in selection mode)
    binding.root.alpha = if (isViewed && !isSelectionMode) 0.6f else 1.0f

    // Update badge colors based on viewed state
    if (isViewed && !isSelectionMode) {
        binding.sourceBadge.setBackgroundResource(R.drawable.badge_bg_viewed)
        binding.sourceBadge.setTextColor(
            binding.root.context.getColor(R.color.badge_viewed_text)
        )
    } else {
        binding.sourceBadge.setBackgroundResource(R.drawable.badge_bg_unviewed)
        binding.sourceBadge.setTextColor(
            binding.root.context.getColor(R.color.badge_unviewed_text)
        )
    }
}
```

> **Note:** This also changes the viewed opacity from `0.5f` to `0.6f` — that's intentional per the design. If Task 01 already made this change, just make sure it's `0.6f`.

> **Why `getColor()` without ContextCompat?** We target minSdk 26 (Android 8), so `context.getColor()` is safe to call directly — no need for the `ContextCompat` wrapper.

### Step 5: Build and verify

```bash
./gradlew assembleDebug
```

If it fails, check:
- The drawable XML files are well-formed (proper `<?xml` header, `<shape>` root element)
- The color names in the drawables match exactly what you added to `colors.xml`
- The `R.drawable.badge_bg_viewed` and `R.drawable.badge_bg_unviewed` references resolve (rebuild if needed)

### Step 6: Run tests

```bash
./gradlew testDebugUnitTest
```

Tests should pass — the adapter tests don't verify visual styling.

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/res/values/colors.xml` | Added 4 badge color resources |
| `app/src/main/res/drawable/badge_bg_unviewed.xml` | **New file** — dark teal pill background |
| `app/src/main/res/drawable/badge_bg_viewed.xml` | **New file** — semi-transparent white pill background |
| `app/src/main/res/layout/item_image.xml` | Updated `sourceBadge` margin, padding, text size, background |
| `app/src/main/java/.../ImageAdapter.kt` | `updateViewedVisuals()` now swaps badge background and text color |

## Acceptance Criteria

- [ ] Unviewed items show a dark teal pill badge with white text
- [ ] Viewed (scrolled-past) items show a white/transparent pill badge with dark gray text
- [ ] Badge is closer to the corner than before (4dp margin)
- [ ] Badge text is slightly smaller than before (11sp)
- [ ] Badge is a rounded pill shape, not a rounded rectangle
- [ ] Entering selection mode still hides the badge
- [ ] Build succeeds, all tests pass
