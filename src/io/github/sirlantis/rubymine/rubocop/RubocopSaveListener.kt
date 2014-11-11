package io.github.sirlantis.rubymine.rubocop

import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.Task
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.project.ProjectLocator
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId

class RubocopSaveListener : FileDocumentManagerAdapter() {
    override fun beforeDocumentSaving(document: Document) {
        val file = FileDocumentManager.getInstance().getFile(document)
        val project = ProjectLocator.getInstance().guessProjectForFile(file)
        val plugin = project.getComponent(javaClass<RubocopPlugin>())
        plugin.beforeDocumentSaving(document)
    }
}
