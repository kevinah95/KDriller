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

import io.github.kevinah95.kdriller.Repository
import java.util.*
import kotlin.collections.List

abstract class ProcessMetric @JvmOverloads constructor(
    val pathToRepo: List<String>,
    val since: Date? = null,
    val to: Date? = null,
    val fromCommit: String? = null,
    val toCommit: String? = null,
) {
    var repoMiner: Repository? = null

    init {
        if (since == null && fromCommit == null) {
            throw Exception("You must pass one between since and from_commit")
        }

        if (to == null && toCommit == null) {
            throw Exception("You must pass one between to and to_commit")
        }

        if (fromCommit != null && toCommit != null && fromCommit == toCommit) {
            // Use 'single' param to avoid Warning
            repoMiner = Repository(pathToRepo, single = fromCommit)

        } else {
            repoMiner = Repository(
                pathToRepo,
                since = since,
                to = to,
                fromCommit = fromCommit,
                toCommit = toCommit,
                order = "reverse"
            )
        }

    }
}