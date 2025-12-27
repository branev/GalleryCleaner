# GalleryCleaner

An Android app that helps users organize and clean up their photo and video library by categorizing media by source (WhatsApp, Camera, Screenshots, etc.) and enabling bulk selection for deletion.

## Features

### Source Detection
Automatically identifies where each image/video came from:
- WhatsApp
- Viber
- Messenger (Facebook)
- Telegram
- Instagram
- Snapchat
- Facebook
- Twitter/X
- TikTok
- Pinterest
- Reddit
- Camera (DCIM)
- Screenshots
- Downloads
- Other (shows folder name)

### Media Type Filtering
- **Photos/Videos Toggle**: Filter to show only photos, only videos, or both
- Video thumbnails display a play icon overlay for easy identification

### Source Filtering
- Filter chips to show/hide media from specific sources
- "All" chip to quickly toggle all sources
- Image/video counts per source
- Filter preferences persist between sessions

### Bulk Selection Mode
- Long-press any item to enter selection mode
- Tap items to toggle selection
- "Select All" button for quick selection
- Selection count displayed in toolbar
- Hidden selection count shown when filtered items are selected

### Grid View
- Responsive grid layout (configurable column count)
- Smooth crossfade animations with Coil image loading
- Source badges on each thumbnail

## Project Structure

```
app/src/main/java/com/example/gallerycleaner/
├── MainActivity.kt          # Main activity, UI state rendering
├── GalleryViewModel.kt      # State management, filtering, selection
├── GalleryUiState.kt        # Sealed class for UI states
├── ImageAdapter.kt          # RecyclerView adapter with selection support
├── MediaItem.kt             # Data class + SourceType/MediaType enums
├── SourceDetector.kt        # Detects media source from path/bucket
└── FilterPreferences.kt     # SharedPreferences wrapper for filter persistence

app/src/main/res/
├── layout/
│   ├── activity_main.xml   # Main layout with chips, RecyclerView, action bar
│   └── item_image.xml      # Grid item with overlay, checkmark, video badge
├── drawable/
│   ├── label_bg.xml        # Semi-transparent source badge background
│   ├── ic_play_circle.xml  # Video indicator icon
│   └── ...                 # Other icons
└── values/
    ├── integers.xml        # Grid column counts
    ├── strings.xml         # App strings
    └── colors.xml          # Theme colors
```

## Tech Stack

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **UI**: Android Views with ViewBinding (NOT Jetpack Compose)
- **Architecture**: MVVM with ViewModel + StateFlow
- **Image Loading**: Coil 2.6.0
- **Build**: Gradle with Kotlin DSL

## Permissions

- `READ_MEDIA_IMAGES` (Android 13+)
- `READ_MEDIA_VIDEO` (Android 13+)
- `READ_EXTERNAL_STORAGE` (Android 12 and below)

## Current Status

### Implemented Features
- Source filtering with persistent preferences
- Media type filtering (Photos/Videos)
- Bulk selection mode with Select All
- Video thumbnail indicator
- Empty states for no filters selected

### Planned Features
- Trash Bin (safe delete with recovery)
- Date Range Filter
- Storage Insights and analytics
- Actual deletion functionality
