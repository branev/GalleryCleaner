# Task 01: Date Section Headers

**Parent:** SDD-20260418-005 — Date Section Headers

## What You're Changing

The grid today shows one flat list of media. After this task, media is
grouped into date buckets (Today, Yesterday, This week, Last week, then
per-month) with a header row above each group.

This is the largest structural change in the redesign. Go slow — each
step builds on the previous one, and if you skip ahead you'll chase
compile errors.

## Before vs After

**Before:** `ImageAdapter` is `ListAdapter<MediaItem, VH>` with one view
type. Positions map 1:1 to `MediaItem`s.

**After:** `ImageAdapter` is `ListAdapter<GridItem, RecyclerView.ViewHolder>`
where `GridItem` is either a `Header` or a `Media`. Positions are mixed;
every place that does `adapter.currentList[i].uri` or `items[i].uri` must
now check the item's type first.

## Prerequisites

- SDD-004 (Grid Tile) merged
- `./gradlew --stop` before you start

## Step-by-Step Instructions

### Step 1 — Add the `GridItem` sealed class

Create `app/src/main/java/com/example/gallerycleaner/GridItem.kt`:

```kotlin
package com.example.gallerycleaner

/**
 * A row in the media grid. Either a date-section header or a media item.
 * The adapter accepts one flat list of these and picks a view type per row.
 */
sealed class GridItem {
    data class Header(val bucketId: String, val title: String, val count: Int) : GridItem() {
        val stableId: Long get() = (bucketId.hashCode().toLong() shl 32) or 0xFFFFFFFFL
    }
    data class Media(val item: MediaItem) : GridItem() {
        val stableId: Long get() = item.uri.hashCode().toLong() and 0x7FFFFFFFL
    }
}
```

> `bucketId` is a stable identifier like `"today"`, `"month-2026-03"` used
> by DiffUtil. `title` is the human-readable string shown in the UI.

### Step 2 — Add the `DateBucket` utility

Create `app/src/main/java/com/example/gallerycleaner/DateBucket.kt`:

```kotlin
package com.example.gallerycleaner

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Groups a flat list of media items into grid rows with date-section headers.
 *
 * Buckets (in sort order of the input):
 *   - "Today"        : same calendar day as `now`
 *   - "Yesterday"    : exactly 1 day before `now`
 *   - "This week"    : 2-7 days before `now`
 *   - "Last week"    : 8-14 days before `now`
 *   - "MMMM yyyy"    : older, one bucket per calendar month (e.g. "March 2026")
 *
 * The function is pure (no Android imports) so it can be unit-tested.
 */
object DateBucket {

    fun bucketize(
        items: List<MediaItem>,
        now: Date = Date()
    ): List<GridItem> {
        if (items.isEmpty()) return emptyList()

        val result = mutableListOf<GridItem>()
        val grouped = linkedMapOf<BucketKey, MutableList<MediaItem>>()

        for (item in items) {
            val key = keyFor(Date(item.dateAdded * 1000L), now)
            grouped.getOrPut(key) { mutableListOf() }.add(item)
        }

        for ((key, bucketItems) in grouped) {
            result += GridItem.Header(
                bucketId = key.id,
                title = key.title,
                count = bucketItems.size
            )
            for (it in bucketItems) result += GridItem.Media(it)
        }
        return result
    }

    private data class BucketKey(val id: String, val title: String)

    private fun keyFor(itemDate: Date, now: Date): BucketKey {
        val nowCal = Calendar.getInstance().apply { time = now; floorToDay() }
        val itemCal = Calendar.getInstance().apply { time = itemDate; floorToDay() }
        val diffDays = ((nowCal.timeInMillis - itemCal.timeInMillis) / 86_400_000L).toInt()

        return when {
            diffDays == 0 -> BucketKey("today", "Today")
            diffDays == 1 -> BucketKey("yesterday", "Yesterday")
            diffDays in 2..7 -> BucketKey("this-week", "This week")
            diffDays in 8..14 -> BucketKey("last-week", "Last week")
            else -> {
                val y = itemCal.get(Calendar.YEAR)
                val m = itemCal.get(Calendar.MONTH)
                val title = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(itemDate)
                BucketKey("month-$y-${"%02d".format(m + 1)}", title)
            }
        }
    }

    private fun Calendar.floorToDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
```

> **Why pure Kotlin?** Testable without Android framework stubs. The only
> Android dependency in `MediaItem` is `Uri`, which is typed but not
> touched here — we only read `dateAdded` (a Long).

### Step 3 — Unit tests for bucketing

Create `app/src/test/java/com/example/gallerycleaner/DateBucketTest.kt`:

```kotlin
package com.example.gallerycleaner

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date

class DateBucketTest {

    private fun now(year: Int, month: Int, day: Int): Date =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }.time

    private fun item(uri: String, year: Int, month: Int, day: Int): MediaItem {
        val epoch = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 10, 0, 0)
        }.timeInMillis / 1000L
        return MediaItem(
            uri = Uri.parse("content://media/$uri"),
            displayName = uri,
            dateAdded = epoch,
            mediaType = MediaType.IMAGE,
            source = SourceType.CAMERA,
            bucket = null,
            size = 0L,
            duration = 0L,
        )
    }

    @Test
    fun `empty list returns empty grid`() {
        assertEquals(emptyList<GridItem>(), DateBucket.bucketize(emptyList(), now(2026, 4, 18)))
    }

    @Test
    fun `today and yesterday buckets`() {
        val now = now(2026, 4, 18)
        val items = listOf(
            item("a", 2026, 4, 18),   // Today
            item("b", 2026, 4, 17),   // Yesterday
        )
        val result = DateBucket.bucketize(items, now)
        assertEquals(4, result.size)
        assertEquals("Today", (result[0] as GridItem.Header).title)
        assertEquals("Yesterday", (result[2] as GridItem.Header).title)
    }

    @Test
    fun `this week and last week boundaries`() {
        val now = now(2026, 4, 18)
        val items = listOf(
            item("a", 2026, 4, 16),  // 2 days ago → This week
            item("b", 2026, 4, 11),  // 7 days ago → This week
            item("c", 2026, 4, 10),  // 8 days ago → Last week
            item("d", 2026, 4, 4),   // 14 days ago → Last week
        )
        val result = DateBucket.bucketize(items, now)
        val headers = result.filterIsInstance<GridItem.Header>().map { it.title }
        assertEquals(listOf("This week", "Last week"), headers)
    }

    @Test
    fun `older items group by month`() {
        val now = now(2026, 4, 18)
        val items = listOf(
            item("a", 2026, 3, 20),
            item("b", 2026, 3, 1),
            item("c", 2025, 12, 5),
        )
        val result = DateBucket.bucketize(items, now)
        val headers = result.filterIsInstance<GridItem.Header>().map { it.title }
        assertEquals(listOf("March 2026", "December 2025"), headers)
    }

    @Test
    fun `header count matches item count in bucket`() {
        val now = now(2026, 4, 18)
        val items = listOf(
            item("a", 2026, 4, 18),
            item("b", 2026, 4, 18),
            item("c", 2026, 4, 18),
        )
        val result = DateBucket.bucketize(items, now)
        assertEquals(3, (result[0] as GridItem.Header).count)
    }
}
```

### Step 4 — Expose `gridItems` from the ViewModel

Open `GalleryViewModel.kt`. Near the other derived StateFlow properties
(search for `val hasActiveFilters: StateFlow<Boolean>`), add:

```kotlin
val gridItems: StateFlow<List<GridItem>> = uiState
    .map { state -> DateBucket.bucketize(state.displayedItems) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
```

Add `import kotlinx.coroutines.flow.map` at the top if not already
imported.

> **Why eagerly shared?** Observers in the Activity subscribe late; we
> want the most recent list cached so first subscription doesn't trigger
> a rebucket.

### Step 5 — Create the header layout

Create `app/src/main/res/layout/item_date_header.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingTop="20dp"
    android:paddingBottom="10dp"
    android:paddingHorizontal="2dp">

    <TextView
        android:id="@+id/headerTitle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textSize="13sp"
        android:textFontWeight="600"
        android:textColor="@color/ink"
        android:letterSpacing="-0.01"
        tools:text="Today" />

    <TextView
        android:id="@+id/headerCount"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="11sp"
        android:textFontWeight="500"
        android:textColor="@color/ink4"
        android:letterSpacing="0.02"
        tools:text="12 items" />

</LinearLayout>
```

### Step 6 — Rewrite `ImageAdapter` as mixed-type

This is the biggest edit. Replace the whole file
`app/src/main/java/com/example/gallerycleaner/ImageAdapter.kt` with:

```kotlin
package com.example.gallerycleaner

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.decode.VideoFrameDecoder
import coil.load
import coil.request.videoFrameMillis
import com.example.gallerycleaner.databinding.ItemDateHeaderBinding
import com.example.gallerycleaner.databinding.ItemImageBinding

class ImageAdapter(
    private val onItemClick: (MediaItem) -> Unit,
    private val onItemLongClick: (MediaItem) -> Unit
) : ListAdapter<GridItem, RecyclerView.ViewHolder>(Diff) {

    private var selectedItems: Set<Uri> = emptySet()
    private var isSelectionMode: Boolean = false
    private var viewedItems: Set<Uri> = emptySet()

    var thumbnailSize: Int = 300

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_MEDIA = 1

        private const val PAYLOAD_SELECTION_MODE = "selection_mode"
        private const val PAYLOAD_SELECTION_STATE = "selection_state"
        private const val PAYLOAD_VIEWED_STATE = "viewed_state"

        fun formatDuration(durationMs: Long): String {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
    }

    object Diff : DiffUtil.ItemCallback<GridItem>() {
        override fun areItemsTheSame(oldItem: GridItem, newItem: GridItem): Boolean = when {
            oldItem is GridItem.Header && newItem is GridItem.Header ->
                oldItem.bucketId == newItem.bucketId
            oldItem is GridItem.Media && newItem is GridItem.Media ->
                oldItem.item.uri == newItem.item.uri
            else -> false
        }

        override fun areContentsTheSame(oldItem: GridItem, newItem: GridItem): Boolean =
            oldItem == newItem
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is GridItem.Header -> VIEW_TYPE_HEADER
        is GridItem.Media -> VIEW_TYPE_MEDIA
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderVH(ItemDateHeaderBinding.inflate(inflater, parent, false))
            else -> ImageVH(ItemImageBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is GridItem.Header -> (holder as HeaderVH).bind(item)
            is GridItem.Media -> (holder as ImageVH).bind(item.item)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty() || holder !is ImageVH) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        val mediaItem = (getItem(position) as? GridItem.Media)?.item ?: return
        for (payload in payloads) {
            when (payload) {
                PAYLOAD_SELECTION_MODE, PAYLOAD_SELECTION_STATE -> {
                    holder.updateSelectionVisuals(mediaItem)
                    holder.updateViewedVisuals(mediaItem)
                }
                PAYLOAD_VIEWED_STATE -> holder.updateViewedVisuals(mediaItem)
            }
        }
    }

    /** Utility: unpack media items from the current list (headers excluded). */
    fun mediaItemsInOrder(): List<MediaItem> =
        currentList.filterIsInstance<GridItem.Media>().map { it.item }

    fun updateSelectionState(selectedItems: Set<Uri>, isSelectionMode: Boolean) {
        val oldSelected = this.selectedItems
        val oldMode = this.isSelectionMode

        this.selectedItems = selectedItems
        this.isSelectionMode = isSelectionMode

        if (oldMode != isSelectionMode) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION_MODE)
        } else if (oldSelected != selectedItems) {
            val changedUris = (oldSelected - selectedItems) + (selectedItems - oldSelected)
            for (i in 0 until itemCount) {
                val gi = getItem(i)
                if (gi is GridItem.Media && gi.item.uri in changedUris) {
                    notifyItemChanged(i, PAYLOAD_SELECTION_STATE)
                }
            }
        }
    }

    fun updateViewedItems(viewedItems: Set<Uri>) {
        val oldViewed = this.viewedItems
        this.viewedItems = viewedItems
        val newlyViewed = viewedItems - oldViewed
        for (i in 0 until itemCount) {
            val gi = getItem(i)
            if (gi is GridItem.Media && gi.item.uri in newlyViewed) {
                notifyItemChanged(i, PAYLOAD_VIEWED_STATE)
            }
        }
    }

    inner class HeaderVH(private val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: GridItem.Header) {
            binding.headerTitle.text = header.title
            binding.headerCount.text = binding.root.resources
                .getQuantityString(R.plurals.header_item_count, header.count, header.count)
        }
    }

    inner class ImageVH(private val binding: ItemImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    (getItem(position) as? GridItem.Media)?.let { onItemClick(it.item) }
                }
            }
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    (getItem(position) as? GridItem.Media)?.let {
                        onItemLongClick(it.item)
                        return@setOnLongClickListener true
                    }
                }
                false
            }
        }

        fun bind(item: MediaItem) {
            binding.imageView.setImageDrawable(null)
            binding.imageView.load(item.uri) {
                crossfade(true)
                size(thumbnailSize)
                if (item.mediaType == MediaType.VIDEO) {
                    decoderFactory { result, options, _ ->
                        VideoFrameDecoder(result.source, options)
                    }
                    videoFrameMillis(1000)
                }
            }

            val label = when (item.source) {
                SourceType.OTHER -> item.bucket ?: SourceType.OTHER.label
                else -> item.source.label
            }
            binding.sourceBadge.text = label

            if (item.mediaType == MediaType.VIDEO) {
                binding.videoDurationBadge.visibility = View.VISIBLE
                binding.videoDuration.text = formatDuration(item.duration)
            } else {
                binding.videoDurationBadge.visibility = View.GONE
            }

            updateSelectionVisuals(item)
            updateViewedVisuals(item)
        }

        fun updateViewedVisuals(item: MediaItem) {
            val isViewed = item.uri in viewedItems
            if (isViewed && !isSelectionMode) {
                binding.imageView.alpha = 0.85f
                val matrix = ColorMatrix().apply { setSaturation(0.75f) }
                binding.imageView.colorFilter = ColorMatrixColorFilter(matrix)
            } else {
                binding.imageView.alpha = 1.0f
                binding.imageView.colorFilter = null
            }
        }

        fun updateSelectionVisuals(item: MediaItem) {
            val isSelected = item.uri in selectedItems
            binding.selectionRing.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.selectionTint.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.dimOverlay.visibility =
                if (isSelectionMode && !isSelected) View.VISIBLE else View.GONE
            binding.sourceBadge.visibility =
                if (isSelectionMode && !isSelected) View.GONE else View.VISIBLE
        }
    }
}
```

Add the plural string resource in
`app/src/main/res/values/strings.xml` (find the Top bar block or near the
end of the resources):

```xml
<plurals name="header_item_count">
    <item quantity="one">%1$d item</item>
    <item quantity="other">%1$d items</item>
</plurals>
```

### Step 7 — Hook up `SpanSizeLookup` in `MainActivity`

Find where the RecyclerView's layout manager is obtained. The manager
comes from XML (`app:layoutManager="androidx.recyclerview.widget.GridLayoutManager"
app:spanCount="@integer/grid_span_count"`). We need to override its
`spanSizeLookup` once — after the adapter is attached.

In `MainActivity.kt`, locate the block where `binding.recyclerView.adapter = adapter`
happens (around line 231). Immediately after it, add:

```kotlin
(binding.recyclerView.layoutManager as? GridLayoutManager)?.let { lm ->
    lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return when (adapter.getItemViewType(position)) {
                ImageAdapter.VIEW_TYPE_HEADER -> lm.spanCount
                else -> 1
            }
        }
    }
}
```

> The lookup has to live after `adapter` is set, because `getItemViewType`
> on an empty adapter would crash. `SpanSizeLookup` is safe to install
> before items arrive — it only queries on bind.

### Step 8 — Subscribe the adapter to `gridItems`

Still in `MainActivity.kt`, find `observeUiState()` (the Flow that calls
`renderState`). Near it, add a new observer:

```kotlin
private fun observeGridItems() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.gridItems.collect { grid ->
                adapter.submitList(grid)
            }
        }
    }
}
```

And call `observeGridItems()` from `onCreate`, alongside the other
`observe...` calls (search for `observeViewedItems()`).

**Important**: `renderState()` currently does `adapter.submitList(items)`
for the `Normal`/`Selection` states. Remove those two `submitList` calls
— the new `observeGridItems()` replaces them. Leave everything else in
`renderState()` alone.

### Step 9 — Adapt position-based callers to skip headers

**9a. Drag-select `onDragRangeChanged`** (around line 60):

```kotlin
private val dragSelectListener = DragSelectTouchListener(
    onDragRangeChanged = { rangeStart, rangeEnd ->
        val list = adapter.currentList
        val dragUris = (rangeStart..rangeEnd)
            .filter { it in list.indices }
            .mapNotNull { (list[it] as? GridItem.Media)?.item?.uri }
            .toSet()
        viewModel.setDragSelection(dragUris, preDragSelection)
    },
    ...
)
```

**9b. Long-press drag start** (around line 701):

```kotlin
val position = adapter.currentList.indexOfFirst {
    it is GridItem.Media && it.item.uri == item.uri
}
if (position >= 0) {
    dragSelectListener.startDragSelection(position)
}
```

**9c. Fast-scroll mark-as-viewed** (around line 238):

```kotlin
onFastScrollPositionChanged = { position ->
    val list = adapter.currentList
    if (position > 0 && list.isNotEmpty()) {
        val urisToMark = (0 until position.coerceAtMost(list.size))
            .mapNotNull { (list[it] as? GridItem.Media)?.item?.uri }
        viewModel.markItemsAsViewed(urisToMark)
    }
}
```

**9d. `formatDateForPosition`** (around line 892 — but now headers
already handle the label). Simplest change: return an empty string when
the tooltip lands on a header (the helper is only called when dragging
the thumb; hitting a header momentarily with an empty tooltip is fine):

```kotlin
private fun formatDateForPosition(position: Int): String {
    val items = adapter.currentList
    if (position !in items.indices) return ""
    val mediaItem = (items[position] as? GridItem.Media)?.item ?: return ""
    // ... existing body unchanged, but use `mediaItem.dateAdded` instead of `items[position].dateAdded`
}
```

Inside the existing body, replace `items[position].dateAdded` with
`mediaItem.dateAdded`.

**9e. `scrollToFirstUnviewed`** (around line 402) and
`updateContinueFabState` (around line 411) both call
`viewModel.getFirstUnviewedIndex(items)`.

Since headers shouldn't be "unreviewed items", change the call sites to
pass the flat list of media:

```kotlin
private fun scrollToFirstUnviewed() {
    val mediaItems = adapter.mediaItemsInOrder()
    val firstUnviewedMediaIndex = viewModel.getFirstUnviewedIndex(mediaItems)
    if (firstUnviewedMediaIndex < 0) return

    // Convert media-index to grid-position
    val targetUri = mediaItems[firstUnviewedMediaIndex].uri
    val gridPosition = adapter.currentList.indexOfFirst {
        it is GridItem.Media && it.item.uri == targetUri
    }
    if (gridPosition >= 0) {
        binding.recyclerView.smoothScrollToPosition(gridPosition)
    }
}

private fun updateContinueFabState(layoutManager: GridLayoutManager) {
    if (binding.fabContinue.visibility != View.VISIBLE) return
    val mediaItems = adapter.mediaItemsInOrder()
    if (mediaItems.isEmpty()) return

    val firstUnviewedMediaIndex = viewModel.getFirstUnviewedIndex(mediaItems)
    if (firstUnviewedMediaIndex < 0) {
        binding.fabContinue.isEnabled = false
        binding.fabContinue.alpha = 0.5f
        return
    }
    val targetUri = mediaItems[firstUnviewedMediaIndex].uri
    val gridPosition = adapter.currentList.indexOfFirst {
        it is GridItem.Media && it.item.uri == targetUri
    }
    val lastVisible = layoutManager.findLastVisibleItemPosition()

    val canScrollToUnviewed = gridPosition > lastVisible
    binding.fabContinue.isEnabled = canScrollToUnviewed
    binding.fabContinue.alpha = if (canScrollToUnviewed) 1.0f else 0.5f
}
```

### Step 10 — Build and verify

```bash
./gradlew --stop
./gradlew clean assembleDebug testDebugUnitTest lint
```

**Unit tests** (`DateBucketTest`) should all pass as part of this run.

Likely errors and fixes:
- `Unresolved reference: GridItem` — Step 1 missed.
- `Type mismatch … List<MediaItem> expected, List<GridItem>` — you
  forgot to switch `submitList` from `items` to the new `gridItems`
  StateFlow in Step 8, or a caller still assumes `currentList` is
  `List<MediaItem>`.
- `ClassCastException: HeaderVH cannot be cast to ImageVH` (runtime) — a
  payload-based bind path forgot to guard with `if (holder !is ImageVH)
  return`. Revisit Step 6 `onBindViewHolder(payloads)`.

### Step 11 — Visual smoke test on-device

Install. On the grid:

- **Section headers appear**: "Today", "Yesterday", "This week",
  "Last week", "April 2026", etc. — ordered newest-first.
- Each header shows `N items` on the right in a lighter color.
- Headers span the **full width** of the grid (not just one column).
- **Pinch** to change columns — headers still span full width at 2, 3, 4,
  and 5 columns.
- **Scroll fast**: the fast-scroll date tooltip moves through dates; if
  you happen to land on a header position the tooltip stays blank
  momentarily but never crashes.
- **Long-press any tile** → selection mode. Drag across several tiles
  (and across a header). The header is never "selected"; only tiles get
  the accent ring.
- **Tap Continue FAB**: scrolls to the next unreviewed tile, not to a
  header.
- **Apply a filter** (e.g. only WhatsApp) → grid re-groups into fewer
  buckets, counts update.

## Definition of Done

- [ ] All changes from the Files Changed table in `requirement.md` landed
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [ ] `DateBucketTest` has at least 5 passing tests (see Step 3)
- [ ] Visual smoke test (Step 11) passes end-to-end
- [ ] PR opened with title
      `SDD-20260418-005 — Date Section Headers`

## Known gotchas

- **Stable IDs** on `ListAdapter` aren't required here — we don't call
  `setHasStableIds(true)`, and we don't need to. The `stableId` in
  `GridItem` is provided for future use (e.g., if we ever want shared
  element transitions).
- **Header counts update cost**: every time `displayedItems` changes,
  `bucketize` runs over the whole list. This is O(n) and runs off the
  main thread (StateFlow `map` executes in the collector's context,
  which for us is the main thread — but n is small, typically <10k
  items, so it stays fast). If you see jank on giant libraries, move
  `bucketize` to `Dispatchers.Default` inside the flow.
- **Sort order inversion**: if the user toggles sort to oldest-first,
  "Today" will appear last, not first. The bucket function respects the
  input order — it does NOT re-sort. This is intentional; the sort
  option is the user's choice.
- **"This week" is rolling 7 days**, not "this calendar week (Mon-Sun)".
  This matches user intuition ("last 7 days") without needing locale
  first-day-of-week handling.
- **plurals use `one`/`other`** — English works fine, other locales
  handle plural rules via Android's resource system. Don't hard-code
  `"${count} items"`.
