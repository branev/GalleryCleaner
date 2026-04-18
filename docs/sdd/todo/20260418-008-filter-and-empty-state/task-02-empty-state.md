# Task 02: Empty State Redesign

**Parent:** SDD-20260418-008 — Filter Sheet & Empty State

**Runs after:** task-01-filter-sheet.md (they land in the same PR —
order just matters because task-01 introduces the shared viewmodel
setters this one uses indirectly via `FilterComboFormatter`, and the
"Edit filters" action opens the sheet task-01 rebuilt).

## What You're Changing

The "no matches" screen gets lighter. The filled 96dp disc shrinks to a
72dp thin ring, the title goes from 22sp to 20sp, and the subtitle
spells out the specific filter combo the user picked (so they see at a
glance why nothing matched). A secondary **"Edit filters"** text button
appears below the `Reset filters` pill, opening the filter sheet
directly — no need to dismiss and tap the pill in the top bar.

## Before vs After

| Thing | Before | After |
|---|---|---|
| Icon container | 96dp disc, `empty_state_icon_bg` fill | 72dp ring, 1.5dp `line_strong` stroke, transparent |
| Title size | 22sp | 20sp |
| Subtitle copy | static "Try adjusting your filters…" | dynamic "Nothing fits **Videos · Viber · Last 7 days**. Try widening the range." |
| Actions | just `Reset filters` pill | `Reset filters` pill + **`Edit filters`** text link under it |

## Prerequisites

- task-01 landed (viewmodel bulk setters exist; filter sheet is
  restyled).
- `./gradlew --stop` before you start.

## Step-by-Step Instructions

### Step 1 — Create the ring drawable

Create `app/src/main/res/drawable/empty_state_ring.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@android:color/transparent" />
    <stroke android:width="1.5dp" android:color="@color/line_strong" />
</shape>
```

> `android:shape="oval"` auto-clips to a circle at any size, so we
> don't need `<corners>`.

### Step 2 — Restyle the empty-state block in `activity_main.xml`

Find the `emptyStateContainer` (around line 357) and rebuild its
children. Replace the whole `<LinearLayout>` block with:

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

    <!-- Icon in outlined ring -->
    <FrameLayout
        android:layout_width="72dp"
        android:layout_height="72dp"
        android:background="@drawable/empty_state_ring">

        <ImageView
            android:id="@+id/emptyIcon"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_gravity="center"
            android:importantForAccessibility="no"
            android:src="@drawable/ic_search_off"
            app:tint="@color/ink3" />

    </FrameLayout>

    <TextView
        android:id="@+id/emptyTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:textSize="20sp"
        android:textStyle="bold"
        android:textColor="@color/ink" />

    <TextView
        android:id="@+id/emptySubtitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:maxWidth="260dp"
        android:textSize="15sp"
        android:textColor="@color/ink3"
        android:gravity="center"
        android:lineSpacingMultiplier="1.3" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnResetFilters"
        android:layout_width="wrap_content"
        android:layout_height="44dp"
        android:layout_marginTop="24dp"
        android:insetTop="0dp"
        android:insetBottom="0dp"
        android:text="@string/reset_filters"
        android:textColor="@android:color/white"
        android:paddingHorizontal="28dp"
        app:backgroundTint="@color/ink"
        app:cornerRadius="22dp"
        android:visibility="gone" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnEditFilters"
        style="@style/Widget.Material3.Button.TextButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:text="@string/edit_filters"
        android:textColor="@color/ink2"
        android:visibility="gone" />

</LinearLayout>
```

> **Icon tint**: we explicitly set `app:tint="@color/ink3"` so
> `ic_search_off` renders muted regardless of its fillColor attribute.
> Change came up in SDD-003 when we retargeted `ic_help_outline` —
> same pattern here.

### Step 3 — Add new strings

In `strings.xml`:

```xml
<!-- Empty state (SDD-20260418-008) -->
<string name="edit_filters">Edit filters</string>
<string name="no_matches_subtitle">Nothing fits %1$s. Try widening the range.</string>
<string name="filter_combo_separator">\u00A0·\u00A0</string>
<plurals name="filter_combo_sources_count">
    <item quantity="one">%1$d source</item>
    <item quantity="other">%1$d sources</item>
</plurals>
<string name="filter_combo_any_filter">your current filters</string>
```

> `\u00A0` is a non-breaking space so the `·` separator never wraps
> mid-combo onto its own line.

### Step 4 — Create `FilterComboFormatter`

New file `app/src/main/java/com/example/gallerycleaner/FilterComboFormatter.kt`:

```kotlin
package com.example.gallerycleaner

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns the four filter fields into a single human-readable string,
 * e.g. "Videos · Viber · Last 7 days". Segments are dropped when
 * the field is at its default/all-selected value.
 *
 * Returns a fallback string when nothing is non-default.
 */
object FilterComboFormatter {

    fun format(
        context: Context,
        mediaTypes: Set<MediaType>,
        sources: Set<SourceType>,
        allAvailableSources: Set<SourceType>,
        dateRange: DateRange,
        sortOption: SortOption,
    ): String {
        val parts = mutableListOf<String>()

        // Media type — only include if one of the two is off
        if (mediaTypes.size == 1) {
            parts += if (mediaTypes.first() == MediaType.PHOTO) {
                context.getString(R.string.photos)
            } else {
                context.getString(R.string.videos)
            }
        }

        // Sources — omit if "all" selected
        val nonDefaultSources = sources.intersect(allAvailableSources)
        if (nonDefaultSources.isNotEmpty() && nonDefaultSources.size != allAvailableSources.size) {
            parts += when (nonDefaultSources.size) {
                1 -> nonDefaultSources.first().label
                else -> context.resources.getQuantityString(
                    R.plurals.filter_combo_sources_count,
                    nonDefaultSources.size,
                    nonDefaultSources.size,
                )
            }
        }

        // Date — omit if preset is ALL_TIME (effectively "no filter")
        if (dateRange.preset != DateRangePreset.ALL_TIME) {
            parts += when (dateRange.preset) {
                DateRangePreset.CUSTOM -> formatCustomRange(dateRange)
                else -> dateRange.preset.label
            }
        }

        // Sort — only include if non-default (newest-first is the app default)
        if (sortOption != SortOption.DATE_DESC) {
            parts += sortOption.label.lowercase(Locale.getDefault())
        }

        return if (parts.isEmpty()) {
            context.getString(R.string.filter_combo_any_filter)
        } else {
            parts.joinToString(context.getString(R.string.filter_combo_separator))
        }
    }

    private fun formatCustomRange(range: DateRange): String {
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        val start = Date((range.startTimestamp ?: 0L) * 1000)
        val end = Date((range.endTimestamp ?: System.currentTimeMillis() / 1000) * 1000)
        return "${fmt.format(start)}–${fmt.format(end)}"
    }
}
```

### Step 5 — Unit tests for the formatter

Create `app/src/test/java/com/example/gallerycleaner/FilterComboFormatterTest.kt`:

```kotlin
package com.example.gallerycleaner

import android.content.Context
import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class FilterComboFormatterTest {

    private lateinit var context: Context
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        resources = mock(Resources::class.java)
        `when`(context.resources).thenReturn(resources)
        `when`(context.getString(R.string.photos)).thenReturn("Photos")
        `when`(context.getString(R.string.videos)).thenReturn("Videos")
        `when`(context.getString(R.string.filter_combo_separator)).thenReturn(" · ")
        `when`(context.getString(R.string.filter_combo_any_filter)).thenReturn("your current filters")
        `when`(resources.getQuantityString(anyInt(), anyInt(), anyInt())).thenAnswer {
            val n = it.arguments[1] as Int
            "$n sources"
        }
    }

    private val allSources = setOf(
        SourceType.WHATSAPP, SourceType.VIBER, SourceType.CAMERA
    )

    @Test
    fun `no non-default filters returns fallback`() {
        val s = FilterComboFormatter.format(
            context,
            mediaTypes = setOf(MediaType.PHOTO, MediaType.VIDEO),
            sources = allSources,
            allAvailableSources = allSources,
            dateRange = DateRange(DateRangePreset.ALL_TIME),
            sortOption = SortOption.DATE_DESC,
        )
        assertEquals("your current filters", s)
    }

    @Test
    fun `only videos`() {
        val s = FilterComboFormatter.format(
            context,
            mediaTypes = setOf(MediaType.VIDEO),
            sources = allSources,
            allAvailableSources = allSources,
            dateRange = DateRange(DateRangePreset.ALL_TIME),
            sortOption = SortOption.DATE_DESC,
        )
        assertEquals("Videos", s)
    }

    @Test
    fun `one source plus date preset`() {
        val s = FilterComboFormatter.format(
            context,
            mediaTypes = setOf(MediaType.PHOTO, MediaType.VIDEO),
            sources = setOf(SourceType.VIBER),
            allAvailableSources = allSources,
            dateRange = DateRange(DateRangePreset.LAST_7_DAYS),
            sortOption = SortOption.DATE_DESC,
        )
        assertEquals("Viber · 7 days", s)
    }

    @Test
    fun `multiple sources show count`() {
        val s = FilterComboFormatter.format(
            context,
            mediaTypes = setOf(MediaType.PHOTO, MediaType.VIDEO),
            sources = setOf(SourceType.VIBER, SourceType.WHATSAPP),
            allAvailableSources = allSources,
            dateRange = DateRange(DateRangePreset.ALL_TIME),
            sortOption = SortOption.DATE_DESC,
        )
        assertEquals("2 sources", s)
    }

    @Test
    fun `videos plus viber plus 7 days`() {
        val s = FilterComboFormatter.format(
            context,
            mediaTypes = setOf(MediaType.VIDEO),
            sources = setOf(SourceType.VIBER),
            allAvailableSources = allSources,
            dateRange = DateRange(DateRangePreset.LAST_7_DAYS),
            sortOption = SortOption.DATE_DESC,
        )
        assertEquals("Videos · Viber · 7 days", s)
    }
}
```

### Step 6 — Wire the empty state in `MainActivity`

Find `renderState`'s `GalleryUiState.NoMatchingItems` branch (around
line 612). Update the subtitle line and add a click handler for the
new `btnEditFilters`:

```kotlin
is GalleryUiState.NoMatchingItems -> {
    // ... existing visibility flips unchanged ...

    val combo = FilterComboFormatter.format(
        context = this,
        mediaTypes = state.selectedMediaTypes,
        sources = state.selectedSources,
        allAvailableSources = state.allSourceCounts.keys,
        dateRange = state.selectedDateRange,
        sortOption = state.selectedSortOption,
    )
    binding.emptyIcon.setImageResource(R.drawable.ic_search_off)
    binding.emptyTitle.text = getString(R.string.no_matching_items)
    binding.emptySubtitle.text = getString(R.string.no_matches_subtitle, combo)
    binding.emptySubtitle.visibility = View.VISIBLE
    binding.btnResetFilters.visibility = View.VISIBLE
    binding.btnEditFilters.visibility = View.VISIBLE

    adapter.submitList(emptyList())
    updateMediaTypeChips(state.selectedMediaTypes)
}
```

Also the `NoFiltersSelected` branch (around line 597) — hide the edit
button there (reset is enough):

```kotlin
binding.btnEditFilters.visibility = View.GONE
```

And the top-level `Empty` branch (around line 580):

```kotlin
binding.btnEditFilters.visibility = View.GONE
```

(Search for every place `binding.btnResetFilters.visibility = View.GONE`
and add a matching `binding.btnEditFilters.visibility = View.GONE` so
stale edits don't linger.)

Finally, add the click listener near `setupResetFiltersButton()`
(search for where `btnResetFilters.setOnClickListener` is set). Add:

```kotlin
binding.btnEditFilters.setOnClickListener {
    FilterBottomSheetFragment.newInstance()
        .show(supportFragmentManager, FilterBottomSheetFragment.TAG)
}
```

### Step 7 — Legacy color cleanup

`@color/empty_state_icon_bg`, `@color/empty_state_icon_color`,
`@color/empty_state_subtitle_color`, `@color/empty_state_reset_btn_bg`,
`@color/empty_state_reset_btn_text` are no longer referenced by this
layout. Before deleting from `colors.xml`:

```bash
grep -rn "empty_state_icon_bg\|empty_state_icon_color\|empty_state_subtitle_color\|empty_state_reset_btn_bg\|empty_state_reset_btn_text" app/src/
```

If nothing remains in source, remove all five from `colors.xml`.

### Step 8 — Build and verify

```bash
./gradlew --stop
./gradlew clean assembleDebug testDebugUnitTest lint
```

`FilterComboFormatterTest` should add 5 passing tests to the suite.

### Step 9 — Visual smoke test

1. Open the Filters sheet, apply a narrow filter (e.g. Videos only,
   Viber source, 7 days). Apply.
2. If no matches: empty state shows the **thin outlined ring**, title
   `No items found`, subtitle `Nothing fits Videos · Viber · 7 days.
   Try widening the range.`
3. Tap **Edit filters** → filter sheet reopens with the same
   non-default selections visible.
4. Tap **Reset filters** → filters clear to defaults, grid populates.

## Definition of Done

- [ ] All task-02 files landed
- [ ] `FilterComboFormatterTest` has 5 passing tests
- [ ] Five `empty_state_*` legacy colors removed from `colors.xml`
- [ ] Visual smoke test (Step 9) passes
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds

## Known gotchas

- **`app:tint` on the icon** is honored by `ImageView`; don't set
  `android:tint` (deprecated) or the ink3 won't stick on older devices.
- **Subtitle line wrapping**: `maxWidth=260dp` keeps it from becoming
  one long line on tablets. At very small screens (<360dp wide) it may
  wrap to 3 lines — acceptable.
- **Edit filters leading-uppercase**: the subtitle combo lowercases
  the sort label (`sortOption.label.lowercase()`) but keeps media types
  capitalized, since "Videos" and "Photos" are proper filter names and
  "newest first" reads as a modifier.
- **`getQuantityString` mock in tests**: `anyInt()` for the plural res
  id means the test doesn't care which plural resource is requested; it
  just echoes back `"N sources"` for any `n`. Fine for a unit test
  that's only verifying joined string structure.
