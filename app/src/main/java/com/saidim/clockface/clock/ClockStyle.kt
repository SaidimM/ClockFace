import com.saidim.clockface.clock.syles.ClockStyleConfig

enum class ClockStyle(
    val displayName: String, 
    val description: String,
    val defaultConfig: ClockStyleConfig
) {
    MINIMAL(
        "Minimal", 
        "Clean and simple digital display",
        ClockStyleConfig.MinimalConfig()
    ),
    ANALOG(
        "Analog", 
        "Classic analog clock face",
        ClockStyleConfig.AnalogConfig()
    ),
    WORD(
        "Word", 
        "Time spelled out in words",
        ClockStyleConfig.WordConfig()
    )
}