# SDD-20260418-001: Visual Redesign

## Summary

Replace the current visual language (teal + green + yellow + red with ad-hoc
badge colors) with a single coherent system: **one warm orange accent, one ink
color, one set of shapes**. Restyle all 9 surfaces, introduce date-grouped
sections, recalibrate the reviewed-tile treatment, and add a "Keep" action in
the media viewer so the reviewer loop closes properly.

Source: handoff bundle from Claude Design, Direction B (Playful).
Live prototype: https://claude.ai/design/p/e4fca561-7ccd-423d-a4cb-46be6d4b1439?file=index.html&via=share

---

## Design System (Direction B)

### Color tokens

| Token | Hex | Role (Android) |
|-------|-----|----------------|
| `bg` | `#FAF6EF` | `colorSurface` (window background) |
| `surface` | `#FFFDF8` | `colorSurfaceContainer` (cards, sheets) |
| `surfaceAlt` | `#F3ECDF` | `colorSurfaceContainerLow` |
| `line` | `#E8DFCE` | `colorOutlineVariant` |
| `lineStrong` | `#D4C6AD` | `colorOutline` |
| `ink` | `#1D1710` | `colorOnSurface`, primary text, ink-filled buttons |
| `ink2` | `#4A3E2E` | Secondary text |
| `ink3` | `#77674F` | Tertiary text / subtle icons |
| `ink4` | `#A8957B` | Captions / placeholders |
| `accent` | `#E85A3D` | `colorPrimary` |
| `accentInk` | `#FFFFFF` | `colorOnPrimary` |
| `accentSoft` | `#FBE0D5` | `colorPrimaryContainer` |
| `accentDeep` | `#A83320` | Pressed accent / accent-on-soft text |
| `danger` | `#C8342B` | `colorError` |
| `dangerSoft` | `#F8DDDB` | `colorErrorContainer` |
| `success` | `#2E7D5B` | Success states (checkmark, freed-space copy) |

**Drops:** `header_title_green`, `chip_selected_*`, `filter_btn_*`,
`badge_unviewed_*`, `badge_viewed_*`, `fab_continue_bg`, `fast_scroll_*`,
`hint_tooltip_bg`, `empty_state_*`, Ko-fi yellow `#FFDD00` + brown `#6F4E37`.
All replaced by the table above.

### Shape

- Corner radius scale: `xs 6 · sm 12 · md 16 · lg 22 · xl 28 · pill 999`
- Tile radius: **14dp** (up from 4dp — softer, more "photo" feel)
- All buttons are pill-shaped (999dp)

### Type

Font stays Inter. Add JetBrains Mono for numeric readouts (size, counts, dates
in fast-scroller and status strings). JetBrains Mono is licensed under SIL Open
Font License 1.1 — free for commercial use; bundling obligation is to append
its license text to the existing OFL notice that ships with Inter.

| Role | Size/LH | Weight | Usage |
|------|---------|--------|-------|
| display | 30/36 | 700 | Hero readouts ("789 kB" on delete success) |
| h1 | 24/30 | 700 | Sheet titles, empty-state title |
| h2 | 18/24 | 600 | Section titles inside sheets |
| body | 15/22 | 400 | Paragraphs |
| label | 13/18 | 500 | Buttons, chips |
| caption | 11/14 | 500 | Micro-labels |
| mono | 12/16 | 500 | Counts, sizes, dates (JetBrains Mono) |

### Elevation

Four levels; used as `android:elevation` on cards/FABs/sheets:

- `1` — subtle card rest
- `2` — hovered/focused card
- `3` — FAB, floating selection bar
- `4` — modal sheets, success overlay

### Spacing

4dp rhythm: `4 · 8 · 12 · 16 · 20 · 24 · 32`.

---

## Per-screen changes

### 01. Main grid

- Title `Gallery Cleaner` in **ink**, not green.
- **Ko-fi button removed** from top bar (relocates to Help sheet).
- **Filter pill active state**: soft accent fill (`accentSoft`) + 1.5dp accent
  border + accent text. Replaces the current dot badge (`filterActiveDot`
  removed).
- Photos/Videos chips — ink-filled when active (white text on ink), 1dp outline
  when inactive. Replaces current teal chip.
- Review progress bar: 2dp hairline in `accent` (was teal, 3dp).
- Right-side **unreviewed counter** on the chip row: `N LEFT` (e.g. `188 LEFT`),
  JetBrains Mono, 10sp, `ink4` color, 0.5sp letter-spacing, all-caps. Pushed to
  the trailing edge with a flex spacer after the Photos/Videos chips. Value =
  count of items in the current filtered set that are not in `viewedItems`.
  Updates live as items are reviewed or deleted.
- **Date section headers** in the grid: sticky "Today · 12 items",
  "Yesterday · 6 items", weekday/date labels. See Behavioral change #1.
- **Fast scroller** thumb: ink fill, rounded pill. Tooltip: ink background,
  mono type, 10sp all-caps (`APR 12`). Replaces current teal thumb + bold date.
- **Continue FAB** gains a second state — see Behavioral change #2.

### 02. Selection mode

- Selection action bar becomes a **single floating pill** (rounded 999dp) at
  bottom, not a wide card. Elevation 4.
- Layout inside pill: `× Select-all · | · {count + size} · Delete`
- **Size readout** under the count: `4 selected` in 13sp bold ink, `789 kB` in
  11sp mono `ink3`. Updates live as selection changes.
- Unselected tiles dim to **55% white overlay** during selection (stronger than
  reviewed dim, which overrides reviewed state — selection is always louder).
- Only Delete is colored (danger fill). Close and Select-all stay ink.

### 03. Filter bottom sheet

- Replace current single scroll of groups with **four titled sections**: Type,
  Source, Date, Sort by. Section label: 11sp 600 `ink4` uppercase 0.8 tracking.
- Source chips carry **counts in mono**: `WhatsApp 5`, `Viber 188`, etc.
- Sheet handle: 36×4dp rounded bar, `line` color.
- Header row: title `Filters` (20sp 700) + `Reset` text link in accent.
- Footer: outline `Cancel` + ink-filled `Apply · N changes` (2:1 width ratio).
  The apply button *counts the pending changes*.
- **Status bar while sheet is open**: scrim extends behind the status bar (no
  seam at the top). Switch `windowLightStatusBar` to `false` so system icons
  render in light/white and stay legible over the dark scrim. Revert on
  dismiss.

### 04. Empty state

- Icon: 72dp circle with **1.5dp outline** (`lineStrong`), not a filled tinted
  disc. Icon inside in `ink3`.
- Title: 20sp 700 ink (e.g. "No matches").
- Subtitle names the active filter combo: "Nothing in your library fits
  'Videos · Viber · Last 7 days'. Try widening the range."
- Primary action: **ink-filled `Reset filters`**.
- Secondary: **text `Edit filters`** (opens the filter sheet). Replaces the
  existing `btnResetFilters` with a two-action layout.

### 05. Delete success overlay

- Card, not full-screen overlay: white `surface`, elevation 4, 20dp radius.
- **Hero number**: freed size at display scale (30sp 700), e.g. `789 kB`.
- Subtitle below: `freed · 4 items moved to trash` (13sp ink3).
- **Confetti**: 22 brand-colored pieces (accent, ink, accentSoft, success).
  Deterministic seed so the layout is stable. No rainbow.
- **Undo**: outline button with a **7-second progress ring** around the undo
  icon (was 8-second numeric countdown). Ring drains via
  `strokeDashoffset`. After 7s, ring empties → commit.
- Primary: ink-filled `Continue`.
- Checkmark badge at top: 56dp `accentSoft` circle with accent check.

### 06. Hint card

- Keep current bottom placement and "Got it" dismiss.
- Restyle: **ink background** (matches FAB ink), 14dp radius, 12dp padding.
- Icon: 28dp rounded circle with 12% white fill, lightbulb inside.
- **Two-line format**: title bold (`Tip · swipe to select`) + detail (`Long-press
  one, then drag across others.`) in 70% white.
- Right side: `×` close icon (60% white) in place of "Got it" text button.
  → *Decision needed: keep explicit "Got it" text button per existing UX, or
  switch to `×`? See Open decisions.*

### 07. Help bottom sheet

- Handle + title like Filters sheet. Title: `Tips & shortcuts` (20sp 700).
- **Tip rows**: icon in accent (22dp) + label (13sp ink2), separated by
  `line`-color dividers (no bullet indentation).
- Right of title: subtle `Reset tips` underlined text (12sp 600 ink3).
- **Ko-fi relocates here** as a support card at the bottom: `surfaceAlt` fill,
  14dp radius, 32dp accent-soft round icon, title `Enjoying the app?`, subtitle
  `One coffee keeps it ad-free.`, chevron right.
- **Status bar while sheet is open**: same treatment as Filter sheet — scrim
  extends behind the status bar, `windowLightStatusBar` flipped to `false`.

### 08. Media viewer

- Header: `×` close + filename + metadata strip (`date · size · source` in 11sp
  55% white). **Overflow `⋯` removed.**
- Background: `#000` (unchanged).
- **Play/pause + scrubber render only for videos.** Photos show neither.
- Scrubber thumb and progress fill use `accent` so they never disappear against
  photo content. Mono time labels.
- Bottom action bar: `Info · Keep · Delete`, 3 equal columns, 10dp radius.
  Info/Keep: 10% white fill. Delete: `danger` fill. See Behavioral change #3
  for Keep. `Info` opens screen 09.

### 09. Media viewer — Info sheet

Triggered by tapping **Info** in the media viewer (§08). Media stays visible
behind a 55% black scrim so the user keeps their place. Status bar icons are
light (white) — scrim covers the status-bar region, no seam.

- **Sheet container**: background `bg` (`#FAF6EF`), top-left/top-right radius
  20dp, elevation via a negative-Y shadow `0 -8px 24px rgba(0,0,0,0.2)`.
  Handle bar: 36×4dp `line` color, 4dp top / 10dp bottom margin.
- **Header row** (above the details list):
  - 48×48dp rounded thumbnail (10dp radius) of the current media; for video,
    overlay a small play glyph on a 25% black tint.
  - Right side: full filename (15sp 700 ink, single-line ellipsized) +
    `Video · Viber` or `Photo · WhatsApp` subtitle (12sp ink3).
- **Details list**: key/value rows separated by `line` dividers, 10dp vertical
  padding per row. 96dp fixed key column, value stretches.

  | Key | Value format |
  |-----|--------------|
  | Size | `8.71 MB` (Inter) |
  | Duration | `0:48` — video only, hide for photos |
  | Resolution | `1920 × 1080` (**mono**) |
  | Captured | `Apr 12, 2026 · 14:22` (Inter) |
  | Source | `Viber / Media` |
  | Path | full filesystem path (**mono**, word-break all) |

- **Single primary action** at bottom: outline ink button, full-width,
  `Locate in source folder` with folder/filter icon. Opens the containing
  directory using a `FileProvider` / system file browser intent.
- **No Share / Open-with** on this sheet — those live in the system share
  sheet, accessible from the source folder or via long-press. Keeps the Info
  sheet focused on inspection.
- **Dismiss**: swipe-down (Material3 bottom-sheet default), tap-outside on the
  scrim, or system back button. Returns to the media viewer in the same
  state.

---

## Behavioral changes (not purely visual)

### B1. Date section headers

**New.** Group the grid by date into sticky sections: "Today · N items",
"Yesterday · N items", "Last week · N items", then by month for older items.
Header renders as a full-row span in the `GridLayoutManager`.

- Text: 13sp 600 ink (left) + 11sp 500 ink4 item count (right), 14/6dp padding
- Grouping buckets: Today, Yesterday, This week, Last week, Month name
  (`April 2026`), then older months
- Implementation: mixed-item-type `ListAdapter` (header vs image) with
  `GridLayoutManager.SpanSizeLookup` returning full span for headers

### B2. Continue FAB — two states

Not new behavior, new visual. Current implementation dims to `alpha=0.5f` +
disables when the first unviewed item is already on screen
([MainActivity.kt:422-424](../../../../app/src/main/java/com/example/gallerycleaner/MainActivity.kt#L422-L424)).
Redesign makes the two states visually distinct:

- **Active**: ink fill, white text, `Continue`, shadow elevation 3
- **Caught up**: transparent `surface` at 72% with 1dp `lineStrong` outline,
  `ink4` text, label changes to `All caught up`, no shadow, not clickable

### B3. Media viewer — Keep action

**New.** The viewer gains a `Keep` action parallel to `Delete`, which:

1. Marks the current item as reviewed (adds its URI to `viewedItems`)
2. Advances to the next unreviewed item in the queue
3. Shows no celebration — silent positive feedback (haptic tick only)

Together with `Delete`, this turns the viewer into a Tinder-style review loop.
`Continue` FAB keeps working in the grid since both paths feed the same
`viewedItems` store.

**Implementation touch points:**
- New `GalleryViewModel.markReviewed(uri)` method (or reuse existing
  `viewedItems` mutator)
- `MediaViewerActivity` wires the Keep button + advances via its pager

---

## Child SDDs

This umbrella is a vision + cross-reference index. Each piece below is its own
SDD folder under `docs/sdd/todo/`, authored as implementation lands. All child
SDDs depend on **SDD-20260418-002 Design Tokens** shipping first.

| SDD | Folder | Scope |
|-----|--------|-------|
| 002 | `20260418-002-design-tokens/` | colors.xml tokens, themes.xml role bindings, JetBrains Mono font + OFL notice, chip color state lists. **Foundation** — no layout changes. |
| 003 | `20260418-003-top-bar/` | Title ink, Ko-fi removed, filter pill active = soft-fill + accent border (remove `filterActiveDot`), chip restyle, hairline progress bar in accent, fast-scroller restyle, firm `N LEFT` counter. |
| 004 | `20260418-004-grid-tile/` | Source pill → 10sp glass-dark corner chip, video badge matching style, tile radius 14dp, **reviewed calibration to 85% opacity + 75% saturation** (from current heavier fade). |
| 005 | `20260418-005-date-section-headers/` | B1. Sticky/inline date section headers in the grid. New adapter item type, SpanSizeLookup, bucket logic. |
| 006 | `20260418-006-selection-bar/` | Floating pill bar, size-readout under count, 55% dim on unselected (override reviewed state), Delete as only colored affordance. |
| 007 | `20260418-007-continue-fab/` | B2. Ghost/caught-up FAB visual state and `All caught up` label swap. |
| 008 | `20260418-008-filter-sheet/` | Four titled sections, source chips with mono counts, Apply shows change count, outline Cancel, light status-bar over scrim. |
| 009 | `20260418-009-empty-state/` | Outline circle, ink-filled Reset, text Edit filters, filter-combo copy. |
| 010 | `20260418-010-delete-success/` | Card (not fullscreen), hero display-size `789 kB`, brand confetti, 7s progress ring around Undo icon. |
| 011 | `20260418-011-hint-card/` | Ink bg, two-line title+detail, lightbulb in 12%-white circle. Decision on × vs "Got it" button. |
| 012 | `20260418-012-help-sheet/` | Tip rows with divider separators, accent tip icons, Ko-fi support card at bottom, light status-bar over scrim. |
| 013 | `20260418-013-media-viewer/` | B3. Remove `⋯`, add Keep action (marks reviewed + advance), accent scrubber, video-only player controls. |
| 014 | `20260418-014-info-sheet/` | Screen 09. New `MediaInfoBottomSheetFragment` with thumbnail+filename header, key/value details list (Size, Duration [video-only], Resolution [mono], Captured, Source, Path [mono]), and outline `Locate in source folder` action. Light status-bar icons over scrim. |

Recommended order: **002 → 003 → 004 → 006 → 007 → 009 → 011 → 008 → 012 → 010 → 005 → 013 → 014**.
(Foundation first, then top-level chrome, then behavioral/new-surface work 005,
013, 014 last.)

---

## Token → Android mapping (for Task 01)

```xml
<!-- colors.xml replacements -->
<color name="md_theme_primary">#E85A3D</color>       <!-- accent -->
<color name="md_theme_onPrimary">#FFFFFF</color>
<color name="md_theme_primaryContainer">#FBE0D5</color>  <!-- accentSoft -->
<color name="md_theme_onPrimaryContainer">#A83320</color> <!-- accentDeep -->

<color name="md_theme_background">#FAF6EF</color>    <!-- bg -->
<color name="md_theme_surface">#FFFDF8</color>       <!-- surface -->
<color name="md_theme_surfaceVariant">#F3ECDF</color> <!-- surfaceAlt -->
<color name="md_theme_onSurface">#1D1710</color>     <!-- ink -->
<color name="md_theme_onSurfaceVariant">#4A3E2E</color> <!-- ink2 -->

<color name="md_theme_outline">#D4C6AD</color>       <!-- lineStrong -->
<color name="md_theme_outlineVariant">#E8DFCE</color> <!-- line -->

<color name="md_theme_error">#C8342B</color>         <!-- danger -->
<color name="md_theme_errorContainer">#F8DDDB</color> <!-- dangerSoft -->

<!-- New named tokens (not all fit Material 3 roles) -->
<color name="ink3">#77674F</color>
<color name="ink4">#A8957B</color>
<color name="success">#2E7D5B</color>
```

Status bar stays light (`windowLightStatusBar=true`) on `bg`.

---

## Out of scope

- Dark theme (design handoff is light-only per user's questionnaire)
- Direction A (Utilitarian) — dropped, only B is being implemented
- Animated confetti physics — deterministic static placement is enough
- Swipe gestures in media viewer (swipe-left=Delete, swipe-right=Keep) — future
- Widget / launcher-icon refresh
- Localization of new strings beyond existing language coverage

---

## Open decisions

1. **Hint card dismiss — `×` icon vs "Got it" text button.** Design shows `×`;
   current UX (SDD-20260322-002) explicitly chose "Got it" to force users to
   read. Default: **keep "Got it"** unless we revisit the rationale.
2. **Section-header grouping granularity.** Design shows three labels (Today /
   Yesterday / Last week). Should older items group by month, by week, or stay
   ungrouped? Default: **Today / Yesterday / This week / Last week / month-name
   for older**.
3. **Keep action — `Keep` label vs `Reviewed` vs `Next`.** `Keep` reads as
   permanent, `Reviewed` is more honest. Default: **`Keep`** (matches design
   and is shorter on 360dp).
4. **Fast-scroller thumb shape.** Design makes it an ink pill with mono date
   tooltip. Current is a teal rounded rect. Default: **adopt design's ink pill
   + mono tooltip** (part of Task 02).

---

## Acceptance criteria (umbrella)

Individual tasks carry their own checklists. This SDD is complete when:

- [ ] All 13 child SDDs (002–014) merged
- [ ] No remaining references to the dropped color tokens
  (`header_title_green`, `chip_selected_*`, `filter_btn_*`,
  `badge_unviewed_*`, `badge_viewed_*`, `fab_continue_bg`, `fast_scroll_*`,
  `hint_tooltip_bg`, `empty_state_*`) in `colors.xml`, layouts, or code
- [ ] App builds, all tests pass, lint passes
- [ ] A manual visual pass across all 8 screens confirms the single-accent
  discipline (no teal, green, or yellow chrome anywhere)
- [ ] Reviewed tiles read as "dealt with, still readable" — not "broken
  thumbnail" (85% opacity × 75% saturation)
- [ ] Keep + Delete in media viewer both advance the review progress bar and
  the Continue-FAB "caught up" state works for both paths
