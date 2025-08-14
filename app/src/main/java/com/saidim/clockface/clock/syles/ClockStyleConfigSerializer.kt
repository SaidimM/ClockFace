package com.saidim.clockface.clock.syles

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

object ClockStyleConfigSerializer {
    // Create a custom type adapter factory for handling enums 
    private class EnumTypeAdapterFactory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            if (!type.rawType.isEnum) {
                return null
            }

            return object : TypeAdapter<T>() {
                override fun write(out: JsonWriter, value: T?) {
                    if (value == null) {
                        out.nullValue()
                    } else {
                        out.value((value as Enum<*>).name)
                    }
                }

                @Suppress("UNCHECKED_CAST")
                override fun read(reader: JsonReader): T? {
                    if (reader.peek().name == "NULL") {
                        reader.nextNull()
                        return null
                    }
                    
                    val enumValue = reader.nextString()
                    
                    // Generic enum handling
                    return try {
                        val enumConstants = type.rawType.enumConstants as Array<T>
                        enumConstants.first { (it as Enum<*>).name == enumValue }
                    } catch (e: Exception) {
                        if (type.rawType.enumConstants.isNotEmpty()) {
                            type.rawType.enumConstants[0] as T
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    // Configure Gson with our enum adapter
    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(EnumTypeAdapterFactory())
        .create()

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