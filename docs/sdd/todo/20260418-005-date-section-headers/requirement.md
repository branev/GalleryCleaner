# SDD-20260418-005: Date Section Headers

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 ✓, SDD-20260418-004 ✓

## Summary

Introduce grouped date sections in the grid: **Today**, **Yesterday**,
**This week**, **Last week**, then per-month (**April 2026**, **March 2026**,
…) for older items. Each section header renders as a full-row span above
its tiles, showing the bucket name and the count of items in that bucket.

This is **new behavior**, not a reskin — it changes the adapter to a
mixed-item-type list (headers + media) and requires position-aware callers
to skip over headers.

## Why it's separate

This is the biggest structural change in the redesign: the adapter's item
type becomes a sealed class, every position-based operation (drag-select,
fast-scroll, continue FAB, long-press drag start) needs header-awareness,
and a new layout + utility module land. Isolating this in one SDD keeps
review scope clear.

## Scope

1. **New `GridItem` sealed class** with two variants: `Header` (bucket name
   + count) and `Media` (wraps `MediaItem`).
2. **Date bucket utility** — pure function that takes `List<MediaItem>`
   and returns `List<GridItem>` with headers inserted.
   - Buckets: `Today`, `Yesterday`, `This week`, `Last week`, then
     `MMMM yyyy` for older months.
   - Respects current sort order: headers are re-inserted after sorting.
3. **ViewModel surface**: expose `gridItems: StateFlow<List<GridItem>>`
   derived from `displayedItems`. Existing `displayedItems` stays (used
   by selection logic, counters, etc.).
4. **Rewrite `ImageAdapter`** as `ListAdapter<GridItem, RecyclerView.ViewHolder>`:
   - Two `ViewHolder` subclasses (`HeaderVH`, `ImageVH`)
   - `getItemViewType(position)` routes on the sealed variant
   - New `DiffUtil.ItemCallback<GridItem>`
   - Selection + viewed payloads only apply to `ImageVH`
5. **New layout `item_date_header.xml`**: 13sp 600 `ink` title left,
   11sp 500 `ink4` count right, 20dp top / 10dp bottom padding.
6. **`GridLayoutManager.SpanSizeLookup`** in `MainActivity`: header rows
   span full grid width; media rows span 1.
7. **Position-aware callers** updated to skip headers:
   - Drag-select `onDragRangeChanged`: filter non-Media positions
   - Long-press drag start: look up position of `GridItem.Media`
   - Fast-scroll `onFastScrollPositionChanged`: collect uris only from
     `GridItem.Media` items
   - `formatDateForPosition`: return empty string for header rows
   - `scrollToFirstUnviewed` + `updateContinueFabState`: find first
     `GridItem.Media` whose uri is not in `viewedItems`

## Not in scope

- Sticky/pinned headers (they scroll with content; deferred)
- Collapsible sections (future)
- Per-bucket "Mark all viewed" actions (future)
- Other position-based features beyond those listed in §7

## Date bucket rules

Given `now` = current date at midnight, an item with date `d`:

| Bucket | Condition |
|---|---|
| `Today` | `d` is same calendar day as `now` |
| `Yesterday` | `d` is exactly 1 calendar day before `now` |
| `This week` | `d` within last 7 days but not Today/Yesterday |
| `Last week` | `d` 8–14 days before `now` |
| `MMMM yyyy` | all older items, grouped by their calendar month |

Buckets are built **in sort order** of the input: if items are sorted
newest-first, Today appears first; oldest-first reverses the list.

> Ambiguous cases: if "today" is Monday and an item is from Sunday,
> Sunday lands in "Yesterday" (not "This week"). If today is Monday and
> an item is Friday of the previous week, it lands in "Last week".

## Files changed

| File | Change |
|------|--------|
| `GridItem.kt` | **New** — sealed class with `Header` + `Media` |
| `DateBucket.kt` | **New** — pure bucket utility |
| `res/layout/item_date_header.xml` | **New** — header row layout |
| `GalleryViewModel.kt` | Add `gridItems: StateFlow<List<GridItem>>` derivation |
| `ImageAdapter.kt` | Major — mixed-type ListAdapter, two ViewHolders, new DiffUtil |
| `MainActivity.kt` | SpanSizeLookup setup; drag-select, long-press, fast-scroll, continue-FAB callers adapted for headers |
| `app/src/test/…/DateBucketTest.kt` | **New** — unit tests for bucket logic |

## Acceptance criteria

- [ ] Grid shows section headers above date groups (Today, Yesterday, This
      week, Last week, April 2026, …)
- [ ] Headers span the full grid width regardless of column count
- [ ] Header text: bucket name on the left, `N items` count on the right
- [ ] Header styling matches spec: 13sp 600 ink title, 11sp 500 ink4 count,
      20dp top / 10dp bottom padding
- [ ] Long-press on any tile still enters selection mode correctly
- [ ] Drag-select across tiles works; dragging across a header skips it
      (no crash, no weird "selected" header)
- [ ] Fast-scrolling tooltip shows the current tile's date (not "Today"
      for a header)
- [ ] "Continue" FAB scrolls to the first unreviewed tile (not to a header)
- [ ] Pinch-zoom columns: 2, 3, 4, 5 all work, headers always span
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [ ] `DateBucketTest` covers: today/yesterday edge cases, week/month
      boundary, empty list, sort-order respect
- [ ] Manual smoke: scroll a large library, add/remove filters, select
      items across multiple buckets, delete items → headers update counts
      live
