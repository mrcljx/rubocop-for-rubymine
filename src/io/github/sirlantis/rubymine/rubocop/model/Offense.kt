package io.github.sirlantis.rubymine.rubocop.model

import com.google.gson.stream.JsonReader

class Offense(val severity: String, val cop: String, val message: String, val location: OffenseLocation) {
    class object {
        fun readFromJsonReader(reader: JsonReader): Offense {
            reader.beginObject()
            var location = OffenseLocation.zero()
            var message = "(no message)"
            var cop = "Style/UnknownCop"
            var severity = "unknown"

            while (reader.hasNext()) {
                val attrName = reader.nextName()

                when (attrName) {
                    "message" -> message = reader.nextString()
                    "cop_name" -> cop = reader.nextString()
                    "severity" -> severity = reader.nextString()
                    "location" -> location = OffenseLocation.readFromJsonReader(reader)
                    else -> reader.skipValue()
                }
            }

            reader.endObject()
            return Offense(severity, cop, message, location)
        }
    }
}
