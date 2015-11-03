package io.github.sirlantis.rubymine.rubocop

import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.psi.PsiFile
import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiDocumentManager
import com.intellij.openapi.module.Module
import io.github.sirlantis.rubymine.rubocop.model.FileResult
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.openapi.editor.Document
import io.github.sirlantis.rubymine.rubocop.model.Offense
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme

class RubocopAnnotator : ExternalAnnotator<RubocopAnnotator.Input, RubocopAnnotator.Result>() {
    class Input(val module: Module,
                val file: PsiFile,
                val content: String,
                val colorScheme: EditorColorsScheme?)

    class Result(val input: Input,
                 val result: FileResult)

    val inspectionKey: HighlightDisplayKey by lazy {
        val id = "Rubocop"
        HighlightDisplayKey.find(id) ?: HighlightDisplayKey(id, id)
    }

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): Input? {
        return collectInformation(file, editor)
    }

    override fun collectInformation(file: PsiFile): Input? {
        return collectInformation(file, null)
    }

    fun collectInformation(file: PsiFile, editor: Editor?): Input? {
        if (file.context != null || !isRubyFile(file)) {
            return null
        }

        val virtualFile = file.virtualFile

        if (!virtualFile.isInLocalFileSystem) {
            return null
        }

        val project = file.project
        val module = RubocopTask.getModuleForFile(project, virtualFile) ?: return null
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return null

        return Input(module,
                file,
                document.text,
                editor?.colorsScheme)
    }

    fun isRubyFile(file: PsiFile): Boolean {
        return file.fileType.name == "Ruby"
    }

    override fun apply(file: PsiFile, annotationResult: Result?, holder: AnnotationHolder) {
        if (annotationResult == null) {
            return
        }

        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return

        annotationResult.result.offenses.forEach { offense ->
            val severity = severityForOffense(offense)
            createAnnotation(holder, document, offense, "RuboCop: ", severity, false)
            // TODO: offer fix option (at least suppress)
        }
    }

    fun severityForOffense(offense: Offense): HighlightSeverity {
        when (offense.severity) {
            "error" -> return HighlightSeverity.ERROR
            "fatal" -> return HighlightSeverity.ERROR
            "warning" -> return HighlightSeverity.WARNING
            "convention" -> return HighlightSeverity.WEAK_WARNING
            "refactor" -> return HighlightSeverity.INFO
            else -> return HighlightSeverity.INFO
        }
    }

    fun createAnnotation(holder: AnnotationHolder,
                         document: Document,
                         offense: Offense,
                         prefix: String,
                         severity: HighlightSeverity,
                         showErrorOnWholeLine: Boolean): Annotation {

        val offenseLine = offense.location.line - 1

        val lineEndOffset = document.getLineEndOffset(offenseLine)
        val lineStartOffset = document.getLineStartOffset(offenseLine)

        val range: TextRange

        if (showErrorOnWholeLine || offense.location.length <= 0) {
            range = TextRange(lineStartOffset, lineEndOffset)
        } else {
            val length = offense.location.length
            val start = lineStartOffset + (offense.location.column - 1)
            range = TextRange(start, start + length)
        }

        val message = prefix + offense.message.trim() + " (" + offense.cop + ")"
        return holder.createAnnotation(severity, range, message)
    }

    override fun doAnnotate(collectedInfo: Input?): Result? {
        if (collectedInfo == null) {
            return null
        }

        val task = RubocopTask.forFiles(collectedInfo.module, collectedInfo.file.virtualFile)

        task.run()

        val fileResult = task.result?.fileResults?.firstOrNull() ?: return null

        return Result(collectedInfo, fileResult)
    }

    companion object {
        val INSTANCE: RubocopAnnotator = RubocopAnnotator()
    }
}
