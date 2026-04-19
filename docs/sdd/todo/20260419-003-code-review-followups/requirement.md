# SDD-20260419-003: Code Review Follow-ups

**Depends on:** SDD-20260418-013 ✓, SDD-20260418-014 ✓, SDD-20260419-002 ✓
(the new Thread and swallowed-exception patterns were introduced by
the viewer/info/confetti work; this SDD cleans them up)

## Summary

Quality pass on six reviewer-flagged issues. All changes are invisible
to the user; the wins are correctness (lifecycle-aware cancellation),
maintainability (no `!!`, actionable logs), and coherence with the
rest of the codebase (coroutines everywhere). No behavior changes,
no token changes, no layout changes.

## Issues being fixed

### Tier 1 — coroutines migration (coupled: 1, 2, 5)

These three must land together. `runCatching { ... }.getOrNull()` is
safe today because we're on raw `Thread`. The moment the thread
becomes a coroutine, `runCatching` swallows `CancellationException`
and silently breaks structured cancellation — worse than doing
nothing.

**1. Replace `Thread {}` in `MediaInfoBottomSheetFragment.loadExtraMetadata`**
- Use `viewLifecycleOwner.lifecycleScope.launch { withContext(Dispatchers.IO) { ... } }`.
- Removes the `activity?.runOnUiThread { if (_binding != null) ... }`
  guard — `viewLifecycleOwner.lifecycleScope` cancels when the view is
  destroyed.

**2. Replace `Thread {}` in `MediaViewerActivity.loadVideoPoster`**
- Use `lifecycleScope.launch { withContext(Dispatchers.IO) { ... } }`.
- Removes the `isFinishing && !isDestroyed` check — `lifecycleScope`
  cancels when the activity is destroyed.

**5. Replace `runCatching` with explicit `try/catch` in both files**
- `MediaViewerActivity.extractVideoPoster`: catch `IOException` +
  `SecurityException` for `loadThumbnail`; catch
  `IllegalArgumentException` + `RuntimeException` for the
  `MediaMetadataRetriever` path. MMR's contract throws undocumented
  `RuntimeException` on malformed streams so a broad catch is
  justified, but `CancellationException` extends `RuntimeException`
  (via `IllegalStateException`) — a naive `catch (RuntimeException)`
  would swallow coroutine cancellation. Rethrow it explicitly:
  `catch (e: RuntimeException) { if (e is CancellationException) throw e; ... }`.
- `MediaInfoBottomSheetFragment.loadExtraMetadata`: catch
  `SecurityException` + `IllegalArgumentException` for the
  `contentResolver.query`.

### Tier 2 — isolated cleanup (3, 4, 6)

**3. `days!!` force-unwrap at `GalleryViewModel.kt:483`**
```kotlin
else -> Pair(now - (dateRange.preset.days!! * 24 * 60 * 60L), now)
```
`days` is nullable only for `CUSTOM` (handled earlier) and `ALL_TIME`
(handled one branch up), so the `!!` is logically safe. Compiler
can't prove it. Fix: `?: return Pair(0L, now)` defensive fallback
is the 1-line change.

**4. Silent `catch (_: Exception)` in `MediaViewerActivity.extractVideoPoster`**
Re-add a `Log.w("MediaViewer", "poster MMR failed", e)` line so
future debugging has signal. The debug-level `Log.d` lines we
stripped for release are different — this is a warn on actual
failure, which happens rarely and is actionable.

**6. `scrollListener!!` force-unwrap at `FastScrollHelper.kt:42`**
```kotlin
scrollListener = object : RecyclerView.OnScrollListener() { ... }
recyclerView.addOnScrollListener(scrollListener!!)
```
Local-val pattern — assign to a `val`, add as listener, assign to
field. Compiler proves non-null.

## Why all in one SDD

Six small, atomic, reviewer-flagged diffs in four files. Splitting
into three SDDs would be pure paperwork; the coupling between 1/2/5
makes a single commit the only safe landing.

## Scope

1. **`MediaInfoBottomSheetFragment.kt`**: `loadExtraMetadata` rewritten
   using `viewLifecycleOwner.lifecycleScope.launch { ... }` and
   `withContext(Dispatchers.IO)`; `runCatching` replaced with explicit
   `try/catch (e: SecurityException)` + `(e: IllegalArgumentException)`;
   lifecycle guard removed (implicit in cancellation).
2. **`MediaViewerActivity.kt`**: `loadVideoPoster` rewritten using
   `lifecycleScope.launch { ... }`; `extractVideoPoster` (still called
   from within the scope) uses explicit `try/catch` per path with a
   `Log.w` on MMR failure; `isFinishing && !isDestroyed` guard removed.
3. **`GalleryViewModel.kt`**: the `days!!` branch gets `?:
   return Pair(0L, now)` or equivalent defensive fallback.
4. **`FastScrollHelper.kt`**: local-val pattern replaces the
   `scrollListener!!` force-unwrap.

## Not in scope

- Any other `!!` or silent catch in the codebase. This SDD closes out
  the reviewer's list only; a broader audit is its own pass.
- Converting `progressHandler` + `Runnable` video-scrubber loop to
  coroutines. That code pre-dates this SDD and works; leaving alone.
- Tests for the coroutine conversions. The behavior is unchanged and
  existing `ConfettiViewTest` / `FilterComboFormatterTest` don't
  exercise the changed paths.

## Files changed

| File | Change |
|------|--------|
| `MediaInfoBottomSheetFragment.kt` | Coroutines migration + explicit try/catch |
| `MediaViewerActivity.kt` | Coroutines migration + explicit try/catch + Log.w on MMR fail |
| `GalleryViewModel.kt` | `days!!` → safe fallback |
| `FastScrollHelper.kt` | `scrollListener!!` → local val |

## Acceptance criteria

- [ ] No `Thread {` construction in `main/java/**`.
- [ ] No `runCatching { ... }.getOrNull()` in
      `MediaViewerActivity.kt` or `MediaInfoBottomSheetFragment.kt`.
- [ ] No `!!` force-unwrap in `GalleryViewModel.kt` or
      `FastScrollHelper.kt`.
- [ ] `Log.w("MediaViewer", ...)` fires when `MediaMetadataRetriever`
      throws (verified via deliberate fail — e.g. pass a non-media
      URI in a one-off debug build).
- [ ] Media info sheet Resolution + Captured still populate after
      the short async load.
- [ ] Media viewer video poster still appears on all videos.
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds.
- [ ] On-device: opening the viewer on a video, navigating back
      mid-load, and opening a different video does NOT produce any
      "view destroyed" crash or stale bitmap.

## Task breakdown

- **[task-01-review-followups.md](task-01-review-followups.md)** —
  single task, ~5 steps: file-by-file edits, build, lint, on-device
  spot check.
