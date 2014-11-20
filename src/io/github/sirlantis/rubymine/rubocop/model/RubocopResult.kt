package io.github.sirlantis.rubymine.rubocop.model

import com.intellij.openapi.vfs.VirtualFile
import java.io.InputStreamReader
import com.google.gson.stream.JsonReader
import java.io.FileReader
import java.util.LinkedList

class RubocopResult(val fileResults: List<FileResult>): List<FileResult> by fileResults {

    class object {
        fun readFromFile(file: VirtualFile): RubocopResult {
            return readFromReader(FileReader(file.getPath()))
        }

        fun readFromReader(reader: InputStreamReader): RubocopResult {
            return readFromJsonReader(JsonReader(reader))
        }

        fun readFromJsonReader(reader: JsonReader): RubocopResult {
            // var timestamp = null
            val fileResults = LinkedList<FileResult>()
            reader.beginObject()

            while (reader.hasNext()) {
                val attrName = reader.nextName()

                if (attrName == "files") {
                    reader.beginArray()

                    while (reader.hasNext()) {
                        fileResults.add(FileResult.readFromJsonReader(reader))
                    }

                    reader.endArray()
                } else {
                    reader.skipValue()
                }
            }

            reader.endObject()
            return RubocopResult(fileResults)
        }
    }

    fun getFileResult(filePath: String): FileResult? {
        return fileResults firstOrNull { it.path == filePath }
    }
}
