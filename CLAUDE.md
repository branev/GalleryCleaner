# CLAUDE.md - GalleryCleaner

## Project Overview

GalleryCleaner is an Android gallery app that helps users organize and clean up their photo library by categorizing images by source (WhatsApp, Camera, Screenshots, etc.) and enabling bulk selection for deletion.

## Tech Stack

- **Language**: Kotlin
- **UI**: Android Views with ViewBinding (NOT Jetpack Compose)
- **Architecture**: MVVM with ViewModel + StateFlow
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build**: Gradle 8.13+ with Kotlin DSL

## Key Dependencies

- AndroidX Lifecycle (ViewModel, StateFlow)
- Material Components 3
- Coil (image loading)
- RecyclerView with ListAdapter + DiffUtil

## Project Structure

```
app/src/main/java/com/example/gallerycleaner/
├── MainActivity.kt          # Main activity, UI state rendering
├── GalleryViewModel.kt      # State management, filtering, selection
├── GalleryUiState.kt        # Sealed class for UI states
├── ImageAdapter.kt          # RecyclerView adapter with selection support
├── MediaItem.kt             # Data class + SourceType enum
├── SourceDetector.kt        # Detects image source from path/bucket
└── FilterPreferences.kt     # SharedPreferences wrapper for filter persistence

app/src/main/res/
├── layout/
│   ├── activity_main.xml    # Main layout with chips, RecyclerView, action bar
│   └── item_image.xml       # Grid item with overlay, checkmark, badge
├── drawable/                 # Icons (ic_check_circle, ic_close, ic_delete, etc.)
└── values/
    ├── strings.xml
    └── integers.xml          # grid_span_count
```

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run Android Lint
./gradlew lint

# Clean build
./gradlew clean assembleDebug
```

## UI States (GalleryUiState)

| State | Description |
|-------|-------------|
| `Loading` | Initial load, shows progress bar |
| `Empty` | No images found or error message |
| `NoFiltersSelected` | All filter chips deselected, shows hint |
| `Normal` | Displaying filtered images |
| `Selection` | Bulk selection mode active |

## Key Patterns

### State Management
- Single `StateFlow<GalleryUiState>` drives all UI updates
- Staged `combine()` with intermediate data classes (`LoadingState`, `FilterState`, `SelectionState`) for type-safe flow merging
- Selection state is separate from items for efficient partial updates

### Filter Chips
- "All" chip toggles between select-all and deselect-all
- Individual chips can be toggled independently
- Filters persist via SharedPreferences

### Selection Mode
- Long-press enters selection mode
- Tap toggles selection in selection mode
- Back button or X exits selection mode
- Selected items persist across filter changes (with "hidden" count shown)

## Testing

Unit tests in `app/src/test/`:
- `SourceDetectorTest.kt` - Source detection logic (pure Kotlin)
- `GalleryViewModelTest.kt` - Filtering and selection logic
- `FilterPreferencesTest.kt` - Preferences with mocked SharedPreferences
- `MediaItemTest.kt` - Data class and enum tests

Run with: `./gradlew testDebugUnitTest`

## Current Features (Implemented)

1. **Source Filtering** - Filter images by source (WhatsApp, Camera, Screenshots, etc.)
2. **Bulk Selection Mode** - Long-press to select, tap to toggle, Select All button
3. **Empty States** - Proper handling for no images and no filters selected

## Planned Features (Not Yet Implemented)

1. **Trash Bin** - Safe delete with recovery (Phase 3)
2. **Date Range Filter** - Filter by time period (Phase 4)
3. **Storage Insights** - Analytics and suggestions (v1.1)

## Common Issues

### ViewBinding not generating
After changing `build.gradle.kts`, run: Sync → Clean → Rebuild

### Compose references in errors
This is a View-based project. Do NOT add Compose dependencies or code.

### Permission denied on Android 13+
Use `READ_MEDIA_IMAGES` permission, not `READ_EXTERNAL_STORAGE`

## Avoid Over-Engineering

Only make changes that are directly requested or clearly necessary. Keep solutions simple and focused.

- Don't add features, refactor code, or make "improvements" beyond what was asked
- A bug fix doesn't need surrounding code cleaned up
- A simple feature doesn't need extra configurability
- Don't add docstrings, comments, or type annotations to code you didn't change
- Only add comments where the logic isn't self-evident
- Don't add error handling, fallbacks, or validation for scenarios that can't happen
- Trust internal code and framework guarantees - only validate at system boundaries (user input, external APIs)
- Don't create helpers, utilities, or abstractions for one-time operations
- Don't design for hypothetical future requirements
- Three similar lines of code is better than a premature abstraction

## Code Style Notes

- Use `SourceType.values()` for enum iteration (not `entries` for compatibility)
- Use staged `combine()` with intermediate data classes for type-safe flow merging (see `GalleryViewModel.kt`)
- Use `@Suppress("DEPRECATION")` for pre-API-29 MediaStore.DATA usage

## Required Checks Before Completing Work

**Always run these commands before considering work complete:**

```bash
# 1. Build must succeed
./gradlew assembleDebug

# 2. All tests must pass
./gradlew testDebugUnitTest

# 3. Lint must pass (no errors)
./gradlew lint
```

If lint reports errors, fix them before finishing. Warnings are acceptable but should be minimized.
