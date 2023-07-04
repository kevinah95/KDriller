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
import org.eclipse.jgit.revwalk.RevCommit
import java.util.*

enum class ModificationType(val value: Int) {
    ADD(1),
    COPY(2),
    RENAME(3),
    DELETE(4),
    MODIFY(5),
    UNKNOWN(6)
}

enum class DMMProperty(val value: Int) {
    UNIT_SIZE(1),
    UNIT_COMPLEXITY(2),
    UNIT_INTERFACING(3)
}

data class Method(val func: Any?) {
    var name: String = ""
    var longName: String = ""
    var fileName: String = ""
    var nloc: Int = 0
    var complexity: Int = 0
    var tokenCount: Int = 0
    var parameters: List<String> = listOf()
    var startLine: Int = 0
    var endLine: Int = 0
    var fanIn: Int = 0
    var fanOut: Int = 0
    var generalFanOut: Int = 0
    var length: Int = 0
    var topNestingLevel: Int = 0

    override fun equals(other: Any?): Boolean {
        // TODO: Change this
        return false
    }

    val UNIT_SIZE_LOW_RISK_THRESHOLD = 15

    val UNIT_COMPLEXITY_LOW_RISK_THRESHOLD = 5

    val UNIT_INTERFACING_LOW_RISK_THRESHOLD = 2

    fun isLowRisk(dmmProp: DMMProperty): Boolean {
        if (dmmProp == DMMProperty.UNIT_SIZE)
            return this.nloc <= UNIT_SIZE_LOW_RISK_THRESHOLD
        if (dmmProp == DMMProperty.UNIT_COMPLEXITY)
            return this.complexity <= UNIT_COMPLEXITY_LOW_RISK_THRESHOLD
        assert(dmmProp == DMMProperty.UNIT_INTERFACING)
        return (this.parameters.size <= UNIT_INTERFACING_LOW_RISK_THRESHOLD)
    }

}

data class ModifiedFile(val diff: DiffEntry){
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
        when(diff.changeType){
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

data class Commit(val commit: RevCommit){
    val cObject = commit

    // TODO: hash
    val hash: String
        get() = cObject.id.name

    val author: Developer
        get() = Developer(cObject.authorIdent.name, cObject.authorIdent.emailAddress)

    val committer: Developer
        get() = Developer(cObject.committerIdent.name, cObject.committerIdent.emailAddress)

    // TODO: projectname and project path

    val authorDate: Date
        get() = cObject.authorIdent.`when`

    val committerDate: Date
        get() = cObject.committerIdent.`when`

    val authorTimeZone: TimeZone
        get() = cObject.authorIdent.timeZone

    val committerTimeZone: TimeZone
        get() = cObject.committerIdent.timeZone

    // TODO: strip
    val msg: String
        get() = cObject.fullMessage

    val parents: List<String>
        get() = cObject.parents.map { parent -> parent.name }

    val merge: Boolean
        get() = cObject.parents.size > 1

    // TODO: Implement shortstat

    // TODO: modified_files
    val modifiedFiles : List<ModifiedFile> = listOf()
    // TODO: possible implementation as branches with config



}