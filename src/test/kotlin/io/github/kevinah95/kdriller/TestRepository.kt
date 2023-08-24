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
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.test.*


internal class TestRepository {
    private val testFolder: File = FileUtils.getFile("src", "test", "resources", "test-repos")

    @Test
    // It should fail when URL is not a string or a List
    fun testBadlyFormattedRepoUrl() {
        assertFails {
            Repository(listOf("repo")).traverseCommits().toList()
        }
    }

    @Test
    // It should fail when URL is not a string or a List
    fun testMalformedUrl() {
        assertFailsWith(MalformedUrl::class) {
            Repository(listOf("https://badurl.git/")).traverseCommits().toList()
        }
    }

    @Test
    fun testSimpleUrl() {
        // Arrange
        val pathToRepo = listOf(testFolder.resolve("small_repo").path)
        val expected = 5

        // Act
        val actual = Repository(pathToRepo).traverseCommits().toList().size

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun testTwoLocalUrls() {
        // Arrange
        val pathToRepo = listOf(
            testFolder.resolve("small_repo").path,
            testFolder.resolve("branches_merged").path
        )
        val expected = 9

        // Act
        val actual = Repository(pathToRepo).traverseCommits().toList().size

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun testSimpleRemoteUrl() {
        // Arrange
        val pathToRepo = listOf(
            "https://github.com/ishepard/pydriller.git"
        )

        val cal = Calendar.getInstance()
        cal.set(2018, 10, 20)
        val expected = 224

        // Act
        val actual = Repository(pathToRepo, to = cal.time).traverseCommits().toList().size

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun testTwoRemoteUrls() {
        // Arrange
        val pathToRepo = listOf(
            "https://github.com/mauricioaniche/repodriller.git",
            "https://github.com/ishepard/pydriller.git"
        )

        val cal = Calendar.getInstance()
        cal.set(2018, 10, 20)
        val expected = 583

        // Act
        val actual = Repository(pathToRepo, to = cal.time).traverseCommits().toList().size

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun test2IdenticalLocalUrls() {
        // Arrange
        val pathToRepo = listOf(
            testFolder.resolve("small_repo").path,
            testFolder.resolve("small_repo").path
        )
        val expected = 10

        // Act
        val actual = Repository(pathToRepo).traverseCommits().toList().size

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun testBothLocalAndRemoteUrls() {
        // Arrange
        val pathToRepo = listOf(
            testFolder.resolve("small_repo").path,
            "https://github.com/ishepard/pydriller.git"
        )

        val cal = Calendar.getInstance()
        cal.set(2018, 10, 20)
        val expected = 229

        // Act
        val actual = Repository(pathToRepo, to = cal.time).traverseCommits().toList().size

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun testBothLocalAndRemoteUrlsList() {
        // Arrange
        val pathToRepo = listOf(
            testFolder.resolve("small_repo").path,
            "https://github.com/mauricioaniche/repodriller.git",
            testFolder.resolve("branches_merged").path,
            "https://github.com/ishepard/pydriller.git"
        )

        val cal = Calendar.getInstance()
        cal.set(2018, 10, 20)
        val expected = 592

        // Act
        val actual = Repository(pathToRepo, to = cal.time).traverseCommits().toList().size

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    // It should fail when URL is not a string or a List
    fun testBadlyFormattedUrl() {
        // Arrange & Act & Assert
        assertFails() {
            Repository(listOf("https://github.com/ishepard.git/test")).traverseCommits().toList()
        }

        assertFails() {
            Repository(listOf("test")).traverseCommits().toList()
        }
    }

    @Test
    fun testDiffWithoutHistogram() {
        // Arrange
        val pathToRepo = listOf(testFolder.resolve("histogram").path)


        // Act
        val commit = Repository(
            pathToRepo,
            single = "93df8676e6fab70d9677e94fd0f6b17db095e890"
        ).traverseCommits().first()
        val diff = commit.modifiedFiles[0].diffParsed
        val diffAdded = diff["added"].orEmpty()
        val diffDeleted = diff["deleted"].orEmpty()

        // Assert
        assertEquals(11, diffAdded.size)
        // TODO: check space to end of line
        assertTrue(diffAdded.contains(Pair(3, "    if (path == null) ")))
        assertTrue(diffAdded.contains(Pair(5, "        log.error(\"Icon path is null\");")))
        assertTrue(diffAdded.contains(Pair(6, "        return null;")))
        assertTrue(diffAdded.contains(Pair(8, "")))
        //            println(diff["added"])
//            println(diff["deleted"])
//            println(diffAdded.filter { it.first == 9 })
        //assertTrue(diffAdded.contains(Pair(9, "    java.net.URL imgURL = GuiImporter.class.getResource(path); ")))
//            assertTrue((6, "        return null;") in diff["added"])
//            assertTrue((8, "") in diff["added"])
//            assertTrue((9, "    java.net.URL imgURL = GuiImporter.class.getResource(path);") in diff["added"])
//            assertTrue((10, "") in diff["added"])
//            assertTrue((11, "    if (imgURL == null)") in diff["added"])
//            assertTrue((12, "    {") in diff["added"])
//            assertTrue((14, "        return null;") in diff["added"])
//            assertTrue((16, "    else") in diff["added"])
//            assertTrue((17, "        return new ImageIcon(imgURL);") in diff["added"])
        assertTrue(diffDeleted.size == 7)
    }

    @Test
    fun testIgnoreAddWhitespacesAndModifiedNormalLine() {
        // Arrange
        val pathToRepo = listOf(testFolder.resolve("whitespace").path)


        // Act
        var commit = Repository(
            pathToRepo,
            single = "52716ef1f11e07308b5df1b313aec5496d5e91ce"
        ).traverseCommits().first()
        assertEquals(1, commit.modifiedFiles.size)
        val parsedNormalDiff = commit.modifiedFiles[0].diffParsed
        commit = Repository(
            pathToRepo,
            skipWhitespaces = true,
            single = "52716ef1f11e07308b5df1b313aec5496d5e91ce"
        ).traverseCommits().first()
        assertEquals(1, commit.modifiedFiles.size)
        val parsedWoWhitespacesDiff = commit.modifiedFiles[0].diffParsed

        assertEquals(2, parsedNormalDiff["added"].orEmpty().size)
        assertEquals(1, parsedWoWhitespacesDiff["added"].orEmpty().size)

        assertEquals(1, parsedNormalDiff["deleted"].orEmpty().size)
        assertEquals(0, parsedWoWhitespacesDiff["deleted"].orEmpty().size)

    }

    @Test
    fun testIgnoreDeletedWhitespaces() {
        // Arrange
        val pathToRepo = listOf(testFolder.resolve("whitespace").path)


        // Act & Assert
        var commit = Repository(
            pathToRepo,
            single = "e6e429f6b485e18fb856019d9953370fd5420b20"
        ).traverseCommits().first()
        assertEquals(1, commit.modifiedFiles.size)

        commit = Repository(
            pathToRepo,
            skipWhitespaces = true,
            single = "e6e429f6b485e18fb856019d9953370fd5420b20"
        ).traverseCommits().first()
        assertEquals(0, commit.modifiedFiles.size)
    }

    @Test
    fun testIgnoreAddWhitespacesAndChangedFile() {
        // Arrange
        val pathToRepo = listOf(testFolder.resolve("whitespace").path)


        // Act & Assert
        var commit = Repository(
            pathToRepo,
            single = "532068e9d64b8a86e07eea93de3a57bf9e5b4ae0"
        ).traverseCommits().first()
        assertEquals(2, commit.modifiedFiles.size)

        commit = Repository(
            pathToRepo,
            skipWhitespaces = true,
            single = "532068e9d64b8a86e07eea93de3a57bf9e5b4ae0"
        ).traverseCommits().first()
        assertEquals(1, commit.modifiedFiles.size)
    }

    @Test
    fun testCloneRepoTo(@TempDir tmpPath: Path) {
        // Arrange
        val cal = Calendar.getInstance()
        cal.set(2018, 10, 20)
        val dt2 = cal.time
        val url = "https://github.com/ishepard/pydriller.git"

        // Act
        val commits = Repository(
            pathToRepo = listOf(url),
            to = dt2,
            cloneRepoTo = tmpPath.pathString
        ).traverseCommits().toList()

        // Assert
        assertEquals(224, commits.size)
        assertTrue(tmpPath.resolve("pydriller/.git").exists())
    }

    @Test
    @Throws(Exception::class)
    fun testCloneRepoToNotExisting() {
        // Arrange
        val cloneFolder = "NOTEXISTINGDIR"

        // Act & Assert
        val exception = assertThrows<Exception> {
            Repository(
                pathToRepo = listOf("https://github.com/ishepard/pydriller"),
                cloneRepoTo = cloneFolder
            ).traverseCommits().toList()
        }

        // Assert
        assertEquals("Not a directory: $cloneFolder", exception.message)
    }

    @Test
    fun testCloneRepoToRepeated(@TempDir tmpPath: Path) {
        // Arrange
        val cal = Calendar.getInstance()
        cal.set(2018, 10, 20)
        val dt2 = cal.time
        val url = "https://github.com/ishepard/pydriller.git"

        // Act
        var commits = Repository(
            pathToRepo = listOf(url),
            to = dt2,
            cloneRepoTo = tmpPath.pathString
        ).traverseCommits().toList()

        // Assert
        assertEquals(224, commits.size)
        assertTrue(tmpPath.resolve("pydriller").isDirectory())

        // Act
        commits = Repository(
            pathToRepo = listOf(url),
            to = dt2,
            cloneRepoTo = tmpPath.pathString
        ).traverseCommits().toList()

        // Assert
        assertEquals(224, commits.size)
        assertTrue(tmpPath.resolve("pydriller").isDirectory())
    }

    @Test
    fun testProjectnameMultipleRepos() {
        // Arrange
        val repos = listOf<String>(
            testFolder.resolve("files_in_directories").path,
            testFolder.resolve("files_in_directories").path,
            testFolder.resolve("files_in_directories").path
        )

        // Act
        val commits = Repository(pathToRepo = repos).traverseCommits()

        // Assert
        for (commit in commits) {
            assertEquals("files_in_directories", commit.projectName)
        }
    }

    @Test
    fun testProjectnameMultipleReposRemote() {
        // Arrange
        val repos = listOf<String>(
            "https://github.com/ishepard/pydriller",
            testFolder.resolve("pydriller").path
        )

        // Act
        val commits = Repository(pathToRepo = repos).traverseCommits()

        // Assert
        for (commit in commits) {
            assertEquals("pydriller", commit.projectName)
        }
    }

    @Test
    fun testGetRepoNameFromUrl() {
        // Arrange
        // with .git in the middle of the name
        val urlSetA = listOf<String>(
            "https://github.com/academicpages/academicpages.github.io",
            "https://github.com/academicpages/academicpages.github.io.git",
        )

        val urlSetB = listOf<String>(
            "https://github.com/ishepard/pydriller",
            "https://github.com/ishepard/pydriller.git",
        )

        // Act & Assert
        for (url in urlSetA) {
            assertEquals("academicpages.github.io", Repository._getRepoNameFromURL(url))
        }

        for (url in urlSetB) {
            assertEquals("pydriller", Repository._getRepoNameFromURL(url))
        }
    }

    @Test
    fun testDeletionRemotes() {
        // Arrange
        val repos = listOf(
            "https://github.com/ishepard/pydriller",
            "https://github.com/ishepard/pydriller"
        )
        var paths = mutableSetOf<String>()

        // Act
        for (commit in Repository(pathToRepo = repos).traverseCommits()) {
            paths.add(commit.projectPath)
        }

        // Assert
        for (path in paths) {
            //assertFalse(Path.of(path).isDirectory())
        }
    }

    @Test
    fun testDeletedFiles() {
        // Arrange & Act & Assert
        val deletedCommits = Repository(
            pathToRepo = listOf("https://github.com/ishepard/pydriller"),
            filepath = ".bettercodehub.yml",
            includeDeletedFiles = true
        ).traverseCommits().toList()
        assertTrue(deletedCommits.isNotEmpty())
    }

}


