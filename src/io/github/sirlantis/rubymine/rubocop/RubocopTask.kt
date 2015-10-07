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
import com.intellij.openapi.project.ProjectLocator
import java.io.InputStreamReader
import io.github.sirlantis.rubymine.rubocop.model.RubocopResult
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import java.io.Closeable
import java.io.BufferedInputStream
import com.intellij.openapi.application.Application
import org.jetbrains.plugins.ruby.ruby.run.RunnerUtil
import io.github.sirlantis.rubymine.rubocop.utils.NotifyUtil
import org.jetbrains.plugins.ruby.gem.util.BundlerUtil
import java.util.*

class RubocopTask(val module: Module, val paths: List<String>) : Task.Backgroundable(module.project, "Running RuboCop", true) {

    var result: RubocopResult? = null

    val sdk: Sdk? by lazy {
        val candidate = getSdkForModule(module) ?: null

        if (candidate != null && isRubySdk(candidate)) {
            candidate
        } else {
            null
        }
    }

    val sdkRoot: String? by lazy {
        sdk?.homeDirectory?.parent?.canonicalPath
    }

    override fun run(indicator: ProgressIndicator) {
        run()
    }

    fun run() {
        val localSdk = sdk ?: return

        if (!isRubySdk(localSdk)) {
            logger.warn("Not a Ruby SDK")
            return
        }

        if (!hasRubocopConfig) {
            logger.warn("Didn't find $RUBOCOP_CONFIG_FILENAME")
            return
        }

        runViaCommandLine(localSdk)
    }

    fun parseProcessOutput(start: () -> Process) {
        val process: Process

        try {
            process = start.invoke()
        } catch (e: Exception) {
            logger.warn("Failed to run RuboCop command", e)
            logger.error("Failed to run RuboCop command - is it (or bundler) installed? (SDK=%s)".format(sdkRoot), e)
            return
        }

        val bufferSize = 5 * 1024 * 1024

        val stdoutStream = BufferedInputStream(process.inputStream, bufferSize)
        val stdoutReader = InputStreamReader(stdoutStream)

        val stderrStream = BufferedInputStream(process.errorStream, bufferSize)
        val stderrReader = InputStreamReader(stderrStream)

        try {
            result = RubocopResult.readFromReader(stdoutReader, stderrReader)
        } catch (e: Exception) {
            logger.warn("Failed to parse RuboCop output", e)
            logParseFailure(stderrStream, stdoutStream)
        }

        var exited = false

        try {
            process.waitFor()
            exited = true
        } catch (e: Exception) {
            logger.error("Interrupted while waiting for RuboCop", e)
        }

        tryClose(stdoutStream)
        tryClose(stderrStream)

        if (exited) {
            when (process.exitValue()) {
                0, 1 -> logger.warn("RuboCop exited with %d".format(process.exitValue()))
                else -> logger.info("RuboCop exited with %d".format(process.exitValue()))
            }
        }

        if (result != null) {
            onComplete?.invoke(this)
        }
    }

    private fun logParseFailure(stderrStream: BufferedInputStream, stdoutStream: BufferedInputStream) {
        val stdout = readStreamToString(stdoutStream, true)
        val stderr = readStreamToString(stderrStream, true)

        logger.warn("=== RuboCop STDOUT START ===\n%s\n=== RuboCop STDOUT END ===".format(stdout))
        logger.warn("=== RuboCop STDERR START ===\n%s\n=== RuboCop STDERR END ===".format(stderr))

        val errorBuilder = StringBuilder("Please make sure that:")
        errorBuilder.append("<ul>")

        if (usesBundler) {
            errorBuilder.append("<li>you added <code>gem 'rubocop'</code> to your <code>Gemfile</code></li>")
            errorBuilder.append("<li>you did run <code>bundle install</code> successfully</li>")
        } else {
            errorBuilder.append("<li>you installed RuboCop for this Ruby version</li>")
        }

        errorBuilder.append("<li>your RuboCop version isn't ancient</li>")
        errorBuilder.append("</ul>")

        errorBuilder.append("<pre><code>")
        errorBuilder.append(stderr)
        errorBuilder.append("</code></pre>")

        NotifyUtil.notifyError(project, "Failed to parse RuboCop output", errorBuilder.toString())
    }

    fun readStreamToString(stream: BufferedInputStream, reset: Boolean = false): String {
        if (reset) {
            try {
                stream.reset()
            } catch(e: Exception) {
                logger.warn("Couldn't reset stream", e)
                return ""
            }
        }

        var result: String

        try {
            result = InputStreamReader(stream).readText()
        } catch (e: Exception) {
            logger.warn("Couldn't read stream", e)
            result = ""
        }

        return result
    }

    fun runViaCommandLine(sdk: Sdk) {
        val runner = RunnerUtil.getRunner(sdk, module)

        val commandLineList = linkedListOf("rubocop", "--format", "json")
        commandLineList.addAll(paths)

        if (usesBundler) {
            prepareBundler(commandLineList)
        }

        // prepend ruby binary to the command
        val rubyInterpreterExecutable = sdk.getHomePath();
        commandLineList.add(0, rubyInterpreterExecutable)

        val command = commandLineList.removeFirst()
        val args = commandLineList.toTypedArray()
        val sudo = false

        val commandLine = runner.createAndSetupCmdLine(workDirectory.canonicalPath!!, null, true, command, sdk, sudo, *args)

        if (usesBundler) {
            val preprocessor = BundlerUtil.createBundlerPreprocessor(module, sdk)
            preprocessor.preprocess(commandLine)
        }

        logger.debug("Executing RuboCop (SDK=%s, Bundler=%b)".format(sdkRoot, usesBundler), commandLine.commandLineString)

        parseProcessOutput { commandLine.createProcess() }
    }

    private fun prepareBundler(commandLineList: LinkedList<String>) {
        val bundler = BundlerUtil.getBundlerGem(sdk, module, true) ?: return
        val bundleCommand = bundler.file?.findChild("bin")?.findChild("bundle")?.canonicalPath ?: return
        commandLineList.addAll(0, linkedListOf(bundleCommand, "exec"))
    }

    val app: Application by lazy {
        ApplicationManager.getApplication()
    }

    val workDirectory: VirtualFile by lazy {
        var file: VirtualFile? = null

        app.runReadAction {
            val roots = ModuleRootManager.getInstance(module).contentRoots
            file = roots.first()
        }

        file as VirtualFile
    }

    val usesBundler: Boolean
        get() {
            // TODO: better check possible?
            return workDirectory.findChild("Gemfile") != null
        }

    val hasRubocopConfig: Boolean
        get() {
            return workDirectory.findChild(RUBOCOP_CONFIG_FILENAME) != null
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

    companion object {
        val logger = Logger.getInstance(RubocopBundle.LOG_ID)
        val RUBOCOP_CONFIG_FILENAME: String = ".rubocop.yml"

        fun isRubySdk(sdk: Sdk): Boolean {
            return sdk.sdkType.name == "RUBY_SDK"
        }

        fun getModuleForFile(project: Project, file: VirtualFile): Module? {
            return ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(file)
        }

        fun getFirstRubyModuleForProject(project: Project): Module? {
            val modules = ModuleManager.getInstance(project).modules

            return modules.firstOrNull {
                val sdk = ModuleRootManager.getInstance(it).sdk
                sdk != null && isRubySdk(sdk)
            }
        }

        fun getSdkForModule(module: Module): Sdk? {
            return ModuleRootManager.getInstance(module).sdk
        }

        fun forFiles(vararg files: VirtualFile): RubocopTask? {
            kotlin.check(files.count() > 0) { "files must not be empty" }
            val project = ProjectLocator.getInstance().guessProjectForFile(files.first()) ?: return null
            val module = getModuleForFile(project, files.first()) ?: return null
            return forFiles(module, *files)
        }

        fun forFiles(module: Module, vararg files: VirtualFile): RubocopTask {
            kotlin.check(files.count() > 0) { "files must not be empty" }

            val paths = files.flatMap {
                if (it.canonicalPath != null) {
                    listOf(it.canonicalPath!!)
                } else {
                    listOf()
                }
            }

            return RubocopTask(module, paths)
        }

        fun forPaths(module: Module, vararg paths: String): RubocopTask {
            return RubocopTask(module, paths.toList())
        }
    }
}
