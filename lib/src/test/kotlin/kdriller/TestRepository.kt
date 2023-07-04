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

package kdriller

import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertFails
import kotlin.test.assertFailsWith


val testFolder = FileUtils.getFile("src", "test", "resources", "test-repos")

internal class TestRepository {

    @Test
    // It should fail when URL is not a string or a List
    fun testBadlyFormattedRepoUrl() {
        assertFailsWith(MalformedUrl::class) {
            Repository(listOf("https://badurl.git/")).traverseCommits().toList()
        }
    }


    @TestFactory
    fun testSimpleUrl() = listOf(
        listOf(testFolder.resolve("small_repo").path) to 5
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("when I calculate $input^2 then I get $expected") {
            assert(Repository(input).traverseCommits().toList().size == expected)
        }
    }


    @TestFactory
    fun testTwoLocalUrls() = listOf(
        listOf(testFolder.resolve("small_repo").path, testFolder.resolve("branches_merged").path) to 9
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("when I calculate $input^2 then I get $expected") {
            assert(Repository(input).traverseCommits().toList().size == expected)
        }
    }

    // TODO: test to param

    @TestFactory
    fun test2IdenticalLocalUrls() = listOf(
        listOf(testFolder.resolve("small_repo").path, testFolder.resolve("small_repo").path) to 10
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("when I calculate $input^2 then I get $expected") {
            assert(Repository(input).traverseCommits().toList().size == expected)
        }
    }

    @Test
    // It should fail when URL is not a string or a List
    fun testBadlyFormattedUrl() {
        assertFails() {
            Repository(listOf("https://github.com/ishepard.git/test")).traverseCommits().toList()
        }

        assertFails() {
            Repository(listOf("test")).traverseCommits().toList()
        }
    }
}


