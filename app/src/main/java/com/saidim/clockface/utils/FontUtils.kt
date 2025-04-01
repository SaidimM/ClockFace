package com.saidim.clockface.utils

import android.content.Context

// Function to get all available font families from assets
fun getFontFamilies(context: Context): List<FontFamily> {
    return try {
        val assetManager = context.assets
        val fontDirs = assetManager.list("fonts") ?: emptyArray()

        fontDirs.mapNotNull { dir ->
            try {
                // Skip directories that don't contain font files or README
                val fontFiles = assetManager.list("fonts/$dir") ?: emptyArray()
                if (fontFiles.isEmpty() || (fontFiles.size == 1 && fontFiles[0].contains("License") || fontFiles[0].contains("README"))) {
                    return@mapNotNull null
                }

                // Get font files (.ttf or .otf)
                val validFontFiles = fontFiles.filter {
                    it.endsWith(".ttf") || it.endsWith(".otf")
                }

                if (validFontFiles.isEmpty()) {
                    return@mapNotNull null
                }

                // Create a FontFamily object with all the font weights
                val fonts = validFontFiles.mapNotNull { file ->
                    try {
                        // Extract weight from filename (e.g., "Roboto-Bold.ttf" -> Bold)
                        val weight = when {
                            file.contains("-Thin") -> FontWeight.Thin
                            file.contains("-Light") -> FontWeight.Light
                            file.contains("-Regular") -> FontWeight.Normal
                            file.contains("-Bold") -> FontWeight.Bold
                            file.contains("-Black") -> FontWeight.Black
                            else -> FontWeight.Normal
                        }

                        FontFamily(
                            name = dir,
                            path = "fonts/$dir/$file",
                            weight = weight,
                            style = FontStyle.Normal,
                            displayName = dir.replace("_", " ")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                if (fonts.isEmpty()) null else fonts
            } catch (e: Exception) {
                null
            }
        }.flatten().groupBy { it.name }.map { (name, fonts) ->
            FontFamily(
                name = name,
                fonts = fonts.distinctBy { it.weight to it.style },
                displayName = fonts.firstOrNull()?.displayName ?: name
            )
        }.sortedBy { it.displayName }
    } catch (e: Exception) {
        // Fallback list in case of errors
        listOf(
            FontFamily(
                name = "Roboto",
                fonts = listOf(
                    FontFamily(name = "Roboto", path = "fonts/Roboto/Roboto-Regular.ttf",
                              weight = FontWeight.Normal, style = FontStyle.Normal, displayName = "Roboto")
                ),
                displayName = "Roboto"
            )
        )
    }
}

// Data classes for font management
data class FontFamily(
    val name: String,
    val fonts: List<FontFamily> = emptyList(),
    val path: String = "",
    val weight: FontWeight = FontWeight.Normal,
    val style: FontStyle = FontStyle.Normal,
    val displayName: String = name
)

enum class FontWeight {
    Thin, Light, Normal, Bold, Black
}

enum class FontStyle {
    Normal
}