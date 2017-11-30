package liveplugin

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import java.io.File
import java.net.URL
import java.util.*
import java.util.Collections.emptyList
import java.util.regex.Pattern
import kotlin.coroutines.experimental.buildSequence


object MyFileUtil {
    fun File.filesList(): List<File> = listFiles()?.toList() ?: emptyList()

    fun fileNamesMatching(regexp: String, path: String): List<String> = fileNamesMatching(regexp, File(path))

    private fun fileNamesMatching(regexp: String, path: File): List<String> =
        FileUtil.findFilesByMask(Pattern.compile(regexp), path).map { it.name }

    fun asUrl(file: File): String = file.toURI().toURL().toString()

    fun File.toUrl(): URL = this.toURI().toURL()!!

    fun findScriptFileIn(path: String?, fileName: String): File? {
        if (path == null) return null

        val result = findScriptFilesIn(path, fileName)
        return when {
            result.isEmpty() -> null
            result.size == 1 -> result[0]
            else -> {
                val filePaths = result.map { it.absolutePath }
                throw IllegalStateException("Found several scripts files under " + path + ":\n" + StringUtil.join(filePaths, ";\n"))
            }
        }
    }

    fun findScriptFilesIn(path: String, fileName: String): List<File> {
        val rootScriptFile = File(path + File.separator + fileName)
        return if (rootScriptFile.exists()) listOf(rootScriptFile)
        else allFilesInDirectory(File(path)).filter { fileName == it.name }.toList()
    }

    fun allFilesInDirectory(dir: File): Sequence<File> = buildSequence {
        val queue = LinkedList(listOf(dir))
        while (queue.isNotEmpty()) {
            val currentDir = queue.removeLast()
            val files = currentDir.listFiles() ?: emptyArray()
            for (file in files) {
                if (file.isFile) yield(file)
                else if (file.isDirectory) queue.addFirst(file)
            }
        }
    }

    fun readLines(url: String) =
        URL(url).openStream()
            .bufferedReader()
            .readLines()
            .dropLastWhile { it.isEmpty() }
}
