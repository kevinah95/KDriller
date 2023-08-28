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

package io.github.kevinah95.kdriller.integration

import io.github.kevinah95.kdriller.Repository
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class TestBetweenTags {

    private val testFolder: File = FileUtils.getFile("src", "test", "resources", "test-repos")

    @Test
    fun testBetweenRevisions() {
        val pathToRepo = listOf(testFolder.resolve("tags").path)

        val fromTag = "tag1"
        val toTag = "tag3"

        val listCommits = Repository(pathToRepo, fromTag = fromTag, toTag = toTag).traverseCommits().toList()

        assertEquals(5, listCommits.size)
        assertEquals("6bb9e2c6a8080e6b5b34e6e316c894b2ddbf7fcd" , listCommits[0].hash)
        assertEquals("f1a90b8d7b151ceefd3e3dfc0dc1d0e12b5f48d0" , listCommits[1].hash)
        assertEquals("4638730126d40716e230c2040751a13153fb1556" , listCommits[2].hash)
        assertEquals("a26f1438bd85d6b22497c0e5dae003812becd0bc" , listCommits[3].hash)
        assertEquals("627e1ad917a188a861c9fedf6e5858b79edbe439" , listCommits[4].hash)
    }

    @Test
    fun testMultipleReposWithTags() {
        val pathToRepo = listOf(
            testFolder.resolve("tags").path,
            testFolder.resolve("tags").path,
            testFolder.resolve("tags").path
        )

        val fromTag = "tag2"
        val toTag = "tag3"

        val listCommits = Repository(pathToRepo, fromTag = fromTag, toTag = toTag).traverseCommits().toList()

        assertEquals(9, listCommits.size)
    }
}