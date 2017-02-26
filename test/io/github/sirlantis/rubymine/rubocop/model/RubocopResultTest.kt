package io.github.sirlantis.rubymine.rubocop.model

import org.junit.Test
import java.io.InputStreamReader
import com.google.gson.stream.JsonReader
import org.junit.Assert.*

class RubocopResultTest {
    fun String.reader(): InputStreamReader {
        return InputStreamReader(byteInputStream())
    }

    fun String.jsonReader(): JsonReader {
        return JsonReader(reader())
    }

    @Test fun testHelperFunctions() {
        val s = "Hallo, ich hätte gerne ein Weißwurstbrötchen!"
        assertEquals(s, s.reader().readText())
    }

    fun readFromString(s: String, stderr: String? = null): RubocopResult {
        return RubocopResult.readFromJsonReader(s.jsonReader(), stderr?.reader())
    }

    @Test fun testEmpty() {
        val result = readFromString("{}")
        assertTrue(result.isEmpty())
    }

    @Test fun testWithoutEmptyFiles() {
        val result = readFromString("""{"files":[]}""")
        assertTrue(result.isEmpty())
    }

    @Test fun testWithConfigError() {
        val result = readFromString("""{"files":[]}""", "Warning: unrecognized cop Style/CaseIndentation found in /project/.rubocop.yml")
        assertEquals(1, result.warnings.count())
        assertEquals("unrecognized cop Style/CaseIndentation found in /project/.rubocop.yml", result.warnings.first())
    }

    @Test fun testWithConfigErrors() {
        val result = readFromString("""{"files":[]}""", "Warning: unrecognized cop Style/CaseIndentation found in /project/.rubocop.yml\nWarning: unrecognized foo\nWarning: unrecognized cop Whatever found in /somewhere/.rubocop.yml")
        assertEquals(2, result.warnings.count())
    }

    @Test fun testWithFileWithoutOffenses() {
        val result = readFromString("""{"files":[{"path":"test.rb","offenses":[]}]}""")
        assertFalse(result.isEmpty())

        val file = result.first()
        assertEquals(file.path, "test.rb")
        assertTrue(file.offenses.isEmpty())
    }

    @Test fun testMultipleFiles() {
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
        assertFalse(result.isEmpty())
        assertEquals(result.size, 2)
    }

    @Test fun testOffense() {
        val offense = Offense.readFromJsonReader("""{
            "severity":"convention",
            "message":"Prefer single-quoted strings when you don't need string interpolation or special symbols.",
            "cop_name":"Style/StringLiterals",
            "corrected":null,
            "location":{"line":5,"column":7,"length":11}}
        """.jsonReader())

        assertEquals(offense.severity, "convention")
        assertEquals(offense.message, "Prefer single-quoted strings when you don't need string interpolation or special symbols.")
        assertEquals(offense.cop, "Style/StringLiterals")
        assertEquals(offense.location.line, 5)
        assertEquals(offense.location.column, 7)
        assertEquals(offense.location.length, 11)
    }

    @Test fun testOffenseLocation() {
        val input = """{"line":42,"column":13,"length":"7"}"""
        val location = OffenseLocation.readFromJsonReader(input.jsonReader())
        assertEquals(location.line, 42)
        assertEquals(location.column, 13)
        assertEquals(location.length, 7)
    }
}
