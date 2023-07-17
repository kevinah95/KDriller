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


import io.github.kevinah95.kdriller.domain.ModificationType
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*
import kotlin.test.*


internal class TestGit {
    private val testFolder: File = FileUtils.getFile("src", "commonTest", "resources", "test-repos")

    @Test
    fun testProjectname() {
        val repo = Git(testFolder.resolve("small_repo").path)
        assertEquals("small_repo", repo.projectName)
    }

    @Test
    fun testGetHead() {
        val repo = Git(testFolder.resolve("small_repo").path)
        val headCommit = repo.getHead()

        assertNotNull(headCommit)
        assertEquals("da39b1326dbc2edfe518b90672734a08f3c13458", headCommit.hash)
        assertEquals(1522164679000, headCommit.authorDate.time)
    }

    @Test
    fun testListCommits() {
        val repo = Git(testFolder.resolve("small_repo").path)
        val changeSets = repo.getListCommits()
        val list_commits = listOf(
            "a88c84ddf42066611e76e6cb690144e5357d132c",
            "6411e3096dd2070438a17b225f44475136e54e3a",
            "09f6182cef737db02a085e1d018963c7a29bde5a",
            "1f99848edadfffa903b8ba1286a935f1b92b2845",
            "da39b1326dbc2edfe518b90672734a08f3c13458"
        )

        for (commit in changeSets) {
            assertContains(list_commits, commit.hash)
        }

        assertEquals(5, changeSets.count())
    }

    @Test
    fun testGetCommit() {
        val repo = Git(testFolder.resolve("small_repo").path)
        val commit = repo.getCommit("09f6182cef737db02a085e1d018963c7a29bde5a")

        assertEquals("09f6182cef737db02a085e1d018963c7a29bde5a", commit.hash)
        assertEquals("ishepard", commit.author.name)
        assertEquals("ishepard", commit.committer.name)
        val timeZone = TimeZone.getTimeZone(commit.authorTimeZone.id)
        val cal = Calendar.getInstance(timeZone)
        cal.set(2018, 2, 22, 10, 42, 3)
        cal.set(Calendar.MILLISECOND, 0)
        assertEquals(cal.time.time, commit.authorDate.time)
        assertEquals(1, commit.modifiedFiles.size)
        assertEquals("Ooops file2", commit.msg)
        assertTrue(commit.inMainBranch)
        assertEquals(4, commit.insertions)
        assertEquals(0, commit.deletions)
        assertEquals(4, commit.lines)
        assertEquals(1, commit.files)
        assertEquals(1, commit.diffEntries?.size)
    }

    @Test
    fun testDetachedHead() {
        val repo = Git(testFolder.resolve("detached_head").path)
        val commit = repo.getCommit("56c5ef54d9d16d2b2255412f9479830b5b97cb99")
        assertFalse(commit.inMainBranch)
    }

    @Test
    fun testGetFirstCommit() {
        val repo = Git(testFolder.resolve("small_repo").path)
        val commit = repo.getCommit("a88c84ddf42066611e76e6cb690144e5357d132c")
        val timeZone = TimeZone.getTimeZone(commit.authorTimeZone.id)
        val cal = Calendar.getInstance(timeZone)
        cal.set(2018, 2, 22, 10, 41, 11)
        cal.set(Calendar.MILLISECOND, 0)

        assertEquals("a88c84ddf42066611e76e6cb690144e5357d132c", commit.hash)
        assertEquals("ishepard", commit.author.name)
        assertEquals("ishepard", commit.committer.name)
        assertEquals(cal.time.time, commit.authorDate.time)
        assertEquals(cal.time.time, commit.committerDate.time)
        assertEquals(2, commit.modifiedFiles.size)
        assertEquals("First commit adding 2 files", commit.msg)
        assertTrue(commit.inMainBranch)

        assertEquals(ModificationType.ADD, commit.modifiedFiles[0].changeType)
        assertEquals(ModificationType.ADD, commit.modifiedFiles[1].changeType)
    }

    @Test
    fun testFiles() {
        val repo = Git(testFolder.resolve("files").path)
        val files = repo.files()
        assertEquals(8, files.size)

        val expectedFiles = listOf<String>(
            testFolder.resolve("files/tmp1.py").absolutePath,
            testFolder.resolve("files/tmp2.py").absolutePath,
            testFolder.resolve("files/fold1/tmp3.py").absolutePath,
            testFolder.resolve("files/fold1/tmp4.py").absolutePath,
            testFolder.resolve("files/fold2/tmp5.py").absolutePath,
            testFolder.resolve("files/fold2/tmp6.py").absolutePath,
            testFolder.resolve("files/fold2/fold3/tmp7.py").absolutePath,
            testFolder.resolve("files/fold2/fold3/tmp8.py").absolutePath,
        )

        for (file in files){
            assertContains(expectedFiles, file)
        }
    }

    @Test
    fun testTotalCommits() {
        val repo = Git(testFolder.resolve("small_repo").path)
        assertEquals(5, repo.totalCommits())
    }

    @Test
    fun testGetCommitFromTag() {
        val repo = Git(testFolder.resolve("small_repo").path)
        val commit = repo.getCommitFromTag("v1.4")
        assertEquals("09f6182cef737db02a085e1d018963c7a29bde5a", commit.hash)

        assertFails {
            repo.getCommitFromTag("v1.5")
        }
    }

    @Test
    fun testListFilesInCommit() {
        val repo = Git(testFolder.resolve("complex_repo").path)
        repo.checkout("a7053a4dcd627f5f4f213dc9aa002eb1caf926f8")
        val files1 = repo.files()
        assertEquals(3, files1.size)
        repo.reset()

        repo.checkout("f0dd1308bd904a9b108a6a40865166ee962af3d4")
        val files2 = repo.files()
        assertEquals(2, files2.size)
        repo.reset()

        repo.checkout("9e71dd5726d775fb4a5f08506a539216e878adbb")
        val files3 = repo.files()
        assertEquals(3, files3.size)
        repo.reset()
    }

    @Test
    fun testCheckoutConsecutiveCommits() {
        val repo = Git(testFolder.resolve("complex_repo").path)
        repo.checkout("a7053a4dcd627f5f4f213dc9aa002eb1caf926f8")
        repo.checkout("f0dd1308bd904a9b108a6a40865166ee962af3d4")
        repo.checkout("9e71dd5726d775fb4a5f08506a539216e878adbb")

        val files3 = repo.files()
        assertEquals(3, files3.size)
        repo.reset()
    }

    @Test
    fun testCheckoutWithCommitNotFullyMergedToMaster() {
        val repo = Git(testFolder.resolve("branches_without_files").path)
        repo.checkout("developing")
        val files1 = repo.files()
        assertEquals(2, files1.size)
        repo.reset()

        val files2 = repo.files()
        assertEquals(1, files2.size)

        repo.checkout("developing")
        val files3 = repo.files()
        assertEquals(2, files3.size)
        repo.reset()
    }

    @Test
    fun testGetAllCommits() {
        val repo = Git(testFolder.resolve("complex_repo").path)
        val changeSets = repo.getListCommits().toList()
        assertEquals(13, changeSets.size)
        assertEquals("866e997a9e44cb4ddd9e00efe49361420aff2559", changeSets[0].hash)
        assertEquals("e7d13b0511f8a176284ce4f92ed8c6e8d09c77f2", changeSets[12].hash)
    }

    @Test
    fun testBranchesFromCommit() {
        val repo = Git(testFolder.resolve("complex_repo").path)
        var commit = repo.getCommit("a997e9d400f742003dea601bb05a9315d14d1124")

        assertEquals(1, commit.branches.size)
        assertContains(commit.branches, "b2")

        commit = repo.getCommit("866e997a9e44cb4ddd9e00efe49361420aff2559")
        assertEquals(2, commit.branches.size)
        assertContains(commit.branches, "master")
        assertContains(commit.branches, "b2")
    }

    @Test
    fun testOtherBranchesWithMerge() {
        val repo = Git(testFolder.resolve("branches_not_merged").path)
        var commit = repo.getCommit("7203c0b8220dcc7a59614bc7549799cd203ac072")
        assertFalse(commit.inMainBranch)

        commit = repo.getCommit("87a31153090808f1e6f679a14ea28729a0b74f4d")
        assertFalse(commit.inMainBranch)

        commit = repo.getCommit("b197ef4f0b4bc5b7d55c8949ecb1c861731f0b9d")
        assertTrue(commit.inMainBranch)

        commit = repo.getCommit("e51421e0beae6a3c20bdcdfc21066e05db675e03")
        assertTrue(commit.inMainBranch)
    }

    @Test
    fun testCommitInMasterBranch() {
        val repo = Git(testFolder.resolve("branches_merged").path)
        assertEquals("29e929fbc5dc6a2e9c620069b24e2a143af4285f", repo.getHead().hash)

        repo.checkout("8986af2a679759e5a15794f6d56e6d46c3f302f1")

        val gitToChangeGead = Git(testFolder.resolve("branches_merged").path)
        var commit = gitToChangeGead.getCommit("8169f76a3d7add54b4fc7bca7160d1f1eede6eda")
        assertFalse(commit.inMainBranch)

        repo.reset()
        assertEquals("29e929fbc5dc6a2e9c620069b24e2a143af4285f", repo.getHead().hash)
    }

    @Test
    fun testShouldDetailACommit(){
        val repo = Git(testFolder.resolve("complex_repo").path)
        val commit = repo.getCommit("866e997a9e44cb4ddd9e00efe49361420aff2559")

        assertEquals("Maurício Aniche", commit.author.name)
        assertEquals("mauricioaniche@gmail.com", commit.author.email)

        assertEquals("Matricula adicionada", commit.msg)
        assertEquals(1, commit.modifiedFiles.size)

        assertEquals("Matricula.java", commit.modifiedFiles[0].newPath)
        assertTrue(commit.modifiedFiles[0].diff.startsWith("diff --git a/Matricula.java b/Matricula.java\nnew file mode 100644"))
        assertNotNull(commit.modifiedFiles[0].content)
        commit.modifiedFiles[0].content?.decodeToString()?.startsWith("package model;")?.let { assertTrue(it) }

        assertNotNull(commit.modifiedFiles[0].sourceCode)
        commit.modifiedFiles[0].sourceCode?.startsWith("package model;")?.let { assertTrue(it) }
    }

    @Test
    fun testMergeCommits(){
        val repo = Git(testFolder.resolve("branches_merged").path)
        var commit = repo.getCommit("168b3aab057ed61a769acf336a4ef5e64f76c9fd")
        assertFalse(commit.merge)

        commit = repo.getCommit("8169f76a3d7add54b4fc7bca7160d1f1eede6eda")
        assertFalse(commit.merge)

        commit = repo.getCommit("29e929fbc5dc6a2e9c620069b24e2a143af4285f")
        assertTrue(commit.merge)
    }

    @Test
    fun testNumberOfModifiedFiles(){
        val repo = Git(testFolder.resolve("complex_repo").path)
        var commit = repo.getCommit("866e997a9e44cb4ddd9e00efe49361420aff2559")
        assertEquals(62, commit.modifiedFiles[0].addedLines)
        assertEquals(0, commit.modifiedFiles[0].deletedLines)

        commit = repo.getCommit("d11dd6734ff4e60cac3a7b58d9267f138c9e05c7")
        assertEquals(1, commit.modifiedFiles[0].addedLines)
        assertEquals(1, commit.modifiedFiles[0].deletedLines)
    }

    @Test
    fun testModificationStatus(){
        val repo = Git(testFolder.resolve("complex_repo").path)
        var commit = repo.getCommit("866e997a9e44cb4ddd9e00efe49361420aff2559")
        assertEquals(ModificationType.ADD, commit.modifiedFiles[0].changeType)
        assertNull(commit.modifiedFiles[0].oldPath)

        commit = repo.getCommit("57dbd017d1a744b949e7ca0b1c1a3b3dd4c1cbc1")
        assertEquals(ModificationType.MODIFY, commit.modifiedFiles[0].changeType)
        assertEquals(commit.modifiedFiles[0].newPath, commit.modifiedFiles[0].oldPath)

        commit = repo.getCommit("ffccf1e7497eb8136fd66ed5e42bef29677c4b71")
        assertEquals(ModificationType.DELETE, commit.modifiedFiles[0].changeType)
        assertNull(commit.modifiedFiles[0].newPath)
    }

    @Test
    fun testDiffs(){
        val repo = Git(testFolder.resolve("two_modifications").path)
        val commit = repo.getCommit("93b4b18673ca6fb5d563bbf930c45cd1198e979b")

        assertEquals(2, commit.modifiedFiles.size)

        for(mod in commit.modifiedFiles){
            if(mod.filename == "file4.java"){
                assertEquals(8, mod.deletedLines)
                assertEquals(0, mod.addedLines)
            }

            if(mod.filename == "file2.java"){
                assertEquals(12, mod.deletedLines)
                assertEquals(0, mod.addedLines)
            }
        }
    }

    @Test
    fun testDetailRename() {
        val repo = Git(testFolder.resolve("complex_repo").path)
        var commit = repo.getCommit("f0dd1308bd904a9b108a6a40865166ee962af3d4")

        assertEquals("Maurício Aniche", commit.author.name)
        assertEquals("mauricioaniche@gmail.com", commit.author.email)

        assertEquals("Matricula.javax", commit.modifiedFiles[0].newPath)
        assertEquals("Matricula.java", commit.modifiedFiles[0].oldPath)
    }

    @Test
    fun testParentCommits() {
        val repo = Git(testFolder.resolve("branches_merged").path)
        val mergedCommit = repo.getCommit("29e929fbc5dc6a2e9c620069b24e2a143af4285f")
        assertEquals(2, mergedCommit.parents.size)
        assertContains(mergedCommit.parents, "8986af2a679759e5a15794f6d56e6d46c3f302f1")
        assertContains(mergedCommit.parents, "8169f76a3d7add54b4fc7bca7160d1f1eede6eda")

        val normalCommit = repo.getCommit("8169f76a3d7add54b4fc7bca7160d1f1eede6eda")
        assertEquals(1, normalCommit.parents.size)
        assertContains(normalCommit.parents, "168b3aab057ed61a769acf336a4ef5e64f76c9fd")
    }

    @Test
    fun testTags() {
        val repo = Git(testFolder.resolve("tags").path)
        var commit = repo.getCommitFromTag("tag1")
        assertEquals("6bb9e2c6a8080e6b5b34e6e316c894b2ddbf7fcd", commit.hash)

        commit = repo.getCommitFromTag("tag2")
        assertEquals("4638730126d40716e230c2040751a13153fb1556", commit.hash)

        assertFailsWith(NullPointerException::class) {
            repo.getCommitFromTag("tag4")
        }
    }

    // TODO: implement testGetCommitsLastModifiedLinesSimple line[344]
    @Test
    fun testGetCommitsLastModifiedLinesSimple() {
        //val repo = Git(testFolder.resolve("szz").path)
    }

    // TODO: implement testGetCommitsLastModifiedLinesMultiple line[353]
    @Test
    fun testGetCommitsLastModifiedLinesMultiple() {
        //val repo = Git(testFolder.resolve("szz").path)
    }

    // TODO: implement testGetCommitsLastModifiedLinesRenameAndFix line[366]
    @Test
    fun testGetCommitsLastModifiedLinesRenameAndFix() {
        //val repo = Git(testFolder.resolve("szz").path)
    }

    // TODO: implement testGetCommitsLastModifiedLinesRename line[375]
    @Test
    fun testGetCommitsLastModifiedLinesRename() {
        //val repo = Git(testFolder.resolve("szz").path)
    }

    // TODO: implement testGetCommitsLastModifiedLinesUselessLines line[384]
    @Test
    fun testGetCommitsLastModifiedLinesUselessLines() {
        //val repo = Git(testFolder.resolve("szz").path)
    }

    // TODO: implement testGetCommitsLastModifiedLinesUselessLines2 line[392]
    @Test
    fun testGetCommitsLastModifiedLinesUselessLines2() {
        //val repo = Git(testFolder.resolve("szz").path)
    }

    // TODO: implement testGetCommitsLastModifiedLinesForSingleFile line[398]
    @Test
    fun testGetCommitsLastModifiedLinesForSingleFile() {
        //val repo = Git(testFolder.resolve("szz").path)
    }

    // TODO: implement testGetCommitsLastModifiedLinesWithMoreModification line[412]
    @Test
    fun testGetCommitsLastModifiedLinesWithMoreModification() {
        //val repo = Git(testFolder.resolve("szz").path)
    }


    @Test
    fun testGetCommitsModifiedFile() {
        val repo = Git(testFolder.resolve("small_repo").path)
        val commits = repo.getCommitsModifiedFile("file2.java")

        assertEquals(3, commits.size)
        assertContains(commits, "09f6182cef737db02a085e1d018963c7a29bde5a")
        assertContains(commits, "6411e3096dd2070438a17b225f44475136e54e3a")
        assertContains(commits, "a88c84ddf42066611e76e6cb690144e5357d132c")
    }

    @Test
    fun testGetCommitsModifiedFileMissingFile() {
        val repo = Git(testFolder.resolve("small_repo").path)
        val commits = repo.getCommitsModifiedFile("non-existing-file.java")

        assertEquals(0, commits.size)
    }

    @Test
    fun testGetTaggedCommits() {
        val repo = Git(testFolder.resolve("tags").path)
        val taggedCommits = repo.getTaggedCommits()

        assertEquals(3, taggedCommits.size)
        assertContains(taggedCommits[0], "6bb9e2c6a8080e6b5b34e6e316c894b2ddbf7fcd")
        assertContains(taggedCommits[1], "4638730126d40716e230c2040751a13153fb1556")
        assertContains(taggedCommits[2], "627e1ad917a188a861c9fedf6e5858b79edbe439")
    }

    @Test
    fun testGetTaggedCommitsWoTags() {
        val repo = Git(testFolder.resolve("different_files").path)
        val taggedCommits = repo.getTaggedCommits()

        assertEquals(0, taggedCommits.size)
    }

    // TODO: implement testGetCommitsLastModifiedLinesWithMoreModification line[454]
    @Test
    fun testGetCommitsLastModifiedLinesHyperBlame() {

    }

    // TODO: implement testGetCommitsLastModifiedLinesHyperBlameUnblamable line[466]
    @Test
    fun testGetCommitsLastModifiedLinesHyperBlameUnblamable() {

    }

    // TODO: implement testGetCommitsLastModifiedLinesHyperBlameIgnoreHash line[480]
    @Test
    fun testGetCommitsLastModifiedLinesHyperBlameIgnoreHash() {

    }

    // TODO: implement testGetCommitsLastModifiedLinesHyperBlameWithRenaming line[494]
    @Test
    fun testGetCommitsLastModifiedLinesHyperBlameWithRenaming() {

    }

}