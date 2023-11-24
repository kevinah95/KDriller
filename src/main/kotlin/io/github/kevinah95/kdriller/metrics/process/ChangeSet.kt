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

package io.github.kevinah95.kdriller.metrics.process

import java.util.*

class ChangeSet(
    pathToRepo: List<String>,
    since: Date? = null,
    to: Date? = null,
    fromCommit: String? = null,
    toCommit: String? = null
) : ProcessMetric(pathToRepo, since, to, fromCommit, toCommit) {

    var committedTogether: MutableList<Int>? = null

    init {
        _initialize()
    }

    fun _initialize() {
        committedTogether = mutableListOf()

        repoMiner?.traverseCommits()?.forEach { commit ->
            committedTogether!!.add(commit.modifiedFiles.size)
        }
    }

    fun max(): Int {
        return committedTogether?.max()?.coerceAtLeast(0) ?: 0
    }

    fun avg(): Long {
        if(committedTogether == null){
            return 0
        }

        return Math.round(committedTogether!!.average())
    }

}