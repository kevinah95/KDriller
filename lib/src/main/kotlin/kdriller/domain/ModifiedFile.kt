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

import org.eclipse.jgit.diff.DiffEntry

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

data class ModifiedFile(val diff: DiffEntry) {
    val cDiff = diff
    var nloc = null
    var complexity = null
    var tokenCount = null
    var filename: String = ""
    var functionList: List<Method> = listOf()
    var functionListBefore: List<Method> = listOf()

    val changeType: ModificationType
        get() = fromChangeToModificationType(cDiff)

    fun fromChangeToModificationType(diff: DiffEntry): ModificationType {
        when (diff.changeType) {
            DiffEntry.ChangeType.ADD -> return ModificationType.ADD
            DiffEntry.ChangeType.DELETE -> return ModificationType.DELETE
            DiffEntry.ChangeType.RENAME -> return ModificationType.RENAME
            DiffEntry.ChangeType.MODIFY -> return ModificationType.MODIFY
            DiffEntry.ChangeType.COPY -> return ModificationType.COPY
            else -> {
                return ModificationType.UNKNOWN
            }
        }
    }

    // TODO: def diff(self) -> str

}
