# Task 01: Filter Sheet Rebuild

**Parent:** SDD-20260418-008 — Filter Sheet & Empty State

## What You're Changing

The filter bottom sheet gets rebuilt end-to-end. Four titled sections
(Type is new — previously only in the top bar), mono-font counts on
source chips, a proper Apply/Cancel footer, and **pending-state** semantics
so the grid doesn't jitter behind the sheet while the user is still
deciding.

## Before vs After

| Thing | Before | After |
|---|---|---|
| Sections | Date range, Source, Sort | **Type**, Source, Date, Sort by |
| Section label | 14sp surfaceVariant | 11sp 600 `ink4` uppercase 0.8 tracking |
| Chips layout | horizontal scroll per section | wrapped (`ChipGroup` default) |
| Source count | `WhatsApp (5)` Inter | `WhatsApp 5` — Inter label + mono count |
| Apply behavior | chips commit instantly | chips update pending; `Apply · N changes` commits all at once |
| Footer | no Apply button | outline Cancel + ink-filled Apply row |
| Handle | 32×4dp | 36×4dp |
| Header `Reset` | MaterialButton | accent-colored text |
| Status bar | dark icons over dark scrim (bad) | light icons while open, restore on dismiss |

## Prerequisites

- SDD-002 (Design Tokens) merged. Accent + ink tokens + JetBrains Mono
  already wired.
- `./gradlew --stop` before you start.

## Step-by-Step Instructions

### Step 1 — Rebuild `bottom_sheet_filters.xml`

Replace the whole file with the structure below. Key structural move:
each section is a titled block with a wrapping `ChipGroup` (no more
`HorizontalScrollView`), followed by a footer row.

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingBottom="16dp">

    <!-- Drag handle (36×4dp) -->
    <View
        android:layout_width="36dp"
        android:layout_height="4dp"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="8dp"
        android:layout_marginBottom="4dp"
        android:background="@drawable/bottom_sheet_handle" />

    <!-- Header: title + Reset accent link -->
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
            android:text="@string/filters"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="@color/ink" />

        <TextView
            android:id="@+id/btnReset"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:padding="6dp"
            android:text="@string/reset"
            android:textSize="13sp"
            android:textStyle="bold"
            android:textColor="@color/accent"
            android:background="?attr/selectableItemBackgroundBorderless" />

    </LinearLayout>

    <!-- TYPE section -->
    <include layout="@layout/item_filter_section_label"
        android:id="@+id/labelType" />
    <com.google.android.material.chip.ChipGroup
        android:id="@+id/typeChipGroup"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingHorizontal="20dp"
        app:chipSpacingHorizontal="6dp"
        app:chipSpacingVertical="6dp"
        app:singleSelection="false" />

    <!-- SOURCE section -->
    <include layout="@layout/item_filter_section_label"
        android:id="@+id/labelSource" />
    <com.google.android.material.chip.ChipGroup
        android:id="@+id/sourceChipGroup"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingHorizontal="20dp"
        app:chipSpacingHorizontal="6dp"
        app:chipSpacingVertical="6dp"
        app:singleSelection="false" />

    <!-- DATE section -->
    <include layout="@layout/item_filter_section_label"
        android:id="@+id/labelDate" />
    <com.google.android.material.chip.ChipGroup
        android:id="@+id/dateRangeChipGroup"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingHorizontal="20dp"
        app:chipSpacingHorizontal="6dp"
        app:chipSpacingVertical="6dp"
        app:singleSelection="true"
        app:selectionRequired="true" />

    <!-- SORT BY section -->
    <include layout="@layout/item_filter_section_label"
        android:id="@+id/labelSort" />
    <com.google.android.material.chip.ChipGroup
        android:id="@+id/sortChipGroup"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingHorizontal="20dp"
        app:chipSpacingHorizontal="6dp"
        app:chipSpacingVertical="6dp"
        app:singleSelection="true"
        app:selectionRequired="true" />

    <!-- Footer: Cancel + Apply -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingHorizontal="20dp"
        android:paddingTop="20dp"
        android:gravity="center_vertical">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnCancel"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="0dp"
            android:layout_height="44dp"
            android:layout_weight="1"
            android:insetTop="0dp"
            android:insetBottom="0dp"
            android:text="@string/cancel"
            android:textColor="@color/ink"
            app:cornerRadius="22dp"
            app:strokeColor="@color/line_strong"
            app:strokeWidth="1dp" />

        <Space
            android:layout_width="8dp"
            android:layout_height="1dp" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnApply"
            android:layout_width="0dp"
            android:layout_height="44dp"
            android:layout_weight="2"
            android:insetTop="0dp"
            android:insetBottom="0dp"
            android:text="@string/apply"
            android:textColor="@android:color/white"
            app:backgroundTint="@color/ink"
            app:cornerRadius="22dp"
            tools:text="Apply · 3 changes" />

    </LinearLayout>

</LinearLayout>
```

> **`@layout/item_filter_section_label`** — we're about to create that
> in Step 2. Without it, the build will fail on these `<include>`s.

### Step 2 — Create the section-label layout

Each of the four `<include>`s above points at the same small layout.
Create `app/src/main/res/layout/item_filter_section_label.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingHorizontal="20dp"
    android:paddingTop="14dp"
    android:paddingBottom="6dp"
    android:textSize="11sp"
    android:textFontWeight="600"
    android:textColor="@color/ink4"
    android:letterSpacing="0.08"
    android:textAllCaps="true" />
```

In the parent `bottom_sheet_filters.xml` you'll set each include's
`android:text` via data binding or — since we're not using data binding
— set the text on the inflated TextView from Kotlin in `onViewCreated`
(Step 3). The four include `android:id`s (`labelType`, `labelSource`,
`labelDate`, `labelSort`) give ViewBinding handles.

Because ViewBinding resolves includes to the root view (`TextView`
here), you can do `binding.labelType.setText(R.string.filter_section_type)`
in Kotlin — see Step 4 for the exact wiring.

### Step 3 — Add the new strings

In `app/src/main/res/values/strings.xml`, add:

```xml
<!-- Filter sheet (SDD-20260418-008) -->
<string name="filter_section_type">Type</string>
<string name="filter_section_source">Source</string>
<string name="filter_section_date">Date</string>
<string name="filter_section_sort">Sort by</string>
<string name="apply">Apply</string>
<string name="apply_with_changes">Apply · %1$d changes</string>
<string name="filter_type_all">All</string>
```

> **Why `apply_with_changes` as a single string?** i18n correctness —
> future locales may order the count differently (`3 cambios · Aplicar`).
> One format string > concatenation.

### Step 4 — Rewrite `FilterBottomSheetFragment` with pending state

Replace the whole file. Key changes:
- Four `pending*` fields hold the in-sheet state, seeded from the
  viewmodel on `onViewCreated`.
- Chip taps update the pending fields, not the viewmodel.
- A helper `refreshApplyButton()` computes the diff and updates the
  button label.
- `Apply` copies pending → viewmodel and dismisses; `Cancel` and
  swipe-dismiss just dismiss.

```kotlin
package com.example.gallerycleaner

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import com.example.gallerycleaner.databinding.BottomSheetFiltersBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFiltersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GalleryViewModel by activityViewModels()

    // Pending state — initialized from viewModel on onViewCreated
    private lateinit var pendingMediaTypes: MutableSet<MediaType>
    private lateinit var pendingSources: MutableSet<SourceType>
    private var pendingDateRange: DateRange = DateRange.DEFAULT
    private lateinit var pendingSortOption: SortOption

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Seed pending = applied
        pendingMediaTypes = viewModel.selectedMediaTypes.value.toMutableSet()
        pendingSources = viewModel.selectedSources.value.toMutableSet()
        pendingDateRange = viewModel.selectedDateRange.value
        pendingSortOption = viewModel.selectedSortOption.value

        setSectionLabels()
        setupTypeChips()
        setupSourceChips()
        setupDateRangeChips()
        setupSortChips()
        setupFooter()
        setupReset()
        refreshApplyButton()
    }

    override fun onStart() {
        super.onStart()
        // Light status-bar icons while the sheet's dark scrim is showing.
        dialog?.window?.let { w ->
            val controller = w.insetsController
            controller?.setSystemBarsAppearance(
                0, // clear LIGHT_STATUS_BARS => icons go light (white) on the scrim
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

    override fun onDestroyView() {
        // Restore dark status-bar icons for the main activity.
        requireActivity().window.insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
        super.onDestroyView()
        _binding = null
    }

    private fun setSectionLabels() {
        binding.labelType.setText(R.string.filter_section_type)
        binding.labelSource.setText(R.string.filter_section_source)
        binding.labelDate.setText(R.string.filter_section_date)
        binding.labelSort.setText(R.string.filter_section_sort)
    }

    private fun setupTypeChips() {
        binding.typeChipGroup.removeAllViews()
        MediaType.values().forEach { type ->
            val label = if (type == MediaType.PHOTO) getString(R.string.photos) else getString(R.string.videos)
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = type in pendingMediaTypes
                setOnClickListener {
                    if (isChecked) pendingMediaTypes.add(type) else pendingMediaTypes.remove(type)
                    refreshApplyButton()
                }
            }
            binding.typeChipGroup.addView(chip)
        }
    }

    private fun setupSourceChips() {
        binding.sourceChipGroup.removeAllViews()
        val state = viewModel.uiState.value
        val sourceCounts = when (state) {
            is GalleryUiState.Normal -> state.sourceCounts
            is GalleryUiState.Selection -> state.sourceCounts
            is GalleryUiState.NoFiltersSelected -> state.sourceCounts
            is GalleryUiState.NoMatchingItems -> state.sourceCounts
            else -> emptyMap()
        }
        val allSourceCounts = when (state) {
            is GalleryUiState.Normal -> state.allSourceCounts
            is GalleryUiState.Selection -> state.allSourceCounts
            is GalleryUiState.NoFiltersSelected -> state.allSourceCounts
            is GalleryUiState.NoMatchingItems -> state.allSourceCounts
            else -> emptyMap()
        }

        // "All" chip
        val totalCount = sourceCounts.values.sum()
        val allAvailableSources = allSourceCounts.keys
        val allSelected = allAvailableSources.isNotEmpty() && pendingSources.containsAll(allAvailableSources)
        val allChip = Chip(requireContext()).apply {
            text = sourceChipLabel(getString(R.string.filter_type_all), totalCount)
            isCheckable = true
            isChecked = allSelected
            setOnClickListener {
                if (isChecked) pendingSources.addAll(allAvailableSources)
                else pendingSources.clear()
                setupSourceChips() // Refresh to sync per-source chips
                refreshApplyButton()
            }
        }
        binding.sourceChipGroup.addView(allChip)

        // Per-source chips
        SourceType.values().forEach { source ->
            val count = sourceCounts[source] ?: 0
            if (count > 0) {
                val chip = Chip(requireContext()).apply {
                    text = sourceChipLabel(source.label, count)
                    isCheckable = true
                    isChecked = source in pendingSources
                    setOnClickListener {
                        if (isChecked) pendingSources.add(source) else pendingSources.remove(source)
                        refreshApplyButton()
                    }
                }
                binding.sourceChipGroup.addView(chip)
            }
        }
    }

    /**
     * Builds a SpannableString like "WhatsApp  5" where " 5" is in JetBrains Mono.
     * Two spaces between label and count for breathing room.
     */
    private fun sourceChipLabel(name: String, count: Int): CharSequence {
        val text = "$name  $count"
        val spannable = SpannableString(text)
        val monoTypeface: Typeface? = ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono)
        if (monoTypeface != null) {
            // Apply mono span to the "  N" suffix
            val start = name.length
            spannable.setSpan(
                CustomTypefaceSpan(monoTypeface),
                start, text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    private fun setupDateRangeChips() {
        binding.dateRangeChipGroup.removeAllViews()
        DateRangePreset.values().forEach { preset ->
            val chip = Chip(requireContext()).apply {
                text = when {
                    preset == DateRangePreset.CUSTOM && pendingDateRange.preset == DateRangePreset.CUSTOM -> {
                        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
                        val start = Date((pendingDateRange.startTimestamp ?: 0L) * 1000)
                        val end = Date((pendingDateRange.endTimestamp ?: System.currentTimeMillis() / 1000) * 1000)
                        "${fmt.format(start)} – ${fmt.format(end)}"
                    }
                    else -> preset.label
                }
                isCheckable = true
                isChecked = preset == pendingDateRange.preset
                setOnClickListener {
                    if (preset == DateRangePreset.CUSTOM) {
                        showDateRangePicker()
                    } else {
                        pendingDateRange = DateRange(preset)
                        setupDateRangeChips()
                        refreshApplyButton()
                    }
                }
            }
            binding.dateRangeChipGroup.addView(chip)
        }
    }

    private fun setupSortChips() {
        binding.sortChipGroup.removeAllViews()
        SortOption.values().forEach { option ->
            val chip = Chip(requireContext()).apply {
                text = option.label
                isCheckable = true
                isChecked = option == pendingSortOption
                setOnClickListener {
                    pendingSortOption = option
                    setupSortChips()
                    refreshApplyButton()
                }
            }
            binding.sortChipGroup.addView(chip)
        }
    }

    private fun setupReset() {
        binding.btnReset.setOnClickListener {
            pendingMediaTypes = MediaType.values().toMutableSet()
            pendingSources = viewModel.uiState.value.let { state ->
                when (state) {
                    is GalleryUiState.Normal -> state.allSourceCounts.keys.toMutableSet()
                    is GalleryUiState.Selection -> state.allSourceCounts.keys.toMutableSet()
                    is GalleryUiState.NoFiltersSelected -> state.allSourceCounts.keys.toMutableSet()
                    is GalleryUiState.NoMatchingItems -> state.allSourceCounts.keys.toMutableSet()
                    else -> mutableSetOf()
                }
            }
            pendingDateRange = DateRange.DEFAULT
            pendingSortOption = SortOption.DATE_DESC
            setupTypeChips()
            setupSourceChips()
            setupDateRangeChips()
            setupSortChips()
            refreshApplyButton()
        }
    }

    private fun setupFooter() {
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnApply.setOnClickListener {
            commitPending()
            dismiss()
        }
    }

    private fun countChanges(): Int {
        var n = 0
        if (pendingMediaTypes != viewModel.selectedMediaTypes.value) n++
        if (pendingSources != viewModel.selectedSources.value) n++
        if (pendingDateRange != viewModel.selectedDateRange.value) n++
        if (pendingSortOption != viewModel.selectedSortOption.value) n++
        return n
    }

    private fun refreshApplyButton() {
        val changes = countChanges()
        binding.btnApply.text = if (changes > 0) {
            getString(R.string.apply_with_changes, changes)
        } else {
            getString(R.string.apply)
        }
    }

    private fun commitPending() {
        // Use existing viewmodel setters — pending-copy → applied
        if (pendingMediaTypes != viewModel.selectedMediaTypes.value) {
            viewModel.setSelectedMediaTypes(pendingMediaTypes)
        }
        if (pendingSources != viewModel.selectedSources.value) {
            viewModel.setSelectedSources(pendingSources)
        }
        if (pendingDateRange != viewModel.selectedDateRange.value) {
            if (pendingDateRange.preset == DateRangePreset.CUSTOM) {
                viewModel.setCustomDateRange(
                    pendingDateRange.startTimestamp ?: 0L,
                    pendingDateRange.endTimestamp ?: (System.currentTimeMillis() / 1000)
                )
            } else {
                viewModel.setDateRangePreset(pendingDateRange.preset)
            }
        }
        if (pendingSortOption != viewModel.selectedSortOption.value) {
            viewModel.setSortOption(pendingSortOption)
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.select_date_range))
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            pendingDateRange = DateRange(
                preset = DateRangePreset.CUSTOM,
                startTimestamp = selection.first / 1000L,
                endTimestamp = selection.second / 1000L,
            )
            setupDateRangeChips()
            refreshApplyButton()
        }
        picker.show(parentFragmentManager, "date_picker")
    }

    companion object {
        const val TAG = "FilterBottomSheetFragment"
        fun newInstance(): FilterBottomSheetFragment = FilterBottomSheetFragment()
    }
}
```

### Step 5 — Add the `CustomTypefaceSpan` helper

`TypefaceSpan` by name doesn't work on older APIs for custom fonts; we
apply a real `Typeface` via a small subclass. Create
`app/src/main/java/com/example/gallerycleaner/CustomTypefaceSpan.kt`:

```kotlin
package com.example.gallerycleaner

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.TypefaceSpan

/** Applies an actual Typeface (not just a family name) to a span. */
class CustomTypefaceSpan(private val typeface: Typeface) : TypefaceSpan(typeface) {
    override fun updateDrawState(ds: TextPaint) = apply(ds)
    override fun updateMeasureState(paint: TextPaint) = apply(paint)
    private fun apply(paint: Paint) {
        paint.typeface = typeface
    }
}
```

> On API 28+ `TypefaceSpan` has a constructor that takes a `Typeface`
> directly, but this subclass is harmless and explicit.

### Step 6 — Add missing viewmodel setters if not present

Check `GalleryViewModel.kt` for these methods:

- `setSelectedMediaTypes(Set<MediaType>)`
- `setSelectedSources(Set<SourceType>)`
- `setDateRangePreset(DateRangePreset)`
- `setCustomDateRange(Long, Long)`
- `setSortOption(SortOption)`

If `setSelectedMediaTypes` or `setSelectedSources` don't exist (the
current codebase uses `toggleSourceFilter` and `toggleMediaType`),
add bulk-set variants. Pattern:

```kotlin
fun setSelectedMediaTypes(types: Set<MediaType>) {
    filterPreferences.saveSelectedMediaTypes(types)
}

fun setSelectedSources(sources: Set<SourceType>) {
    filterPreferences.saveSelectedSources(sources)
}
```

(And the corresponding `save*` methods in `FilterPreferences.kt` if
they aren't there. Match the style of `saveSelectedSortOption`.)

### Step 7 — Verify no instant-apply callbacks remain

Grep for direct viewmodel filter mutations from the fragment:

```bash
grep -n "viewModel\.\(toggle\|set\)" app/src/main/java/com/example/gallerycleaner/FilterBottomSheetFragment.kt
```

Only the ones inside `commitPending()` should remain. If you see any
in `setup*Chips`, they leaked — move them to pending mutation.

### Step 8 — Build

```bash
./gradlew --stop
./gradlew clean assembleDebug testDebugUnitTest lint
```

Likely first-run failures:
- `Unresolved reference: setSelectedMediaTypes` → Step 6 incomplete.
- `ViewBinding labelType` → Step 2's include didn't get an `android:id`.
- Chip counts overlap → switch `chipSpacingHorizontal` from 6dp to 8dp.

### Step 9 — Visual smoke test

1. Tap Filters pill → sheet slides up. Status-bar icons are **light**.
2. Four sections visible in order: Type, Source, Date, Sort by.
3. Source chips show `Name  N` with the count in mono.
4. Tap Photos off → Photos chip deselects, **grid behind doesn't
   change**. Apply label updates to `Apply · 1 changes`.
5. Tap a source → `Apply · 2 changes`.
6. Tap Cancel → sheet dismisses, grid unchanged, status bar restores
   to dark.
7. Reopen, change some chips, tap Apply → sheet dismisses, grid
   updates to reflect new filters.
8. Reopen, tap Reset → all chips reset to defaults, Apply count
   reflects the diff between defaults and currently-applied.

## Definition of Done

- [ ] All files in Files Changed (task-01 scope) landed
- [ ] No auto-apply chip taps remain in the fragment
- [ ] Apply label matches pending-vs-applied diff
- [ ] Status-bar flip works on sheet open/close
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [ ] Visual smoke test passes

## Known gotchas

- **`DateRange.equals` works by-field** because it's a data class; the
  `pendingDateRange != viewModel.selectedDateRange.value` comparison is
  correct even for custom ranges.
- **"All" source-chip selection** uses `allSourceCounts.keys`, which is
  the set of sources in the library — not the set that pass the current
  date filter. That matches the existing toggle-all behavior.
- **`setupSourceChips()` re-inflates all chips** on every tap of the
  "All" chip (to sync visual state). That's fine for <20 chips; if the
  library explodes to dozens of sources, switch to per-chip state
  updates.
- **BottomSheetDialogFragment `onDestroyView` vs `onDismiss`**: the
  status-bar restore belongs in `onDestroyView` so it runs regardless
  of how the sheet was closed (swipe, back, Apply, Cancel).
- **`TypefaceSpan(Typeface)` constructor** is API 28+; the subclass
  works below that. minSdk here is 26, so the subclass is required.
