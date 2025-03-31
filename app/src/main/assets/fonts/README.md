# Stylized Fonts for ClockFace App

This directory contains stylized fonts used in the ClockFace application.

## Included Fonts

### General Purpose Fonts
- **Roboto**: A clean, modern sans-serif typeface
- **Montserrat**: An elegant, versatile typeface
- **Open Sans**: A humanist sans-serif typeface with open forms
- **Lato**: A warm and stable sans-serif typeface
- **Playfair Display**: An elegant serif font for display usage
- **Raleway**: An elegant sans-serif typeface family
- **Josefin Sans**: A geometric, elegant sans-serif typeface
- **Poppins**: A geometric sans-serif typeface

### Specialty Clock Fonts
- **Bebas Neue**: A condensed sans-serif with excellent legibility for digital displays
- **Rajdhani**: A technical, modern font perfect for digital interfaces
- **Orbitron**: A futuristic, geometric font ideal for sci-fi themed clocks
- **Eurostile**: A squared-off geometric font commonly used in futuristic designs

## Font Variations

Each font includes the following main variations:
- Light
- Regular
- Medium
- Bold
- Black (where available)

All italic versions have been removed to maintain a clean, consistent display.

## Usage

To use these fonts in your application:

```kotlin
// Example for loading a custom font
val typeface = Typeface.createFromAsset(assets, "fonts/Roboto-Bold.ttf")
textView.typeface = typeface
```

For Jetpack Compose:

```kotlin
val robotoFont = FontFamily(
    Font(R.font.roboto_regular),
    Font(R.font.roboto_bold, FontWeight.Bold)
)

Text(
    text = "Your Text Here",
    fontFamily = robotoFont
)
```

## Recommended Fonts for Time Display

For displaying time, the following fonts are particularly recommended:

1. **Roboto** - Excellent legibility and modern appearance
2. **Bebas Neue** - Perfect for bold, striking time displays
3. **Orbitron** - Ideal for futuristic or sci-fi themed clocks
4. **Rajdhani** - Great for technical, dashboard-style clocks

## License Information

All included fonts are licensed under either the SIL Open Font License or Apache License, making them free for commercial use, with the exception of Eurostile which is a commercial font requiring purchase.

- [Google Fonts Repository](https://github.com/google/fonts)
- [Open Font License](https://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&id=OFL)
- [Apache License](https://www.apache.org/licenses/LICENSE-2.0)
