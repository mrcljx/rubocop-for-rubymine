package io.github.sirlantis.rubymine.rubocop

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ex.UnfairLocalInspectionTool
import com.intellij.codeInspection.BatchSuppressableTool
import com.intellij.openapi.util.Key
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiFile
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ExternalAnnotatorInspectionVisitor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.codeInspection.SuppressQuickFix

class RubocopInspection : LocalInspectionTool(), BatchSuppressableTool, UnfairLocalInspectionTool {
    companion object {
        val INSPECTION_SHORT_NAME: String = "RubocopInspection"
        val KEY: Key<RubocopInspection> = Key.create(INSPECTION_SHORT_NAME)
        val LOG = Logger.getInstance(RubocopBundle.LOG_ID)
    }

    override fun getStaticDescription(): String? {
        return "Uses RuboCop for linting.<br>Make sure Rubocop gem is installed.<br><br><b>Note:</b> Selected color doesn't have an effect!"
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<out ProblemDescriptor>? {
        return ExternalAnnotatorInspectionVisitor.checkFileWithExternalAnnotator(file, manager, isOnTheFly, RubocopAnnotator.INSTANCE)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return ExternalAnnotatorInspectionVisitor(holder, RubocopAnnotator.INSTANCE, isOnTheFly)
    }

    override fun getID(): String {
        return "Settings.Ruby.Linters.Rubocop"
    }

    override fun isSuppressedFor(element: PsiElement): Boolean {
        return false
    }

    override fun runForWholeFile(): Boolean {
        return true
    }

    override fun getBatchSuppressActions(element: PsiElement?): Array<out SuppressQuickFix> {
        return arrayOf()
    }
}
