# Task 01: Top Bar Redesign

**Parent:** SDD-20260418-003 — Top Bar Redesign

## What You're Changing

You're restyling the main-screen top bar — the header row with the title,
help icon, Filters pill, and the chip row underneath it — to match the new
Direction B visual language.

This is a UI-only task. No new behavior, no new fragments. You're touching
one layout file, one Kotlin file, a few color/drawable resources, and
deleting one drawable that's no longer needed.

## Before vs After

| Element | Before | After |
|---|---|---|
| App title | green `#1B5E20` | ink `#1D1710` |
| Top-bar actions | Help (?) · Filters · Buy me a coffee | Help (?) · Filters (Ko-fi gone) |
| Filters active signal | red dot badge in the corner | whole pill toggles soft-orange fill + accent outline |
| Chip size | 40dp tall, 14sp | 32dp tall, 13sp |
| Progress bar | 3dp teal | 2dp warm orange |
| Unreviewed count | not shown | right-aligned `188 LEFT` in JetBrains Mono |
| Fast-scroll thumb | flat 4dp-corner teal rect | full pill ink |
| Fast-scroll tooltip | `Apr 12` bold 14sp | `APR 12` mono 10sp all-caps |

## Prerequisites

- SDD-20260418-002 (Design Tokens) is merged. The palette and JetBrains Mono
  are already wired up.
- Run `./gradlew --stop` before you start, to avoid Windows file-lock issues
  on the build dir.

## Step-by-Step Instructions

### Step 1 — Remove the Ko-fi button from the top bar

Keep the `kofi_logo` drawable and the `support_developer` string in the
project — SDD-012 (Help sheet) will reuse them.

**1a. Layout.** Open `app/src/main/res/layout/activity_main.xml`. Find the
Ko-fi button (around line 125). Delete the whole `MaterialButton` block:

```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnKofi"
    style="@style/Widget.Material3.Button.TextButton"
    android:layout_width="wrap_content"
    android:layout_height="40dp"
    android:text="@string/support_developer"
    android:textSize="13sp"
    android:textColor="#6F4E37"
    app:icon="@drawable/kofi_logo"
    app:iconSize="20dp"
    app:iconGravity="start"
    app:iconTint="@null"
    app:backgroundTint="#FFDD00"
    app:cornerRadius="999dp" />
```

> The empty flex `<View>` right above it (the `layout_weight="1"` spacer) —
> **keep it**. It stays to push the `N LEFT` counter (added in Step 7) to
> the trailing edge.

**1b. Kotlin.** Open `MainActivity.kt`. Around line 397 there's:

```kotlin
binding.btnKofi.setOnClickListener {
    ...
}
```

Delete the whole block (the listener declaration and its body). Search the
file for any other `btnKofi` reference to make sure nothing else points at
it (there shouldn't be any).

### Step 2 — Change the title color to ink

In `activity_main.xml`, find the `appTitle` TextView (around line 31). Change
`android:textColor="@color/header_title_green"` to `"@color/ink"`.

### Step 3 — Create filter-pill state-list color resources

These three files make the filters pill swap between "inactive" and "active"
looks when you set `isActivated = true` on it. We use Android's built-in
`android:state_activated` mechanism so the color change is one Kotlin line.

**3a.** Create `app/src/main/res/color/filter_pill_bg_tint.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/accent_soft" android:state_activated="true" />
    <item android:color="@android:color/transparent" />
</selector>
```

**3b.** Create `app/src/main/res/color/filter_pill_text_tint.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/accent_deep" android:state_activated="true" />
    <item android:color="@color/ink2" />
</selector>
```

**3c.** Create `app/src/main/res/color/filter_pill_stroke_tint.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/accent" android:state_activated="true" />
    <item android:color="@color/line_strong" />
</selector>
```

> **Why three files?** The MaterialButton properties `backgroundTint`,
> `textColor`, `iconTint`, and `strokeColor` each take a color state list.
> When the button's `isActivated` flag flips, all four follow.

### Step 4 — Rewire the Filters pill and remove the red dot

**4a. Layout.** In `activity_main.xml`, the Filters pill is inside a
`FrameLayout` around line 50. Replace that entire `FrameLayout` (including
the `filterActiveDot` View inside it) with just the MaterialButton below —
the frame was only there to position the dot over the button, and the dot
goes away:

```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnFilters"
    style="@style/Widget.Material3.Button.OutlinedButton"
    android:layout_width="wrap_content"
    android:layout_height="40dp"
    android:text="@string/filters"
    android:textSize="14sp"
    android:textColor="@color/filter_pill_text_tint"
    android:gravity="center"
    app:backgroundTint="@color/filter_pill_bg_tint"
    app:cornerRadius="20dp"
    app:strokeColor="@color/filter_pill_stroke_tint"
    app:strokeWidth="1.5dp"
    app:icon="@drawable/ic_filter_list"
    app:iconSize="18dp"
    app:iconTint="@color/filter_pill_text_tint"
    app:iconGravity="start" />
```

The style changed from `Widget.Material3.Button.TextButton` to
`Widget.Material3.Button.OutlinedButton` so `strokeColor` + `strokeWidth`
are honoured by the button chrome.

**4b. Kotlin.** Open `MainActivity.kt`, find `updateFiltersButtonAppearance`
(around line 495). Replace its body so it now just flips `isActivated`:

```kotlin
private fun updateFiltersButtonAppearance(isActive: Boolean) {
    binding.btnFilters.isActivated = isActive
}
```

You can delete the `ColorStateList` + `getColor` imports if they're only used
here (Android Studio's "Optimize Imports" handles this — `Ctrl+Alt+O` on
Windows).

**4c. Delete the dot drawable.** Remove the file:
`app/src/main/res/drawable/filter_active_dot.xml`.

### Step 5 — Shrink the Photos/Videos chips

In `activity_main.xml`, find the two `Chip` views (around lines 99–117).
For **each** chip, change:

- `android:layout_height="40dp"` → `android:layout_height="32dp"`
- Add `android:textSize="13sp"`
- `android:paddingStart="12dp"` and `android:paddingEnd="12dp"` stay as-is

You do not need to touch `style="@style/PillFilterChip"` — the chip colors
already follow the state lists updated in SDD-002.

### Step 6 — Thin the progress bar and switch to accent color

In `activity_main.xml`, find the `reviewProgressBar` (around line 172):

- `app:indicatorColor="@color/fast_scroll_tooltip_bg"` → `"@color/accent"`
- `app:trackThickness="3dp"` → `"2dp"`

`app:trackColor="#E0E0E0"` can stay (barely visible behind the fill) or
change to `@color/line` for consistency — your call. Keep the
`app:trackCornerRadius="2dp"` line.

### Step 7 — Add the N LEFT counter to the chip row

**7a. String.** In `app/src/main/res/values/strings.xml`, add:

```xml
<!-- Top bar (SDD-20260418-003) -->
<string name="n_left_counter">%1$d LEFT</string>
```

**7b. Layout.** In `activity_main.xml`, the chip row's `LinearLayout` (around
line 86) currently contains the `ChipGroup` and a flex `<View>` spacer (and,
before Step 1, the Ko-fi button). After the spacer, add the counter TextView:

```xml
<TextView
    android:id="@+id/unreviewedCounter"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:fontFamily="@font/jetbrains_mono"
    android:textSize="10sp"
    android:textColor="@color/ink4"
    android:letterSpacing="0.05"
    android:textAllCaps="true"
    android:textFontWeight="500"
    android:paddingEnd="4dp"
    tools:text="188 LEFT" />
```

> `android:textFontWeight` requires API 28+; minSdk here is 26. Android Studio
> may show a lint warning — fine, the attribute is ignored on older
> API levels and falls back to the family's regular weight.

Make sure the `tools` namespace is declared on the root of the XML (it
should already be — check the top `<androidx.constraintlayout…>` tag).

**7c. Kotlin.** Open `MainActivity.kt`. Find `observeViewedItems` (around
line 427). Inside the `.collect { (viewedItems, uiState) -> … }` block,
after the existing `val items = uiState.displayedItems` line, add:

```kotlin
// Update N LEFT counter
val unreviewedCount = items.count { it.uri !in viewedItems }
binding.unreviewedCounter.visibility =
    if (unreviewedCount > 0) View.VISIBLE else View.GONE
binding.unreviewedCounter.text =
    getString(R.string.n_left_counter, unreviewedCount)
```

> We hide the counter when everything has been reviewed — an empty "0 LEFT"
> read as negative UX.

### Step 8 — Fast-scroller restyle

**8a. Thumb.** Replace `app/src/main/res/drawable/fast_scroll_thumb.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <corners android:radius="999dp" />
    <solid android:color="@color/fast_scroll_thumb" />
    <size android:width="6dp" />
</shape>
```

> Only the `android:radius` changed: `4dp` → `999dp`. Visual shape goes from
> rounded rectangle to a full pill.

**8b. Tooltip text.** In `activity_main.xml`, find `fastScrollTooltip`
(around line 222). Change:

- `android:textSize="14sp"` → `"10sp"`
- Remove `android:textStyle="bold"`
- Add `android:fontFamily="@font/jetbrains_mono"`
- Add `android:textFontWeight="600"`
- Add `android:letterSpacing="0.1"`
- Add `android:textAllCaps="true"`

The tooltip background colors come from `fast_scroll_tooltip_bg` (already
ink via SDD-002's remap) — no drawable change needed.

### Step 9 — Legacy cleanup

Now that nothing references these color names, remove them from
`app/src/main/res/values/colors.xml`. Find and delete these three lines:

```xml
<color name="header_title_green">#1D1710</color>
...
<color name="filter_btn_active_bg">#E85A3D</color>
<color name="filter_btn_active_text">#FFFFFF</color>
```

> **Do NOT** delete `badge_unviewed_bg`, `badge_unviewed_text`,
> `filter_btn_bg`, `filter_btn_text`, `fast_scroll_*`, or any other legacy
> color names. Those are still referenced elsewhere (grid tile badges, fast
> scroller, etc.) and belong to later SDDs.

**Sanity check** — from the project root, search to confirm nothing still
references the three you just removed:

```bash
grep -r "header_title_green\|filter_btn_active_bg\|filter_btn_active_text" app/
```

Expected: no matches. If any show up, undo the deletion of that specific
color and figure out what's still using it.

### Step 10 — Build and verify

```bash
./gradlew clean assembleDebug testDebugUnitTest lint
```

If the build breaks on a "resource not found" error, Step 9 removed a color
that is still referenced. Add it back and re-run.

### Step 11 — Visual smoke test on-device

Install. Verify on the main grid screen:

- **Title** reads in near-black ink, not green.
- **Only two things** right of the title: help icon and the Filters pill
  (Ko-fi gone).
- Open a filter (e.g. Source → deselect one), close the sheet. The
  **Filters pill** should go from neutral outline to **soft-orange fill with
  an accent outline** — no red dot anywhere. Re-reset filters to default;
  the pill returns to neutral.
- **Photos/Videos chips** look noticeably shorter and less wordy than before.
- **Progress bar** at the top of the grid is a **hair-thin warm-orange line**
  (not thick teal).
- **`N LEFT` counter** appears right-aligned on the chip row (e.g.
  `188 LEFT`) in what looks like code-editor font. Delete some items or
  tap through the viewer → counter decrements live.
- **Drag the right edge** to fast-scroll. The thumb is a rounded pill (not a
  stubby rectangle); the date tooltip reads `APR 12` style in mono font.

## Definition of Done

- [ ] All changes in the Files Changed table of `requirement.md` landed
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [ ] No references left in `app/` to `header_title_green`,
      `filter_btn_active_bg`, or `filter_btn_active_text`
- [ ] `drawable/filter_active_dot.xml` file removed from disk
- [ ] Visual smoke test passes (Step 11)
- [ ] PR opened with title `SDD-20260418-003 — Top Bar Redesign`

## Known gotchas

- **OutlinedButton enforces a minimum horizontal padding** that can make the
  pill look wider than you want. If it looks bloated, add
  `android:insetLeft="0dp"` and `android:insetRight="0dp"` to the button.
- **`android:textFontWeight` and `android:letterSpacing`** require API 21+
  and API 21+ respectively (we target 26+). Safe to use.
- **MaterialButton with `isActivated`** sometimes doesn't repaint when you
  set it from code the first time. If the active state sticks in "inactive"
  look on first filter change, add `binding.btnFilters.invalidate()` right
  after the `isActivated = ...` line.
- **tools:text warnings**: `tools:text="188 LEFT"` only shows in Layout
  Editor previews — it never ships. Ignore Android Studio's "hardcoded
  text" warning for it.
