# SDD-20260322-002: Hint UX Rework

## Summary

Rework the contextual hint display from small dark PopupWindows to full-width bottom cards with a "Got it" dismiss button. Keeps the existing queuing system (max 2 per session, priority order) but fixes the visual presentation.

---

## Problem

The current hints appear as small dark popups that:
- Can show two simultaneously (one top, one bottom)
- Are hard to read (small, dark, no clear dismiss action)
- Feel like system errors rather than helpful tips
- Are easy to accidentally dismiss or miss entirely

## Solution

Replace the PopupWindow-based tooltips with a **single full-width card** at the bottom of the screen, similar to Google Maps/Drive feature discovery.

### Card Specification:

```
┌─────────────────────────────────────┐
│  💡  Long-press to select items     │
│       for deletion                  │
│                          [Got it]   │
└─────────────────────────────────────┘
```

- **Position**: bottom of screen, above navigation bar, 16dp margin
- **Width**: match_parent with 16dp horizontal margin
- **Background**: `@color/hint_tooltip_bg` (#2C2C2E), 16dp corner radius
- **Icon**: small lightbulb or info icon (16dp, teal) at the start of the text
- **Text**: white, 14sp, the hint message
- **"Got it" button**: teal text, right-aligned, 14sp bold
- **Elevation**: 8dp (floats above content)
- **Animation**: slide up from bottom (200ms) on show, slide down on dismiss
- **Strictly one at a time** — never two cards on screen simultaneously
- **"Got it" required** — card only dismisses via the button (not tap-outside). This ensures the user actually reads it
- **Max 2 per session** — after dismissing 2 hints, no more until app restart
- **Next hint delay** — after "Got it", the next queued hint appears after 1 second

### Changes from Current Implementation:

| Aspect | Before (PopupWindow) | After (Bottom Card) |
|--------|---------------------|---------------------|
| Display | Small dark popup anchored to view | Full-width card at bottom |
| Dismiss | Tap anywhere | "Got it" button only |
| Simultaneous | Could show 2 at once | Strictly 1 at a time |
| Position | Anchored to specific views | Always at bottom |
| Readability | Small, easy to miss | Large, clear, unmissable |

### Implementation:

Replace the `PopupWindow` logic in `HintManager.kt` with a `View` that's added to `activity_main.xml` (always present, visibility toggled). This avoids PopupWindow quirks and is more reliable.

Add to `activity_main.xml` (above the delete overlay, so overlay covers it):
```xml
<!-- Hint Card -->
<com.google.android.material.card.MaterialCardView
    android:id="@+id/hintCard"
    ...
    android:visibility="gone">
    <!-- icon + text + Got it button -->
</com.google.android.material.card.MaterialCardView>
```

Update `HintManager` to:
- Accept the hint card views (not rootView for PopupWindow)
- Show/hide the card with slide animation
- Use "Got it" click listener instead of tap-outside
- Keep existing queue, priority, and session limit logic

---

## Files Summary

| File | Change Type |
|------|------------|
| `activity_main.xml` | Minor — add hint card layout at bottom |
| `HintManager.kt` | Major — replace PopupWindow with card show/hide |
| `drawable/ic_lightbulb.xml` | **New file** — lightbulb icon for hint card |

## Acceptance Criteria

- [ ] Hints appear as a full-width card at the bottom of the screen
- [ ] Only one hint card visible at a time (never two)
- [ ] "Got it" button dismisses the card (no tap-outside dismiss)
- [ ] Next queued hint appears 1 second after "Got it"
- [ ] Max 2 hints per session
- [ ] Slide-up/down animation
- [ ] Card is readable and clearly a tip (not an error)
- [ ] Existing hint triggers (all 8) still work
- [ ] Build succeeds, all tests pass
