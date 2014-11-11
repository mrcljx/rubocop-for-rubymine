package io.github.sirlantis.rubymine.rubocop.ui

import com.intellij.openapi.editor.TextAnnotationGutterProvider
import com.intellij.openapi.project.Project
import io.github.sirlantis.rubymine.rubocop.model.FileResult
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.colors.ColorKey
import java.awt.Color
import com.intellij.openapi.actionSystem.AnAction

class RubocopAnnotationGutterProvider(val project: Project, val fileResult: FileResult) : TextAnnotationGutterProvider {
    override fun getLineText(lineNumber: Int, editor: Editor?): String? {
        val offenses = fileResult.getOffensesAt(lineNumber + 1)

        if (offenses.empty) {
            return null
        }

        return offenses.first().severity.substring(0, 1).toUpperCase()
    }

    override fun getToolTip(lineNumber: Int, editor: Editor?): String? {
        val offenses = fileResult.getOffensesAt(lineNumber + 1)

        if (offenses.empty) {
            return null
        }

        return offenses.first().message
    }

    override fun getStyle(lineNumber: Int, editor: Editor?): EditorFontType? {
        return EditorFontType.PLAIN
    }

    override fun getColor(lineNumber: Int, editor: Editor?): ColorKey? {
        return null
    }

    override fun getBgColor(lineNumber: Int, editor: Editor?): Color? {
        return null
    }

    override fun getPopupActions(lineNumber: Int, editor: Editor?): MutableList<AnAction>? {
        return null
    }

    override fun gutterClosed() {
        // ignore
    }

}
