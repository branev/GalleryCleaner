# SDD-20260419-001: Bump minSdk to 30

**Parent:** none (standalone tooling SDD, unrelated to the visual redesign)

## Summary

Raise the app's `minSdk` from **26** (Android 8.0) to **30** (Android 11)
and clean up the now-unnecessary compat shims and version-gated branches
that the bump makes obsolete.

## Why

- The visual redesign surfaced three compat dances we wouldn't have
  needed at `minSdk=30`: `CustomTypefaceSpan` (to dodge `TypefaceSpan(Typeface)`,
  API 28+), and two uses of `WindowCompat.getInsetsController` just to
  access `window.insetsController` (API 30+).
- The existing code already has several `if (Build.VERSION.SDK_INT >= 29)`
  and `>= Build.VERSION_CODES.R` (API 30) branches whose `else` arms are
  dead code at a new floor of 30.
- Android 11 is ~5.5 years old. Devices that don't support it are a
  dwindling minority (~5-10%) and are unlikely targets for a gallery-
  cleanup app.
- No functional benefit to users lost by bumping — we're only shedding
  support, not adding features. Those features (e.g. smarter insets,
  better media permissions) come for free in later SDDs if we want
  them.

## What this SDD does NOT do

- **Does not bump `targetSdk`** (already at 35 — fine).
- **Does not bump `compileSdk`** (already at 35 — fine).
- **Does not remove API-33 version checks** (`TIRAMISU`, READ_MEDIA_*).
  Those are still above our new floor and must stay.
- **Does not touch `ViewCompat.setOnApplyWindowInsetsListener` or
  `ContextCompat.checkSelfPermission`** — the compat versions use the
  more ergonomic `WindowInsetsCompat` / `Context` types and cost nothing.
- **Does not add any new features** enabled by the API floor (e.g.
  predictive back, themed icons, conversation shortcuts). Out of scope.

## Scope

1. **`app/build.gradle.kts`**: `minSdk = 26` → `minSdk = 30`.
2. **Delete `CustomTypefaceSpan.kt`**. Replace its one usage in
   `FilterBottomSheetFragment.kt` with
   `android.text.style.TypefaceSpan(typeface)` directly (API 28+).
3. **`FilterBottomSheetFragment.kt`** status-bar flip: replace the two
   `WindowCompat.getInsetsController(w, w.decorView).isAppearanceLightStatusBars = ...`
   calls with `w.insetsController?.setSystemBarsAppearance(...)`
   directly. Drop the `androidx.core.view.WindowCompat` import.
4. **`GalleryViewModel.kt`**: four `if (Build.VERSION.SDK_INT >= 29)`
   checks (lines 359, 364, 383, 412) become always-true. Delete the
   conditionals and their `else` branches; keep only the modern branch.
5. **`MainActivity.kt`**: three `if (Build.VERSION.SDK_INT >=
   Build.VERSION_CODES.R)` checks (lines 770, 805, 838) become
   always-true. Delete the conditionals and their `else` branches.
   (Keep the `>= 33` checks for media permissions as-is.)
6. **`README.md`**: update the "Requirements" line from "Android 8.0
   (API 26) or higher" to "Android 11 (API 30) or higher".
7. **Lint baseline sanity check**: re-run lint; confirm no new
   warnings appeared and that the `NewApi` suppressions / workarounds
   we had in flight are gone.

## Not in scope (explicitly left for later)

- Replacing `WindowCompat.setDecorFitsSystemWindows(window, false)`
  with `window.setDecorFitsSystemWindows(false)`. The compat wrapper
  compiles to the same call at minSdk=30 and the direct API loses
  nothing. Churn for no gain.
- Replacing `ViewCompat.setOnApplyWindowInsetsListener` with
  `view.setOnApplyWindowInsetsListener` directly. The compat version
  uses `WindowInsetsCompat`, which exposes type-safe accessors
  (`Type.systemBars()`, `Type.displayCutout()`) that the raw platform
  API doesn't. Keep.
- Reviewing every `@Suppress("DEPRECATION")` annotation in the code
  base. Some are about `var statusBarColor` (API 35+ deprecation,
  unrelated).

## Files changed

| File | Change |
|------|--------|
| `app/build.gradle.kts` | `minSdk = 26` → `30` |
| `CustomTypefaceSpan.kt` | **Deleted** |
| `FilterBottomSheetFragment.kt` | Use `TypefaceSpan(typeface)` directly; swap `WindowCompat` for `window.insetsController.setSystemBarsAppearance(...)`; drop import |
| `GalleryViewModel.kt` | Collapse 4 `SDK_INT >= 29` conditionals |
| `MainActivity.kt` | Collapse 3 `SDK_INT >= R` conditionals |
| `README.md` | Requirements line update |

## Acceptance criteria

- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds.
- [ ] `minSdk=30` in the merged AGP output (`assembleDebug` APK's
      `AndroidManifest.xml` shows `uses-sdk android:minSdkVersion="30"`).
- [ ] No references left to `CustomTypefaceSpan`.
- [ ] No `WindowCompat.getInsetsController` calls in the app (the
      MainActivity `WindowCompat.setDecorFitsSystemWindows` stays).
- [ ] No `Build.VERSION.SDK_INT >= 29` or
      `>= Build.VERSION_CODES.R` branches remain.
- [ ] App launches on an Android 11+ device, basic flows work
      (grid scroll, filter sheet open/close with status-bar flip,
      selection mode, delete + undo).
- [ ] `README.md` reflects the new minimum.

## Task breakdown

- **[task-01-bump-min-sdk.md](task-01-bump-min-sdk.md)** — single task,
  small scope. ~7 steps, pure mechanical cleanup.
