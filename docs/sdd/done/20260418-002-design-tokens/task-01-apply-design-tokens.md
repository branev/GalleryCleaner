# Task 01: Apply Design Tokens

**Parent:** SDD-20260418-002 — Design Tokens

## What You're Changing

You're laying the foundation for a full visual redesign. This task swaps the
app's color palette to a new warm-orange-and-ink system, adds a monospace
font for numeric readouts, and wires the new values into the Material 3
theme.

You will **only touch resource files** — no Kotlin, no layouts. After this
task the app still builds and runs. Colors on some screens will look
different (title becomes ink instead of green, teal becomes orange), but
nothing should break or crash.

## Before vs After

| | Before | After |
|---|---|---|
| Primary color | teal `#006874` | warm orange `#E85A3D` |
| Window background | white `#FFFFFF` | cream `#FAF6EF` |
| App title color | green `#1B5E20` | ink `#1D1710` |
| Chip selected bg | light teal `#E0F2F1` | ink `#1D1710` |
| Chip selected text | teal `#00796B` | white |
| Fonts available | Inter only | Inter + JetBrains Mono |

## Prerequisites

- Android Studio with the project open
- Internet access (to download JetBrains Mono)

## Step-by-Step Instructions

### Step 1 — Download JetBrains Mono

1. Open https://github.com/JetBrains/JetBrainsMono/releases/latest in a
   browser.
2. Download the asset named `JetBrainsMono-*.zip` (not the source tarball).
3. Extract the zip. You'll see a `fonts/` directory inside.
4. Navigate into `fonts/ttf/`. Locate these four files:
   - `JetBrainsMono-Regular.ttf`
   - `JetBrainsMono-Medium.ttf`
   - `JetBrainsMono-SemiBold.ttf`
   - `JetBrainsMono-Bold.ttf`
5. Also grab `OFL.txt` from the zip root — you'll need its contents in
   Step 4.

> **Why these four weights?** The design uses Inter at weights 400/500/600/
> 700. To keep the type system consistent, JetBrains Mono ships the same
> four weights. Other weights (Thin, Light, ExtraBold) are not needed.

### Step 2 — Add TTFs to the project

Android resource filenames must be lowercase with underscores. Place the
four TTFs in `app/src/main/res/font/` with these exact names:

- `app/src/main/res/font/jetbrains_mono_regular.ttf`
- `app/src/main/res/font/jetbrains_mono_medium.ttf`
- `app/src/main/res/font/jetbrains_mono_semibold.ttf`
- `app/src/main/res/font/jetbrains_mono_bold.ttf`

**Verify:** the folder should already contain Inter TTFs (`inter_regular.ttf`,
etc.). Your new files sit alongside them.

### Step 3 — Create the font family XML

Create `app/src/main/res/font/jetbrains_mono.xml` with this content. It
mirrors the existing `inter.xml` pattern:

```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <font
        android:font="@font/jetbrains_mono_regular"
        android:fontStyle="normal"
        android:fontWeight="400"
        app:font="@font/jetbrains_mono_regular"
        app:fontStyle="normal"
        app:fontWeight="400" />

    <font
        android:font="@font/jetbrains_mono_medium"
        android:fontStyle="normal"
        android:fontWeight="500"
        app:font="@font/jetbrains_mono_medium"
        app:fontStyle="normal"
        app:fontWeight="500" />

    <font
        android:font="@font/jetbrains_mono_semibold"
        android:fontStyle="normal"
        android:fontWeight="600"
        app:font="@font/jetbrains_mono_semibold"
        app:fontStyle="normal"
        app:fontWeight="600" />

    <font
        android:font="@font/jetbrains_mono_bold"
        android:fontStyle="normal"
        android:fontWeight="700"
        app:font="@font/jetbrains_mono_bold"
        app:fontStyle="normal"
        app:fontWeight="700" />

</font-family>
```

**Verify:** In Android Studio, right-click `app/src/main/res/font/` → View →
Preview should show both `inter` and `jetbrains_mono` entries.

### Step 4 — Add the OFL license notice

The project already ships Inter under the SIL Open Font License but doesn't
currently have a notices file. Create one at the project root:
`THIRD_PARTY_NOTICES.md`.

Use this structure. Copy the OFL license text from the `OFL.txt` file you
downloaded in Step 1 for both sections (the license text is identical; only
the copyright line differs):

```markdown
# Third-Party Notices

This app includes the following third-party assets. Their licenses are
reproduced in full below.

---

## Inter

Copyright 2016 The Inter Project Authors
(https://github.com/rsms/inter)

This Font Software is licensed under the SIL Open Font License, Version 1.1.

<paste the full OFL text here — from Inter's OFL.txt, available at
 https://github.com/rsms/inter/blob/master/LICENSE.txt>

---

## JetBrains Mono

Copyright 2020 The JetBrains Mono Project Authors
(https://github.com/JetBrains/JetBrainsMono)

This Font Software is licensed under the SIL Open Font License, Version 1.1.

<paste the full OFL text here — from the OFL.txt in the JetBrains Mono zip>
```

Then add one line to `README.md` — put it right under the "Inter font"
bullet under "Tech Stack":

```markdown
- See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for font licenses
```

> **Why a Markdown file and not an in-app screen?** This project doesn't
> have a settings screen or licenses activity yet. A root-level notices
> file is the minimum OFL obligation and keeps scope tight. A future SDD
> can surface it in-app.

### Step 5 — Rewrite `colors.xml`

Replace the contents of `app/src/main/res/values/colors.xml` with the block
below. This does three things:

1. Declares the 17 new Direction B tokens (neutral, ink, accent, semantic).
2. Points every `md_theme_*` Material 3 role at the new tokens.
3. Remaps every legacy color name (like `header_title_green` or
   `badge_unviewed_bg`) to a Direction B approximation, so existing layouts
   keep working without layout edits.

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- ============================================================
         Direction B palette (SDD-20260418-002)
         Source of truth for every color in the app.
         ============================================================ -->

    <!-- Neutrals -->
    <color name="bg">#FAF6EF</color>
    <color name="surface">#FFFDF8</color>
    <color name="surface_alt">#F3ECDF</color>
    <color name="line">#E8DFCE</color>
    <color name="line_strong">#D4C6AD</color>

    <!-- Ink scale (primary/secondary/tertiary/muted text) -->
    <color name="ink">#1D1710</color>
    <color name="ink2">#4A3E2E</color>
    <color name="ink3">#77674F</color>
    <color name="ink4">#A8957B</color>

    <!-- Accent family (warm orange) -->
    <color name="accent">#E85A3D</color>
    <color name="accent_ink">#FFFFFF</color>
    <color name="accent_soft">#FBE0D5</color>
    <color name="accent_deep">#A83320</color>

    <!-- Semantic -->
    <color name="danger">#C8342B</color>
    <color name="danger_soft">#F8DDDB</color>
    <color name="success">#2E7D5B</color>

    <!-- ============================================================
         Material 3 role bindings — every attribute in themes.xml
         maps to one of these names.
         ============================================================ -->
    <color name="md_theme_primary">#E85A3D</color>
    <color name="md_theme_onPrimary">#FFFFFF</color>
    <color name="md_theme_primaryContainer">#FBE0D5</color>
    <color name="md_theme_onPrimaryContainer">#A83320</color>

    <color name="md_theme_secondary">#4A3E2E</color>
    <color name="md_theme_onSecondary">#FFFFFF</color>
    <color name="md_theme_secondaryContainer">#F3ECDF</color>
    <color name="md_theme_onSecondaryContainer">#1D1710</color>

    <color name="md_theme_tertiary">#77674F</color>
    <color name="md_theme_onTertiary">#FFFFFF</color>
    <color name="md_theme_tertiaryContainer">#F3ECDF</color>
    <color name="md_theme_onTertiaryContainer">#1D1710</color>

    <color name="md_theme_error">#C8342B</color>
    <color name="md_theme_onError">#FFFFFF</color>
    <color name="md_theme_errorContainer">#F8DDDB</color>
    <color name="md_theme_onErrorContainer">#7A1A12</color>

    <color name="md_theme_background">#FAF6EF</color>
    <color name="md_theme_onBackground">#1D1710</color>
    <color name="md_theme_surface">#FFFDF8</color>
    <color name="md_theme_onSurface">#1D1710</color>
    <color name="md_theme_surfaceVariant">#F3ECDF</color>
    <color name="md_theme_onSurfaceVariant">#4A3E2E</color>

    <color name="md_theme_outline">#D4C6AD</color>
    <color name="md_theme_outlineVariant">#E8DFCE</color>

    <!-- ============================================================
         Legacy color names — used by existing layouts today.
         Every name here will be deleted by a later SDD (003–014)
         once its surface has migrated to the tokens above.
         Do NOT remove any of these in this task, even if unused
         locally — grep across layouts before deleting.
         ============================================================ -->
    <color name="viewer_toolbar_bg">#99000000</color>

    <color name="header_title_green">#1D1710</color>
    <color name="filter_btn_bg">#FBE0D5</color>
    <color name="filter_btn_text">#A83320</color>
    <color name="chip_selected_bg">#1D1710</color>
    <color name="chip_selected_text">#FFFFFF</color>
    <color name="chip_unselected_border">#E8DFCE</color>
    <color name="chip_unselected_text">#4A3E2E</color>

    <color name="badge_unviewed_bg">#FBE0D5</color>
    <color name="badge_unviewed_text">#A83320</color>
    <color name="badge_viewed_bg">#D9FFFDF8</color>
    <color name="badge_viewed_text">#1D1710</color>

    <color name="fab_continue_bg">#1D1710</color>

    <color name="empty_state_icon_bg">#F3ECDF</color>
    <color name="empty_state_icon_color">#77674F</color>
    <color name="empty_state_subtitle_color">#77674F</color>
    <color name="empty_state_reset_btn_bg">#1D1710</color>
    <color name="empty_state_reset_btn_text">#FFFFFF</color>

    <color name="filter_btn_active_bg">#E85A3D</color>
    <color name="filter_btn_active_text">#FFFFFF</color>

    <color name="fast_scroll_track">#00000000</color>
    <color name="fast_scroll_thumb">#CC1D1710</color>
    <color name="fast_scroll_tooltip_bg">#1D1710</color>

    <color name="overlay_bg">#99000000</color>
    <color name="success_checkmark">#2E7D5B</color>

    <color name="hint_tooltip_bg">#1D1710</color>

    <!-- Legacy AndroidX defaults — leave alone -->
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
```

> **Why literal hex values in the legacy block and not `@color/accent`
> aliases?** XML color references to other `@color/` entries work in many
> places but cause issues in a few (color state lists, some drawables).
> Using hex values everywhere keeps this foundation task dependency-free.

### Step 6 — Update `themes.xml`

Open `app/src/main/res/values/themes.xml`. You only need to change **one
line** — the status bar color:

Find:
```xml
<item name="android:statusBarColor">@android:color/white</item>
```

Replace with:
```xml
<item name="android:statusBarColor">@color/bg</item>
```

Everything else in this file already resolves correctly because the
`md_theme_*` color names now point to Direction B values.

**Do NOT change** `android:windowLightStatusBar` — keep it `true`. The new
background is light, so system icons should still be dark.

### Step 7 — Update chip color state lists

These three files control how filter chips look based on their checked
state. The new logic: **ink-filled when checked, outlined with line-color
when not**.

Replace `app/src/main/res/color/chip_bg_color.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/ink" android:state_checked="true" />
    <item android:color="@android:color/transparent" android:state_checked="false" />
</selector>
```

Replace `app/src/main/res/color/chip_text_color.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@android:color/white" android:state_checked="true" />
    <item android:color="@color/ink2" android:state_checked="false" />
</selector>
```

Replace `app/src/main/res/color/chip_stroke_color.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/ink" android:state_checked="true" />
    <item android:color="@color/line" android:state_checked="false" />
</selector>
```

### Step 8 — Build and verify

Run these three commands in order. All must succeed.

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lint
```

**If `assembleDebug` fails** with `AAPT: error: resource color/xyz not found`:
- The Step 5 `colors.xml` is missing a legacy color name that a layout
  references. Find the offending layout, find the missing name, add it to
  the legacy block in `colors.xml` mapped to a sensible Direction B color.

**If `lint` reports new errors** (warnings are fine, only errors block):
- Post the output in your PR — we'll decide together whether to fix or
  suppress.

### Step 9 — Visual smoke test on-device

Install and launch the app. Verify:

- Window background is warm cream, not pure white.
- App title "Gallery Cleaner" is near-black, not green.
- Filters button background is soft orange, text is deep orange.
- Photos/Videos chips: when selected, ink-black background with white text.
  When unselected, transparent background with subtle line-color outline.
- Status bar is cream-colored at the top; time/battery icons are dark.
- No crashes. No missing-image placeholders. The app behaves the same
  as before — only colors have shifted.

> Some screens won't look *great* yet — the filter pill active state is
> crude, the grid tile badges are still large, the FAB is still teal-ish.
> That's expected. Each follow-up SDD (003–014) polishes one surface.

## Definition of Done

- [x] All nine files changed per the table in `requirement.md`
- [x] `./gradlew clean assembleDebug testDebugUnitTest lint` runs clean
- [x] App launches, no crashes, no missing resources at runtime
- [x] Visual smoke test (Step 9) matches expectations
- [x] `THIRD_PARTY_NOTICES.md` exists with both full OFL texts
- [x] PR opened against `master` with title
      `SDD-20260418-002 — Design Tokens`

## Known gotchas

- **"Preview gone wrong" in Android Studio**: the Layout Editor caches
  resources. If a preview shows weird colors after you edit `colors.xml`,
  click the refresh icon in the preview toolbar.
- **OFL text line endings**: when pasting from `OFL.txt`, make sure line
  endings stay as `LF` (not `CRLF`). `git status` will flag the file if
  this is wrong.
- **Missing hex digits**: colors like `#D9FFFDF8` (alpha + rgb) have 8
  digits, not 6. Double-check any you type manually.
- **Do NOT delete any legacy color names** even if they look unused — they
  may be referenced by drawables you haven't opened. Later SDDs remove
  them one by one as each surface migrates.
