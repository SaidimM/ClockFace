package com.saidim.clockface.clock

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.Log

/**
 * Utility class for working with typefaces, including system fonts and custom fonts.
 */
class TypefaceUtil {
    companion object {
        private const val TAG = "TypefaceUtil"
        
        /**
         * A data class representing a font family with its available weights
         */
        data class FontFamily(
            val displayName: String,      // Display name for the font family
            val familyName: String,       // Internal/system name for the font family
            val weights: List<FontWeight> // Available weights for this family
        )
        
        /**
         * A data class representing a font weight within a family
         */
        data class FontWeight(
            val styleName: String,  // Display name for the style (e.g., "Regular", "Bold")
            val style: Int,         // Typeface style constant (Typeface.NORMAL, Typeface.BOLD, etc.)
            val typeface: Typeface  // The actual typeface object
        )
        
        /**
         * Helper method to get system fonts based on Android version
         */
        private fun getSystemFontsForVersion(): Array<Typeface> {
            // In Android 10+ (API 29+), getSystemFonts() is available
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    Typeface::class.java.getMethod("getSystemFonts").invoke(null) as Array<Typeface>
                } catch (e: Exception) {
                    Log.e(TAG, "Error calling getSystemFonts: $e")
                    getBasicFonts()
                }
            } else {
                // For older versions, use basic fonts
                getBasicFonts()
            }
        }
        
        /**
         * Returns basic default fonts available on all Android versions
         */
        private fun getBasicFonts(): Array<Typeface> {
            return arrayOf(
                Typeface.DEFAULT,
                Typeface.DEFAULT_BOLD,
                Typeface.MONOSPACE,
                Typeface.SANS_SERIF,
                Typeface.SERIF
            )
        }
        
        /**
         * Gets all font families from the assets folder, regardless of system font availability.
         * Only includes files with .ttf or .otf extensions.
         * 
         * @param context The context to access assets
         * @return List of FontFamily objects representing fonts from assets
         */
        fun getFontFamilies(context: Context): List<FontFamily> {
            return getAssetFontFamilies(context)
        }
        
        /**
         * Gets all font families from the assets folder.
         * This should only be used as a fallback when system fonts are not available.
         */
        private fun getAssetFontFamilies(context: Context): List<FontFamily> {
            val fontFamilies = mutableListOf<FontFamily>()
            
            try {
                val assetManager = context.assets
                // Get all directories in the fonts folder
                val fontDirs = assetManager.list("fonts") ?: emptyArray()
                
                for (dir in fontDirs) {
                    try {
                        // Get all TTF files in this directory
                        val fontFiles = assetManager.list("fonts/$dir") ?: emptyArray()
                        val ttfFiles = fontFiles.filter { it.endsWith(".ttf") || it.endsWith(".otf") }
                        
                        if (ttfFiles.isNotEmpty()) {
                            // Extract family name and weights
                            val displayName = dir
                            val familyName = dir.replace(" ", "")
                            val weights = mutableListOf<FontWeight>()
                            
                            for (fontFile in ttfFiles) {
                                try {
                                    // Extract style name from filename (e.g., "Roboto-Bold.ttf" -> "Bold")
                                    val fileNameWithoutExt = fontFile.substringBeforeLast(".")
                                    val styleName = if (fileNameWithoutExt.contains("-")) {
                                        fileNameWithoutExt.substringAfterLast("-")
                                    } else {
                                        "Regular"
                                    }
                                    
                                    // Determine style value
                                    val style = when (styleName.toLowerCase()) {
                                        "bold" -> Typeface.BOLD
                                        "italic" -> Typeface.ITALIC
                                        "bolditalic" -> Typeface.BOLD_ITALIC
                                        else -> Typeface.NORMAL
                                    }
                                    
                                    // Create typeface
                                    val typeface = Typeface.createFromAsset(
                                        assetManager,
                                        "fonts/$dir/$fontFile"
                                    )
                                    
                                    weights.add(FontWeight(styleName, style, typeface))
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error loading font file $fontFile: $e")
                                }
                            }
                            
                            if (weights.isNotEmpty()) {
                                fontFamilies.add(
                                    FontFamily(
                                        displayName = displayName,
                                        familyName = familyName,
                                        weights = weights
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing font directory $dir: $e")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting asset fonts: $e")
            }
            
            return fontFamilies
        }

        /**
         * Helper method to extract font family name using reflection
         * This is needed because the Android API doesn't expose this information directly
         */
        private fun getFontFamilyName(typeface: Typeface): String? {
            try {
                // For Android 10+, try to get the family name from the typeface
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // This is using reflection to access private API, which is not ideal
                    // but it's the only way to get the font family name on some devices
                    val field = Typeface::class.java.getDeclaredField("mFamilyName")
                    field.isAccessible = true
                    return field.get(typeface) as? String
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting font family name: $e")
            }
            return null
        }
        
        /**
         * Get a Typeface from a family name and style
         */
        fun getTypefaceFromFamilyAndStyle(
            context: Context,
            familyName: String,
            styleName: String
        ): Typeface? {
            val families = getFontFamilies(context)
            
            // Look for the family
            val family = families.find { it.familyName == familyName }
            if (family != null) {
                // Look for the weight
                val weight = family.weights.find { it.styleName == styleName }
                if (weight != null) {
                    return weight.typeface
                }
                
                // If the requested style isn't found, fall back to Regular or the first available weight
                return family.weights.find { it.styleName == "Regular" }?.typeface
                    ?: family.weights.firstOrNull()?.typeface
            }
            
            // If not found, return default typeface
            return Typeface.DEFAULT
        }
        
        /**
         * Convert a ClockStyleConfig font family string to a Typeface
         */
        fun getTypefaceFromConfig(context: Context, fontFamilyConfig: String): Typeface {
            try {
                // Parse the font family string (format: "familyName-styleName")
                val parts = fontFamilyConfig.split("-")
                val familyName = parts.getOrNull(0) ?: "Roboto"
                val styleName = parts.getOrNull(1) ?: "Regular"
                
                return getTypefaceFromFamilyAndStyle(context, familyName, styleName) ?: Typeface.DEFAULT
            } catch (e: Exception) {
                Log.e(TAG, "Error converting config to typeface: $e")
                return Typeface.DEFAULT
            }
        }
        
        /**
         * Get available weights for a font family
         */
        fun getAvailableWeights(context: Context, familyName: String): List<String> {
            val families = getFontFamilies(context)
            val family = families.find { it.familyName == familyName }
            
            return family?.weights?.map { it.styleName } ?: listOf("Regular")
        }
    }
} 