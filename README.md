# GalleryCleaner

A simple Android gallery app to help clean up your photo library by organizing media by source and enabling bulk deletion.

## What it does

- **Organizes media by source** - Automatically categorizes your photos and videos by where they came from (WhatsApp, Camera, Screenshots, Telegram, Instagram, etc.)
- **Bulk selection and deletion** - Long-press to select multiple items, then delete them all at once
- **Smart filtering** - Filter by media type (photos/videos), date range, and source
- **Sorting options** - Sort by date, name, or file size
- **Progress tracking** - Keeps track of which items you've scrolled past so you can pick up where you left off
- **Size info on delete** - Shows total size of deleted items so you know how much space you freed

## Screenshots

*Coming soon*

## Tech Stack

- Kotlin
- Android Views with ViewBinding
- MVVM architecture with ViewModel + StateFlow
- Material Design 3
- Coil for image loading

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
Copyright 2024 Branimir Parashkevov

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
