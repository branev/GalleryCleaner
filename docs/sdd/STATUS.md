# SDD Status Tracker

## Completed (`done/`)

| SDD | Feature | Tasks | Completed |
|-----|---------|-------|-----------|
| 20250321-001 | Grid visual redesign | 4/4 | Header, badges, video duration, grid styling |
| 20250321-002 | Empty state redesign | 1/1 | Circular icon, larger text, Reset Filters button |
| 20260321-001 | Delete flow improvements | 1/1 | No custom dialog, success overlay, 8s undo, live size |
| 20260321-002 | Drag-to-select | 1/1 | Range selection, auto-scroll, re-drag, scroll vs drag |
| 20260321-003 | Active filter indicator | 1/1 | Color change, dot badge, grayed Reset button |
| 20260321-004 | Fast scroller with date tooltip | 1/1 | Draggable thumb, date tooltip, transparent track |
| 20260321-006 | Pinch-to-zoom grid | 1/1 | 2-5 columns, continuous, persisted, pinch-safe in selection |
| 20260321-007 | Review progress bar | 1/1 | Thin teal bar, updates on scroll/delete/undo |
| 20260321-008 | Contextual hints & help | 3/3 | 8 hints, help button, tips sheet, priority queue |
| 20260322-001 | Code review fixes & test coverage | 1/1 | 7 review fixes, 16 new tests |
| 20260322-002 | Hint UX rework | 1/1 | Bottom card with Got it, 3/session, slide animation |
| 20260418-002 | Design tokens | 1/1 | Direction B palette, JetBrains Mono, Material 3 theme bindings |
| 20260418-003 | Top bar | 1/1 | Ink title, Ko-fi out, filter pill active state, N LEFT counter, 32dp pills |
| 20260418-004 | Grid tile | 1/1 | 14dp corners, glass-dark chips, reviewed 85%/75% fade, accent ring selection |
| 20260418-005 | Date section headers | 1/1 | GridItem sealed class, DateBucket utility, mixed-type ListAdapter, SpanSizeLookup |
| 20260418-006 | Selection action bar | 1/1 | Outlined pill, stacked count+size readout, mono size, hairline divider |
| 20260418-007 | Continue FAB states | 1/1 | Shorter "Continue" label, hide-when-useless visibility; ghost state deferred |
| 20260418-008 | Filter sheet & empty state | 2/2 | Pending-state sheet with four sections, mono source counts, light status-bar over scrim, outlined empty-state ring, ink `Reset filters` + text `Edit filters`, filter-combo subtitle. Merges old SDD-009. |
| 20260418-010 | Delete success | 1/1 | 320dp card on 30% scrim, 56dp accent-soft check badge, mono hero size, 7s progress ring around Undo, 60 falling-confetti pieces with fresh random pattern per delete, Snackbar removed on Android-11+ path. |
| 20260418-011 | Hint card | 1/1 | Two-line title+detail split, 28dp lightbulb-in-circle, × close top-right (after preview, design's × chosen over "Got it"), stroke removed, 16 new hint strings. |
| 20260418-012 | Help sheet | 1/1 | Header `Reset tips` link, 22dp accent tip icons with line dividers, full-width Ko-fi support card (accent-soft icon + chevron), status-bar flip, OK button + `overlay_ok` string removed. Reset keeps sheet open and confirms via snackbar. |
| 20260418-013 | Media viewer | 1/1 | Chrome metadata gains source (11sp 55% white), Info·Keep·Delete bottom bar with icon+text stack (Delete on danger), custom accent-tinted video player row replaces MediaController, white-outlined center play button, video poster via loadThumbnail+MMR fallback (videoContainer hidden until play to dodge SurfaceView black-hole), result-launcher routes Keep → markAsViewed and Delete → performTrash. Info wired to SDD-014 sheet. |
| 20260418-014 | Info sheet | 1/1 | New `MediaInfoBottomSheetFragment` with thumbnail + filename header, details list (Size, Duration video-only, Resolution, Captured, Source, Path), async MediaStore query for Resolution + `DATE_TAKEN`, path-row copy-to-clipboard icon. "Locate in folder" dropped — Android's scoped-storage rules reject `ACTION_VIEW` on `DocumentsContract` URIs without a prior SAF grant. |
| 20260419-002 | Confetti physics rework | 1/1 | Two-origin fountain pop (460–680 dp/s up, 18%/82% of card), linear + angular drag + per-piece turbulence, edge-on ribbon illusion via `canvas.scale(widthScale,1f)`, weighted 3-shape (ribbon 55 / streamer 25 / disc 20) and 4-color (32/32/18/18 — discs skip ink) distribution, 2.5s life with ease-in/out alpha, upward whoosh on top-15 fastest + per-piece mass jitter. Claude Design spec. |

## Pending (`todo/`)

| SDD | Feature | Priority | Dependencies | Notes |
|-----|---------|----------|-------------|-------|
| 20260321-005 | Smart filter summary | Medium | None | Dismissible chips for active filters |
| 20260419-001 | Bump minSdk to 30 | Low | None | Tooling. Drops CustomTypefaceSpan, WindowCompat.getInsetsController, SDK_INT >= 29/R branches |
| 20260418-001 | Visual redesign (umbrella) | High | None | All 13 child SDDs done; ready to close. Vision + cross-ref index |

## Other

| Item | Type | Status |
|------|------|--------|
| Inter font | Enhancement | Complete |
| AGP upgrade | Tooling | Complete |
| Kotlin 2.2.20 | Tooling | Complete |
| Date section headers | Idea | Backlog (docs/ideas.md) |
| Trash access | Idea | Backlog (docs/ideas.md) |
| "Left off here" divider | Idea | Backlog (docs/ideas.md) |
