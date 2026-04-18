# Task 01: Continue FAB States

**Parent:** SDD-20260418-007 — Continue FAB States

## What You're Changing

The Continue FAB today has one look — solid ink pill — and fades to 50%
alpha when you're already at the bottom of the unreviewed queue. You're
turning that fade into a fully distinct **"All caught up"** ghost state:
translucent pill, `line_strong` outline, muted text, no shadow, not
clickable.

Behavior stays the same (the FAB still hides entirely when all items
are reviewed). Only the in-between state — "there are unreviewed items
but you're already past them" — gets a new visual identity and a new
label.

## Before vs After

| Element | Before | After |
|---|---|---|
| Active (unreviewed items ahead) | ink pill, white "Continue reviewing", elevation 6dp | ink pill, white **"Continue"**, elevation 6dp |
| Past the queue | same pill at `alpha=0.5`, `isEnabled=false` | 72% white pill, 1dp `line_strong` outline, `ink4` text, **"All caught up"**, no shadow, `isEnabled=false` |

## Prerequisites

- SDD-002 (Design Tokens) merged — the palette used by both states is
  already wired.
- `./gradlew --stop` before you start.

## Step-by-Step Instructions

### Step 1 — Add the two new string resources

Open `app/src/main/res/values/strings.xml`. Find `continue_reviewing`
(around line 75). Replace that single line with three lines (the old
string stays for now — we remove it in Step 5 after the layout and
code have stopped using it):

```xml
<string name="continue_reviewing">Continue reviewing</string>
<string name="fab_continue">Continue</string>
<string name="fab_all_caught_up">All caught up</string>
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
    app:layout_constraintBottom_toTopOf="@id/selectionActionBar"
    app:layout_constraintEnd_toEndOf="parent" />
```

> **What changed:**
> - `android:text` swapped from `continue_reviewing` to `fab_continue`
> - `android:contentDescription` removed (ExtendedFAB announces its
>   visible text via TalkBack automatically — redundant)
> - Hard-coded `android:textColor="@android:color/white"` and
>   `app:iconTint="@android:color/white"` removed — set from Kotlin on
>   each state change
> - `app:strokeWidth="1dp"` added — visible only when the stroke color
>   is opaque (caught-up state), invisible when the stroke color is
>   transparent (active state)

### Step 3 — Rewrite `updateContinueFabState`

Open `MainActivity.kt` and find `updateContinueFabState` (around line
417). Replace the whole method body with the logic below. It sets six
visual properties (bg tint, text color, icon tint, stroke color, label,
elevation) plus `isEnabled` for each of the two states.

```kotlin
private fun updateContinueFabState(layoutManager: GridLayoutManager) {
    // Only update if FAB is visible
    if (binding.fabContinue.visibility != View.VISIBLE) return

    val mediaItems = adapter.mediaItemsInOrder()
    if (mediaItems.isEmpty()) return

    val firstUnviewedMediaIndex = viewModel.getFirstUnviewedIndex(mediaItems)
    val canScrollToUnviewed = if (firstUnviewedMediaIndex < 0) {
        false
    } else {
        val targetUri = mediaItems[firstUnviewedMediaIndex].uri
        val gridPosition = adapter.currentList.indexOfFirst {
            it is GridItem.Media && it.item.uri == targetUri
        }
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        gridPosition > lastVisiblePosition
    }

    applyFabState(active = canScrollToUnviewed)
}

private fun applyFabState(active: Boolean) {
    val fab = binding.fabContinue
    if (active) {
        fab.isEnabled = true
        fab.setText(R.string.fab_continue)
        fab.backgroundTintList = ColorStateList.valueOf(getColor(R.color.ink))
        fab.setTextColor(getColor(android.R.color.white))
        fab.iconTint = ColorStateList.valueOf(getColor(android.R.color.white))
        fab.strokeColor = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        fab.elevation = 6.dp().toFloat()
    } else {
        fab.isEnabled = false
        fab.setText(R.string.fab_all_caught_up)
        fab.backgroundTintList = ColorStateList.valueOf(0xB8FFFFFF.toInt()) // 72% white
        fab.setTextColor(getColor(R.color.ink4))
        fab.iconTint = ColorStateList.valueOf(getColor(R.color.ink4))
        fab.strokeColor = ColorStateList.valueOf(getColor(R.color.line_strong))
        fab.elevation = 0f
    }
}
```

Add the import near the top of the file if it isn't already present:

```kotlin
import android.content.res.ColorStateList
```

> **Why programmatic and not state-list drawables?** The ExtendedFAB's
> `app:strokeWidth` and `app:elevation` attributes don't accept state
> lists — only `backgroundTint`, `textColor`, `iconTint`, and
> `strokeColor` do. Using Kotlin for everything keeps the two states in
> one place, easy to reason about.
>
> **Why `0xB8FFFFFF.toInt()` instead of `@color/...`?** No token exists
> for 72% white; adding one for a single use site would be churn. If
> the color proves reusable later, promote it then.

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

1. Open the app on a library where you have some viewed + some
   unviewed items. The FAB appears bottom-right as a **solid ink
   "Continue"** pill.
2. Tap the FAB — grid scrolls down to the first unreviewed tile. FAB
   stays active while tiles below it are still unreviewed.
3. Keep scrolling past the last unreviewed tile. The FAB should
   transition to a **translucent "All caught up"** pill with a visible
   thin outline. No shadow. Tapping it should do nothing.
4. Scroll back up. FAB returns to the active solid state.
5. Review all items (long-press → delete a few, or view them
   individually). When *nothing* unreviewed is left in the current
   filter, the FAB should hide entirely (regression check — existing
   visibility logic still works).

## Definition of Done

- [ ] All Files Changed from `requirement.md` landed
- [ ] `continue_reviewing` removed from `strings.xml`; no references
      remain in `app/src/`
- [ ] `./gradlew clean assembleDebug testDebugUnitTest lint` succeeds
- [ ] Visual smoke test (Step 7) passes end-to-end
- [ ] PR opened with title `SDD-20260418-007 — Continue FAB States`

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
