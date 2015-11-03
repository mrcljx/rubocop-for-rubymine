package io.github.sirlantis.rubymine.rubocop.model

import com.google.gson.stream.JsonReader
import java.util.LinkedList
import java.util.HashMap

class FileResult(val path: String, val offenses: List<Offense>) {
    companion object {
        fun readFromJsonReader(reader: JsonReader): FileResult {
            val offenses = LinkedList<Offense>()
            var path: String? = null

            reader.beginObject()

            while (reader.hasNext()) {
                val attrName = reader.nextName()

                if (attrName == "offenses") {
                    reader.beginArray()

                    while (reader.hasNext()) {
                        offenses.add(Offense.readFromJsonReader(reader))
                    }

                    reader.endArray()
                } else if (attrName == "path") {
                    path = reader.nextString()
                } else {
                    reader.skipValue()
                }
            }

            reader.endObject()
            return FileResult(path as String, offenses)
        }
    }

    var offenseCache: Map<Number, List<Offense>>

    init {
        val cache = HashMap<Number, LinkedList<Offense>>()

        for (offense in offenses) {
            val lineNumber = offense.location.line
            val offensesAtLocation = cache.getOrPut(lineNumber) { LinkedList<Offense>() }
            offensesAtLocation.add(offense)
        }

        offenseCache = cache
    }

    fun getOffensesAt(lineNumber: Number): List<Offense> {
        return offenseCache[lineNumber] ?: LinkedList<Offense>()
    }
}
