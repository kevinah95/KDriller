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

package kdriller

/**
 * This module includes 1 class, Git, representing a repository in Git.
 */

import kdriller.domain.Commit
import kdriller.utils.Conf
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
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
class Git(path: String, var conf: Conf? = null) : Closeable {

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
        var revSort = RevSort.NONE
        if (_conf.get("reverse") != null) {
            revSort = RevSort.REVERSE
        }

        val revId: ObjectId? = if (_conf.get("single") != null) {
            repo?.resolve(_conf.get("single") as String)?.toObjectId()
        } else {
            repo?.exactRef(rev)?.objectId!!
        }

        RevWalk(repo).use { walk ->
            if (revFilter.size == 1)
                walk.revFilter = revFilter[0];
            else if (revFilter.size > 1)
                walk.revFilter = AndRevFilter.create(revFilter)
            val commit = walk.parseCommit(revId)
            walk.markStart(commit)
            walk.sort(revSort, true) // We can extend multiple sorts
            for (revCommit in walk) {
                yield(getCommitFromJGit(revCommit))
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

    // TODO: checkout. Note: I don't know if it is possible with repo

    /**
     * Obtain the list of the files (excluding .git directory).
     *
     * @return the list of the files
     */
    fun files(): List<String> {
        return path.toFile()
            .walkTopDown()
            .filter { !it.path.toString().contains(".git") }
            .map { it.absolutePath.toString() }
            .toList()
    }

    // TODO: reset

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
        RevWalk(repo).use { walk ->
            val commit = walk.parseCommit(repo?.resolve(tag))
            return Commit(commit, _conf)
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

    // TODO: get_commits_last_modified_lines


    // TODO: implement includeDeletedFiles
    /**
     * Given a filepath, returns all the commits that modified this file (following renames).
     *
     * @param [filepath] path to the file
     * @param [includeDeletedFiles] if true, include commits that modifies a deleted file
     * @return the list of commits' hash
     */
    fun getCommitsModifiedFile(filepath: String, includeDeletedFiles: Boolean): List<String> {
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
        //repo?.close()
    }
}