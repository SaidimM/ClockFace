package com.saidim.clockface.clock.syles

import com.google.gson.Gson

object ClockStyleConfigSerializer {
    private val gson = Gson()

    fun serialize(config: ClockStyleConfig): String {
        return gson.toJson(config)
    }

    fun deserialize(json: String): ClockStyleConfig {
        // Directly deserialize to the refactored ClockStyleConfig
        return try {
            gson.fromJson(json, ClockStyleConfig::class.java)
        } catch (e: Exception) {
            // Fallback to default ClockStyleConfig
            ClockStyleConfig()
        }
    }
}