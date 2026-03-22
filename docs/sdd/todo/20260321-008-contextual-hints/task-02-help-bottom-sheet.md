# Task 02: Help Button & Bottom Sheet

**Parent:** SDD-20260321-008 — Contextual Hints & Help

## What You're Building

A `?` help button in the header that opens a bottom sheet listing all app tips, with a "Reset Tips" button that re-enables dismissed hints.

## Step-by-Step Instructions

### Step 1: Create the help icon

Create `app/src/main/res/drawable/ic_help_outline.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/badge_unviewed_text"
        android:pathData="M11,18h2v-2h-2v2zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8zM12,6c-2.21,0 -4,1.79 -4,4h2c0,-1.1 0.9,-2 2,-2s2,0.9 2,2c0,2 -3,1.75 -3,5h2c0,-2.25 3,-2.5 3,-5 0,-2.21 -1.79,-4 -4,-4z" />
</vector>
```

### Step 2: Create Material icons for the help sheet tips

Create these icon drawables. Each is a 24dp Material Symbol:

**`app/src/main/res/drawable/ic_touch_app.xml`:**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/fast_scroll_tooltip_bg"
        android:pathData="M9,11.24V7.5C9,6.12 10.12,5 11.5,5S14,6.12 14,7.5v3.74c1.21,-0.81 2,-2.18 2,-3.74C16,5.01 13.99,3 11.5,3S7,5.01 7,7.5c0,1.56 0.79,2.93 2,3.74zM18.84,15.87l-4.54,-2.26c-0.17,-0.07 -0.35,-0.11 -0.54,-0.11H13v-6C13,6.67 12.33,6 11.5,6S10,6.67 10,7.5v10.74l-3.43,-0.72c-0.08,-0.01 -0.15,-0.03 -0.24,-0.03 -0.31,0 -0.59,0.13 -0.79,0.33l-0.79,0.8 4.94,4.94c0.27,0.27 0.65,0.44 1.06,0.44h6.79c0.75,0 1.33,-0.55 1.44,-1.28l0.75,-5.27c0.01,-0.07 0.02,-0.14 0.02,-0.2 0,-0.62 -0.38,-1.16 -0.91,-1.38z" />
</vector>
```

**`app/src/main/res/drawable/ic_swipe.xml`:**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/fast_scroll_tooltip_bg"
        android:pathData="M19.75,6.07L17.75,4.32L14.25,8.32V4H12.75V8.32L9.25,4.32L7.25,6.07L12,11.57L16.75,6.07ZM4.25,17.93L6.25,19.68L9.75,15.68V20H11.25V15.68L14.75,19.68L16.75,17.93L12,12.43L7.25,17.93Z" />
</vector>
```

**`app/src/main/res/drawable/ic_pinch_zoom.xml`:**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/fast_scroll_tooltip_bg"
        android:pathData="M6,2l-4,4 4,4V7h4V5H6V2zM18,14l4,-4 -4,-4v3h-4v2h4v3zM14,16H10v2h4v-2zM14,20H10v2h4v-2zM14,12H10v2h4v-2z" />
</vector>
```

**`app/src/main/res/drawable/ic_undo.xml`:**
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/fast_scroll_tooltip_bg"
        android:pathData="M12.5,8c-2.65,0 -5.05,0.99 -6.9,2.6L2,7v9h9l-3.62,-3.62c1.39,-1.16 3.16,-1.88 5.12,-1.88 3.54,0 6.55,2.31 7.6,5.5l2.37,-0.78C21.08,11.03 17.15,8 12.5,8z" />
</vector>
```

> **Note:** We already have `ic_filter_list.xml`, `ic_arrow_downward.xml`, and `ic_delete.xml` — reuse those. Only create icons we don't have.

### Step 3: Add help sheet strings

Open `app/src/main/res/values/strings.xml` and add:

```xml
<!-- Help bottom sheet (SDD-20260321-008) -->
<string name="tips_and_shortcuts">Tips &amp; Shortcuts</string>
<string name="tip_long_press">Long-press an image to start selecting</string>
<string name="tip_drag_select">Swipe horizontally across items to select multiple</string>
<string name="tip_pinch_zoom">Pinch to zoom the grid in or out (2–5 columns)</string>
<string name="tip_filters">Use Filters to narrow by source, date, or type</string>
<string name="tip_fast_scroll">Drag the right edge to scroll quickly through your library</string>
<string name="tip_continue">Tap Continue to jump to unreviewed items</string>
<string name="tip_trash">Deleted items stay in trash for 30 days</string>
<string name="tip_undo">Tap Undo after deleting to restore items</string>
<string name="reset_tips">Reset Tips</string>
```

### Step 4: Create bottom_sheet_help.xml

Create `app/src/main/res/layout/bottom_sheet_help.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingBottom="24dp">

    <!-- Drag handle -->
    <View
        android:layout_width="32dp"
        android:layout_height="4dp"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="8dp"
        android:layout_marginBottom="8dp"
        android:background="@drawable/bottom_sheet_handle" />

    <!-- Title -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingHorizontal="16dp"
        android:paddingVertical="12dp"
        android:text="@string/tips_and_shortcuts"
        android:textSize="20sp"
        android:textStyle="bold"
        android:textColor="?attr/colorOnSurface" />

    <!-- Tip rows -->
    <LinearLayout android:id="@+id/tipContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingHorizontal="16dp" />

    <!-- Reset Tips button -->
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnResetTips"
        style="@style/Widget.Material3.Button.TextButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="24dp"
        android:text="@string/reset_tips"
        android:textColor="@color/empty_state_reset_btn_text"
        android:textStyle="bold"
        android:paddingHorizontal="32dp"
        android:paddingVertical="12dp"
        app:backgroundTint="@color/empty_state_reset_btn_bg"
        app:cornerRadius="999dp" />

</LinearLayout>
```

### Step 5: Create HelpBottomSheetFragment.kt

Create `app/src/main/java/com/example/gallerycleaner/HelpBottomSheetFragment.kt`:

```kotlin
package com.example.gallerycleaner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.gallerycleaner.databinding.BottomSheetHelpBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar

class HelpBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetHelpBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTips()
        setupResetButton()
    }

    private fun setupTips() {
        val tips = listOf(
            Pair(R.drawable.ic_touch_app, R.string.tip_long_press),
            Pair(R.drawable.ic_swipe, R.string.tip_drag_select),
            Pair(R.drawable.ic_pinch_zoom, R.string.tip_pinch_zoom),
            Pair(R.drawable.ic_filter_list, R.string.tip_filters),
            Pair(R.drawable.ic_arrow_downward, R.string.tip_fast_scroll),
            Pair(R.drawable.ic_arrow_downward, R.string.tip_continue),
            Pair(R.drawable.ic_delete, R.string.tip_trash),
            Pair(R.drawable.ic_undo, R.string.tip_undo)
        )

        for ((iconRes, textRes) in tips) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }

            val icon = ImageView(requireContext()).apply {
                setImageResource(iconRes)
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                    marginEnd = dp(16)
                }
            }

            val text = TextView(requireContext()).apply {
                setText(textRes)
                textSize = 15f
                setTextColor(requireContext().getColor(R.color.md_theme_onSurface))
            }

            row.addView(icon)
            row.addView(text)
            binding.tipContainer.addView(row)
        }
    }

    private fun setupResetButton() {
        binding.btnResetTips.setOnClickListener {
            HintPreferences(requireContext()).resetAllHints()
            Snackbar.make(requireView(), getString(R.string.hints_reset_confirmation), Snackbar.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val TAG = "HelpBottomSheet"
        fun newInstance() = HelpBottomSheetFragment()
    }
}
```

### Step 6: Add the help button to activity_main.xml

Open `app/src/main/res/layout/activity_main.xml`.

Find the Filters button's FrameLayout wrapper. Add the help button **before** it (inside the title row LinearLayout):

```xml
<ImageButton
    android:id="@+id/btnHelp"
    android:layout_width="40dp"
    android:layout_height="40dp"
    android:layout_marginEnd="8dp"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:src="@drawable/ic_help_outline"
    android:contentDescription="@string/tips_and_shortcuts" />
```

### Step 7: Wire the help button in MainActivity

Open `app/src/main/java/com/example/gallerycleaner/MainActivity.kt`.

Add in `onCreate`, near the other button setups:

```kotlin
binding.btnHelp.setOnClickListener {
    HelpBottomSheetFragment.newInstance().show(supportFragmentManager, HelpBottomSheetFragment.TAG)
}
```

### Step 8: Build and verify

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Test: tap the `?` button → help sheet opens with all tips → tap "Reset Tips" → snackbar confirms.

## Files Changed

| File | What changed |
|------|-------------|
| `app/src/main/res/drawable/ic_help_outline.xml` | **New file** — help icon |
| `app/src/main/res/drawable/ic_touch_app.xml` | **New file** — touch icon |
| `app/src/main/res/drawable/ic_swipe.xml` | **New file** — swipe icon |
| `app/src/main/res/drawable/ic_pinch_zoom.xml` | **New file** — pinch icon |
| `app/src/main/res/drawable/ic_undo.xml` | **New file** — undo icon |
| `app/src/main/res/layout/bottom_sheet_help.xml` | **New file** — help sheet layout |
| `app/src/main/res/values/strings.xml` | Added tip and help sheet strings |
| `app/src/main/java/.../HelpBottomSheetFragment.kt` | **New file** — help bottom sheet |
| `app/src/main/res/layout/activity_main.xml` | Added help button in header |
| `app/src/main/java/.../MainActivity.kt` | Help button click handler |

## Acceptance Criteria

- [ ] `?` button visible in header, to the left of Filters
- [ ] Tapping `?` opens a bottom sheet with all tips listed with icons
- [ ] "Reset Tips" button clears all hint preferences
- [ ] Snackbar confirms "Tips will appear again as you use the app"
- [ ] Build succeeds, all tests pass
