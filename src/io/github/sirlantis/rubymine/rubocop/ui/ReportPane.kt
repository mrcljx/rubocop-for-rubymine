package io.github.sirlantis.rubymine.rubocop.ui

import com.intellij.ui.components.JBScrollPane
import javax.swing.JEditorPane

class ReportPane : JBScrollPane() {
    val editorPane: JEditorPane = JEditorPane();
    {
        editorPane.setContentType("text/html")
        editorPane.setEditable(false)
        setViewportView(editorPane)
    }

    fun setUrl(url: String) {
        editorPane.setPage(url)
    }
}
