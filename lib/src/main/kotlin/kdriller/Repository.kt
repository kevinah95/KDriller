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
 * This module includes 1 class, Repository, main class of KDriller.
 */

import kdriller.domain.Commit
import kdriller.utils.Conf
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.apache.commons.io.FilenameUtils
import org.eclipse.jgit.lib.TextProgressMonitor
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.math.ceil
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import org.eclipse.jgit.api.Git as Repo

private val logger = KotlinLogging.logger {}

/**
 * This is the main class of KDriller, responsible for running the study.
 *
 * @constructor Init a repository. The only required parameter is
 * "pathToRepo": to analyze a single repo, pass the absolute path to
 * the repo; if you need to analyze more repos, pass a list of absolute
 * paths.
 *
 * Furthermore, KDriller supports local and remote repositories: if
 * you pass a path to a repo, KDriller will run the study on that
 * repo; if you pass a URL, KDriller will clone the repo in a
 * temporary folder, run the study, and delete the temporary folder.
 *
 * @param[pathToRepo] absolute path (or list of absolute paths) to the repository(ies) to analyze
 * @param[single] hash of a single commit to analyze
 * @param[since] starting date
 * @param[sinceAsFilter] starting date (scans all commits, does not stop at first commit with date < [sinceAsFilter])
 * @param[to] ending date
 * @param[fromCommit] starting commit (only if [since] is *null*)
 * @param[toCommit] ending commit (only if [to] is *null*)
 * @param[fromTag] starting the analysis from specified tag (only if [since] and [fromCommit] are null)
 * @param[toTag] ending the analysis from specified tag (only if [to] and [toCommit] are null)
 * @param[includeRefs] whether to include refs and HEAD in commit analysis
 * @param[includeRemotes] whether to include remote commits in analysis
 * @param[numWorkers] number of workers (i.e., threads). Please note, if num_workers > 1 the commits order is not maintained.
 * @param[onlyInBranch] only commits in this branch will be analyzed
 * @param[onlyModificationsWithFileTypes] only modifications with that file types will be analyzed
 * @param[onlyNoMerge] if *true*, merges will not be analyzed
 * @param[onlyAuthors] only commits of these authors will be analyzed (the check is done on the username, NOT the email)
 * @param[onlyCommits] only these commits will be analyzed
 * @param[onlyReleases] analyze only tagged commits
 * @param[histogramDiff] add the "--histogram" option when asking for the diff
 * @param[skipWhitespaces] add the "-w" option when asking for the diff
 * @param[cloneRepoTo] if the repo under analysis is remote, clone the repo to the specified directory
 * @param[filepath] only commits that modified this file will be analyzed
 * @param[includeDeletedFiles] include commits modifying a deleted file (useful when analyzing a deleted [filepath])
 * @param[order] order of commits. It can be one of: 'date-order', 'author-date-order', 'topo-order', or 'reverse'. If order=null, KDriller returns the commits from the oldest to the newest.
 */
@Suppress("FunctionName", "RedundantIf", "unused")
class Repository(
    val pathToRepo: List<String>,
    single: String? = null,
    since: Date? = null, sinceAsFilter: Date? = null, to: Date? = null,
    fromCommit: String? = null, toCommit: String? = null,
    fromTag: String? = null, toTag: String? = null,
    includeRefs: Boolean = false,
    includeRemotes: Boolean = false,
    numWorkers: Int = 1,
    onlyInBranch: String? = null,
    onlyModificationsWithFileTypes: List<String>? = null,
    onlyNoMerge: Boolean = false,
    onlyAuthors: List<String>? = null,
    onlyCommits: List<String>? = null,
    onlyReleases: Boolean = false,
    filepath: String? = null,
    includeDeletedFiles: Boolean = false,
    histogramDiff: Boolean = false,
    skipWhitespaces: Boolean = false,
    cloneRepoTo: String? = null,
    order: String? = null
) {

    private lateinit var _tmpDir: File

    private var git: Git? = null

    private var _conf: Conf

    private var _cleanup: Boolean = false


    init {
        val fileModificationSet = onlyModificationsWithFileTypes?.toSet()
        val commitSet = onlyCommits?.toSet()
        _conf = Conf(
            mapOf(
                "git" to null,
                "path_to_repo" to pathToRepo,
                "from_commit" to fromCommit,
                "to_commit" to toCommit,
                "from_tag" to fromTag,
                "to_tag" to toTag,
                "since" to since,
                "since_as_filter" to sinceAsFilter,
                "to" to to,
                "single" to single,
                "include_refs" to includeRefs,
                "include_remotes" to includeRemotes,
                "num_workers" to numWorkers,
                "only_in_branch" to onlyInBranch,
                "only_modifications_with_file_types" to fileModificationSet,
                "only_no_merge" to onlyNoMerge,
                "only_authors" to onlyAuthors,
                "only_commits" to commitSet,
                "only_releases" to onlyReleases,
                "skip_whitespaces" to skipWhitespaces,
                "filepath" to filepath,
                "include_deleted_files" to includeDeletedFiles,
                "filepath_commits" to null,
                "tagged_commits" to null,
                "histogram" to histogramDiff,
                "clone_repo_to" to cloneRepoTo,
                "order" to order
            )
        )

        // If the user provides a directory where to clone the repositories,
        // make sure we do not delete the directory after the study completes
        _cleanup = if (cloneRepoTo != null) false else true
    }

    // Static methods
    companion object {
        fun _isRemote(repo: String): Boolean {
            return arrayOf("git@", "https://", "http://").any { repo.startsWith(it) }
        }

        /**
         * Given the list of commits return chunks of commits based on the number of workers.
         *
         * @param[fullList] full list of commits
         * @param[numWorkers] number of workers (i.e., threads)
         * @return Chunks of commits
         */
        @JvmStatic
        fun _splitInChunks(fullList: List<Commit>, numWorkers: Int): List<List<Commit>> {
            val numChunks = ceil((fullList.size / numWorkers).toDouble()).toInt()
            val chunks = mutableListOf<List<Commit>>()
            for (i in 0..fullList.size step numChunks) {
                chunks.add(fullList.slice(i until i + numChunks))
            }
            return chunks
        }

        @JvmStatic
        fun _getRepoNameFromURL(url: String): String {
            val lastSlashIndex = url.lastIndexOf('/')
            val lenURL = url.length
            if (lastSlashIndex < 0 || lastSlashIndex >= lenURL - 1) {
                throw MalformedUrl("Badly formatted url $url")
            }
            val lastDotIndex = url.lastIndexOf('.')
            val lastSuffixIndex = if (url.subSequence(lastDotIndex, lenURL) == ".git") {
                lastDotIndex
            } else {
                lenURL
            }

            return url.subSequence(lastSlashIndex + 1, lastSuffixIndex).toString()
        }
    }


    private fun _cloneRemoteRepo(tmpFolder: String, repo: String): String {
        val repoFolder = FilenameUtils.concat(tmpFolder, _getRepoNameFromURL(repo))
        val repoFolderPath = Path(repoFolder)
        if (Files.isDirectory(repoFolderPath)) {
            logger.info("Reusing folder $repoFolder for $repo")
        } else {
            Repo.cloneRepository().setURI(repo).setDirectory(repoFolderPath.toFile())
                .setProgressMonitor(TextProgressMonitor()).call().use {
                logger.info("Cloning $repo in temporary folder $repoFolder")
            }
        }

        return repoFolder
    }

    private fun _cloneFolder(): String {
        val cloneFolder: String?
        if (_conf.get("clone_repo_to") != null) {
            val cloneFolderPath = Path(_conf.get("clone_repo_to") as String)
            if (!Files.isDirectory(cloneFolderPath)) {
                throw Exception("Not a directory: $cloneFolderPath")
            }
            cloneFolder = cloneFolderPath.toString()
        } else {
            // Save the temporary directory, so we can clean it up later
            _tmpDir = createTempDirectory().toFile()
            cloneFolder = _tmpDir.path
        }

        return cloneFolder!!
    }

    private fun _prepRepo(pathRepo: String) = sequence<Git> {
        var localPathRepo = pathRepo
        if (_isRemote(pathRepo)) {
            localPathRepo = _cloneRemoteRepo(_cloneFolder(), pathRepo)
        }
        localPathRepo = Path(localPathRepo).toString()

        // when multiple repos are given in input, this variable will serve as a reminder
        // of which one we are currently analyzing
        _conf.setValue("path_to_repo", localPathRepo)

        git = Git(localPathRepo, _conf)

        // saving the Git object for further use
        _conf.setValue("git", git)

        // checking that the filters are set correctly
        _conf.sanityCheckFilters()

        yield(git!!)

        // cleaning, this is necessary since GitPython issues on memory leaks
        _conf.setValue("git", null)
        // TODO: implement clear method
        git = null


        // delete the temporary directory if created
        if (_isRemote(pathRepo) && _cleanup) {
            assert(_tmpDir != null)
            _tmpDir.deleteRecursively()
        }
    }

    /**
     * Analyze all the specified commits (all of them by default), returning
     * a generator of commits.
     */
    // TODO: Make it parallel
    @OptIn(ExperimentalTime::class)
    fun traverseCommits() = sequence<Commit> {

        for (pathRepo in _conf.get("path_to_repos") as List<String>) {
            val elapsedTime = measureTime {
                //val workerPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
                val workerPool = Executors.newFixedThreadPool(_conf.get("num_workers") as Int)

                val iterator = _prepRepo(pathRepo = pathRepo).iterator()
                while (iterator.hasNext()) {
                    iterator.next().use { git ->
                        logger.info("Analyzing git repository in ${git.path}")

                        // Get the commits that modified the filepath. In this case, we can not use
                        // git rev-list since it doesn't have the option --follow, necessary to follow
                        // the renames. Hence, we manually call git log instead
                        if (_conf.get("filepath") != null) {
                            _conf.setValue(
                                "filepath_commits",
                                git.getCommitsModifiedFile(
                                    _conf.get("filepath") as String,
                                    _conf.get("include_deleted_files") as Boolean
                                )
                            )
                        }

                        // Gets only the commits that are tagged
                        if (_conf.get("only_releases") as Boolean) {
                            _conf.setValue("tagged_commits", git.getTaggedCommits())
                        }

                        // TODO: Build the arguments to pass to git rev-list.
                        val (revFilter, kwargs) = _conf.buildArgs()

                        for (job in git.getListCommits(revFilter = revFilter).map { Callable { it } }
                            .map(workerPool::submit)) {
                            val commit = job.get() as Commit
                            logger.info("Commit #${commit.hash} in ${commit.committerDate} from ${commit.author.name}")

                            if (_conf.isCommitFiltered(commit)) {
                                logger.info("Commit #${commit.hash} filtered")
                                continue
                            }
                            yield(commit)
                        }
                    }
                }

                workerPool.shutdown()
                workerPool.awaitTermination(1, TimeUnit.HOURS)

            }

            println(elapsedTime)
        }


    }


}

class MalformedUrl(message: String) : Exception(message)

fun <A, B> List<A>.pmap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async(Dispatchers.Default) { f(it) } }.map { it.await() }
}

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}