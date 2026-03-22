# SDD-20260321-002: Drag-to-Select

## Status: COMPLETE

## Summary

Add drag-to-select functionality so users can select multiple grid items in one gesture. After long-pressing to enter selection mode, dragging the finger across items selects/deselects them continuously — essential for a bulk cleanup app.

---

## Changes Required

### 1. Add DragSelectRecyclerView Library

The `DragSelectRecyclerView` library by afollestad provides a well-tested, drop-in implementation of the drag-to-select pattern used by Google Photos and Samsung Gallery.

**Dependency:**
```kotlin
implementation("com.github.nickhall:drag-select-recyclerview:0.4.0")
```

> **Note:** Evaluate the latest stable version. If the library is unmaintained or incompatible, implement manually using `RecyclerView.OnItemTouchListener` to track finger position across grid cells during a drag gesture.

**Alternative (manual implementation):**
If no suitable library exists, implement via:
- Attach an `OnItemTouchListener` to the RecyclerView
- On `ACTION_MOVE` during selection mode, find the child view under the touch point via `findChildViewUnder(x, y)`
- Get the adapter position and toggle selection for all items between the drag start position and current position
- Use `ViewHolder.bindingAdapterPosition` to map touch to items

### 2. Integrate with Existing Selection Logic

#### Activation:
- Long-press enters selection mode AND starts drag-select (existing behavior + drag tracking)
- User can lift finger, then start a new drag from any selected/unselected item to continue selecting

#### Selection behavior during drag:
- Range-based: the full range from drag start to current position is selected
- Dragging back **shrinks** the range and deselects items that fall outside it
- Items selected before the drag (via previous taps) are preserved via a `preDragSelection` snapshot
- This matches the behavior of Google Photos and Samsung Gallery

#### Edge scrolling:
- When dragging near the top/bottom edge of the RecyclerView, auto-scroll to reveal more items
- Scroll speed increases as finger gets closer to the edge
- This is critical for selecting many items across multiple screen-heights

### 3. Visual Feedback During Drag

- Items should show their selection state change immediately as the finger passes over them
- Use the existing `PAYLOAD_SELECTION_STATE` partial bind for efficient updates
- Optional: subtle haptic feedback (short vibration) when each new item is added to selection

### 4. Remove Top Selection Toolbar

The top selection toolbar ("X selected" bar with close icon) causes a layout shift when it appears/disappears, pushing the grid down. Remove it and consolidate all selection info into the bottom action bar.

- Bottom bar center text shows: count + size (e.g., "3 selected · 1.2 MB")
- Exit selection mode via back button (already wired via `OnBackPressedCallback`)
- Hidden selected count still shown (e.g., "3 selected (1 hidden) · 1.2 MB")

### 5. Maintain Existing Selection Features

- Tap-to-toggle in selection mode must still work
- Select All button still works
- Back to exit selection mode still works
- Hidden selected count tracking still works

---

## Files Summary

| File | Change Type |
|------|------------|
| `DragSelectTouchListener.kt` | **New file** — custom touch listener with range selection + auto-scroll |
| `MainActivity.kt` | Moderate — wire drag listener, remove top toolbar, show count+size in bottom bar |
| `GalleryViewModel.kt` | Minor — add `setDragSelection()` method |
| `activity_main.xml` | Minor — add `overScrollMode="never"` to RecyclerView |

## Acceptance Criteria

- [ ] Long-press + drag selects multiple items in one gesture
- [ ] Auto-scrolls when dragging near top/bottom edges
- [ ] Tap-to-toggle still works in selection mode
- [ ] All existing selection features (Select All, exit, hidden count) still work
- [ ] Build succeeds, all tests pass

## Out of Scope

- Two-finger drag gestures
- Lasso/freeform selection
- Select-by-date-header (requires date headers feature)
