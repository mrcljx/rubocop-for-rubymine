package io.github.sirlantis.rubymine.rubocop

import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.ui.content.ContentFactory
import io.github.sirlantis.rubymine.rubocop.ui.ReportPane
import java.util.HashMap
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import io.github.sirlantis.rubymine.rubocop.model.RubocopResult
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import java.io.File
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.editor.Document
import io.github.sirlantis.rubymine.rubocop.ui.RubocopAnnotationGutterProvider
import java.util.HashSet
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.module.ModuleManager
import io.github.sirlantis.rubymine.rubocop.model.FileResult

class RubocopPlugin(val project: Project) : ProjectComponent, EditorFactoryListener, DocumentListener {

    var rubocopResult: RubocopResult? = null
    val DETAILS_TOOL_WINDOW_ID = "Details";
    val editors: MutableSet<Editor> = HashSet()

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.getEditor()
        editor.getDocument().removeDocumentListener(this)
        editors.remove(editor)
    }

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.getEditor()
        editors.add(editor)
        editor.getDocument().addDocumentListener(this)
        setupGutterAnnotationsForEditor(editor)
    }

    override fun projectClosed() {

    }

    override fun projectOpened() {
        registerToolWindow()
        updateAllEditorGutters()
        registerAsEditorFactoryListener()
    }

    override fun disposeComponent() {}

    override fun initComponent() {}

    override fun getComponentName() = "RubocopPlugin"

    fun registerToolWindow() {
        var toolWindowManager = ToolWindowManager.getInstance(project)
        var toolWindow = toolWindowManager.registerToolWindow(DETAILS_TOOL_WINDOW_ID, false, ToolWindowAnchor.RIGHT)
        var contentFactory = ContentFactory.SERVICE.getInstance()
        var reportPane = ReportPane()

        var content = contentFactory.createContent(reportPane, "", true)
        toolWindow.getContentManager().addContent(content)
    }

    fun updateAllEditorGutters() {
        for (editor in editors) {
            editor.getGutter().closeAllAnnotations()
            setupGutterAnnotationsForEditor(editor)
        }
    }

    fun determineRelativePathFromFileToDirectory(file: VirtualFile, directory: VirtualFile): String {
        val directoryURI = File(directory.getPath()).toURI()
        val fileURI = File(file.getPath()).toURI()
        return directoryURI.relativize(fileURI).getPath()
    }

    fun fileResultForFile(file: VirtualFile): FileResult? {
        val module = RubocopTask.getModuleForFile(project, file)
        val root = ModuleRootManager.getInstance(module).getContentRoots().first()
        val relativeFilePath = determineRelativePathFromFileToDirectory(file, root)
        return rubocopResult?.getFileResult(relativeFilePath)
    }

    fun setupGutterAnnotationsForEditor(editor: Editor) {
        val file = FileDocumentManager.getInstance().getFile(editor.getDocument())

        if (file == null) {
            return
        }

        val fileResult = fileResultForFile(file)

        if (fileResult != null) {
            val annotationProvider = RubocopAnnotationGutterProvider(project, fileResult)
            editor.getGutter().registerTextAnnotation(annotationProvider)
        }
    }

    fun registerAsEditorFactoryListener() {
        EditorFactory.getInstance().addEditorFactoryListener(this, Disposable() {
            fun dispose() {}
        })
    }

    override fun beforeDocumentChange(event: DocumentEvent?) {
        // ignore
    }

    var checkOnChange: Boolean = false

    fun shouldRunForFile(file: VirtualFile): Boolean {
        val extension = file.getExtension()

        // TODO: detect via IntelliJ registered file types
        return array("rb", "rake", "ru", "Gemfile").contains(extension)
    }

    override fun documentChanged(event: DocumentEvent?) {
        if (event == null) {
            return
        }

        if (!checkOnChange) {
            return
        }

        val file = FileDocumentManager.getInstance().getFile(event.getDocument())

        if (!shouldRunForFile(file)) {
            return
        }

        val module = RubocopTask.getModuleForFile(project, file)

        val text = event.getDocument().getText()
        val temporaryFile = File.createTempFile("r4r", ".rb")
        temporaryFile.writeText(text)
        val task = RubocopTask.forPaths(module, temporaryFile.canonicalPath)
        queueTask(task)
        // temporaryFile.delete()
    }

    fun beforeDocumentSaving(document: Document) {
        val file = FileDocumentManager.getInstance().getFile(document)

        if (!shouldRunForFile(file)) {
            return
        }

        val module = RubocopTask.getModuleForFile(project, file)
        val task = RubocopTask.forFiles(module, file)
        queueTask(task)
    }

    fun queueTask(task: RubocopTask) {
        task.onComplete = {
            rubocopResult = RubocopResult.merge(rubocopResult, it.result)
            ApplicationManager.getApplication().invokeLater { updateAllEditorGutters() }
        }
        task.queue()
    }
}
