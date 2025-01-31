package liveplugin.pluginrunner

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import kotlin.Unit
import liveplugin.FilePath
import liveplugin.Result
import liveplugin.pluginrunner.groovy.GroovyPluginRunner
import org.junit.After
import org.junit.Before
import org.junit.Test

import static liveplugin.Result.Failure
import static liveplugin.Result.Success
import static liveplugin.pluginrunner.AnError.RunningError
import static liveplugin.pluginrunner.groovy.GroovyPluginRunner.mainScript

class GroovyPluginRunnerTest {
	static final Binding noBindings = new Binding(null, false, "", Disposer.newDisposable())
	static final LinkedHashMap emptyEnvironment = [:]
	private final GroovyPluginRunner pluginRunner = new GroovyPluginRunner(mainScript, emptyEnvironment)
	private File rootFolder
	private File myPackageFolder


	@Test void "run correct groovy script without errors"() {
		def scriptCode = """
			// import to ensure that script has access to parent classloader from which test is run
			import com.intellij.openapi.util.io.FileUtil

			// some groovy code
			def a = 1
			def b = 2
			a + b
		"""
		createFile("plugin.groovy", scriptCode, rootFolder)
		def result = runPlugin()

		assert result instanceof Success
	}

	@Test void "run incorrect groovy script with errors"() {
		def scriptCode = """
			invalid code + 1
		"""
		createFile("plugin.groovy", scriptCode, rootFolder)
		def result = runPlugin()

		assert result instanceof Failure
		assert (result.reason as RunningError).throwable.toString().startsWith("groovy.lang.MissingPropertyException")
	}

	@Test void "run groovy script which uses groovy class from another file"() {
		def scriptCode = """
			import myPackage.Util
			Util.myFunction()
		"""
		def scriptCode2 = """
			package myPackage
			class Util {
				static myFunction() { 42 }
			}
		"""
		createFile("plugin.groovy", scriptCode, rootFolder)
		createFile("Util.groovy", scriptCode2, myPackageFolder)

		def result = runPlugin()

		assert result instanceof Success
	}

	@Before void setup() {
		PluginClassLoader_Fork.class.classLoader.defaultAssertionStatus = false // This is to avoid "Core loader must be not specified in parents" error.
		rootFolder = FileUtil.createTempDirectory("", "")
		myPackageFolder = new File(rootFolder, "myPackage")
		myPackageFolder.mkdir()
	}

	@After void teardown() {
		FileUtil.delete(rootFolder)
	}

	private Result<Unit, AnError> runPlugin() {
		def result = pluginRunner.setup(new LivePlugin(new FilePath(rootFolder.absolutePath)), null)
		pluginRunner.run(result.value, noBindings)
	}

	static createFile(String fileName, String fileContent, File directory) {
		def file = new File(directory, fileName)
		file.write(fileContent)
		file
	}
}
