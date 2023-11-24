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

package io.github.kevinah95.kdriller

/**
 * This module includes 1 class, Git, representing a repository in Git.
 */

import io.github.kevinah95.kdriller.domain.Commit
import io.github.kevinah95.kdriller.domain.ModificationType
import io.github.kevinah95.kdriller.domain.ModifiedFile
import io.github.kevinah95.kdriller.utils.Conf
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.AndRevFilter
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name


private val logger = KotlinLogging.logger {}

/**
 * Class representing a repository in Git. It contains most of the logic of
 * PyDriller: obtaining the list of commits, checkout, reset, etc.
 *
 * @constructor Init the Git Repository.
 *
 * @param [path] path to the repository
 * @property [repo] JGit object Repository
 */
@Suppress("FunctionName", "MemberVisibilityCanBePrivate")
class Git @JvmOverloads constructor(path: String, var conf: Conf? = null) : Closeable {

    var path: Path
    val projectName: String
    private var _repo: Repository? = null
    private val _conf: Conf

    init {
        this.path = Path(path)
        projectName = this.path.name

        // if no configuration is passed, then creates a new "empty" one
        // with just "path_to_repo" inside.
        if (conf == null) {
            conf = Conf(
                mapOf(
                    "path_to_repo" to this.path.toString(),
                    "git" to this
                )
            )
        }

        _conf = conf!!
        _conf.setValue("main_branch", null) // init main_branch to None

        // Initialize repository
        _openRepository()
    }


    var repo: Repository? = null
        get() {
            if (_repo == null) {
                _openRepository()
            }

            assert(_repo != null)

            return _repo
        }


    private fun _openRepository() {
        val builder = FileRepositoryBuilder()

        // TODO: set blame config
        //var conf = Config()
        //conf.setBoolean("blame", null, "markUnblamableLines", true)

        // Needs to resolve .git directory because jgit read from this folder
        _repo = builder.setGitDir(path.resolve(".git").toFile())
            .readEnvironment()
            .findGitDir()
            .build()

        if (_conf.get("main_branch") == null) {
            _discoverMainBranch(_repo)
        }

    }

    private fun _discoverMainBranch(repo: Repository?) {
        try {
            _conf.setValue("main_branch", repo?.branch)
        } catch (e: Exception) {
            // The current HEAD is detached. In this case, it doesn't belong to
            // any branch, hence we return an empty string
            logger.info("HEAD is a detached symbolic reference, setting main branch to empty string")
            _conf.setValue("main_branch", "")
        }
    }

    /**
     * Get the head commit.
     *
     * @return [Commit] of the head commit
     */
    fun getHead(): Commit {
        val headId: ObjectId = repo?.resolve(Constants.HEAD)!!

        RevWalk(repo).use { walk ->
            val headCommit = walk.parseCommit(headId)
            return Commit(headCommit, _conf)
        }
    }

    /**
     * Return a generator of commits of all the commits in the repo.
     *
     * @return the generator of all the commits in the repo
     */
    fun getListCommits(rev: String = Constants.HEAD, revFilter: List<RevFilter> = listOf()) = sequence<Commit> {
        val order = _conf.get("order")

        var revSort = when (order) {
            null -> RevSort.REVERSE
            "reverse" -> RevSort.REVERSE
            "topo-order" -> RevSort.TOPO
            "date-order" -> RevSort.COMMIT_TIME_DESC
            else -> RevSort.REVERSE
        }

        if (_conf.get("reverse") != null) {
            revSort = RevSort.NONE
        }

        val fromCommit = _conf.get("from_commit") as String?
        val toCommit = _conf.get("to_commit") as String?

        val revId: ObjectId? = if (_conf.get("single") != null) {
            repo?.resolve(_conf.get("single") as String)?.toObjectId()
        } else if(fromCommit != null) {
            repo?.resolve(fromCommit)
        } else {
            repo?.exactRef(rev)?.objectId!!
        }

        RevWalk(repo)
            .use { walk ->

                if (revFilter.size == 1)
                    walk.revFilter = revFilter[0];
                else if (revFilter.size > 1)
                    walk.revFilter = AndRevFilter.create(revFilter)
                walk.sort(revSort, true) // We can extend multiple sorts

                val commit = walk.parseCommit(revId)
                walk.markStart(commit)
                for (revCommit in walk) {
                    yield(getCommitFromJGit(revCommit))
                    if(toCommit != null && revCommit.getId().getName().equals(toCommit)) {
                        //Found to, stopping walk
                        break;
                    }
                }
            }


    }

    /**
     * Get the specified commit.
     *
     * @param [commitId] hash of the commit to analyze
     * @return [Commit]
     */
    fun getCommit(commitId: String): Commit {
        RevWalk(repo).use { walk ->
            val commit = walk.parseCommit(repo?.resolve(commitId))
            return Commit(commit, _conf)
        }
    }

    /**
     * Build a KDriller commit object from a JGit commit object.
     * This is internal of KDriller, I don't think users generally will need it.
     *
     * @param [commit] JGit commit
     * @return [Commit] KDriller commit
     */
    fun getCommitFromJGit(commit: RevCommit): Commit {
        return Commit(commit, _conf)
    }

    /**
     * Checkout the repo at the speficied commit.
     * BE CAREFUL: this will change the state of the repo, hence it should
     * *not* be used with more than 1 thread.
     *
     * @param [_hash] commit hash to checkout
     */
    fun checkout(_hash: String) {
        Git.open(path.toFile()).use { git ->
            git.checkout().setForced(true).setName(_hash).call()
            git.clean()
        }
    }

    /**
     * Obtain the list of the files (excluding .git directory).
     *
     * @return the list of the files
     */
    fun files(): List<String> {
        return path.toFile()
            .walkTopDown()
            .filter { !it.path.toString().contains(".git") && it.isFile }
            .map { it.absolutePath.toString() }
            .toList()
    }

    /**
     * Reset the state of the repo, checking out the main branch and
     * discarding local changes (-f option).
     *
     */
    fun reset() {
        Git.open(path.toFile()).use { git ->
            git.checkout().setForced(true).setName(_conf.get("main_branch") as String).call()
        }
    }

    /**
     * Calculate total number of commits.
     *
     * @return the total number of commits
     */
    fun totalCommits(): Int {
        return getListCommits().count()
    }


    /**
     * Obtain the tagged commit.
     *
     * @param [tag] the tag
     * @return [Commit] the commit the tag referred to
     */
    fun getCommitFromTag(tag: String): Commit {
        try {
            RevWalk(repo).use { walk ->
                val commit = walk.parseCommit(repo?.resolve(tag))
                return Commit(commit, _conf)
            }
        } catch (e: Exception) {
            logger.debug("Tag $tag not found")
            throw e
        }

    }


    /**
     * Obtain the hash of all the tagged commits.
     *
     * @return list of tagged commits (can be empty if there are no tags)
     */
    fun getTaggedCommits(): List<String> {
        val tags = mutableListOf<String>()

        Git(repo).use { git ->
            val call = git.tagList().call()
            for (ref in call) {
                val peeledRef: Ref? = repo?.refDatabase?.peel(ref)
                if (peeledRef?.peeledObjectId != null) {
                    tags.add(peeledRef.peeledObjectId!!.name)
                } else {
                    tags.add(ref.objectId.name)
                }
            }
        }

        return tags
    }

    fun getCommitsLastModifiedLines(
        commit: Commit,
        modification: ModifiedFile? = null,
        hashesToIgnorePath: String? = null
    ): MutableMap<String, MutableSet<String>> {
        val modifications = if (modification != null) {
            listOf(modification)
        } else {
            commit.modifiedFiles
        }

        return _calculateLastCommits(commit, modifications, hashesToIgnorePath)
    }

    fun _calculateLastCommits(
        commit: Commit,
        modifications: List<ModifiedFile>,
        hashesToIgnorePath: String? = null
    ): MutableMap<String, MutableSet<String>> {
        var commits: MutableMap<String, MutableSet<String>> =
            mutableMapOf<String, MutableSet<String>>().withDefault { mutableSetOf() }

        for (mod in modifications) {
            var path = mod.newPath
            if (mod.changeType == ModificationType.RENAME || mod.changeType == ModificationType.DELETE) {
                path = mod.oldPath
            }

            val deletedLines = mod.diffParsed["deleted"]

            assert(path != null)

            try {
                val blame = _getBlame(commit.cObject.name, path!!, hashesToIgnorePath)

                if (deletedLines != null) {
                    for ((numLine, line) in deletedLines) {
                        if (!_uselessLine(line.trim())) {
                            val buggyCommit = blame?.getSourceCommit(numLine - 1)
                            val buggyLine = blame?.resultContents?.getString(numLine - 1)

                            // Skip unblamable lines.
                            if (buggyLine?.startsWith("*") == true) {
                                continue
                            }

                            if (mod.changeType == ModificationType.RENAME) {
                                path = mod.newPath
                            }

                            assert(path != null)


                            val set = commits.getOrDefault(path, mutableSetOf())
                            if (path != null) {
                                set.add(buggyCommit?.name!!)
                                commits[path] = set
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                logger.debug("Could not found file ${mod.filename} in commit ${commit.hash}. Probably a double rename!")
            }
        }
        return commits
    }

    fun _getBlame(commitHash: String, path: String, hashesToIgnorePath: String? = null): BlameResult? {
        if (hashesToIgnorePath != null) {
            throw NotImplementedError("Not supported yet by jgit see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=559671")
        }
        Git(repo).use { git ->
            val parentCommit = repo?.resolve("$commitHash^")
            return git.blame().setStartCommit(parentCommit).setFilePath(path)
                .setTextComparator(RawTextComparator.WS_IGNORE_ALL).call()
        }
    }

    fun _uselessLine(line: String): Boolean {
        return (line.isBlank()) ||
                line.startsWith("//") ||
                line.startsWith("#") ||
                line.startsWith("/*") ||
                line.startsWith("'''") ||
                line.startsWith("\"\"\"") ||
                line.startsWith("*")
    }


    // TODO: implement includeDeletedFiles

    /**
     * Given a filepath, returns all the commits that modified this file (following renames).
     *
     * @param [filepath] path to the file
     * @param [includeDeletedFiles] if true, include commits that modifies a deleted file
     * @return the list of commits' hash
     */
    fun getCommitsModifiedFile(filepath: String, includeDeletedFiles: Boolean = false): List<String> {
        val path = Path(filepath).toString()
        val commits = mutableListOf<String>()

        // see this: https://stackoverflow.com/questions/11471836/how-to-git-log-follow-path-in-jgit-to-retrieve-the-full-history-includi
//        RevWalk(repo).use { walk ->
//            val config = Config()
//            config.setBoolean("diff", null, "renames", true)
//            val followFilter = FollowFilter.create(filepath, config.get(DiffConfig.KEY))
//            val commit = walk.parseCommit(repo?.resolve(Constants.HEAD))
//            walk.treeFilter = followFilter
//            walk.markStart(commit)
//            for (revCommit in walk) {
//                commits.add(revCommit.name)
//            }
//        }


        Git(repo).use { git ->
            val logs = git.log().addPath(path).call()
            for (rev in logs) {
                commits.add(rev.name)
            }
        }
        return commits
    }

    // TODO: not implemented yet
    fun clear() {
        if (_repo != null) {
            repo
        }
    }

    override fun close() {
        //_repo?.close()
    }
}