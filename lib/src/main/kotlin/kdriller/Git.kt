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

import kdriller.domain.Commit
import kdriller.utils.Conf
import mu.KotlinLogging
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.Closeable
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

@Suppress("FunctionName")
class Git(path: String, var conf: Conf? = null) : Closeable {

    var path: Path
    var projectName: String
    private var _repo: Repository? = null
    private var _conf: Conf? = null

    init {
        this.path = Path(path)
        projectName = this.path.name

        // if no configuration is passed, then creates a new "emtpy" one
        // with just "path_to_repo" inside.
        if (conf == null) {
            conf = Conf(
                mapOf(
                    "path_to_repo" to this.path.toString(),
                    "git" to this
                )
            )
        }

        _conf = conf
        _conf?.setValue("main_branch", null)
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

        if (_conf?.get("main_branch") == null) {
            _discoverMainBranch(_repo)
        }

    }

    private fun _discoverMainBranch(repo: Repository?) {
        try {
            _conf?.setValue("main_branch", repo?.branch)
        } catch (e: Exception) {
            // The current HEAD is detached. In this case, it doesn't belong to
            // any branch, hence we return an empty string
            logger.info("HEAD is a detached symbolic reference, setting main branch to empty string")
            _conf?.setValue("main_branch", "")
        }
    }

    fun getHead(): Commit {
        val headId: ObjectId = repo?.resolve("HEAD")!!

        RevWalk(repo).use { walk ->
            val headCommit = walk.parseCommit(headId)
            return Commit(headCommit)
        }
    }

    fun getListCommits(rev: String = "HEAD") = sequence<Commit> {
        repo.use {
            val revId: ObjectId = it?.exactRef(rev)?.objectId!!
            RevWalk(it).use { walk ->
                val commit = walk.parseCommit(revId)
                walk.markStart(commit)
                for (revCommit in walk) {
                    yield(getCommitFromJGit(revCommit))
                }
                walk.dispose()
            }
        }
    }

    fun getCommit(commitId: String): Commit {
        RevWalk(repo).use { walk ->
            val commit = walk.parseCommit(repo?.resolve(commitId))
            return Commit(commit)
        }
    }

    fun getCommitFromJGit(commit: RevCommit): Commit {
        return Commit(commit)
    }

    // TODO: checkout. Note: I don't know if it is possible with repo

    fun files(): List<String> {
        return path.toFile()
            .walkTopDown()
            .filter { !(it.isDirectory && it.name == ".git") }
            .map { it.absolutePath.toString() }
            .toList()
    }

    fun getCommitFromTag(tag: String): Commit {
        RevWalk(repo).use { walk ->
            val commit = walk.parseCommit(repo?.resolve(tag))
            return Commit(commit)
        }
    }



    // TODO: get_tagged_commits
    fun getTaggedCommits(): List<String>{
        throw NotImplementedError()
    }

    // TODO: get_commits_last_modified_lines


    fun getCommitsModifiedFile(filepath: String, includeDeletedFiles: Boolean){
        throw NotImplementedError()
    }

    // TODO: not implemented yet
    fun clear(){
        if (_repo != null) {
            repo
        }
    }

    override fun close() {
        //repo?.close()
    }
}