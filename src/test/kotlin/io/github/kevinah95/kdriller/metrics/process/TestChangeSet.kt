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
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class TestChangeSet {
    private val testFolder: File = FileUtils.getFile("src", "test", "resources", "test-repos")

    @Test
    fun testWithCommits(){
        val pathToRepo = listOf(testFolder.resolve("pydriller").path)
        val fromCommit = "ab36bf45859a210b0eae14e17683f31d19eea041"
        val toCommit = "71e053f61fc5d31b3e31eccd9c79df27c31279bf"
        val expectedMax = 13
        val expectedAvg = 8L
        val metric = ChangeSet(pathToRepo,
            fromCommit = fromCommit,
            toCommit = toCommit)
        val actualMax = metric.max()
        val actualAvg = metric.avg()

        assertEquals(expectedMax, actualMax)
        assertEquals(expectedAvg, actualAvg)
    }

}