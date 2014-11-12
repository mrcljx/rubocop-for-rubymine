package io.github.sirlantis.rubymine.rubocop

import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.roots.ProjectRootManager
import java.io.File
import com.intellij.openapi.project.ProjectLocator
import java.io.IOException
import java.util.LinkedList
import java.io.InputStreamReader
import io.github.sirlantis.rubymine.rubocop.model.RubocopResult
import com.intellij.psi.impl.file.impl.FileManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import kotlin.modules.module
import java.io.Closeable
import java.io.BufferedInputStream
import com.intellij.execution.configurations.GeneralCommandLine

class RubocopTask(val module: Module, val paths: List<String>) : Task.Backgroundable(module.getProject(), "Running RuboCop", true) {
    var result: RubocopResult? = null

    val sdk: Sdk
    {
        sdk = getSdkForModule(module)
    }

    val sdkRoot: String
        get() {
            return sdk.getHomeDirectory().getParent().getCanonicalPath()
        }

    override fun run(indicator: ProgressIndicator) {
        run()
    }

    fun run() {
        if (!isRubySdk(sdk)) {
            logger.warn("Not a Ruby SDK")
            return
        }

        runViaCommandLine()
    }

    fun parseProcessOutput(start: () -> Process) {
        val process: Process

        try {
            process = start.invoke()
        } catch (e: Exception) {
            logger.error("Failed to run RuboCop command - is it (or bundler) installed? (SDK=%s)".format(sdkRoot), e)
            return
        }

        val bufferSize = 5 * 1024 * 1024
        val stdoutStream = BufferedInputStream(process.getInputStream(), bufferSize)
        val stdoutReader = InputStreamReader(stdoutStream)

        val stderrStream = BufferedInputStream(process.getErrorStream(), bufferSize)

        try {
            result = RubocopResult.readFromReader(stdoutReader)
        } catch (e: Exception) {
            logger.error("Failed to parse RuboCop output.", e)

            logger.error("ERROR", InputStreamReader(stderrStream).readText())
            tryClose(stderrStream)

            stdoutStream.reset()
            logger.error("OUTPUT", InputStreamReader(stdoutStream).readText())
            tryClose(stdoutStream)

            return
        }

        try {
            process.waitFor()

            if (process.exitValue() != 0) {
                logger.warn("RuboCop exited with %d".format(process.exitValue()))
            }
        } catch (e: Exception) {
            logger.error("Interrupted while waiting for RuboCop.", e)
        }

        tryClose(stdoutStream)
        tryClose(stderrStream)

        onComplete?.invoke(this)
    }

    fun runViaCommandLine() {
        val commandLine = GeneralCommandLine()
        commandLine.setExePath(File(sdkRoot, "rubocop").canonicalPath)

        val app = ApplicationManager.getApplication()

        app.runReadAction {
            val roots = ModuleRootManager.getInstance(module).getContentRoots()
            val root = File(roots.first().getCanonicalPath())
            commandLine.setWorkDirectory(root)
            commandLine.setExePath(File(sdkRoot, "bundle").canonicalPath)
            commandLine.addParameters("exec", "rubocop")
        }

        commandLine.addParameters("--format", "json")
        commandLine.addParameters(paths)

        logger.debug("Executing RuboCop", commandLine.getCommandLineString())

        parseProcessOutput { commandLine.createProcess() }
    }

    fun tryClose(closable: Closeable?) {
        if (closable != null) {
            try {
                closable.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    var onComplete: ((RubocopTask) -> Unit)? = null

    class object {
        val logger = Logger.getInstance(RubocopBundle.LOG_ID)

        fun isRubySdk(sdk: Sdk): Boolean {
            return sdk.getSdkType().getName() == "RUBY_SDK"
        }

        fun getModuleForFile(project: Project, file: VirtualFile): Module {
            return ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(file)
        }

        fun getFirstRubyModuleForProject(project: Project): Module {
            val modules = ModuleManager.getInstance(project).getModules()
            return modules first { isRubySdk(ModuleRootManager.getInstance(it).getSdk()) }
        }

        fun getSdkForModule(module: Module): Sdk {
            return ModuleRootManager.getInstance(module).getSdk()
        }

        fun forFiles(vararg files: VirtualFile): RubocopTask {
            kotlin.check(files.count() > 0) { "files must not be empty" }
            val project = ProjectLocator.getInstance().guessProjectForFile(files.first())
            val module = getModuleForFile(project, files.first())
            return forFiles(module, *files)
        }

        fun forFiles(module: Module, vararg files: VirtualFile): RubocopTask {
            kotlin.check(files.count() > 0) { "files must not be empty" }
            val paths = files map { it.getCanonicalPath() }
            return RubocopTask(module, paths)
        }

        fun forPaths(module: Module, vararg paths: String): RubocopTask {
            return RubocopTask(module, paths.toList())
        }
    }
}
