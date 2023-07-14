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

package io.github.kevinah95.kdriller.domain

/**
 * Maintainability properties of the Delta Maintainability Model.
 */
enum class DMMProperty(val value: Int) {
    UNIT_SIZE(1),
    UNIT_COMPLEXITY(2),
    UNIT_INTERFACING(3)
}

data class Method @JvmOverloads constructor(val func: Any?) {
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
