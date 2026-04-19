# SDD-20260418-013: Media Viewer

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 ✓ (tokens, mono font)

**Enables:** SDD-20260418-014 (Info sheet wires into the Info button
this SDD adds)

## Summary

Turn the fullscreen media viewer from a passive "preview tile" into
the **review loop** half of the app. Today it's read-only: open a
tile, see it fullscreen, tap `×` to return. After this SDD, the user
can **Keep** (mark reviewed + close) or **Delete** (system-trash +
close) directly from the viewer. A new metadata strip names the file's
source. Video playback gets a custom accent-tinted control row
because the system `MediaController` fades into bright photos.

Design handoff umbrella §08 + Behavioral change §B3 drives this.
SDD-014 will add the Info sheet that the `Info` button opens — we
stub that wire with a brief "Coming soon" snackbar until 014 lands.

## User-visible changes

1. **Header metadata** gains source: `MMM d, yyyy · {size} · {source}`
   (e.g. `Apr 12, 2026 · 8.71 MB · Viber`). Text color shifts from
   70% white to 55% white, size from 12sp to 11sp — matches the
   design's subtler chrome.
2. **New bottom action bar** — three equal-width buttons:
   `Info · Keep · Delete`. Info + Keep use a 10%-white fill on the
   black viewer background. Delete uses the `danger` token (red fill,
   white icon). 10dp radius. Always visible when the toolbar is
   (same tap-to-toggle as today's header).
3. **Keep action** — taps the ✓ glyph, fires a haptic tick, calls
   `viewModel.markAsViewed(uri)`, closes the viewer. No celebration.
4. **Delete action** — taps the 🗑 glyph, returns
   `RESULT_OK + EXTRA_ACTION=delete + uri` to MainActivity, which
   routes it through the existing `performTrash()` path (so the
   redesigned delete-success card fires as normal).
5. **Custom video player row** — stock Android `MediaController` is
   replaced with a bottom-edge row containing:
   - Play/pause button (left, reuse existing 72dp overlay but make
     smaller — 48dp, on the row)
   - `SeekBar` with accent thumb + accent progress fill
   - Mono `current / total` labels (e.g. `0:12 / 0:48`) in 11sp 70%
     white.
   Row is only visible for videos; photos show neither row nor
   play/pause glyph.
6. **`⋯` overflow menu** — design specifies removal. Current code
   has none, so there's nothing to remove. Noted so this bullet
   reads "✅ by inspection" rather than being silently skipped.

## Behavioral changes

- **Review-loop integration**: Keep and Delete both close the viewer
  after acting. Advance-to-next-unreviewed lives in the grid's
  Continue FAB, not in the viewer. *(Swipe-through pager is out of
  scope — see Not in scope.)*
- **Result plumbing**: the viewer becomes a result-returning activity.
  New result contract:
  - `RESULT_OK` + `EXTRA_ACTION = "delete" | "kept"` +
    `EXTRA_URI = <Uri>`
  - `RESULT_CANCELED` if user just taps `×` or back (no action taken)
- **MainActivity integration**: `handleItemClick` switches from
  `startActivity` to a
  `registerForActivityResult(ActivityResultContracts.StartActivityForResult)`
  launcher. On `RESULT_OK` with action:
  - `kept`  → no-op; the viewer already called `markAsViewed`.
  - `delete` → call the existing `performTrash(setOf(uri))`.

## Open decisions

1. **Should Keep persist across app restarts?** Today `viewedItems`
   is session-only. Design doesn't specify. Default: **keep
   session-only** — matches existing semantics; persistence would
   need a separate SharedPreferences layer (out of scope for 013).
2. **Haptic strength.** `HapticFeedbackConstants.CLOCK_TICK` is
   subtle; `KEYBOARD_TAP` is heavier. Default: `CLOCK_TICK`.

## Scope

1. **`res/layout/activity_media_viewer.xml`**:
   - Extend metadata strip format (+source, 11sp, 55% white).
   - Add bottom action bar: three equal `FrameLayout` buttons with
     `viewer_btn_bg` / `viewer_btn_bg_danger` backgrounds. Each with
     centered icon (and, optionally, small label below — design shows
     icon-only, so icon-only it is).
   - Add custom video player row above the action bar, visible only
     when `videoContainer` is visible.
   - Remove the center 72×72dp overlay play/pause — moves into the
     row.
2. **`res/drawable/viewer_btn_bg.xml`** — new. Rectangle, 10dp
   radius, `#1AFFFFFF` (10% white).
3. **`res/drawable/viewer_btn_bg_danger.xml`** — new. Rectangle, 10dp
   radius, `@color/danger` fill.
4. **`res/drawable/viewer_seekbar_thumb.xml`** — new. Accent circle,
   ~12dp diameter.
5. **`res/drawable/viewer_seekbar_progress.xml`** — new. Layer-list
   with `@android:id/background` (25% white) +
   `@android:id/progress` (clip, accent).
6. **Icons** — verify existing `ic_info`, `ic_check` (reused from
   delete success), `ic_delete`. Tint white in layout.
7. **`res/values/strings.xml`** — add `viewer_keep`, `viewer_delete`,
   `viewer_info`, `viewer_info_coming_soon` (stub toast),
   `viewer_delete_failed`.
8. **`MediaViewerActivity.kt`**:
   - Add `EXTRA_SOURCE` plumbing. Pull `SourceType` enum ordinal, map
     to label.
   - Format metadata as `date · size · source`.
   - Remove `setupVideoPlayer`'s `MediaController` integration;
     replace with custom row wired to `VideoView`:
     - Play/pause toggles `videoView.pause()` / `videoView.start()`
     - A `runOnUiThread` loop polls `videoView.currentPosition` every
       250ms to update SeekBar + time label while playing.
     - SeekBar listener seeks on user drag.
   - Wire `btnOverlayInfo`, `btnOverlayKeep`, `btnOverlayDelete`:
     - Info: `Snackbar("viewer_info_coming_soon")` — stub for 014.
     - Keep: haptic + `setResult(RESULT_OK, intent{ACTION=kept, URI})`
       + `finish()`. The VM call happens on MainActivity's side (see
       below) so Keep behaves symmetrically with Delete — OR, simpler,
       viewer calls `GalleryViewModel` via a singleton... but we
       don't have DI. Use the result-pattern: MainActivity's launcher
       calls `viewModel.markAsViewed(uri)` on receiving ACTION=kept.
     - Delete: `setResult(RESULT_OK, intent{ACTION=delete, URI})` +
       `finish()`.
9. **`MainActivity.kt`**:
   - Add `EXTRA_SOURCE` when building the viewer intent.
   - Swap `startActivity` for `registerForActivityResult` launcher.
   - On `RESULT_OK`:
     - `kept` → `viewModel.markAsViewed(uri)`.
     - `delete` → `performTrash(setOf(uri))`.
10. **`GalleryViewModel.kt`** — no changes needed; `markAsViewed(uri)`
    is the API, already exists.

## Not in scope

- **Swipe-through pager.** Turning the viewer into a `ViewPager2` so
  Keep/Delete can advance to the next unreviewed item without
  leaving the viewer. Design calls it "Tinder-style review loop"
  but also says *"Continue FAB keeps working in the grid since both
  paths feed the same viewedItems store"* — so grid-based advance
  is the sanctioned fallback. Pager conversion can be its own SDD.
- **Info sheet functionality.** The Info button stubs a snackbar.
  SDD-014 replaces that with the real sheet.
- **Keep persistence across sessions.** Session-only for now.
- **Haptic on Delete.** Silent per design (the scrim + trash card is
  the confirmation).
- **Rotating viewer to landscape.** Not broken, not changed.
- **Share / Save-as.** Never existed; design doesn't add them.

## Files changed

| File | Change |
|------|--------|
| `res/layout/activity_media_viewer.xml` | Metadata format; new bottom action bar; new video player row; remove center play/pause overlay |
| `res/drawable/viewer_btn_bg.xml` | **New** — 10dp rect, 10% white |
| `res/drawable/viewer_btn_bg_danger.xml` | **New** — 10dp rect, `danger` fill |
| `res/drawable/viewer_seekbar_thumb.xml` | **New** — accent 12dp circle |
| `res/drawable/viewer_seekbar_progress.xml` | **New** — layer-list, accent progress on 25% white track |
| `res/values/strings.xml` | Add `viewer_keep`, `viewer_delete`, `viewer_info`, `viewer_info_coming_soon`, `viewer_delete_failed` |
| `MediaViewerActivity.kt` | Metadata string with source; custom video player row; Info/Keep/Delete click wiring; result plumbing |
| `MainActivity.kt` | `EXTRA_SOURCE` added to viewer intent; launcher-based result handling; routes delete → `performTrash`, kept → `markAsViewed` |

## Acceptance criteria

- [ ] Tapping a tile opens the viewer and the header metadata reads
      `MMM d, yyyy · {size} · {source}` in 11sp at 55% white.
- [ ] The bottom action bar shows three equal-width buttons, 10dp
      radius, icons centered. Info + Keep on 10%-white fill, Delete
      on danger-red fill.
- [ ] Tapping **Keep** triggers a brief haptic tick, closes the
      viewer, and marks the item as viewed (visible as the dimmed
      "reviewed" tile back in the grid).
- [ ] Tapping **Delete** closes the viewer and triggers the same
      trash flow used from selection mode — the redesigned
      delete-success card appears with the freed size + Undo ring.
- [ ] Tapping **Info** shows a snackbar `Coming soon` (placeholder
      until SDD-014).
- [ ] For **videos**: a bottom-edge custom player row is visible
      with a play/pause button, an accent-tinted SeekBar, and mono
      time labels `current / total`. Dragging the seekbar seeks.
      Playback pauses/resumes via the button.
- [ ] For **photos**: no player row, no play/pause glyph.
- [ ] Tapping `×` or system back returns to the grid **without**
      marking viewed or deleting.
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds.
- [ ] No reference to `MediaController` remains in
      `MediaViewerActivity.kt`.

## Task breakdown

- **[task-01-media-viewer.md](task-01-media-viewer.md)** — the full
  piece. Drawables, strings, layout rebuild, activity rewrite (header
  metadata, custom player row, action bar wiring, result plumbing),
  MainActivity launcher + action routing. ~10 steps.
