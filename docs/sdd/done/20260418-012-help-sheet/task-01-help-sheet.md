# Task 01: Help Sheet Redesign

**Parent:** SDD-20260418-012 — Help / Tips Sheet

## What You're Changing

The "Tips & shortcuts" bottom sheet (opens when you tap the `?` in the
top bar) gets a full visual refresh:

- Tips rendered with **accent-colored icons** and divider lines
  between rows.
- `Reset tips` link moves from a centered underlined link at the
  bottom to a small text link in the **header**, next to the title.
- The current "Buy me a coffee" pill button is replaced by a
  **full-width support card** at the bottom (soft-cream fill, accent
  circle, two-line copy, chevron).
- The explicit `OK` button is **removed** — swipe-down / tap-outside
  dismiss the sheet, same as the Filters sheet.
- Status-bar icons flip to **light** while the sheet is open and
  revert on dismiss.

The `overlay_ok` string (historically shared with the old delete
overlay, last caller was the OK button here) finally drops.

## Before vs After

| Thing | Before | After |
|---|---|---|
| Title | `Tips & Shortcuts` (20sp bold) | `Tips & shortcuts` (20sp bold, lowercase "s") |
| Reset tips | Centered underlined link at bottom | Accent text link in header row, right of title |
| Tip icon | 24dp, `fast_scroll_tooltip_bg` tint (ink) | 22dp, `accent` tint |
| Tip label | 15sp `onSurface` | 13sp `ink2` |
| Between rows | Nothing | 1dp `line` divider |
| Ko-fi | Tonal pill button with Ko-fi logo | Full-width support card: accent-soft 32dp icon + title + subtitle + chevron |
| OK button | Ink-filled pill | *removed* |
| Status bar | Unchanged (dark icons on dark scrim) | Light icons while open |

## Prerequisites

- SDD-002, SDD-008 merged. Tokens + Filters sheet's status-bar-flip
  pattern are in place.
- `./gradlew --stop` before you start.
- Files to understand first:
  - [bottom_sheet_help.xml](../../../../app/src/main/res/layout/bottom_sheet_help.xml) — current layout
  - [HelpBottomSheetFragment.kt](../../../../app/src/main/java/com/example/gallerycleaner/HelpBottomSheetFragment.kt) — current logic
  - [FilterBottomSheetFragment.kt onStart/onDestroyView](../../../../app/src/main/java/com/example/gallerycleaner/FilterBottomSheetFragment.kt) — reference for status-bar flip

## Step-by-Step Instructions

### Step 1 — Drawables

Create **`res/drawable/kofi_icon_bg.xml`** (32dp `accent_soft` oval):

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/accent_soft" />
</shape>
```

Create **`res/drawable/ic_chevron_right.xml`** (20dp chevron vector,
tint applied in layout):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp"
    android:height="20dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="M9,6 L15,12 L9,18"
        android:strokeWidth="2"
        android:strokeColor="#FFFFFF"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="@android:color/transparent" />
</vector>
```

> Stroke color is `#FFFFFF` so that `app:tint` in the layout fully
> controls the final color (stroke-only vectors still respect tint).

### Step 2 — Strings

Open `res/values/strings.xml`:

1. **Change** `tips_and_shortcuts` value from `Tips &amp; Shortcuts`
   to `Tips &amp; shortcuts` (lowercase "s").
2. **Add** two new strings after the existing Ko-fi block:

```xml
<string name="kofi_card_title">Enjoying the app?</string>
<string name="kofi_card_subtitle">One coffee keeps it ad-free.</string>
```

3. **Remove** the `overlay_ok` entry (added back during SDD-010 cleanup
   because this sheet was its last caller — which we're removing here).

Leave `tip_*`, `reset_tips`, `support_developer`, and
`hints_reset_confirmation` alone.

### Step 3 — Rebuild `bottom_sheet_help.xml`

Replace the whole file with the structure below. Key moves: handle
size bumps 32 → 36dp (matches Filters sheet); new horizontal header
row; tips container stays; everything below the container (divider +
pill button + OK button + reset link) is replaced with a single Ko-fi
card.

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

    <!-- Drag handle (36x4dp) -->
    <View
        android:layout_width="36dp"
        android:layout_height="4dp"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="8dp"
        android:layout_marginBottom="4dp"
        android:background="@drawable/bottom_sheet_handle" />

    <!-- Header: title + Reset tips accent link -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingHorizontal="20dp"
        android:paddingTop="10dp"
        android:paddingBottom="10dp"
        android:gravity="center_vertical">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/tips_and_shortcuts"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="@color/ink" />

        <TextView
            android:id="@+id/btnResetTips"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:padding="6dp"
            android:text="@string/reset_tips"
            android:textSize="12sp"
            android:textStyle="bold"
            android:textColor="@color/accent"
            android:background="?attr/selectableItemBackgroundBorderless" />
    </LinearLayout>

    <!-- Tip rows (populated in HelpBottomSheetFragment.setupTips) -->
    <LinearLayout
        android:id="@+id/tipContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingHorizontal="20dp" />

    <!-- Ko-fi support card -->
    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/kofiCard"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:layout_marginHorizontal="16dp"
        android:padding="14dp"
        android:background="@drawable/kofi_card_bg"
        android:clickable="true"
        android:focusable="true"
        android:foreground="?attr/selectableItemBackground">

        <FrameLayout
            android:id="@+id/kofiIconWrap"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:background="@drawable/kofi_icon_bg"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent">

            <ImageView
                android:layout_width="18dp"
                android:layout_height="18dp"
                android:layout_gravity="center"
                android:src="@drawable/kofi_logo"
                android:importantForAccessibility="no" />
        </FrameLayout>

        <TextView
            android:id="@+id/kofiTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:text="@string/kofi_card_title"
            android:textSize="14sp"
            android:textStyle="bold"
            android:textColor="@color/ink"
            app:layout_constraintStart_toEndOf="@id/kofiIconWrap"
            app:layout_constraintEnd_toStartOf="@id/kofiChevron"
            app:layout_constraintTop_toTopOf="@id/kofiIconWrap" />

        <TextView
            android:id="@+id/kofiSubtitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:text="@string/kofi_card_subtitle"
            android:textSize="12sp"
            android:textColor="@color/ink3"
            app:layout_constraintStart_toEndOf="@id/kofiIconWrap"
            app:layout_constraintEnd_toStartOf="@id/kofiChevron"
            app:layout_constraintTop_toBottomOf="@id/kofiTitle" />

        <ImageView
            android:id="@+id/kofiChevron"
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:src="@drawable/ic_chevron_right"
            app:tint="@color/ink3"
            android:importantForAccessibility="no"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>

</LinearLayout>
```

Also create **`res/drawable/kofi_card_bg.xml`** (rounded
`surface_alt` container):

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/surface_alt" />
    <corners android:radius="14dp" />
</shape>
```

### Step 4 — Rewrite `HelpBottomSheetFragment.kt`

Replace the whole file:

```kotlin
package com.example.gallerycleaner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.gallerycleaner.databinding.BottomSheetHelpBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar

class HelpBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTips()
        setupResetButton()
        setupKofiCard()
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

    private fun setupTips() {
        val tips = listOf(
            R.drawable.ic_touch_app to R.string.tip_long_press,
            R.drawable.ic_swipe to R.string.tip_drag_select,
            R.drawable.ic_pinch_zoom to R.string.tip_pinch_zoom,
            R.drawable.ic_filter_list to R.string.tip_filters,
            R.drawable.ic_arrow_downward to R.string.tip_fast_scroll,
            R.drawable.ic_arrow_downward to R.string.tip_continue,
            R.drawable.ic_delete to R.string.tip_trash,
            R.drawable.ic_undo to R.string.tip_undo,
        )

        val ctx = requireContext()
        val accent = ContextCompat.getColor(ctx, R.color.accent)
        val inkSecondary = ContextCompat.getColor(ctx, R.color.ink2)
        val lineColor = ContextCompat.getColor(ctx, R.color.line)

        tips.forEachIndexed { index, (iconRes, textRes) ->
            // Divider between rows (not before the first)
            if (index > 0) {
                val divider = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                    )
                    setBackgroundColor(lineColor)
                }
                binding.tipContainer.addView(divider)
            }

            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(12), 0, dp(12))
            }

            val icon = ImageView(ctx).apply {
                setImageResource(iconRes)
                setColorFilter(accent)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    marginEnd = dp(12)
                }
            }

            val text = TextView(ctx).apply {
                setText(textRes)
                textSize = 13f
                setTextColor(inkSecondary)
            }

            row.addView(icon)
            row.addView(text)
            binding.tipContainer.addView(row)
        }
    }

    private fun setupResetButton() {
        binding.btnResetTips.setOnClickListener {
            HintPreferences(requireContext()).resetAllHints()
            Snackbar.make(
                requireView(),
                getString(R.string.hints_reset_confirmation),
                Snackbar.LENGTH_SHORT,
            ).show()
            dismiss()
        }
    }

    private fun setupKofiCard() {
        binding.kofiCard.setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/branev"))
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "HelpBottomSheet"
        fun newInstance() = HelpBottomSheetFragment()
    }
}
```

### Step 5 — Sanity grep

Make sure no stale references to removed ids and strings remain:

```bash
grep -rn "btnOkHelp\|btnSupportDeveloper\|overlay_ok" \
  app/src/main/java app/src/main/res
```

Expected: no matches. (The new layout doesn't contain those ids; no
other file should.)

### Step 6 — Build + test + lint

```bash
./gradlew clean assembleDebug testDebugUnitTest lint
```

All three must be green. Lint may flag an unused string if you miss
one during step 2 — fix it and rerun.

### Step 7 — On-device smoke

```bash
./gradlew installDebug
```

Then:

1. Open the app, tap the `?` in the top bar. Sheet slides up from
   the bottom.
2. Verify header shows `Tips & shortcuts` on the left and a small
   accent `Reset tips` link on the right.
3. Verify each of 8 tip rows has a small accent-colored icon and a
   smaller grey label. Thin horizontal divider between each pair.
4. Status-bar time/icons render **light** while sheet is open.
5. Scroll to bottom. Tap the Ko-fi card — browser opens
   `https://ko-fi.com/branev`.
6. Back to app. Reopen sheet. Tap `Reset tips` — snackbar appears,
   sheet dismisses. Reopen the app and verify the first hint (e.g.
   long-press) appears after ~1s.
7. Reopen help sheet, swipe down. Sheet dismisses. Status-bar icons
   revert to dark.

### Step 8 — Mark done + commit

1. Flip every `[ ]` to `[x]` on `requirement.md`'s acceptance list.
2. Move `docs/sdd/todo/20260418-012-help-sheet/` →
   `docs/sdd/done/20260418-012-help-sheet/`.
3. Update `docs/sdd/STATUS.md` — move row from Pending to Completed,
   Tasks `1/1`, short summary:
   `Accent tip icons + dividers, Reset tips in header, Ko-fi support card, status-bar flip, OK button + overlay_ok string removed`.
4. **Atomic commits:**
   - Implementation: layout + 3 drawables + strings + fragment.
   - Doc-done: acceptance ticks + folder move + STATUS.md update.
