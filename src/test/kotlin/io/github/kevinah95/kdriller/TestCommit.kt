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

import io.github.kevinah95.kdriller.domain.ModifiedFile
import io.github.kevinah95.kdriller.utils.Conf
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkClass
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevTree
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals


@ExtendWith(MockKExtension::class)
class TestCommit {

    private val testFolder: File = FileUtils.getFile("src", "test", "resources", "test-repos")

    // TODO: repo, test_equal, test_filename, test_metrics_python



    @Test
    fun testEqual() {
        val repo = Git(testFolder.resolve("complex_repo").path)
        val c1 = repo.getCommit("e7d13b0511f8a176284ce4f92ed8c6e8d09c77f2")
        val c2 = repo.getCommit(c1.parents[0])
        val c3 = repo.getCommit("a4ece0762e797d2e2dcbd471115108dd6e05ff58")

        assertEquals("a4ece0762e797d2e2dcbd471115108dd6e05ff58", c1.parents[0])
        assertEquals(c3, c2)
        assertNotEquals(c1, c3)
    }

    @Test
    fun testFilename(@MockK mockedDiff: DiffEntry) {
        every { mockedDiff.oldPath } returns "dspadini/pydriller/myfile.py"
        every { mockedDiff.newPath } returns "dspadini/pydriller/mynewfile.py"

        val m1 = ModifiedFile(mockedDiff, "", mockkClass(RevTree::class), "", mockkClass(Conf::class))
        assertEquals("mynewfile.py", m1.filename)

        assertEquals("dspadini/pydriller/mynewfile.py", m1.newPath)
        assertEquals("dspadini/pydriller/myfile.py", m1.oldPath)
    }

    @Test
    fun testChangedMethods() {
        val gr = Git(testFolder.resolve("diff").path)

        // add a new method
        var mod  = gr.getCommit("ea95227e0fd128aa69c7ab6a8ac485f72251b3ed").modifiedFiles[0]

        assertEquals(1, mod.changedMethods.size)
        assertEquals("GitRepository::singleProjectThirdMethod", mod.changedMethods[0].name)

        // add 2 new methods
        mod  = gr.getCommit("d8eb8e80b671246a43c98d97b05f6d1c5ada14fb").modifiedFiles[0]

        assertEquals(2, mod.changedMethods.size)

        // remove one method
        mod  = gr.getCommit("0c8f9fdec926785198b399a2c49adb5884aa952c").modifiedFiles[0]

        assertEquals(1, mod.changedMethods.size)

        // add and remove one method at different locations
        mod  = gr.getCommit("d8bb142c5616041b71cbfaa11eeb768d9a1a296e").modifiedFiles[0]

        assertEquals(2, mod.changedMethods.size)

        // add and remove one method at the same location
        // this is equivalent to replacing a method - although we expect 2 methods
        mod  = gr.getCommit("9e9473d5ca310b7663e9df93c402302b6b7f24aa").modifiedFiles[0]

        assertEquals(2, mod.changedMethods.size)

        // update a method
        mod  = gr.getCommit("b267a14e0503fdac36d280422f16360d1f661f12").modifiedFiles[0]

        assertEquals(1, mod.changedMethods.size)

        // update and add a new method
        mod  = gr.getCommit("2489099dfd90edb99ddc2c82b62524b66c07c687").modifiedFiles[0]

        assertEquals(2, mod.changedMethods.size)

        // update and delete methods
        mod  = gr.getCommit("5aebeb30e0238543a93e5bed806639481460cd9a").modifiedFiles[0]

        assertEquals(2, mod.changedMethods.size)

        // delete 3 methods (test cleanup - revert the test file to its
        // initial set of methods)
        mod  = gr.getCommit("9f6ddc2aac740a257af59a76860590cb8a84c77b").modifiedFiles[0]

        assertEquals(3, mod.changedMethods.size)
    }
}