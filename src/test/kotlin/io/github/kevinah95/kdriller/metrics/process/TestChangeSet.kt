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

import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.*
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestChangeSet {
    private val testFolder: File = FileUtils.getFile("src", "test", "resources", "test-repos")

    private fun dateTestData(): Stream<Arguments> {
        return Stream.of(
            Arguments.arguments(
                "pydriller",
                getDate(2018, 2, 21),
                getDate(2018, 2, 27),
                13, 8L
            ),
            Arguments.arguments(
                "pydriller",
                getDate(2018, 2, 23),
                getDate(2018, 2, 23),
                0, 0L
            ),
        )
    }

    private fun getDate(year: Int, month: Int, date: Int): Date{
        return Calendar.getInstance().run {
            set(year, month, date)
            time
        }
    }

    @Test
    fun testWithCommits() {
        val pathToRepo = listOf(testFolder.resolve("pydriller").path)
        val fromCommit = "ab36bf45859a210b0eae14e17683f31d19eea041"
        val toCommit = "71e053f61fc5d31b3e31eccd9c79df27c31279bf"
        val expectedMax = 13
        val expectedAvg = 8L
        val metric = ChangeSet(
            pathToRepo,
            fromCommit = fromCommit,
            toCommit = toCommit
        )
        val actualMax = metric.max()
        val actualAvg = metric.avg()

        assertEquals(expectedMax, actualMax)
        assertEquals(expectedAvg, actualAvg)
    }

    @ParameterizedTest
    @MethodSource("dateTestData")
    fun testWithDates(pathToRepoDir: String, since: Date, to: Date, expectedMax: Int, expectedAvg: Long) {
        val pathToRepo = listOf(testFolder.resolve(pathToRepoDir).path)
        val metric = ChangeSet(
            pathToRepo,
            since,
            to
        )
        val actualMax = metric.max()
        val actualAvg = metric.avg()

        assertEquals(expectedMax, actualMax)
        assertEquals(expectedAvg, actualAvg)
    }

}