# Task 01: Grid Tile Redesign

**Parent:** SDD-20260418-004 — Grid Tile Redesign

## What You're Changing

The main grid has a bunch of visual noise: big peach badges on every tile,
hard selection darkening, aggressive fade on reviewed items. You're dialing
all of that down so the photo content is the hero.

Scope is contained: one layout file (`item_image.xml`), one adapter
(`ImageAdapter.kt`), five drawable additions, four drawable deletions, one
theme tweak, a handful of color-token removals. No behavior change — just
how tiles look.

## Before vs After

| Element | Before | After |
|---|---|---|
| Tile corners | 4dp | 14dp (softer, more "photo" feel) |
| Source badge | peach pill (999dp radius), orange text | glass-dark 4dp chip, white 10sp text |
| Video duration badge | glass-dark 4dp chip | same, unified language with source |
| Reviewed tile | root-view 60% alpha + darker badge | photo only at 85% alpha + 75% saturation, badge unchanged |
| Selected tile | 30% black overlay + tinted check icon | 3dp accent ring + 14% accent tint + 22dp accent check circle |
| Non-selected during selection | no visual change | 55% white overlay (dims non-picked tiles) |

## Prerequisites

- SDD-20260418-002 (Design Tokens) merged
- SDD-20260418-003 (Top Bar) merged (not strictly required but avoids
  needless rebase churn)
- `./gradlew --stop` before you start

## Step-by-Step Instructions

### Step 1 — Bump tile corner radius

Open `app/src/main/res/values/themes.xml`. Find the `RoundedImageCorners`
style:

```xml
<style name="RoundedImageCorners" parent="">
    <item name="cornerSize">4dp</item>
</style>
```

Change `cornerSize` to `14dp`.

### Step 2 — Create the shared glass-dark chip drawable

Source badge and video badge share the same style now. Create
`app/src/main/res/drawable/glass_dark_chip.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="4dp" />
    <solid android:color="#99000000" />
</shape>
```

> 60% black (`#99`) over arbitrary photo content reads as a *glass* chip —
> legible in light and dark photos alike without needing per-photo logic.

### Step 3 — Create the three selection-state drawables

**3a.** `app/src/main/res/drawable/tile_selection_ring.xml` (3dp accent
stroke overlay — matches tile corner radius):

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="14dp" />
    <stroke android:width="3dp" android:color="@color/accent" />
    <solid android:color="@android:color/transparent" />
</shape>
```

**3b.** `app/src/main/res/drawable/tile_selection_tint.xml` (14% warm glow
on the photo when selected):

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="14dp" />
    <solid android:color="#24E85A3D" />
</shape>
```

> `#24` = ~14% opacity on the accent color (`#E85A3D`).

**3c.** `app/src/main/res/drawable/tile_dim_overlay.xml` (55% white dim for
non-selected tiles during selection mode):

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="14dp" />
    <solid android:color="#8CFFFFFF" />
</shape>
```

> `#8C` = ~55% opacity on white.

**3d.** `app/src/main/res/drawable/tile_selected_check_bg.xml` (22dp solid
accent circle for the check glyph):

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/accent" />
</shape>
```

### Step 4 — Delete the four legacy drawables

Delete:

- `app/src/main/res/drawable/badge_bg_unviewed.xml`
- `app/src/main/res/drawable/badge_bg_viewed.xml`
- `app/src/main/res/drawable/video_badge_bg.xml`
- `app/src/main/res/drawable/selection_overlay.xml`

From the project root:

```bash
rm app/src/main/res/drawable/badge_bg_unviewed.xml \
   app/src/main/res/drawable/badge_bg_viewed.xml \
   app/src/main/res/drawable/video_badge_bg.xml \
   app/src/main/res/drawable/selection_overlay.xml
```

### Step 5 — Rewrite `item_image.xml`

Replace the entire contents of `app/src/main/res/layout/item_image.xml`
with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="1dp">

    <com.google.android.material.imageview.ShapeableImageView
        android:id="@+id/imageView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:scaleType="centerCrop"
        android:contentDescription="@string/image_thumbnail"
        app:shapeAppearanceOverlay="@style/RoundedImageCorners"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintDimensionRatio="1:1"
        app:layout_constraintBottom_toBottomOf="parent" />

    <!-- Dim overlay: shown on NON-selected tiles while in selection mode -->
    <View
        android:id="@+id/dimOverlay"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="@drawable/tile_dim_overlay"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="@id/imageView"
        app:layout_constraintBottom_toBottomOf="@id/imageView"
        app:layout_constraintStart_toStartOf="@id/imageView"
        app:layout_constraintEnd_toEndOf="@id/imageView" />

    <!-- Accent tint: 14% warm glow on selected tile -->
    <View
        android:id="@+id/selectionTint"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="@drawable/tile_selection_tint"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="@id/imageView"
        app:layout_constraintBottom_toBottomOf="@id/imageView"
        app:layout_constraintStart_toStartOf="@id/imageView"
        app:layout_constraintEnd_toEndOf="@id/imageView" />

    <!-- 3dp accent ring around selected tile -->
    <View
        android:id="@+id/selectionRing"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="@drawable/tile_selection_ring"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="@id/imageView"
        app:layout_constraintBottom_toBottomOf="@id/imageView"
        app:layout_constraintStart_toStartOf="@id/imageView"
        app:layout_constraintEnd_toEndOf="@id/imageView" />

    <!-- 22dp accent check circle (top-right when selected) -->
    <FrameLayout
        android:id="@+id/checkMark"
        android:layout_width="22dp"
        android:layout_height="22dp"
        android:layout_margin="6dp"
        android:background="@drawable/tile_selected_check_bg"
        android:elevation="2dp"
        android:visibility="gone"
        android:contentDescription="@string/selected"
        app:layout_constraintTop_toTopOf="@id/imageView"
        app:layout_constraintEnd_toEndOf="@id/imageView">

        <ImageView
            android:layout_width="12dp"
            android:layout_height="12dp"
            android:layout_gravity="center"
            android:src="@drawable/ic_check_large"
            android:importantForAccessibility="no"
            app:tint="@android:color/white" />
    </FrameLayout>

    <!-- Video duration badge (top-left) — glass-dark chip -->
    <LinearLayout
        android:id="@+id/videoDurationBadge"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_margin="6dp"
        android:background="@drawable/glass_dark_chip"
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
            android:src="@drawable/ic_play_small"
            app:tint="@android:color/white" />

        <TextView
            android:id="@+id/videoDuration"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="4dp"
            android:textColor="@android:color/white"
            android:textSize="10sp"
            android:textStyle="bold" />

    </LinearLayout>

    <!-- Source badge (bottom-left) — glass-dark chip -->
    <TextView
        android:id="@+id/sourceBadge"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_margin="6dp"
        android:background="@drawable/glass_dark_chip"
        android:ellipsize="end"
        android:maxLines="1"
        android:paddingStart="6dp"
        android:paddingEnd="6dp"
        android:paddingTop="2dp"
        android:paddingBottom="2dp"
        android:textColor="@android:color/white"
        android:textSize="10sp"
        android:textStyle="bold"
        app:layout_constraintBottom_toBottomOf="@id/imageView"
        app:layout_constraintStart_toStartOf="@id/imageView" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

> **Key differences from the old layout:**
> - `selectionOverlay` is gone; three new views (`dimOverlay`,
>   `selectionTint`, `selectionRing`) split its responsibilities.
> - `checkMark` is now a `FrameLayout` (accent circle background) containing
>   an inner `ImageView` for the white check glyph — not a tinted
>   `ic_check_circle`.
> - Both badges use `@drawable/glass_dark_chip` and have 6dp horizontal, 2dp
>   vertical padding, 10sp text, white color.
> - `@drawable/ic_play_small` gets `app:tint="@android:color/white"` so it
>   always reads white against the chip regardless of its own fill color.

### Step 6 — Rewrite `ImageAdapter.kt`'s visual updaters

Open `app/src/main/java/com/example/gallerycleaner/ImageAdapter.kt`.

**6a.** Add these imports at the top (after the existing imports):

```kotlin
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
```

> `android.view.View` is already imported — don't duplicate. Just confirm.

**6b.** Replace the entire `updateViewedVisuals` function:

```kotlin
fun updateViewedVisuals(item: MediaItem) {
    val isViewed = item.uri in viewedItems
    // Gentle fade on the photo only — badges stay at full contrast.
    // During selection mode, selection dim overrides the reviewed fade.
    if (isViewed && !isSelectionMode) {
        binding.imageView.alpha = 0.85f
        val matrix = ColorMatrix().apply { setSaturation(0.75f) }
        binding.imageView.colorFilter = ColorMatrixColorFilter(matrix)
    } else {
        binding.imageView.alpha = 1.0f
        binding.imageView.colorFilter = null
    }
}
```

**6c.** Replace the entire `updateSelectionVisuals` function:

```kotlin
fun updateSelectionVisuals(item: MediaItem) {
    val isSelected = item.uri in selectedItems

    // Ring + warm tint + check circle on the selected tile
    binding.selectionRing.visibility = if (isSelected) View.VISIBLE else View.GONE
    binding.selectionTint.visibility = if (isSelected) View.VISIBLE else View.GONE
    binding.checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE

    // 55% white dim on NON-selected tiles while in selection mode
    binding.dimOverlay.visibility =
        if (isSelectionMode && !isSelected) View.VISIBLE else View.GONE

    // Hide source badge while dim overlay is on — cleaner during selection
    binding.sourceBadge.visibility =
        if (isSelectionMode && !isSelected) View.GONE else View.VISIBLE
}
```

> **Why the source badge hides only on dimmed tiles** (not selected ones):
> on dimmed tiles it'd be buried under the 55% white overlay; on selected
> tiles the photo is still visible so the badge reads fine.

### Step 7 — Legacy color cleanup

Open `app/src/main/res/values/colors.xml` and delete these four lines from
the legacy-tokens block:

```xml
<color name="badge_unviewed_bg">#FBE0D5</color>
<color name="badge_unviewed_text">#A83320</color>
<color name="badge_viewed_bg">#D9FFFDF8</color>
<color name="badge_viewed_text">#1D1710</color>
```

Sanity check from the project root:

```bash
grep -r "badge_unviewed_bg\|badge_unviewed_text\|badge_viewed_bg\|badge_viewed_text" app/
```

Expected: no matches. If any show up, undo the deletion of that specific
color and find what's still referencing it.

### Step 8 — Build and verify

```bash
./gradlew --stop
./gradlew clean assembleDebug testDebugUnitTest lint
```

If build fails with `AAPT: error: resource drawable/badge_bg_unviewed not
found` (or similar for the other deleted drawables), some layout/code is
still referencing them. The most likely spots:

- `ImageAdapter.kt` `updateViewedVisuals` if you missed Step 6b (it used to
  call `setBackgroundResource(R.drawable.badge_bg_viewed)`)
- `item_image.xml` if you didn't fully replace it in Step 5

### Step 9 — Visual smoke test on-device

Install. On the main grid:

- **Corners** look softer — noticeably rounder than before.
- **Source badges** are small dark chips with white text (not peach/orange).
  Video tiles have a matching dark chip with play glyph top-left.
- **Fast-scroll down** to items you've opened before: those tiles are
  **subtly faded** (you can still see the photo clearly; they don't look
  broken or "loading"). Unopened tiles pop at full color.
- **Long-press** any tile to enter selection mode:
  - The one you picked gets an **orange ring** and a **white check inside
    an orange circle** top-right.
  - **Every other tile** dims to look like it's behind frosted glass.
- **Tap another tile** — it also gets the ring + check; its dim goes away.
- **Tap the × in the bottom bar** to exit. All tiles restore immediately,
  including the reviewed-fade on previously-viewed ones.
- **Scroll fast** (recycler view) to confirm states don't "ghost" onto
  wrong tiles — if colorFilter or alpha leaks between bindings, you'll see
  it here.

## Definition of Done

- [x] All changes from the Files Changed table in `requirement.md` landed
- [x] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [x] No lingering references in `app/` to the four deleted drawables or
      four deleted color tokens
- [x] Visual smoke test (Step 9) passes — every tile state behaves as
      described
- [x] PR opened with title `SDD-20260418-004 — Grid Tile Redesign`

## Known gotchas

- **Coil re-binds** when the view holder recycles. If you see a momentarily
  gray tile followed by a saturated photo, that's normal (the image is
  loading). It's NOT the colorFilter leaking — the filter resets in
  `updateViewedVisuals` on every bind.
- **ColorMatrix saturation isn't linear** with CSS `saturate()` — 0.75 in
  Android looks close to 0.75 in CSS but not identical. If the design
  looks too gray, try `0.85f`; too strong, try `0.65f`. The spec is "85%
  opacity + 75% saturation" — 0.75f is correct per design.
- **Partial payloads**: the adapter uses `notifyItemChanged(i, PAYLOAD)` to
  avoid full rebind. The existing logic in `onBindViewHolder(holder,
  position, payloads)` already routes `PAYLOAD_VIEWED_STATE` →
  `updateViewedVisuals`, `PAYLOAD_SELECTION_STATE` →
  `updateSelectionVisuals`. Don't change that plumbing.
- **tile_selection_ring.xml** has a hardcoded 14dp corner radius — if you
  change `RoundedImageCorners` in Step 1 to a different value, update the
  ring (and tint + dim) drawables to match, or the ring won't fit the
  tile edge.
- **Video badge `ic_play_small` tint**: the existing drawable may have a
  fillColor baked in. If the play glyph renders in a weird color (e.g.
  the legacy teal), add `app:tint="@android:color/white"` to its
  ImageView — the updated layout in Step 5 already does this.
