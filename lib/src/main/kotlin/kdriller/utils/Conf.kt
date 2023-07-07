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

package kdriller.utils

import kdriller.Git
import kdriller.domain.Commit
import mu.KotlinLogging
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.filter.AuthorRevFilter
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter
import org.eclipse.jgit.revwalk.filter.MaxCountRevFilter
import org.eclipse.jgit.revwalk.filter.RevFilter
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Configuration class. This class holds all the possible configurations of
 * the mining process (i.e., starting and ending dates, branches, etc.)
 * It's also responsible for checking whether the filters are correct (i.e.,
 * the user did not specify 2 starting commits).
 */
data class Conf(val options: Map<String, Any?>) {
    private val _options: MutableMap<String, Any?> = mutableMapOf()

    init {
        // insert all the configurations in a local dictionary
        for ((key, value) in options) {
            _options[key] = value
        }

        _sanityCheckRepos(get("path_to_repo")!!)
        if (get("path_to_repo") is String) {
            setValue("path_to_repos", listOf(get("path_to_repo")))
        } else {
            setValue("path_to_repos", get("path_to_repo") as List<String>)
        }
    }

    /**
     * Save the value of a configuration.
     *
     * @param [key] configuration (i.e., start date)
     * @param [value] value
     */
    fun setValue(key: String, value: Any?) {
        _options[key] = value
    }

    /**
     * Return the value of the configuration.
     *
     * @param [key] configuration name
     * @return value of the configuration, null if not present
     */
    fun get(key: String): Any? {
        return _options.getOrDefault(key, null)
    }

    companion object {
        /**
         * Checks if repo is of type str or list.
         *
         * @param [pathToRepo] path to the repo as provided by the user.
         */
        @JvmStatic
        fun _sanityCheckRepos(pathToRepo: Any) {
            if (pathToRepo !is String && pathToRepo !is List<*>) {
                throw Exception("The path to the repo has to be of type 'string' or 'list of strings'!")
            }
        }

        @JvmStatic
        fun _isCommitBefore(commitBefore: Commit, commitAfter: Commit): Boolean {
            if (commitBefore.committerDate < commitAfter.committerDate) {
                return true
            }
            if (commitBefore.committerDate == commitAfter.committerDate &&
                commitBefore.authorDate < commitAfter.authorDate
            ) {
                return true
            }
            return false
        }

        /**
         * Return true if in 'arr' there is at most 1 filter to true.
         *
         * @param [arr] iterable object
         */
        @JvmStatic
        fun onlyOneFilter(arr: List<Any?>): Boolean {
            return arr.filterNotNull().size <= 1
        }

        @JvmStatic
        fun _replaceTimezone(dt: Date): Date {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.time = dt
            return cal.time
        }

    }

    fun _checkOnlyOneFromCommit() {
        if (!onlyOneFilter(
                listOf(
                    get("since"),
                    get("since_as_filter"),
                    get("from_commit"),
                    get("from_tag")
                )
            )
        ) {
            throw Exception("You can only specify one filter between since, since_as_filter, from_tag and from_commit")
        }
    }

    fun _checkOnlyOneToCommit() {
        if (!onlyOneFilter(
                listOf(
                    get("to"),
                    get("to_commit"),
                    get("to_tag")
                )
            )
        ) {
            throw Exception("You can only specify one between to, to_tag and to_commit")
        }
    }

    /**
     * Check if the values passed by the user are correct.
     */
    fun sanityCheckFilters() {
        _checkCorrectFiltersOrder()
        _checkOnlyOneFromCommit()
        _checkOnlyOneToCommit()
        _checkTimezones()

        // Check if from_commit and to_commit point to the same commit, in which case
        // we remove both filters and use the "single" filter instead. This prevents
        // errors with dates.
        if (get("from_commit") != null && get("to_commit") != null && get("from_commit") == get("to_commit")) {
            logger.warn("You should not point from_commit and to_commit to the same commit, but use the 'single' filter instead.")
            val single = get("to_commit") as String
            setValue("from_commit", null)
            setValue("to_commit", null)
            setValue("single", single)
        }

        if (get("single") != null) {
            if (
                listOf(
                    get("since")
                ).any { it != null }
            ) {
                throw Exception("You can not specify a single commit with other filters")
            }
            try {
                setValue("single", (get("git") as Git).getCommit(get("single") as String).hash)
            } catch (e: Exception) {
                // TODO: BadName exception
                throw Exception("The commit ${get("single") as String} defined in the 'single' filtered does not exist")
            }
        }


    }

    /**
     * Check that from_commit comes before to_commit
     */
    fun _checkCorrectFiltersOrder() {
        if (get("from_commit") != null && get("to_commit") != null) {
            val git = get("git") as Git
            val chronologicalOrder = _isCommitBefore(
                git.getCommit(get("from_commit") as String),
                git.getCommit(get("to_commit") as String)
            )

            if (!chronologicalOrder) {
                _swapCommitFilters()
            }
        }
    }


    fun _swapCommitFilters() {
        // reverse from and to commit
        val fromCommit = get("from_commit")
        val toCommit = get("to_commit")
        setValue("from_commit", toCommit)
        setValue("to_commit", fromCommit)
    }

    /**
     * Get the starting commit from the 'from_commit' or 'from_tag' filter.
     */
    fun getStartingCommit(): List<String>? {
        val fromTag = get("from_tag") as String?
        var fromCommit = get("from_commit") as String?
        if (fromTag != null) {
            val taggedCommit = (get("git") as Git).getCommitFromTag(fromTag)
            fromCommit = taggedCommit.hash
        }

        if (fromCommit != null) {
            try {
                val commit = (get("git") as Git).getCommit(fromCommit)
                when (commit.parents.size) {
                    0 -> {
                        return listOf("--ancestry-path=${commit.hash}")
                    }

                    1 -> {
                        return listOf("--ancestry-path=${commit.hash}", "^${commit.hash}^")
                    }

                    else -> {
                        return listOf("--ancestry-path=${commit.hash}") + commit.parents.map { "^$it" }
                    }
                }
            } catch (e: Exception) {
                throw Exception("The $fromCommit defined in the 'from_tag' or 'from_commit' filter does not exist")
            }
        }

        return null
    }

    /**
     * Get the ending commit from the 'to', 'to_commit' or 'to_tag' filter.
     */
    fun getEndingCommit(): String? {
        val toTag = get("to_tag") as String
        var toCommit = get("to_commit") as String

        if (toTag != null) {
            val taggedCommit = (get("git") as Git).getCommitFromTag(toTag)
            toCommit = taggedCommit.hash
        }

        if (toCommit != null) {
            try {
                return (get("git") as Git).getCommit(toCommit).hash
            } catch (e: Exception) {
                throw Exception("The commit $toCommit defined in the 'to_tag' or 'to_commit' filter does not exist")
            }
        }

        return null
    }

    // TODO: build_args
    fun buildArgs(): Pair<List<RevFilter>, List<String>> {

        val single = get("single")
        val since = get("since")
        val sinceAsFilter = get("since_as_filter")
        val until = get("to")
        //val from_commit = self.get_starting_commit()
        //val to_commit = self.get_ending_commit()
        val includeRefs = get("include_refs")
        val includeRemotes = get("include_remotes")
        val branch = get("only_in_branch")
        val authors = get("only_authors")
        val order = get("order")

        val rev = mutableListOf<RevFilter>()

        if (single != null) {
            rev.add(MaxCountRevFilter.create(1))
        }

        // from
        // see https://stackoverflow.com/questions/27361538/how-to-show-changes-between-commits-with-jgit

        if (get("only_no_merge") == true) {
            rev.add(RevFilter.NO_MERGES)
        }

        when (order) {
            null -> RevSort.NONE
            "reverse" -> RevSort.REVERSE
            "topo-order" -> RevSort.TOPO
            "date-order" -> RevSort.COMMIT_TIME_DESC
        }

        if (includeRefs != null) {
            // TODO: remove this rev.add(RevFilter.ALL) and use RefDatabase.ALL
            // RefDatabase.ALL
            // repo?.refDatabase?.getRefsByPrefix(RefDatabase.ALL)
            // see: http://www.java2s.com/example/java-src/pkg/kr/re/ec/grigit/graph/ui/revwalker-7bda3.html
        }

        if (includeRemotes != null) {
            // TODO: includeRemotes
            // see https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ListRemotes.java
            //repo?.refDatabase?.findRef(Constants.R_REMOTES)
        }

        if (authors != null) {
            for (author in authors as List<String>) {
                rev.add(AuthorRevFilter.create(author))
            }
        }

        // http://www.java2s.com/example/java-src/pkg/kr/re/ec/grigit/graph/ui/revwalker-7bda3.html
        // if (revLimiter.size() == 1)
        //      walk.setRevFilter(revLimiter.get(0));
        // else if (revLimiter.size() > 1)
        //      walk.setRevFilter(AndRevFilter.create(revLimiter))
        if (since != null) {
            rev.add(CommitTimeRevFilter.after(since as Date))
        }

        if (until != null) {
            rev.add(CommitTimeRevFilter.before(until as Date))
        }

        return Pair(rev, listOf())
    }

    /**
     * Check if commit has to be filtered according to the filters provided by the user.
     *
     * @param [commit] Commit to check
     */
    fun isCommitFiltered(commit: Commit): Boolean {
        if (get("only_modifications_with_file_types") != null) {
            if (!_hasModificationWithFileType(commit)) {
                logger.debug("Commit filtered for modification types")
                return true
            }
        }
        if (get("only_commits") != null && commit.hash !in get("only_commits") as Set<String>) {
            logger.debug("Commit filtered because it is not one of the specified commits")
            return true
        }

        if (get("filepath_commits") != null && commit.hash !in get("filepath_commits") as List<String>) {
            logger.debug("Commit filtered because it did not modify the specified file")
            return true
        }

        if (get("tagged_commits") != null && commit.hash !in get("tagged_commits") as List<String>) {
            logger.debug("Commit filtered because it is not tagged")
            return true
        }
        return false
    }

    fun _hasModificationWithFileType(commit: Commit): Boolean {
        val fileTypes = get("only_modifications_with_file_types") as List<String>
        for (mod in commit.modifiedFiles) {
            if (fileTypes.any { mod.filename.endsWith(it) }) {
                return true
            }
        }
        return false
    }

    fun _checkTimezones() {
        if (get("since") != null) {
            setValue("since", _replaceTimezone((get("since") as Date)))
        }
        if (get("since_as_filter") != null) {
            setValue("since_as_filter", _replaceTimezone((get("since_as_filter") as Date)))
        }
        if (get("to") != null) {
            setValue("to", _replaceTimezone((get("to") as Date)))
        }
    }
}