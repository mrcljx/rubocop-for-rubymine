package io.github.sirlantis.rubymine.rubocop.model

import com.google.gson.stream.JsonReader

class OffenseLocation(val line: Int, val column: Int, val length: Int) {
    companion object {
        fun zero(): OffenseLocation {
            return OffenseLocation(0, 0, 0)
        }

        fun readFromJsonReader(reader: JsonReader): OffenseLocation {
            reader.beginObject()
            var line = 0
            var column = 0
            var length = 0

            while (reader.hasNext()) {
                val attrName = reader.nextName()

                when (attrName) {
                    "line" -> line = reader.nextInt()
                    "column" -> column = reader.nextInt()
                    "length" -> length = reader.nextInt()
                    else -> reader.skipValue()
                }
            }

            reader.endObject()

            return OffenseLocation(line, column, length)
        }
    }
}
