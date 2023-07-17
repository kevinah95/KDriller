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

package io.github.kevinah95.kdriller.domain

import io.github.kevinah95.kdriller.utils.Conf
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.*
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectLoader
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name

/**
 * Class representing a Commit. Contains all the important information such
 * as hash, author, dates, and modified files.
 *
 * @constructor Create a commit object.
 *
 * @param [commit] JGit Commit object
 * @param [conf] Configuration class
 */
@Suppress("MemberVisibilityCanBePrivate", "FunctionName")
data class Commit(@JvmField val commit: RevCommit, private val conf: Conf) {
    @JvmField val cObject = commit

    /**
     * Return the SHA of the commit.
     *
     * @return hash
     */
    val hash: String
        get() = cObject.id.name

    /**
     * Return the author of the commit as a Developer object.
     *
     * @return author
     */
    val author: Developer
        get() = Developer(cObject.authorIdent.name, cObject.authorIdent.emailAddress)

    /**
     * Return the committer of the commit as a Developer object.
     *
     * @return committer
     */
    val committer: Developer
        get() = Developer(cObject.committerIdent.name, cObject.committerIdent.emailAddress)

    /**
     * Return the project name.
     *
     * @return project name
     */
    val projectName: String
        get() = Path(conf.get("path_to_repo") as String).name

    /**
     * Return the absolute path of the project.
     *
     * @return project path
     */
    val projectPath: String
        get() = Path(conf.get("path_to_repo") as String).absolutePathString()

    /**
     * Return the authored datetime.
     *
     * @return datetime
     */
    val authorDate: Date
        get() = cObject.authorIdent.`when`

    /**
     * Return the committer datetime.
     *
     * @return datetime
     */
    val committerDate: Date
        get() = cObject.committerIdent.`when`

    /**
     * Author timezone expressed in seconds from epoch.
     *
     * @return TimeZone
     */
    val authorTimeZone: TimeZone
        get() = cObject.authorIdent.timeZone

    /**
     * Committer timezone expressed in seconds from epoch.
     *
     * @return TimeZone
     */
    val committerTimeZone: TimeZone
        get() = cObject.committerIdent.timeZone

    /**
     * Return commit message.
     *
     * @return String
     */
    val msg: String
        get() = cObject.fullMessage.trim()

    /**
     * Return the list of parents SHAs.
     *
     * @return list of parents
     */
    val parents: List<String>
        get() = cObject.parents.map { parent -> parent.name }

    /**
     * Return true if the commit is a merge, false otherwise.
     *
     * @return Boolean
     */
    val merge: Boolean
        get() = cObject.parents.size > 1

    /**
     * Return the number of added lines in the commit (as shown from --shortstat).
     *
     * @return Int insertion lines
     */
    val insertions: Int
        get() {
            var linesAdded = 0
            Git.open(Path(projectPath).resolve(".git").toFile()).use { git ->
                for (diffEntry in diffEntries!!) {
                    DiffFormatter(DisabledOutputStream.INSTANCE).use { diffFormatter ->
                        diffFormatter.setRepository(git.repository)
                        val fileHeader = diffFormatter.toFileHeader(diffEntry)

                        for (edit in fileHeader.toEditList()) {
                            linesAdded += edit.lengthB
                        }
                    }
                }
            }

            return linesAdded
        }

    /**
     * Return the number of deleted lines in the commit (as shown from --shortstat).
     *
     * @return Int deletion lines
     */
    val deletions: Int
        get() {
            var linesDeleted = 0
            Git.open(Path(projectPath).resolve(".git").toFile()).use { git ->
                for (diffEntry in diffEntries!!) {
                    DiffFormatter(DisabledOutputStream.INSTANCE).use { diffFormatter ->
                        diffFormatter.setRepository(git.repository)
                        val fileHeader = diffFormatter.toFileHeader(diffEntry)

                        for (edit in fileHeader.toEditList()) {
                            linesDeleted += edit.lengthA
                        }
                    }
                }
            }

            return linesDeleted
        }

    /**
     * Return the number of modified lines in the commit (as shown from --shortstat).
     *
     * @return Int insertion + deletion lines
     */
    val lines: Int
        get() = insertions + deletions

    /**
     * Return the number of modified files of the commit (as shown from --shortstat).
     *
     * @return Int modified files number
     */
    val files: Int
        get() = diffEntries!!.size

    /**
     * Return a list of modified files. The list is empty if the commit is
     * a merge commit. For more info on this, see
     * https://haacked.com/archive/2014/02/21/reviewing-merge-commits/ or
     * https://github.com/ishepard/pydriller/issues/89#issuecomment-590243707
     *
     * @return List<ModifiedFile> modifications
     */
    val modifiedFiles: List<ModifiedFile>
        get() {
            var diffIndex: List<DiffEntry>?
            Git.open(Path(projectPath).resolve(".git").toFile()).use { git ->
                RevWalk(git.repository).use { walk ->
                    if (parents.size == 1){
                        // the commit has a parent
                        val parentCommit = commit.getParent(0).name // TODO: oldTreeIterator
                        val parentCommitRev = walk.parseCommit(ObjectId.fromString(parentCommit))
                        val oldTreeIterator = getCanonicalTreeParser(parentCommitRev)
                        val newTreeIterator = getCanonicalTreeParser(cObject)
                        diffIndex = getDiffEntries(oldTreeIterator!!, newTreeIterator!!)
                    } else if (parents.size > 1){
                        diffIndex = listOf()
                    } else {
                        // this is the first commit of the repo. Comparing it with git NULL TREE
                        val oldTreeIterator = EmptyTreeIterator() // NULL_TREE
                        val newTreeIterator = getCanonicalTreeParser(cObject)
                        diffIndex = getDiffEntries(oldTreeIterator, newTreeIterator!!)
                    }

                    return _parseDiff(diffIndex)
                }
            }


        }

    fun _parseDiff(diffIndex: List<DiffEntry>?): List<ModifiedFile> {
        val modifiedFilesList = mutableListOf<ModifiedFile>()
        var parent: String? = null
        if (cObject.parents.size != 0){
            parent = commit.getParent(0).name
        }
        for (diff in diffIndex!!){
            modifiedFilesList.add(ModifiedFile(diff, projectPath, cObject.tree, parent, conf))
        }

        return modifiedFilesList
    }

    /**
     * Return True if the commit is in the main branch, False otherwise.
     *
     * @return Boolean in_main_branch
     */
    val inMainBranch: Boolean
        get() {
            val mainBranch = conf.get("main_branch") as String
            return mainBranch in branches
        }

    /**
     * Return the set of branches that contain the commit.
     *
     * @return Set<String> branches
     */
    val branches: Set<String>
        get() {
            // TODO: include_remotes
            // TODO: include_refs
            val branches = mutableSetOf<String>()
            Git.open(Path(projectPath).resolve(".git").toFile()).use { git ->
                git.branchList()
                val call: List<Ref> = git.branchList().setContains(hash).call()
                for (ref in call) {
                    branches.add(ref.name.substring(ref.name.lastIndexOf("/") + 1, ref.name.length))
                }
            }
            return branches
        }

    // TODO: dmm_unit_size
    // TODO: dmm_unit_complexity
    // TODO: dmm_unit_interfacing
    // TODO: _delta_maintainability
    // TODO: _delta_risk_profile
    // TODO: _good_change_proportion

    val diffEntries: List<DiffEntry>?
        get() {
            Git.open(Path(projectPath).resolve(".git").toFile()).use { git ->
                RevWalk(git.repository).use { walk ->
                    if (parents.size == 1){
                        // the commit has a parent
                        val parentCommit = commit.getParent(0).name // TODO: oldTreeIterator
                        val parentCommitRev = walk.parseCommit(ObjectId.fromString(parentCommit))
                        val oldTreeIterator = getCanonicalTreeParser(parentCommitRev)
                        val newTreeIterator = getCanonicalTreeParser(cObject)
                        return getDiffEntries(oldTreeIterator!!, newTreeIterator!!)
                    } else if (parents.size > 1){
                        return listOf<DiffEntry>()
                    } else {
                        val oldTreeIterator = EmptyTreeIterator()
                        val newTreeIterator = getCanonicalTreeParser(cObject)
                        return getDiffEntries(oldTreeIterator, newTreeIterator!!)
                    }

                }
            }
        }

    @Throws(IOException::class)
    private fun getDiffEntries(
        oldTreeIterator: AbstractTreeIterator,
        newTreeIterator: AbstractTreeIterator
    ): MutableList<DiffEntry>? {
        val diffAlgorithm = if (conf.get("histogram")!= null && conf.get("histogram") as Boolean) {
            HistogramDiff()
        } else {
            MyersDiff.INSTANCE
        }

        val rawTextComparator = if (conf.get("skip_whitespaces")!= null && conf.get("skip_whitespaces") as Boolean) {
            RawTextComparator.WS_IGNORE_ALL
        } else {
            RawTextComparator.DEFAULT
        }

        Git.open(Path(projectPath).resolve(".git").toFile()).use { git ->
            DiffFormatter(DisabledOutputStream.INSTANCE).use { diffFormatter ->
                diffFormatter.setRepository(git.repository)
                diffFormatter.setDiffAlgorithm(diffAlgorithm)
                diffFormatter.setDiffComparator(rawTextComparator)
                diffFormatter.isDetectRenames = true
                return diffFormatter.scan(oldTreeIterator, newTreeIterator)

            }
        }
    }

    @Throws(IOException::class)
    private fun getCanonicalTreeParser(commitId: ObjectId): AbstractTreeIterator? {
        Git.open(Path(projectPath).resolve(".git").toFile()).use { git ->
            RevWalk(git.repository).use { walk ->
                val commit: RevCommit = walk.parseCommit(commitId)
                val treeId = commit.tree.id
                git.repository.newObjectReader().use { reader ->
                    return CanonicalTreeParser(null, reader, treeId)
                }
            }
        }
    }

}