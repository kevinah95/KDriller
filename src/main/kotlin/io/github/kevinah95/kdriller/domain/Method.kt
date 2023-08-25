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

import io.github.kevinah95.FunctionInfo
import java.util.Objects

/**
 * This class represents a method in a class. Contains various information
 * extracted through Lizard.
 */
data class Method @JvmOverloads constructor(val func: FunctionInfo) {
    var name: String = func.name
    var longName: String = func.longName
    var fileName: String = func.filename
    var nloc: Int = func.nloc
    var complexity: Int = func.cyclomaticComplexity
    var tokenCount: Int = func.tokenCount
    var parameters: List<String> = func.parameters
    var startLine: Int = func.startLine
    var endLine: Int = func.endLine
    var fanIn: Int = func.fanIn
    var fanOut: Int = func.fanOut
    var generalFanOut: Int = func.generalFanOut
    var length: Int = func.length
    var topNestingLevel: Int = func.topNestingLevel

    // hashcode is needed to comparison
    override fun hashCode(): Int {
        return Objects.hash(name, longName, parameters)
    }

    override fun equals(obj: Any?): Boolean {
        val other = obj as? Method
        if (other == null) return false
        if (name == other.name && parameters.toTypedArray().contentDeepEquals(other.parameters.toTypedArray())) return true
        return false
    }

    /**
     * Threshold used in the Delta Maintainability Model to establish whether a method
     * is low risk in terms of its size.
     * The procedure to obtain the threshold is described in the
     * [PyDriller documentation **Properties**](https://pydriller.readthedocs.io/en/latest/deltamaintainability.html#properties).
     */
    val UNIT_SIZE_LOW_RISK_THRESHOLD = 15

    /**
     * Threshold used in the Delta Maintainability Model to establish whether a method
     * is low risk in terms of its cyclomatic complexity.
     * The procedure to obtain the threshold is described in the
     * [PyDriller documentation **Properties**](https://pydriller.readthedocs.io/en/latest/deltamaintainability.html#properties).
     */
    val UNIT_COMPLEXITY_LOW_RISK_THRESHOLD = 5

    /**
     * Threshold used in the Delta Maintainability Model to establish whether a method
     * is low risk in terms of its interface.
     * The procedure to obtain the threshold is described in the
     * [PyDriller documentation **Properties**](https://pydriller.readthedocs.io/en/latest/deltamaintainability.html#properties).
     */
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
