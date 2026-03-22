# Gallery Cleaner

A native Android gallery app that helps you clean up your photo library by organizing media by source and enabling fast bulk deletion.

## What it does

- **Organizes media by source** — Automatically categorizes photos and videos by where they came from (WhatsApp, Camera, Screenshots, Telegram, Instagram, Viber, and more)
- **Drag-to-select** — Long-press to start selecting, then drag across items to select many at once. Swipe horizontally to select, vertically to scroll
- **Pinch-to-zoom** — Pinch the grid to change between 2 and 5 columns. Preference persists across restarts
- **Smart filtering** — Filter by media type (photos/videos), date range, source, and sort order. The Filters button shows a visual indicator when filters are active
- **Fast scroller** — Drag the right edge to quickly navigate large libraries. A date tooltip shows where you are
- **Bulk deletion with undo** — Delete selected items with a single tap. A celebration overlay shows how much space you freed, with an 8-second undo window
- **Review progress** — A thin progress bar tracks how much of your library you've reviewed. Updates instantly on delete and undo
- **Video duration badges** — Videos show their duration directly on the thumbnail

## Screenshots

*Coming soon*

## Tech Stack

- Kotlin 2.2
- Android Views with ViewBinding
- MVVM architecture with ViewModel + StateFlow
- Material Design 3
- Coil for image loading
- Inter font (SIL Open Font License)

## Building

```bash
# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lint
```

## Requirements

- Android 8.0 (API 26) or higher
- Storage permission for accessing photos and videos

## Author

Branimir Parashkevov

This is a hobbyist project built with [Claude Code](https://claude.ai/claude-code), Anthropic's AI coding assistant.

## License

```
Copyright 2024-2026 Branimir Parashkevov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
