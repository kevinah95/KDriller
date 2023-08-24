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

import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class TestCommit {

    private val testFolder: File = FileUtils.getFile("src", "test", "resources", "test-repos")

    @Test
    fun testChangedMethods() {
        val gr = Git(testFolder.resolve("diff").path)

        val mod  = gr.getCommit("ea95227e0fd128aa69c7ab6a8ac485f72251b3ed").modifiedFiles[0]

        println(mod._complexity)

        assertEquals(1, mod.methods.size)
        assertEquals("GitRepository::singleProjectThirdMethod", mod.methods[0].name)
    }
}