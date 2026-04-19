# SDD-20260418-014: Media Info Bottom Sheet

**Parent:** SDD-20260418-001 — Visual Redesign (umbrella)

**Depends on:** SDD-20260418-002 ✓, SDD-20260418-013 ✓ (the Info
button in the viewer currently stubs a snackbar — this SDD wires
the real sheet behind it)

## Summary

Add the Info bottom sheet that the media viewer's `Info` button
opens. The sheet slides up over a scrim that keeps the underlying
media visible, so the user doesn't lose their place. It shows a
thumbnail + filename header, a key/value details list (size,
duration for videos, resolution, captured date, source, path), and
a single outlined `Locate in source folder` action at the bottom.

This is the final SDD in the Visual Redesign umbrella (SDD-001).

## Visual spec

- **Container**: `surface` bg, top corners 20dp, `BottomSheetDialog`
  default elevation. Drag handle 36×4dp `line`.
- **Header row** (20dp horizontal padding, 16dp top, 12dp bottom):
  - 48×48dp thumbnail with 10dp rounded corners. For videos, a
    small `▶` glyph overlays the bottom-right corner on a 25% black
    tint disc so you can tell the media type at a glance.
  - Right of thumbnail: filename (15sp 700 `ink`, single-line,
    ellipsize middle) + subtitle (`Video · Viber` or `Photo ·
    WhatsApp`, 12sp `ink3`).
- **Details list** — vertical stack of 6 rows (5 for photos: Duration
  is hidden). Each row:
  - 96dp fixed key column (`ink3`, 13sp, title-cased label)
  - value stretches (`ink`, 13sp by default)
  - 1dp `line` divider below every row except the last
  - 10dp vertical padding per row
  - 20dp horizontal padding

  | Key        | Value format                            | Font   |
  |------------|-----------------------------------------|--------|
  | Size       | `8.71 MB` (Android `Formatter`)         | Inter  |
  | Duration   | `0:48` — video only, hide for photos    | Mono   |
  | Resolution | `1920 × 1080` or `—` if unknown         | Mono   |
  | Captured   | `Apr 12, 2026 · 14:22`                  | Inter  |
  | Source     | `Viber`                                 | Inter  |
  | Path       | full path, word-break-all               | Mono   |
- **Primary action** (20dp horizontal, 16dp top, 16dp bottom):
  outlined pill, full-width, `Locate in source folder`, leading
  24dp folder icon in `ink`. Stroke `line_strong`, 1dp, text `ink`,
  cornerRadius 22dp (matches the rest of the pill buttons in the
  app).
- **Status bar while sheet is open**: icons flipped light, same as
  Filters / Help sheets.

## Behavioral spec

- **Trigger**: tapping `Info` in the media viewer. Replaces the
  `viewer_info_coming_soon` snackbar stub from SDD-013.
- **Dismiss**: swipe-down, tap-outside on scrim, or system back.
  Returns to the viewer in whatever state it was in (paused or
  playing, same seek position).
- **Resolution load**: fragment queries `MediaStore.MediaColumns.WIDTH`
  + `HEIGHT` for the URI on a background thread in `onViewCreated`.
  Until the query returns, shows `—`. Photos almost always return
  valid dimensions; some codecs may not expose them for videos —
  fall through to `—`.
- **Locate in source folder**: best-effort.
  - Primary attempt: `Intent.ACTION_VIEW` with the parent folder's
    `DocumentsContract` tree URI, wrapped in a chooser.
  - Fallback (most devices post-API-29): `Intent.ACTION_VIEW` on
    the **media URI itself**, wrapped in a chooser — opens the
    file in the user's chosen gallery / file manager, not strictly
    the folder but the closest reliable equivalent.
  - If both fail (no app handles the intent): snackbar `Can't open
    folder on this device`.

## Open decisions

1. **`Captured` date source.** `MediaItem.dateAdded` is "added to
   MediaStore," which for Camera captures equals capture date but
   for downloaded media is the download time. True capture date
   lives in EXIF (`DATE_TAKEN` column on API 29+). Default:
   **use `DATE_TAKEN` when the column returns a value, fall back
   to `dateAdded`.** Negligible extra query cost (already hitting
   MediaStore for width/height).
2. **Localize "Video · Viber" subtitle format.** The `·` separator
   and English word order are fine for EN; other locales may need
   a template. Default: **single `info_media_type_and_source`
   plural-free format string with a bullet separator**, matching
   existing sheet patterns. If localization is needed later it can
   be split.

## Scope

1. **New layout** `res/layout/bottom_sheet_media_info.xml`:
   - Root `LinearLayout`, vertical, `surface` bg, 16dp bottom padding.
   - Handle View 36×4dp.
   - Header row: 48dp thumbnail (`ImageView` wrapped in `FrameLayout`
     so we can overlay the video glyph) + filename/subtitle column.
   - Details container (`LinearLayout` vertical) populated in code.
   - Full-width outlined `MaterialButton` for Locate in source
     folder.
2. **New fragment** `MediaInfoBottomSheetFragment.kt`:
   - Extends `BottomSheetDialogFragment`.
   - `newInstance(uri, displayName, mediaType, source, size, dateAdded,
     duration, path)` — static builder stashes args in the bundle.
     (All already available in `MediaItem` at the call site.)
   - `onViewCreated`: load thumbnail via Coil into the 48dp
     `ImageView` (`videoFrameMillis(0L)` for videos); show video
     glyph overlay for `MediaType.VIDEO`; populate header text;
     populate detail rows; wire `Locate in source folder` click.
   - Background thread queries `MediaStore.MediaColumns.WIDTH`,
     `HEIGHT`, `DATE_TAKEN` for the URI; posts back to UI thread
     to update Resolution and Captured rows.
   - `onStart` / `onDestroyView`: flip status-bar icons light /
     restore (mirror of `FilterBottomSheetFragment`).
3. **Row builder helper** inside the fragment — private function
   that takes `(label, value, fontFamily)` and inflates a row with
   the 96dp key + value + optional divider. Used by all 6 rows.
4. **New drawables**:
   - `ic_folder.xml` — 24dp folder vector, `ink` fill via tint.
   - `ic_play_badge.xml` — 12dp white play triangle on a 16dp
     circular 25%-black tint. Used as the video overlay on the
     thumbnail.
   - `media_info_thumb_bg.xml` — 10dp rounded-corner mask for the
     thumbnail `ImageView` (applied via `shapeAppearance` or
     clipToOutline).
5. **New strings**:
   - `info_size`, `info_duration`, `info_resolution`, `info_captured`,
     `info_source`, `info_path` — key column labels.
   - `info_locate_folder` = `Locate in source folder`.
   - `info_media_type_and_source` = `%1$s · %2$s` (e.g. `Video · Viber`).
   - `info_photo`, `info_video` — `Photo` / `Video` for the subtitle
     prefix.
   - `info_unknown` = `—`.
   - `info_locate_failed` = `Can't open folder on this device`.
6. **`MediaViewerActivity.kt`**:
   - Replace the snackbar stub in `btnOverlayInfo.setOnClickListener`
     with a call to `MediaInfoBottomSheetFragment.newInstance(...)`
     and `show(supportFragmentManager, TAG)`.
   - The fragment needs more args than the viewer currently has in
     extras. The viewer already has `displayName`, `size`, `dateAdded`,
     `source`, `mediaType`; need to add `EXTRA_DURATION` and
     `EXTRA_PATH` to the intent, and pass them through.
7. **`MainActivity.openMediaViewer`** — add `EXTRA_DURATION` and
   `EXTRA_PATH` alongside the existing extras:
   - `EXTRA_DURATION = item.duration`
   - `EXTRA_PATH = item.relativePathOrData`

## Not in scope

- Editing metadata in place (rename, change date, etc.).
- Per-app sharing from the Info sheet (Share is deliberately out —
  umbrella §08 notes it's excluded).
- EXIF-beyond-`DATE_TAKEN` (location, camera model, lens). Might
  warrant a separate "More" expansion later.
- Saving the path to clipboard. Could be a future tap-and-hold gesture
  on the path row.
- Unit tests — the fragment is view-heavy and tightly coupled to
  `MediaStore`; Robolectric or instrumentation required for
  coverage. Defer.

## Files changed

| File | Change |
|------|--------|
| `res/layout/bottom_sheet_media_info.xml` | **New** — handle, header, details container, action button |
| `res/drawable/ic_folder.xml` | **New** — 24dp folder vector |
| `res/drawable/ic_play_badge.xml` | **New** — 16dp overlay for video thumbnails |
| `res/drawable/media_info_thumb_bg.xml` | **New** — 10dp rounded-corner mask |
| `res/values/strings.xml` | 10 new strings (`info_*`) |
| `MediaInfoBottomSheetFragment.kt` | **New** — full fragment with async metadata load + locate-folder intent |
| `MediaViewerActivity.kt` | Replace Info snackbar stub with sheet show; add `EXTRA_DURATION` / `EXTRA_PATH` reads |
| `MainActivity.kt` | Add `EXTRA_DURATION` / `EXTRA_PATH` when building viewer intent |

## Acceptance criteria

- [ ] Tapping `Info` in the media viewer opens a bottom sheet; the
      underlying media stays visible behind a scrim.
- [ ] Header shows a 48dp rounded thumbnail (with a `▶` overlay for
      videos), the filename, and a `Video · Source` / `Photo · Source`
      subtitle.
- [ ] Details list shows Size, Duration (video only), Resolution,
      Captured, Source, Path — with a 1dp `line` divider between rows
      and none after the last.
- [ ] Resolution populates with real dimensions (e.g. `1920 × 1080`)
      after a short load; shows `—` if the MediaStore query returns
      no values.
- [ ] Captured date uses `DATE_TAKEN` when available, else falls back
      to `dateAdded`.
- [ ] Path renders in mono font and wraps (no horizontal scroll).
- [ ] Tapping `Locate in source folder` opens a chooser; on success
      the user is in a file browser at the folder (or viewing the
      file). On failure a snackbar explains.
- [ ] Swipe-down / tap-outside / system back all dismiss the sheet,
      returning to the media viewer in its prior state.
- [ ] Status-bar icons render light while the sheet is open and
      revert to dark on dismiss.
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds.
- [ ] No reference to `viewer_info_coming_soon` remains in code
      (string may stay in `strings.xml` if anything else references it;
      otherwise remove).

## Task breakdown

- **[task-01-info-sheet.md](task-01-info-sheet.md)** — the full piece.
  New layout, 3 drawables, 10 strings, fragment (with async MediaStore
  query), viewer wiring, MainActivity extras. ~9 steps.
