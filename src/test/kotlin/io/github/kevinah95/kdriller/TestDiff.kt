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
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.management.MemoryUsage
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestDiff {
    private val testFolder: File = FileUtils.getFile("src", "test", "resources", "test-repos")

    @Test
    fun testExtractLineNumberAndContent() {
        val repo = Git(testFolder.resolve("diff").path)
        val modification = repo.getCommit("9a985d4a12a3a12f009ef39750fd9b2187b766d1").modifiedFiles[0]

        assert(modification.diffParsed.isNotEmpty())

        val added = modification.diffParsed["added"]
        val deleted = modification.diffParsed["deleted"]

        assertNotNull(deleted)
        assertNotNull(added)
        assertTrue(Pair(127, "            RevCommit root = rw.parseCommit(headId);") in deleted)
        assertTrue(Pair(128, "            rw.sort(RevSort.REVERSE);") in deleted)
        assertTrue(Pair(129, "            rw.markStart(root);") in deleted)
        assertTrue(Pair(130, "            RevCommit lastCommit = rw.next();") in deleted)
        assertTrue(Pair(131, "            throw new RuntimeException(\"Changing this line \" + path);") in added)
    }

    @Test
    fun testAdditions() {
        val repo = Git(testFolder.resolve("diff").path)
        val modification = repo.getCommit("f45ee2f8976d5f018a1e4ec83eb4556a3df8b0a5").modifiedFiles[0]

        assert(modification.diffParsed.isNotEmpty())

        val added = modification.diffParsed["added"]
        val deleted = modification.diffParsed["deleted"]

        assertNotNull(deleted)
        assertNotNull(added)
        assertTrue(Pair(127, "            RevCommit root = rw.parseCommit(headId);") in added)
        assertTrue(Pair(128, "            rw.sort(RevSort.REVERSE);") in added)
        assertTrue(Pair(129, "            rw.markStart(root);") in added)
        assertTrue(Pair(130, "            RevCommit lastCommit = rw.next();") in added)
        assertTrue(Pair(131, "") in added)
        assertEquals(0, deleted.size)
        assertEquals(5, added.size)
    }

    @Test
    fun testDeletions() {
        val repo = Git(testFolder.resolve("diff").path)
        val modification = repo.getCommit("147c7ce9f725a0e259d63f0bf4e6c8ac085ff8c8").modifiedFiles[0]

        assert(modification.diffParsed.isNotEmpty())

        val added = modification.diffParsed["added"]
        val deleted = modification.diffParsed["deleted"]

        assertNotNull(deleted)
        assertNotNull(added)
        assertTrue(Pair(184, "            List<ChangeSet> allCs = new ArrayList<>();") in deleted)
        assertTrue(Pair(221, "    private GregorianCalendar convertToDate(RevCommit revCommit) {") in deleted)
        assertTrue(Pair(222, "        GregorianCalendar date = new GregorianCalendar();") in deleted)
        assertTrue(Pair(223, "        date.setTimeZone(revCommit.getAuthorIdent().getTimeZone());") in deleted)
        assertTrue(Pair(224, "        date.setTime(revCommit.getAuthorIdent().getWhen());") in deleted)
        assertTrue(Pair(225, "") in deleted)
        assertTrue(Pair(226, "        return date;") in deleted)
        assertTrue(Pair(227, "    }") in deleted)
        assertTrue(Pair(228, "") in deleted)
        assertTrue(Pair(301, "        if(!collectConfig.isCollectingBranches())") in deleted)
        assertTrue(Pair(302, "            return new HashSet<>();") in deleted)
        assertTrue(Pair(303, "") in deleted)
        assertEquals(12, deleted.size)
        assertEquals(0, added.size)
    }

    /**
     * If a file ends without a newline git represents this with the additional line
     *         \\ No newline at end of file
     * in diffs. This test asserts these additional lines are parsed correctly.
     */
    @Test
    fun testDiffNoNewline() {
        val repo = Git(testFolder.resolve("no_newline").path)
        val modification = repo.getCommit("52a78c1ee5d100528eccba0a3d67371dbd22d898").modifiedFiles[0]

        assert(modification.diffParsed.isNotEmpty())

        val added = modification.diffParsed["added"]
        val deleted = modification.diffParsed["deleted"]

        assertNotNull(deleted)
        assertNotNull(added)
        assert(Pair(1, "test1") in deleted)  // is considered as deleted as a "newline" command is added
        assert(Pair(1, "test1") in added)  // now with added "newline"
        assert(Pair(2, "test2") in added)
    }
}