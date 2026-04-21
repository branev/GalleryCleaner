# Icon Source Files

Master files for the app icon. Generated with Google Nano Banana (Gemini 2.5 Flash Image) in April 2026.

## Files

| File | Purpose | Background |
|---|---|---|
| `playstore_icon_1024.png` | Source for the Play Store 512×512 hi-res icon. Downscale to 512 when uploading to the Play Console listing. | White (no alpha — Play Store requires it) |
| `adaptive_foreground_1024.png` | Source for the Android adaptive icon foreground layer. Fed into Android Studio's Image Asset Studio. | Transparent |

## Regenerating the launcher icons

Android Studio → right-click `app` module → **New → Image Asset** → **Launcher Icons (Adaptive and Legacy)**.

- **Foreground layer**: pick `adaptive_foreground_1024.png`, enable Trim, adjust Resize so the subject stays inside the inner safe-zone circle across all mask previews
- **Background layer**: solid color (currently `#FFFFFF`)

Click Finish — Android Studio regenerates all `app/src/main/res/mipmap-*dpi/` files automatically.

## Regenerating the Play Store icon

Downscale `playstore_icon_1024.png` to 512×512 using any tool (e.g., [Squoosh](https://squoosh.app)). Upload the 512×512 to Play Console → Store listing → App icon.
