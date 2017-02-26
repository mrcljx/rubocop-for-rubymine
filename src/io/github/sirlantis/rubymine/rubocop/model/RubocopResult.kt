package io.github.sirlantis.rubymine.rubocop.model

import com.intellij.openapi.vfs.VirtualFile
import java.io.InputStreamReader
import com.google.gson.stream.JsonReader
import java.io.FileReader
import java.util.LinkedList
import java.util.regex.Pattern

class RubocopResult(val fileResults: List<FileResult>, val warnings: List<String>) : List<FileResult> by fileResults {

    companion object {
        fun readFromFile(file: VirtualFile): RubocopResult {
            return readFromReader(FileReader(file.path), null)
        }

        fun readFromReader(reader: InputStreamReader, stderrReader: InputStreamReader?): RubocopResult {
            return readFromJsonReader(JsonReader(reader), stderrReader)
        }

        fun readFromJsonReader(reader: JsonReader, stderrReader: InputStreamReader?): RubocopResult {
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

            return RubocopResult(fileResults, extractWarnings(stderrReader))
        }

        private fun extractWarnings(stderrReader: InputStreamReader?): List<String> {
            if (stderrReader == null) {
                return listOf()
            }

            val pattern = Pattern.compile("Warning: (unrecognized cop .*)$", Pattern.MULTILINE)
            val matcher = pattern.matcher(stderrReader.readText())
            val list = LinkedList<String>()

            while (matcher.find()) {
                list.add(matcher.group(1)!!)
            }

            return list
        }
    }

    fun getFileResult(filePath: String): FileResult? {
        return fileResults.firstOrNull { it.path == filePath }
    }
}
