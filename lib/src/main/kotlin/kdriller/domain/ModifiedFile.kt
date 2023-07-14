/*
 * Copyright 2023 Kevin Hernández
 * Copyright 2018-2023 Davide Spadini
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kdriller.domain

import kdriller.utils.Conf
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.*
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.MessageWriter
import java.io.IOException
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

/**
 * Type of Modification. Can be ADD, COPY, RENAME, DELETE, MODIFY or UNKNOWN.
 */
enum class ModificationType(val value: Int) {
    ADD(1),
    COPY(2),
    RENAME(3),
    DELETE(4),
    MODIFY(5),
    UNKNOWN(6)
}

@Suppress("FunctionName")
data class ModifiedFile(
    val diffEtry: DiffEntry,
    private val projectPath: String,
    private val tree: RevTree,
    private val parent: String?,
    val conf: Conf
) {
    val cDiff = diffEtry
    var nloc = null
    var complexity = null
    var tokenCount = null
    var functionList: List<Method> = listOf()
    var functionListBefore: List<Method> = listOf()

    val changeType: ModificationType
        get() = _fromChangeToModificationType(cDiff)

    companion object {
        fun _fromChangeToModificationType(diff: DiffEntry): ModificationType {
            return when (diff.changeType) {
                DiffEntry.ChangeType.ADD -> ModificationType.ADD
                DiffEntry.ChangeType.DELETE -> ModificationType.DELETE
                DiffEntry.ChangeType.RENAME -> ModificationType.RENAME
                DiffEntry.ChangeType.MODIFY -> ModificationType.MODIFY
                DiffEntry.ChangeType.COPY -> ModificationType.COPY
                else -> {
                    ModificationType.UNKNOWN
                }
            }
        }
    }

    val diff: String
        get() {
            return _getDecodedString()
        }

    private fun _getDecodedString(): String {
        Git.open(Path(projectPath).resolve(".git").toFile()).use { git ->
            MessageWriter().use { writer ->
                writer.rawStream.use { output ->
                    DiffFormatter(output).use { df ->
                        // TODO: Improve this:
                        val diffAlgorithm = if (conf.get("histogram") != null && conf.get("histogram") as Boolean) {
                            HistogramDiff()
                        } else {
                            MyersDiff.INSTANCE
                        }

                        val rawTextComparator =
                            if (conf.get("skip_whitespaces") != null && conf.get("skip_whitespaces") as Boolean) {
                                RawTextComparator.WS_IGNORE_ALL
                            } else {
                                RawTextComparator.DEFAULT
                            }

                        df.setRepository(git.repository)
                        df.setDiffAlgorithm(diffAlgorithm)
                        df.setDiffComparator(rawTextComparator)
                        df.format(cDiff)
                    }
                    return output.toString()
                }
            }
        }
    }

    val content: ByteArray?
        get() {
            return getCommitContent(cDiff.newPath)
        }

    val contentBefore: ByteArray?
        get() {
            return getCommitContent(cDiff.newPath, true)
        }

    val sourceCode: String?
        get() {
            val tempContent = content
            return if(tempContent != null){
                _getDecodedContent(tempContent)
            } else{
                null
            }
        }

    val sourceCodeBefore: String?
        get() {
            val tempContentBefore = contentBefore
            return if(tempContentBefore != null){
                _getDecodedContent(tempContentBefore)
            } else{
                null
            }
        }

    val addedLines: Int
        get() {
            var addedLines = 0
            for(line in diff.replace("\r", "").split("\n")){
                if (line.startsWith("+") && !line.startsWith("+++")){
                    addedLines++
                }
            }
            return addedLines
        }

    val deletedLines: Int
        get() {
            var deletedLines = 0
            for(line in diff.replace("\r", "").split("\n")){
                if (line.startsWith("-") && !line.startsWith("---")){
                    deletedLines++
                }
            }
            return deletedLines
        }

    val diffParsed: Map<String, List<Pair<Int, String>>>
        get() {
            val modifiedLines = mutableMapOf<String, MutableList<Pair<Int, String>>>(
                "added" to mutableListOf(),
                "deleted" to mutableListOf()
            )
            Scanner(diff).use { scanner ->
                var countDeletions = 0
                var countAdditions = 0
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    countDeletions += 1
                    countAdditions += 1

                    if (line.startsWith("@@")) {
                        val (delitions, additions) = _getLineNumbers(line)
                        countDeletions = delitions
                        countAdditions = additions
                    }
                    if (line.startsWith("-") && !line.startsWith("---")) {
                        modifiedLines["deleted"]?.add(Pair(countDeletions, line.substring(1)))
                        countAdditions -= 1
                    }

                    if (line.startsWith("+") && !line.startsWith("+++")) {
                        modifiedLines["added"]?.add(Pair(countAdditions, line.substring(1)))
                        countDeletions -= 1
                    }

                    if (line == "\\ No newline at end of file") {
                        countDeletions -= 1
                        countAdditions -= 1
                    }
                }

                return modifiedLines
            }
        }

    fun _getLineNumbers(line: String): Pair<Int, Int> {
        val token = line.split(" ")
        val numbersOldFile = token[1]
        val numbersNewFile = token[2]
        val deleteLineNumber = (
                (numbersOldFile.split(",")[0].replace("-", "")).toInt() - 1
                )
        val additionsLineNumber = (numbersNewFile.split(",")[0]).toInt() - 1
        return Pair(deleteLineNumber, additionsLineNumber)
    }


    /**
     * Old path of the file. Can be null if the file is added.
     *
     * @return String old path
     */
    val oldPath: String?
        get() {
            if (cDiff.oldPath != DiffEntry.DEV_NULL) {
                return cDiff.oldPath
            }
            return null
        }

    /**
     * New path of the file. Can be null if the file is deleted.
     *
     * @return String new path
     */
    val newPath: String?
        get() {
            if (cDiff.newPath != DiffEntry.DEV_NULL) {
                return cDiff.newPath
            }
            return null
        }

    /**
     * Return the filename. Given a path-like-string (e.g.
     * "/Users/dspadini/pydriller/myfile.py") returns only the filename
     * (e.g. "myfile.py")
     *
     * @return String filename
     */
    val filename: String
        get() {
            val path: String?
            if (newPath != null) {
                path = newPath
            } else {
                assert(oldPath != null)
                path = oldPath
            }
            return path?.let { Path(it).name }!!
        }

    val filepath: String
        get() {
            val path: String?
            if (newPath != null) {
                path = newPath
            } else {
                assert(oldPath != null)
                path = oldPath
            }
            return path?.let { Path(it).pathString }!!
        }

    @Throws(IOException::class)
    private fun getCommitContent(path: String, walkToParent: Boolean = false): ByteArray? {
        Git.open(Path(projectPath).resolve(".git").toFile()).use { git ->
            var treeToWalk: RevTree
            if (walkToParent && parent != null) {
                RevWalk(git.repository).use { walk ->
                    treeToWalk = walk.parseCommit(git.repository.resolve(parent)).tree
                }
            } else {
                treeToWalk = tree
            }


            TreeWalk.forPath(git.repository, path, treeToWalk).use { treeWalk ->
                val blobId: ObjectId = treeWalk.getObjectId(0)
                git.repository.newObjectReader().use { objectReader ->
                    val objectLoader: ObjectLoader = objectReader.open(blobId)
                    return objectLoader.bytes
                }
            }
        }
    }

    private fun _getDecodedContent(content: ByteArray) : String?{
        return try {
            String(content, StandardCharsets.UTF_8)
        } catch (e: Exception){
            logger.debug("Could not load the content for file $filename")
            null
        }
    }
}
