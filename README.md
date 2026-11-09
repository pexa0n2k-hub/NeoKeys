# NeoKeys V1.0

Android IME starter project for a real system keyboard.

## Included
- Kotlin `InputMethodService`
- Spanish / English toggle
- Persistent number row
- QWERTY key rows, shift, delete, space and enter
- Initial dark Liquid Glass-inspired rendering
- Settings screen with Android IME activation and picker shortcuts
- GitHub Actions workflow to build a debug APK

## Build
Open this folder in Android Studio and build the `app` debug variant.
The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Install
1. Install the APK.
2. Open NeoKeys.
3. Tap **Activar NeoKeys** and enable it in Android settings.
4. Return and tap **Seleccionar NeoKeys**.
5. Choose NeoKeys from the input-method picker.

## Important V1.0 scope
This is the functional foundation, not a production keyboard yet. It does not include swipe typing, predictive suggestions, a full dictionary, clipboard history, emoji/GIF search, or cloud AI. Those are planned modules. No typed text is transmitted by this starter project.
