package io.github.sirlantis.rubymine.rubocop.model

import org.junit.Test
import java.io.StringReader
import java.io.InputStreamReader
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.CharBuffer
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import com.google.gson.stream.JsonReader
import kotlin.test.assertEquals

class RubocopResultTest {
    fun toJsonReader(s: String): JsonReader {
        val encoder = StandardCharsets.UTF_8.newEncoder()
        val bytes = encoder.encode(CharBuffer.wrap(s.toCharArray()))
        val stream = ByteArrayInputStream(bytes.array())
        val reader = InputStreamReader(stream)
        return JsonReader(reader)
    }

    fun readFromString(s: String): RubocopResult {
        return RubocopResult.readFromJsonReader(toJsonReader(s))
    }

    Test fun testEmpty() {
        val result = readFromString("{}")
        assertTrue(result.empty)
    }

    Test fun testWithoutEmptyFiles() {
        val result = readFromString("""{"files":[]}""")
        assertTrue(result.empty)
    }

    Test fun testWithFileWithoutOffenses() {
        val result = readFromString("""{"files":[{"path":"test.rb","offenses":[]}]}""")
        assertFalse(result.empty)

        val file = result.first!!
        assertEquals(file.path, "test.rb")
        assertTrue(file.offenses.empty)
    }

    Test fun testMultipleFiles() {
        val input = """
        {
            "metadata":{
                "rubocop_version":"0.27.0",
                "ruby_engine":"ruby",
                "ruby_version":"2.1.3",
                "ruby_patchlevel":"242",
                "ruby_platform":"x86_64-darwin14.0"
            },
            "files":[
                {
                    "path":"Gemfile",
                    "offenses":[]
                }, {
                    "path":"test.rb",
                    "offenses":[
                        {
                            "severity":"convention",
                            "message":"Prefer single-quoted strings when you don't need string interpolation or special symbols.",
                            "cop_name":"Style/StringLiterals",
                            "corrected":null,
                            "location":{
                                "line":5,
                                "column":7,
                                "length":11
                            }
                        }, {
                            "severity":"warning",
                            "message":"`end` at 11, 6 is not aligned with `if` at 8, 4",
                            "cop_name":"Lint/EndAlignment",
                            "corrected":null,
                            "location":{
                                "line":11,
                                "column":7,
                                "length":3
                            }
                        }
                    ]
                }
            ],
            "summary":{
                "offense_count":2,
                "target_file_count":2,
                "inspected_file_count":2
            }
        }
        """

        val result = readFromString(input)
        assertFalse(result.empty)
        assertEquals(result.size, 2)
    }

    Test fun testOffense() {
        val offense = Offense.readFromJsonReader(toJsonReader("""{
            "severity":"convention",
            "message":"Prefer single-quoted strings when you don't need string interpolation or special symbols.",
            "cop_name":"Style/StringLiterals",
            "corrected":null,
            "location":{"line":5,"column":7,"length":11}}
        """))

        assertEquals(offense.severity, "convention")
        assertEquals(offense.message, "Prefer single-quoted strings when you don't need string interpolation or special symbols.")
        assertEquals(offense.cop, "Style/StringLiterals")
        assertEquals(offense.location.line, 5)
        assertEquals(offense.location.column, 7)
        assertEquals(offense.location.length, 11)
    }

    Test fun testOffenseLocation() {
        val input = """{"line":42,"column":13,"length":"7"}"""
        val location = OffenseLocation.readFromJsonReader(toJsonReader(input))
        assertEquals(location.line, 42)
        assertEquals(location.column, 13)
        assertEquals(location.length, 7)
    }
}
