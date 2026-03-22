# SDD-20260321-001: Delete Flow Improvements

## Status: COMPLETE

## Summary

Streamline and enhance the deletion UX: remove the redundant custom confirmation dialog, show a celebratory overlay after successful deletion with item count and space saved, show live file size in the selection bar, and extend the undo snackbar duration.

## Design Reference

After successful deletion, a full-screen dimmed overlay appears on top of the grid showing:
- A teal checkmark on one of the (now-deleted) grid items
- Bold "Done! 128 items deleted" text centered on screen
- The grid remains visible but dimmed (50% opacity) underneath
- A bottom snackbar reads "1.0 MB of space saved" with an "Undo" pill button
- The overlay dismisses after the snackbar timeout or on tap

---

## Changes Required

### 1. Remove Custom Confirmation Dialog

**Current:** Tapping Delete shows a `MaterialAlertDialog` asking "Move N items to trash?" with Cancel/Move to Trash buttons. Android 11+ then shows its own system trash dialog — double confirmation.

**New:** Skip the custom dialog entirely. Tapping Delete goes straight to the system `createTrashRequest()` which shows its own confirmation. For pre-Android 11 (direct delete, no system dialog, no undo), keep a simple confirmation dialog since the delete is irreversible.

#### Changes:
- Remove `showTrashConfirmationDialog()` method
- In `handleDelete()`, call `performTrash()` directly for Android 11+
- For pre-Android 11 only, keep a simple confirmation dialog

**Files affected:**
- `MainActivity.kt` — simplify `handleDelete()`, conditionalize dialog

### 2. Live File Size in Selection Action Bar

**Current:** The bottom selection bar shows `[Select All]` on the left and `[Delete]` on the right, with an empty spacer between them.

**New:** Replace the spacer with a live file size display that updates as items are selected/deselected.

```
[  ✓ Select All  ]     1.2 GB     [ 🗑 Delete  ]
```

#### Specification:
- **Text**: formatted file size (e.g., "1.2 GB", "45.3 MB", "120 KB")
- **Style**: 14sp, semi-bold, `colorOnSurface`
- **Position**: centered between Select All and Delete buttons (replace the empty spacer `View`)
- **Updates**: recalculated every time selection changes via `getSelectedItemsTotalSize()`

**Files affected:**
- `activity_main.xml` — replace spacer View with a TextView in the selection bar
- `MainActivity.kt` — set size text in the Selection state rendering block

### 3. Deletion Success Overlay

**Current:** After successful trash, a small `Snackbar.LENGTH_LONG` snackbar shows "X items (size) moved to trash" with an Undo button. Easy to miss.

**New:** A full-screen celebration overlay that emphasizes the achievement.

#### Overlay Specification:
- **Background**: semi-transparent dark overlay (`#B3000000`, ~70% black) covering the entire screen including the grid
- **Checkmark**: large teal checkmark icon (72dp) centered vertically, roughly in the upper portion of the screen
- **Title**: "Done! X items deleted" — 24sp bold, white, centered below the checkmark
- **Animation**: overlay fades in (300ms), checkmark scales up from 0 to 1 (400ms with overshoot)
- **Dismiss**: overlay fades out when snackbar dismisses, or when user taps the overlay

#### Snackbar (shown simultaneously):
- **Text**: "X.X MB of space saved" (or GB/KB as appropriate)
- **Action**: "Undo" button (teal text, pill-shaped)
- **Duration**: 8 seconds (custom, not LENGTH_LONG)
- **Position**: bottom of screen, above navigation bar
- **On dismiss**: overlay fades out, deleted items removed from grid

#### Flow:
1. System trash succeeds → overlay fades in + snackbar appears
2. User waits 8 seconds → snackbar auto-dismisses → overlay fades out → items removed
3. OR user taps "Undo" → overlay fades out immediately → items restored
4. OR user taps overlay → snackbar dismisses → overlay fades out → items removed

**Files affected:**
- `activity_main.xml` — add overlay FrameLayout with checkmark + title (visibility gone)
- `MainActivity.kt` — show/hide overlay, coordinate with snackbar lifecycle
- `colors.xml` — overlay background color
- `strings.xml` — new strings for overlay text
- New drawable: `ic_check_large.xml` — large teal checkmark icon

### 4. Longer Undo Snackbar

Covered by Change 3 above — the snackbar duration is 8 seconds as part of the new overlay flow.

---

## Files Summary

| File | Change Type |
|------|------------|
| `activity_main.xml` | Moderate — size TextView in selection bar, overlay layout |
| `MainActivity.kt` | Major — remove dialog, size display, overlay logic, snackbar coordination |
| `colors.xml` | Minor — overlay color |
| `strings.xml` | Minor — overlay and size strings |
| New drawable `ic_check_large.xml` | New — large teal checkmark |

## Out of Scope

- Final Deletion Review screen with thumbnail grid
- Swipe-to-delete on grid items
- Shake-to-undo
- Changes to the system trash dialog appearance (OS-controlled)
- Dark theme variants of overlay
