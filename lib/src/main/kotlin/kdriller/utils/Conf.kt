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
import java.lang.Exception
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
data class Conf(val options: Map<String, Any?>) {
    private val _options: MutableMap<String, Any?> = mutableMapOf()

    init {
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

    fun setValue(key: String, value: Any?) {
        _options[key] = value
    }

    fun get(key: String): Any? {
        return _options.getOrDefault(key, null)
    }

    companion object {
        @JvmStatic
        fun _sanityCheckRepos(pathToRepo: Any) {
            // TODO: second check List<*>
            if (pathToRepo !is String && pathToRepo !is List<*>) {
                throw Exception("The path to the repo has to be of type 'string' or 'list of strings'!")
            }
        }

        @JvmStatic
        fun onlyOneFilter(arr: List<Any>): Boolean {
            return arr.filterNotNull().size <= 1
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

        @JvmStatic
        fun _replaceTimezone(dt: LocalDateTime): ZonedDateTime{
            return dt.atZone(ZoneId.of("UTC"))
        }

    }

    fun _checkOnlyOneFromCommit() {
        if (!onlyOneFilter(
                listOf(
                    get("since")!!,
                    get("since_as_filter")!!,
                    get("from_commit")!!,
                    get("from_tag")!!
                )
            )
        ) {
            throw Exception("You can only specify one filter between since, since_as_filter, from_tag and from_commit")
        }
    }

    fun _checkOnlyOneToCommit() {
        if (!onlyOneFilter(
                listOf(
                    get("to")!!,
                    get("to_commit")!!,
                    get("to_tag")!!
                )
            )
        ) {
            throw Exception("You can only specify one between to, to_tag and to_commit")
        }
    }

    fun sanityCheckFilters() {
        _checkCorrectFiltersOrder()
        _checkOnlyOneFromCommit()
        _checkOnlyOneToCommit()
        _checkTimezones()

        // Check if from_commit and to_commit point to the same commit, in which case
        // we remove both filters and use the "single" filter instead. This prevents
        // errors with dates.
        if(get("from_commit") != null && get("to_commit") != null && get("from_commit") == get("to_commit")){
            logger.warn("You should not point from_commit and to_commit to the same commit, but use the 'single' filter instead.")
            val single = get("to_commit") as String
            setValue("from_commit", null)
            setValue("to_commit", null)
            setValue("single", single)
        }

        if(get("single") != null){
            if (
                listOf(
                    get("since")
                ).any {it != null}
            ){
                throw Exception("You can not specify a single commit with other filters")
            }
            try {
                setValue("single", (get("git") as Git).getCommit(get("single") as String).hash)
            } catch (e: Exception){
                // TODO: BadName exception
                throw Exception("The commit ${get("single") as String} defined in the 'single' filtered does not exist")
            }
        }


    }

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
        val fromCommit = get("from_commit")
        val toCommit = get("to_commit")
        setValue("from_commit", toCommit)
        setValue("to_commit", fromCommit)
    }

    fun getStartingCommit(): List<String>? {
        val fromTag = get("from_tag") as String
        var fromCommit = get("from_commit") as String
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

    fun getEndingCommit(): String?{
        val toTag = get("to_tag") as String
        var toCommit = get("to_commit") as String

        if (toTag != null) {
            val taggedCommit = (get("git") as Git).getCommitFromTag(toTag)
            toCommit = taggedCommit.hash
        }

        if(toCommit != null){
            try {
                return (get("git") as Git).getCommit(toCommit).hash
            } catch (e: Exception){
                throw Exception("The commit $toCommit defined in the 'to_tag' or 'to_commit' filter does not exist")
            }
        }

        return null
    }

    // TODO: build_args

    fun isCommitFiltered(commit: Commit): Boolean{
        if(get("only_modifications_with_file_types") != null){
            if(!_hasModificationWithFileType(commit)) {
                logger.debug("Commit filtered for modification types")
                return true
            }
        }
        if(get("only_commits") != null && commit.hash !in get("only_commits") as Set<String>){
            logger.debug("Commit filtered because it is not one of the specified commits")
            return true
        }

        if(get("filepath_commits") != null && commit.hash !in get("filepath_commits") as List<String>){
            logger.debug("Commit filtered because it did not modify the specified file")
            return true
        }

        if(get("tagged_commits") != null && commit.hash !in get("tagged_commits") as List<String>){
            logger.debug("Commit filtered because it is not tagged")
            return true
        }
        return false
    }

    fun _hasModificationWithFileType(commit: Commit): Boolean{
        // TODO: modifiedFiles
        val fileTypes = get("only_modifications_with_file_types") as List<String>
        for(mod in commit.modifiedFiles){
            if(fileTypes.any { mod.filename.endsWith(it) }){
                return true
            }
        }
        return false
    }

    fun _checkTimezones(){
        if (get("since") != null){
            setValue("since", _replaceTimezone((get("since") as LocalDateTime)))
        }
        if (get("since_as_filter") != null){
            setValue("since_as_filter", _replaceTimezone((get("since_as_filter") as LocalDateTime)))
        }
        if (get("to") != null){
            setValue("to", _replaceTimezone((get("to") as LocalDateTime)))
        }
    }
}