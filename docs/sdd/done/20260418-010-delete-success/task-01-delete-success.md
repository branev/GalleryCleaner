# Task 01: Delete Success Card

**Parent:** SDD-20260418-010 — Delete Success

## What You're Changing

Rebuild the post-delete celebration. Today it's a full-screen dark scrim
with a 120dp bouncy green checkmark and a separate bottom Snackbar
holding the Undo action + the freed-size copy. After this task, it's a
**centered white card** with a hero size readout, a 7-second progress
ring draining around the Undo icon, and 22 pieces of deterministic
confetti scattered behind. Two peer buttons live in the card: **Undo**
and **Continue**. The Snackbar goes away.

## Before vs After

| Thing | Before | After |
|---|---|---|
| Container | Full-screen FrameLayout with dark scrim + inline LinearLayout | FrameLayout scrim (kept) + centered MaterialCardView |
| Check icon | 120dp green checkmark, scale-up with OvershootInterpolator | 56dp `accent_soft` circle with accent-colored check glyph |
| Title | `Done! N items deleted` (28sp white bold) | Hero `789 kB` (30sp, 700, `ink`, JetBrains Mono) |
| Subtitle | *(none on card)* | `freed · 4 items moved to trash` (13sp `ink3`) |
| Primary action | `OK` pill (orange accent fill) | `Continue` pill (ink fill, white text) |
| Secondary action | Snackbar "Undo" at bottom | `Undo` outline pill inside card with 7s progress ring |
| Countdown | Snackbar's internal 8s timer | `ValueAnimator` 7s on ring's `progress` property |
| Dismiss | Tap scrim / tap OK / snackbar timeout | Tap Continue / tap Undo / ring hits 0 |
| Confetti | None | 22 static deterministic pieces behind card |

## Prerequisites

- SDD-002 (Design Tokens) merged. `accent`, `accent_soft`, `ink`,
  `ink3`, `line`, `line_strong`, `surface`, `success` already in
  `res/values/colors.xml`. JetBrains Mono shipped.
- `./gradlew --stop` before you start (Windows file-lock prevention).
- Current code to understand before touching:
  - [activity_main.xml:487-541](../../../../app/src/main/res/layout/activity_main.xml#L487-L541) — existing overlay
  - [MainActivity.kt:851-908](../../../../app/src/main/java/com/example/gallerycleaner/MainActivity.kt#L851-L908) — `showTrashSuccessSnackbar`
  - [MainActivity.kt:910-932](../../../../app/src/main/java/com/example/gallerycleaner/MainActivity.kt#L910-L932) — `dismissSuccessOverlay`

## Step-by-Step Instructions

### Step 1 — Drawables

Create **`res/drawable/check_badge_bg.xml`** — a flat `accent_soft`
oval used as the card's top badge background.

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/accent_soft" />
</shape>
```

Verify **`res/drawable/ic_undo.xml`** exists. If missing, add a 24dp
vector of a standard rotate-left arrow tinted `ink`. Material Icons
"Undo" (Outlined) is fine — any 24dp undo glyph whose default tint
picks up `android:tint="@color/ink"` works.

Verify **`res/drawable/ic_check.xml`** exists for the badge glyph.
If missing, add a 24dp checkmark vector. Will be tinted `accent`
(`#E85A3D`) from the layout.

### Step 2 — `ConfettiView.kt`

Create a new file
`app/src/main/java/com/example/gallerycleaner/ConfettiView.kt`.

This is a plain `View` that draws 22 static pieces. Determinism:
`Random(seed)` where `seed` is set once via `setSeed(Long)`. The
view recomputes positions on `onSizeChanged` or whenever the seed
changes, then draws in `onDraw`. **No animation** here — simpler,
cheaper, visually stable.

```kotlin
package com.example.gallerycleaner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val PIECE_COUNT = 22
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val palette = listOf(
        ContextCompat.getColor(context, R.color.accent),
        ContextCompat.getColor(context, R.color.ink),
        ContextCompat.getColor(context, R.color.accent_soft),
        ContextCompat.getColor(context, R.color.success),
    )

    private data class Piece(
        val cx: Float,
        val cy: Float,
        val size: Float,
        val rotationDeg: Float,
        val isRect: Boolean,
        val color: Int,
    )

    private var seed: Long = 0L
    private var pieces: List<Piece> = emptyList()

    fun setSeed(newSeed: Long) {
        if (seed != newSeed) {
            seed = newSeed
            regenerate()
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        regenerate()
    }

    private fun regenerate() {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) {
            pieces = emptyList()
            return
        }
        val rng = Random(seed)
        val minSize = resources.displayMetrics.density * 6f   // 6dp
        val maxSize = resources.displayMetrics.density * 14f  // 14dp

        pieces = List(PIECE_COUNT) {
            Piece(
                cx = rng.nextFloat() * w,
                cy = rng.nextFloat() * h,
                size = minSize + rng.nextFloat() * (maxSize - minSize),
                rotationDeg = rng.nextFloat() * 360f,
                isRect = rng.nextBoolean(),
                color = palette[rng.nextInt(palette.size)],
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in pieces) {
            paint.color = p.color
            val half = p.size / 2f
            if (p.isRect) {
                canvas.save()
                canvas.rotate(p.rotationDeg, p.cx, p.cy)
                canvas.drawRect(
                    RectF(p.cx - half, p.cy - half * 0.45f, p.cx + half, p.cy + half * 0.45f),
                    paint,
                )
                canvas.restore()
            } else {
                canvas.drawCircle(p.cx, p.cy, half * 0.6f, paint)
            }
        }
    }

    /** Exposed for tests. */
    internal fun piecePositionsForTest(w: Int, h: Int, testSeed: Long): List<Pair<Float, Float>> {
        val rng = Random(testSeed)
        val minSize = resources.displayMetrics.density * 6f
        val maxSize = resources.displayMetrics.density * 14f
        return List(PIECE_COUNT) {
            val cx = rng.nextFloat() * w
            val cy = rng.nextFloat() * h
            rng.nextFloat().let { _ -> minSize + rng.nextFloat() * (maxSize - minSize) }
            rng.nextFloat()
            rng.nextBoolean()
            rng.nextInt(palette.size)
            cx to cy
        }
    }
}
```

> **Why this shape:** Static placement lets us ship a stable, testable
> look without worrying about animator lifecycle or dropped frames.
> `Random(seed)` is deterministic — same seed, same layout — which the
> acceptance test verifies.

### Step 3 — `UndoProgressRing.kt`

Create a new file
`app/src/main/java/com/example/gallerycleaner/UndoProgressRing.kt`.

A small custom view that draws a 2dp `accent` arc over a 2dp `line`
track. Exposes a `progress: Float` property (1f = full ring, 0f =
empty). No self-driving animation — the caller uses `ValueAnimator`.

```kotlin
package com.example.gallerycleaner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class UndoProgressRing @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val strokePx = resources.displayMetrics.density * 2f  // 2dp

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        color = ContextCompat.getColor(context, R.color.line)
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.accent)
    }

    private val rect = RectF()

    var progress: Float = 1f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (field != clamped) {
                field = clamped
                invalidate()
            }
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = strokePx / 2f + paddingLeft
        rect.set(
            inset.toFloat(),
            inset.toFloat(),
            (width - inset).toFloat(),
            (height - inset).toFloat(),
        )
        canvas.drawOval(rect, trackPaint)
        val sweep = 360f * progress
        if (sweep > 0f) {
            canvas.drawArc(rect, -90f, sweep, false, arcPaint)
        }
    }
}
```

### Step 4 — Strings

Open `res/values/strings.xml`. Add:

```xml
<string name="continue_action">Continue</string>
<string name="freed_subtitle">freed · %1$d items moved to trash</string>
<string name="freed_subtitle_single">freed · 1 item moved to trash</string>
```

> We're skipping proper `<plurals>` for simplicity — two fixed strings
> + a count-of-1 branch in Kotlin. If the project already has a
> `plurals.xml`, prefer adding a `<plurals name="freed_subtitle">`
> block there and reading it via `resources.getQuantityString(...)`.

Do **not** delete `delete_success_title`, `overlay_ok`, `space_saved`
yet — do that in Step 9 after the new wiring compiles.

### Step 5 — Replace the overlay block in `activity_main.xml`

In `res/layout/activity_main.xml`, find the `<FrameLayout
android:id="@+id/deleteSuccessOverlay">` block (currently around
line 488) and replace **its contents** (keep the outer `FrameLayout`
+ its layout constraints + `id`, `background`, `elevation`,
`clickable`, `focusable`, `visibility`).

New inner contents:

```xml
<!-- Confetti layer spans the whole scrim, drawn under the card -->
<com.example.gallerycleaner.ConfettiView
    android:id="@+id/confettiLayer"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:importantForAccessibility="no" />

<com.google.android.material.card.MaterialCardView
    android:id="@+id/successCard"
    android:layout_width="280dp"
    android:layout_height="wrap_content"
    android:layout_gravity="center"
    app:cardBackgroundColor="@color/surface"
    app:cardCornerRadius="20dp"
    app:cardElevation="4dp"
    app:contentPadding="28dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center_horizontal">

        <FrameLayout
            android:id="@+id/successCheckBadge"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:background="@drawable/check_badge_bg">

            <ImageView
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:layout_gravity="center"
                android:src="@drawable/ic_check"
                app:tint="@color/accent"
                android:importantForAccessibility="no" />
        </FrameLayout>

        <TextView
            android:id="@+id/successHeroSize"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:textColor="@color/ink"
            android:textSize="30sp"
            android:textStyle="bold"
            android:fontFamily="@font/jetbrains_mono"
            android:gravity="center"
            tools:text="789 kB" />

        <TextView
            android:id="@+id/successSubtitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textColor="@color/ink3"
            android:textSize="13sp"
            android:gravity="center"
            tools:text="freed · 4 items moved to trash" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <FrameLayout
                android:id="@+id/btnOverlayUndo"
                android:layout_width="0dp"
                android:layout_height="44dp"
                android:layout_weight="1"
                android:background="@drawable/undo_button_bg"
                android:clickable="true"
                android:focusable="true">

                <com.example.gallerycleaner.UndoProgressRing
                    android:id="@+id/undoProgressRing"
                    android:layout_width="28dp"
                    android:layout_height="28dp"
                    android:layout_gravity="center_vertical|start"
                    android:layout_marginStart="10dp" />

                <ImageView
                    android:layout_width="18dp"
                    android:layout_height="18dp"
                    android:layout_gravity="center_vertical|start"
                    android:layout_marginStart="15dp"
                    android:src="@drawable/ic_undo"
                    app:tint="@color/ink"
                    android:importantForAccessibility="no" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center"
                    android:layout_marginStart="12dp"
                    android:text="@string/undo"
                    android:textColor="@color/ink"
                    android:textSize="13sp"
                    android:textStyle="bold" />
            </FrameLayout>

            <Space
                android:layout_width="8dp"
                android:layout_height="1dp" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnOverlayContinue"
                android:layout_width="0dp"
                android:layout_height="44dp"
                android:layout_weight="1"
                android:insetTop="0dp"
                android:insetBottom="0dp"
                android:text="@string/continue_action"
                android:textColor="@android:color/white"
                app:backgroundTint="@color/ink"
                app:cornerRadius="22dp" />

        </LinearLayout>

    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

Create the Undo button background **`res/drawable/undo_button_bg.xml`**:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/surface" />
    <stroke android:width="1dp" android:color="@color/line_strong" />
    <corners android:radius="22dp" />
</shape>
```

> **Why the Undo button is a `FrameLayout` not a `MaterialButton`:**
> We need to overlay the progress ring *behind* the undo icon and
> inline the label. `MaterialButton` won't let us stack children
> without fighting its internal layout. A `FrameLayout` with a
> drawable background gives us the same pill look and is still
> `clickable`.

### Step 6 — Rewrite `showTrashSuccessSnackbar` in `MainActivity`

Rename the function to `showTrashSuccessCard` and update the one
caller in the `trashRequestLauncher` success branch. Full new
function:

```kotlin
private var undoAnimator: android.animation.ValueAnimator? = null

private fun showTrashSuccessCard(count: Int, totalSize: Long, trashedUris: Set<Uri>) {
    binding.deleteSuccessOverlay.visibility = View.VISIBLE
    binding.deleteSuccessOverlay.bringToFront()
    window.statusBarColor = getColor(R.color.overlay_bg)
    WindowCompat.getInsetsController(window, window.decorView)
        .isAppearanceLightStatusBars = false

    // Hero size + subtitle
    binding.successHeroSize.text = Formatter.formatFileSize(this, totalSize)
    binding.successSubtitle.text = if (count == 1) {
        getString(R.string.freed_subtitle_single)
    } else {
        getString(R.string.freed_subtitle, count)
    }

    // Confetti seed from freed size (deterministic, visually stable per size)
    binding.confettiLayer.setSeed(totalSize)

    // Progress ring starts full
    binding.undoProgressRing.progress = 1f

    // Fade in
    binding.deleteSuccessOverlay.alpha = 0f
    binding.deleteSuccessOverlay.animate()
        .alpha(1f)
        .setDuration(300)
        .start()

    // 7-second ring drain → auto commit
    undoAnimator?.cancel()
    undoAnimator = android.animation.ValueAnimator.ofFloat(1f, 0f).apply {
        duration = 7000
        interpolator = android.view.animation.LinearInterpolator()
        addUpdateListener { binding.undoProgressRing.progress = it.animatedValue as Float }
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Commit (do nothing — items already in system trash)
                dismissSuccessOverlay()
            }
            override fun onAnimationCancel(animation: android.animation.Animator) {
                removeListener(this)
            }
        })
        start()
    }

    // Buttons
    binding.btnOverlayUndo.setOnClickListener {
        undoAnimator?.cancel()
        performRestore(trashedUris)
        dismissSuccessOverlay()
    }
    binding.btnOverlayContinue.setOnClickListener {
        undoAnimator?.cancel()
        dismissSuccessOverlay()
    }

    // NOTE: scrim tap-to-dismiss intentionally removed. Dismiss is
    // Continue / Undo / ring timeout only.
    binding.deleteSuccessOverlay.setOnClickListener(null)
    binding.deleteSuccessOverlay.isClickable = true  // still consumes taps so they don't reach the grid

    hintManager.showHint(
        HintPreferences.HINT_TRASH_UNDO,
        getString(R.string.hint_trash_undo),
    )
}
```

Update the caller (find it by searching for `showTrashSuccessSnackbar(`
in `MainActivity.kt`) to call `showTrashSuccessCard(...)` with the
same arguments.

### Step 7 — Strip the Snackbar wiring

In the same file:

- Delete the `private var successSnackbar: Snackbar? = null` field.
- Delete any `Snackbar` import that's now unused (the compiler will
  tell you on the next build — leave alone if still used by other
  callers like `showSnackbar()` for pre-Android-11 path or error
  messages).
- In `dismissSuccessOverlay()`, remove the `successSnackbar = null`
  line. Add `undoAnimator?.cancel(); undoAnimator = null` at the top.

### Step 8 — Remove old layout-ID references

Anywhere in `MainActivity.kt` that touches
`binding.successCheckmark`, `binding.successTitle`, or
`binding.btnOverlayOk` — remove. All three IDs are gone from the
layout.

Search for each:

```bash
grep -n "successCheckmark\|successTitle\|btnOverlayOk" \
  app/src/main/java/com/example/gallerycleaner/MainActivity.kt
```

Expect: no matches after this step.

### Step 9 — String cleanup

Run this before deleting:

```bash
grep -rn "delete_success_title\|overlay_ok\|R\\.string\\.space_saved" \
  app/src/main/java app/src/main/res
```

For each returned match (expected: none after Step 6 + Step 8 land),
delete the corresponding `<string>` entry from
`res/values/strings.xml`. If the grep shows matches elsewhere in the
codebase (e.g. unrelated code still using `space_saved`), leave that
string in place and flag it in your commit message.

### Step 10 — Unit test

Create
`app/src/test/java/com/example/gallerycleaner/ConfettiViewTest.kt`.

Since `ConfettiView` touches `Context` and `Resources`, we can't
instantiate it in a pure unit test without Robolectric. Instead, pull
the position-generation logic into a pure function and cover *that*.
Add to `ConfettiView.kt`:

```kotlin
object ConfettiLayout {
    fun positions(w: Int, h: Int, seed: Long, count: Int = ConfettiView.PIECE_COUNT): List<Pair<Float, Float>> {
        if (w <= 0 || h <= 0) return emptyList()
        val rng = Random(seed)
        return List(count) {
            val cx = rng.nextFloat() * w
            val cy = rng.nextFloat() * h
            // Consume the other random draws so the sequence matches what
            // the view produces (size, rotation, isRect, colorIndex).
            rng.nextFloat(); rng.nextFloat(); rng.nextBoolean(); rng.nextInt(4)
            cx to cy
        }
    }
}
```

Use this from `ConfettiView.regenerate` so there's only one source
of truth. Then the test:

```kotlin
package com.example.gallerycleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ConfettiViewTest {

    @Test
    fun `same seed produces same positions`() {
        val a = ConfettiLayout.positions(400, 600, 12345L)
        val b = ConfettiLayout.positions(400, 600, 12345L)
        assertEquals(a, b)
    }

    @Test
    fun `different seeds produce different positions`() {
        val a = ConfettiLayout.positions(400, 600, 1L)
        val b = ConfettiLayout.positions(400, 600, 2L)
        assertNotEquals(a, b)
    }

    @Test
    fun `piece count matches PIECE_COUNT`() {
        val positions = ConfettiLayout.positions(400, 600, 42L)
        assertEquals(ConfettiView.PIECE_COUNT, positions.size)
    }

    @Test
    fun `zero-dimension canvas returns empty`() {
        assertEquals(emptyList<Pair<Float, Float>>(), ConfettiLayout.positions(0, 600, 42L))
        assertEquals(emptyList<Pair<Float, Float>>(), ConfettiLayout.positions(400, 0, 42L))
    }
}
```

### Step 11 — Build, test, lint

```bash
./gradlew --stop
./gradlew clean assembleDebug testDebugUnitTest lint
```

All three must be green. Fix anything red before moving on — don't
leave the build broken overnight.

### Step 12 — On-device smoke test

Install the debug APK on your USB phone (`./gradlew installDebug`),
then:

1. Select 1 item → delete. Card should show `N.N kB freed · 1 item
   moved to trash`. Wait 7s — card auto-dismisses, item stays trashed.
2. Select 3+ items → delete. Subtitle is `freed · N items moved to
   trash`.
3. Select 2 items → delete → tap **Undo** inside card. Items return
   to the grid. Card dismisses.
4. Select 2 items → delete → tap **Continue** before ring drains.
   Card dismisses immediately.
5. Tap the scrim outside the card — nothing happens.
6. Rotate while the card is up — card survives; ring progress resets
   (acceptable for now; long-term: persist via savedInstanceState,
   but that's out of scope for this SDD).
7. Status-bar icons render light while the card is up and revert to
   dark after dismiss.

## Acceptance verification

Check each box on the parent `requirement.md`'s acceptance list.
Every one should now be ticked.

## When you're done

1. Update
   `docs/sdd/todo/20260418-010-delete-success/requirement.md` —
   flip every `[ ]` to `[x]` on the acceptance list.
2. Move the folder `docs/sdd/todo/20260418-010-delete-success/` →
   `docs/sdd/done/20260418-010-delete-success/`.
3. Update `docs/sdd/STATUS.md` — row moves from *Pending* to
   *Completed*. Task count `1/1`. One-line summary:
   `Card layout, 7s progress ring, deterministic confetti, Snackbar removed on Android-11+ path`.
4. **Atomic commits** (two separate commits):
   - Implementation: all code + layout + drawable + string + test
     changes.
   - Doc-done: the requirement.md acceptance ticks + folder move +
     STATUS.md update.
