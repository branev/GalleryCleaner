# Task 01: Continue FAB States

**Parent:** SDD-20260418-007 — Continue FAB States

> **As-shipped note:** during implementation we reviewed the UX and
> decided to **only** render the active state and hide the FAB in every
> other case (instead of showing a "ghost" state when the user is past
> the first unreviewed tile). The design's two-state treatment only
> makes sense when the user has reviewed *every* item in the current
> filter, which is rare in a gallery cleaner. The ghost state is
> deferred — the helper code still has the branch commented-style so
> it's easy to re-enable. The steps below have been updated to reflect
> what actually shipped.

## What You're Changing

The Continue FAB today has one look — solid ink pill — and fades to 50%
alpha when you're already at the bottom of the unreviewed queue. You're
replacing that half-dimmed state with a clean hide: the FAB only
appears when tapping it would actually jump the user somewhere useful.

## Before vs After

| Element | Before | After |
|---|---|---|
| Active (user scrolled above the first unreviewed) | ink pill, white "Continue reviewing", elevation 6dp | ink pill, white **"Continue"**, elevation 6dp |
| User is at/past first unreviewed | same pill at `alpha=0.5`, `isEnabled=false` | **hidden** (visibility `GONE`) |
| All items reviewed | hidden | **still hidden** (ghost state deferred) |

## Prerequisites

- SDD-002 (Design Tokens) merged — the palette used by both states is
  already wired.
- `./gradlew --stop` before you start.

## Step-by-Step Instructions

### Step 1 — Add the new string resource

Open `app/src/main/res/values/strings.xml`. Find `continue_reviewing`
(around line 75) and add `fab_continue` after it (the old string stays
for now — we remove it in Step 5 after the layout and code have
stopped using it):

```xml
<string name="continue_reviewing">Continue reviewing</string>
<string name="fab_continue">Continue</string>
```

> **Why keep `continue_reviewing` temporarily?** Some code paths may
> still reference it until Step 4 lands. Remove at the end of Step 5.

### Step 2 — Trim the FAB layout

Open `app/src/main/res/layout/activity_main.xml` and find the
`ExtendedFloatingActionButton` with `android:id="@+id/fabContinue"`
(around line 329). Replace its whole block with:

```xml
<!-- Continue Extended FAB (scroll to first unviewed item) -->
<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
    android:id="@+id/fabContinue"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_margin="24dp"
    android:text="@string/fab_continue"
    android:visibility="gone"
    app:backgroundTint="@color/ink"
    app:icon="@drawable/ic_arrow_downward"
    app:strokeWidth="1dp"
    app:shapeAppearanceOverlay="@style/PillShape"
    app:layout_constraintBottom_toTopOf="@id/selectionActionBar"
    app:layout_constraintEnd_toEndOf="parent" />
```

> **What changed:**
> - `android:text` swapped from `continue_reviewing` to `fab_continue`
> - `android:contentDescription` removed (ExtendedFAB announces its
>   visible text via TalkBack automatically — redundant)
> - Hard-coded `android:textColor="@android:color/white"` and
>   `app:iconTint="@android:color/white"` removed — set from Kotlin
> - `app:strokeWidth="1dp"` added — reserved for a future ghost state;
>   set transparent in Kotlin for the active state so it's invisible
> - `app:shapeAppearanceOverlay="@style/PillShape"` added — once you
>   specify `strokeWidth`, ExtendedFAB falls off its default pill
>   shape and renders slightly rectangular; this overlay (already
>   defined in `themes.xml`) pins the corners back to a true pill

### Step 3 — Make `updateContinueFabState` own visibility + state

Open `MainActivity.kt` and find `updateContinueFabState` (around line
417). Replace the whole method with the version below. It becomes the
single authority on FAB visibility: hide in every case where tapping
wouldn't help, show the active ink pill otherwise.

```kotlin
/**
 * Continue FAB is shown only when tapping it is useful:
 * - there's at least one unreviewed item ahead of the last visible row,
 * - and the app is in a state where the grid is actually displayed.
 * In every other case the FAB hides. The design's "All caught up" ghost
 * state is reserved for when every item is reviewed — not implemented
 * here because in practice users delete as they go.
 */
private fun updateContinueFabState(layoutManager: GridLayoutManager) {
    val state = viewModel.uiState.value
    val isGridShown = state is GalleryUiState.Normal || state is GalleryUiState.Selection
    val viewedItems = viewModel.viewedItems.value
    val mediaItems = adapter.mediaItemsInOrder()

    if (!isGridShown || viewedItems.isEmpty() || mediaItems.isEmpty()) {
        binding.fabContinue.visibility = View.GONE
        return
    }

    val firstUnviewedMediaIndex = viewModel.getFirstUnviewedIndex(mediaItems)
    if (firstUnviewedMediaIndex < 0) {
        binding.fabContinue.visibility = View.GONE
        return
    }

    val targetUri = mediaItems[firstUnviewedMediaIndex].uri
    val gridPosition = adapter.currentList.indexOfFirst {
        it is GridItem.Media && it.item.uri == targetUri
    }
    val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

    if (gridPosition > lastVisiblePosition) {
        val fab = binding.fabContinue
        fab.visibility = View.VISIBLE
        fab.isEnabled = true
        fab.setText(R.string.fab_continue)
        fab.backgroundTintList = ColorStateList.valueOf(getColor(R.color.ink))
        fab.setTextColor(getColor(android.R.color.white))
        fab.iconTint = ColorStateList.valueOf(getColor(android.R.color.white))
        fab.strokeColor = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        fab.elevation = 6.dp().toFloat()
    } else {
        binding.fabContinue.visibility = View.GONE
    }
}
```

Add the import near the top of the file if it isn't already present:

```kotlin
import android.content.res.ColorStateList
```

Also update `observeViewedItems` so it stops toggling `fabContinue`
visibility on its own — let `updateContinueFabState` own that. Find
the block that does `binding.fabContinue.visibility = View.VISIBLE / GONE`
(around line 481) and replace it with:

```kotlin
// Refresh Continue FAB (updateContinueFabState owns visibility + state)
val layoutManager = binding.recyclerView.layoutManager as? GridLayoutManager
layoutManager?.let { updateContinueFabState(it) }

// Show the hint only the first time the FAB actually appears
if (binding.fabContinue.visibility == View.VISIBLE) {
    hintManager.showHint(
        HintPreferences.HINT_CONTINUE_FAB,
        getString(R.string.hint_continue_fab)
    )
}
```

> **Why programmatic styling?** The ExtendedFAB's `app:strokeWidth`
> and `app:elevation` attributes don't accept state lists — only
> `backgroundTint`, `textColor`, `iconTint`, and `strokeColor` do.
> Kotlin keeps the whole active-state setup in one block, easy to
> reason about, and lets us fall back to `visibility = GONE` cleanly
> for every non-active case.

### Step 4 — Point the FAB to the new default text

In `MainActivity.kt`, search for any leftover `R.string.continue_reviewing`
reference:

```bash
grep -rn "continue_reviewing" app/
```

You should see hits in:
- `app/build/...` — stale generated code, ignore (regenerates on next
  build)
- No source files — if any appear, update them to `fab_continue` or
  `fab_all_caught_up` as appropriate.

### Step 5 — Remove the legacy string

Return to `app/src/main/res/values/strings.xml` and delete the line
you kept from Step 1:

```xml
<string name="continue_reviewing">Continue reviewing</string>
```

### Step 6 — Build and verify

```bash
./gradlew --stop
./gradlew clean assembleDebug testDebugUnitTest lint
```

**Expected:** `BUILD SUCCESSFUL`. Two pre-existing `statusBarColor`
deprecation warnings are fine — they have nothing to do with this task.

Likely failures:
- `Unresolved reference: continue_reviewing` → a Kotlin caller still
  refers to the old string. Grep and fix.
- `Unresolved reference: R.color.line_strong` / `ink4` → SDD-002 wasn't
  merged yet, or the tokens were renamed locally. Verify the two
  color names exist in `colors.xml`.
- ExtendedFAB "attribute not found" on `app:strokeWidth` → Material
  lib too old; unlikely with this project's deps, but confirm the
  Gradle dep resolves `com.google.android.material:material:1.11+`.

### Step 7 — Visual smoke test on-device

Install. Then:

1. Fresh launch (nothing reviewed yet): FAB is hidden.
2. Scroll down through the grid. As tiles get marked reviewed by the
   fast-scroller, you stay below the first unreviewed tile, so the
   FAB stays hidden.
3. Scroll back up, past the reviewed tiles. The FAB appears as a
   **solid ink "Continue"** pill.
4. Tap it. Grid smooth-scrolls down to the first unreviewed tile; once
   your viewport reaches it, the FAB hides again.
5. Keep scrolling past all unreviewed tiles — FAB remains hidden.
6. Review every item in the current filter (via deletes or the media
   viewer) — FAB still hidden (no ghost state in this iteration).

## Definition of Done

- [x] All Files Changed from `requirement.md` landed
- [x] `continue_reviewing` removed from `strings.xml`; no references
      remain in `app/src/`
- [x] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [x] Visual smoke test (Step 7) passes end-to-end
- [x] PR opened with title `SDD-20260418-007 — Continue FAB States`

## Known gotchas

- **`setElevation(0f)` on FAB still shows a thin shadow on some
  OEMs** — Samsung One UI likes to render a 1dp baseline shadow on
  Material FABs. Acceptable; design tolerates this.
- **`stateListAnimator`**: ExtendedFAB sets a built-in animator that
  bumps elevation on touch. When we set `elevation = 0f` in the ghost
  state but leave the animator in place, the FAB momentarily pops up
  on touch before being ignored (since `isEnabled=false`). If this
  visually bothers you, add `android:stateListAnimator="@null"` to the
  layout XML — but most users won't see it because the button doesn't
  respond to taps anyway.
- **ColorStateList.valueOf(int)**: cheap; doesn't allocate a new list
  each render. Don't hoist to a field unless profiling shows it matters.
- **`.dp()` extension**: already defined in `MainActivity.kt` as
  `private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()`
  (search for it if you're not sure). Returns pixel count, so cast to
  `Float` before passing to `elevation`.
