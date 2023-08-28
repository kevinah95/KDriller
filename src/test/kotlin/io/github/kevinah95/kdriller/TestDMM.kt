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

package io.github.kevinah95.kdriller

import io.github.kevinah95.kdriller.domain.Commit
import io.github.kevinah95.kdriller.domain.DMMProperty
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestDMM {
    private val testFolder: File = FileUtils.getFile("src", "test", "resources", "test-repos")

    fun commitByMsg(repo: Git, msg: String): Commit {
        for (commit in repo.getListCommits()){
            if (commit.msg == msg) {
                return commit
            }
        }
        throw Exception("cannot find commit with msg $msg")
    }

    fun unitSizeTestData(): Stream<Arguments> {
        return Stream.of(
            // delta high > 0, delta low = 0 -- always DMM 0.0
            arguments("Commit with one large method", 0.0),
            // delta high > 0, delta low > 0 -- DMM = ratio
            arguments("Make large larger, add small method", 0.8),
            // delta high > 0, delta low < 0 --- always DMM 0.0
            arguments("Make large larger, make small smaller", 0.0),
            // delta high = 0, delta low = 0 --- no delta-changes, dmm None
            arguments("Modify every line in large method", null),
            // delta high = 0, delta low > 0 --- always DMM 1.0
            arguments("Make small method a bit larger", 1.0),
            // delta high = 0, delta low < 0 --- alwyas DMM 0.0
            arguments("Make small smaller", 0.0),
            // delta high < 0, delta low < 0 --- DMM = ratio
            arguments("Make large smaller, make small smaller", 2.toDouble()/3),
            // delta  high < 0, delta low = 0 -- always 1.0
            arguments("Make large smaller", 1.0),
            // delta high < 0, delta low > 0 -- always DMM 1.0
            arguments("Make large smaller, make small larger", 1.0),
            // File 1: large larger; File 2: small larger -- dmm fraction
            arguments("Increase in one, decrease in other file", 3.toDouble()/4),
            // Method with unit size exactly on the border
            arguments("Add method with unit size on-point", 1.0),
            // Method with unit size at off point
            arguments("Increase unit size to risky", 0.0)
        )
    }

    fun unitComplexityTestData(): Stream<Arguments> {
        return Stream.of(
            // Large method, but no conditional logic
            arguments("Commit with one large method", 1.0),
            // Method with cyclomatic complexity exactly on the border
            arguments("Add method with complexity on-point", 1.0),
            // Method with cyclomatic complexity at off point
            arguments("Increase complexity to risky", 0.0)
        )
    }

    fun unitInterfacingTestData(): Stream<Arguments> {
        return Stream.of(
            // Large method, but no parameters
            arguments("Commit with one large method", 1.0),
            // Adjust method with nr of parameters exactly on the border, same size
            arguments("Add method with interfacing on-point", null),
            // Method with nr of parameters at off point
            arguments("Increase interfacing to risky", 0.0)
        )
    }

    @ParameterizedTest
    @MethodSource("unitSizeTestData")
    fun testDmmUnitSize(msg: String, dmm: Double?) {
        val repo = Git(testFolder.resolve("dmm-test-repo").path)
        val commit = commitByMsg(repo, msg)
        assertEquals(dmm, commit.dmmUnitSize)
    }

    @ParameterizedTest
    @MethodSource("unitComplexityTestData")
    fun testDmmUnitComplexity(msg: String, dmm: Double?) {
        val repo = Git(testFolder.resolve("dmm-test-repo").path)
        val commit = commitByMsg(repo, msg)
        assertEquals(dmm, commit.dmmUnitComplexity)
    }

    @ParameterizedTest
    @MethodSource("unitInterfacingTestData")
    fun testDmmUnitInterfacing(msg: String, dmm: Double?) {
        val repo = Git(testFolder.resolve("dmm-test-repo").path)
        val commit = commitByMsg(repo, msg)
        assertEquals(dmm, commit.dmmUnitInterfacing)
    }

    @Test
    fun testUnsupportedLanguage(){
        // Add .md file that cannot be analyzed by Lizard
        val repo = Git(testFolder.resolve("dmm-test-repo").path)
        val commit = commitByMsg(repo, "Offer README explaining the repo purpose")
        assertNull(commit.dmmUnitSize)
    }

    @Test
    fun testMixinUnsupportedLanguage(){
        // Add .txt file and update (comments in) .java files
        val repo = Git(testFolder.resolve("dmm-test-repo").path)
        val commit = commitByMsg(repo, "Release under Apache 2 license")
        assertNull(commit.dmmUnitSize)
    }

    @Test
    fun testDeltaProfileModification(){
        val repo = Git(testFolder.resolve("dmm-test-repo").path)
        val commit = commitByMsg(repo, "Increase unit size to risky")
        val mod = commit.modifiedFiles[0]
        assertEquals(Pair(-15, 16), mod._deltaRiskProfile(DMMProperty.UNIT_SIZE))
    }

    @Test
    fun testDeltaProfileCommit(){
        val repo = Git(testFolder.resolve("dmm-test-repo").path)
        val commit = commitByMsg(repo, "Increase in one, decrease in other file")
        val m0 = commit.modifiedFiles[0]
        assertEquals(Pair(0, 1), m0._deltaRiskProfile(DMMProperty.UNIT_SIZE))
        val m1 = commit.modifiedFiles[1]
        assertEquals(Pair(3, 0), m1._deltaRiskProfile(DMMProperty.UNIT_SIZE))
        assertEquals(Pair(3, 1), commit._deltaRiskProfile(DMMProperty.UNIT_SIZE))
    }

    @Test
    fun testSupportedLanguages(){
        val repo = Git(testFolder.resolve("dmm-test-repo").path)
        val commit = commitByMsg(repo, "Offer README explaining the repo purpose")
        val mod = commit.modifiedFiles[0]
        assertFalse(mod.languageSupported)
    }

    fun goodProportionTestData(): Stream<Arguments> {
        return Stream.of(
            arguments(0,  0, null),
            arguments(1,  0, 1.0),
            arguments(-1,  0, 0.0),
            arguments(0,  1, 0.0),
            arguments(0, -1, 1.0),
            arguments(1,  1, 0.5),
            arguments(-1, -1, 0.5),
            arguments(1, -1, 1.0),
            arguments(-1,  1, 0.0)
        )
    }

    @ParameterizedTest
    @MethodSource("goodProportionTestData")
    fun testGoodProportion(dlo: Int, dhi: Int, prop: Double?) {
        assertEquals(prop, Commit._goodChangeProportion(dlo, dhi))
    }

}