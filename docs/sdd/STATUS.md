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

## Pending (`todo/`)

| SDD | Feature | Priority | Dependencies | Notes |
|-----|---------|----------|-------------|-------|
| 20260321-005 | Smart filter summary | Medium | None | Dismissible chips for active filters |
| 20260419-001 | Bump minSdk to 30 | Low | None | Tooling. Drops CustomTypefaceSpan, WindowCompat.getInsetsController, SDK_INT >= 29/R branches |
| 20260418-001 | Visual redesign (umbrella) | High | None | Vision + cross-ref index for 002–014. Claude Design handoff |
| 20260418-011 | Hint card | Low | 002 ✓ | Ink bg, two-line format |
| 20260418-012 | Help sheet | Low | 002 ✓ | Dividers, accent icons, Ko-fi card |
| 20260418-013 | Media viewer | Medium | 002 ✓ | **New behavior** — Keep action, remove ⋯ |
| 20260418-014 | Info sheet | Medium | 002, 013 | **New screen** — file metadata bottom sheet |

## Other

| Item | Type | Status |
|------|------|--------|
| Inter font | Enhancement | Complete |
| AGP upgrade | Tooling | Complete |
| Kotlin 2.2.20 | Tooling | Complete |
| Date section headers | Idea | Backlog (docs/ideas.md) |
| Trash access | Idea | Backlog (docs/ideas.md) |
| "Left off here" divider | Idea | Backlog (docs/ideas.md) |
