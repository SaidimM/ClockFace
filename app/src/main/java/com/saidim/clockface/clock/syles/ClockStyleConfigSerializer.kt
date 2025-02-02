package com.saidim.clockface.clock.syles

import ClockStyle
import com.google.gson.Gson

object ClockStyleConfigSerializer {
    private val gson = Gson()

    fun serialize(config: ClockStyleConfig): String {
        return gson.toJson(config)
    }

    fun deserialize(json: String, style: ClockStyle): ClockStyleConfig {
        return when (style) {
            ClockStyle.MINIMAL -> gson.fromJson(json, ClockStyleConfig.MinimalConfig::class.java)
            ClockStyle.ANALOG -> gson.fromJson(json, ClockStyleConfig.AnalogConfig::class.java)
            ClockStyle.WORD -> gson.fromJson(json, ClockStyleConfig.WordConfig::class.java)
        }
    }
}