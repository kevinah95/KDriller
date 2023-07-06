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
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*


internal class TestGit {
    private val testFolder: File = FileUtils.getFile("src", "test", "resources", "test-repos")

    @Test
    fun testProjectname() {
        val repo = Git(testFolder.resolve("small_repo").path)
        assert(repo.projectName == "small_repo")
    }

    @Test
    fun testGetHead() {
        val repo = Git(testFolder.resolve("small_repo").path)
        val headCommit = repo.getHead()

        assert(headCommit != null)
        assert(headCommit.hash == "da39b1326dbc2edfe518b90672734a08f3c13458")
        assert(headCommit.authorDate.time == 1522164679L)
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
            assert(commit.hash in list_commits)
        }

        assert(changeSets.count() == 5)
    }

    @Test
    fun testGetCommit() {
        val repo = Git(testFolder.resolve("small_repo").path)
        val commit = repo.getCommit("09f6182cef737db02a085e1d018963c7a29bde5a")

        assert(commit.hash == "09f6182cef737db02a085e1d018963c7a29bde5a")
        assert(commit.author.name == "ishepard")
        assert(commit.committer.name == "ishepard")
        val timeZone = TimeZone.getTimeZone(commit.authorTimeZone.id)
        val cal = Calendar.getInstance(timeZone)
        cal.set(2018, 2, 22, 10, 42, 3)
        cal.set(Calendar.MILLISECOND, 0)
        assert(commit.authorDate.time == cal.time.time)
        // TODO: assert len(c.modified_files) == 1
        //println(commit.msg)
        assert(commit.msg == "Ooops file2")
        assert(commit.inMainBranch)
        assert(commit.insertions == 4)
        assert(commit.deletions == 0)
        assert(commit.lines == 4)
        // TODO: commit.files
        assert(commit.diffEntries?.size == 1)


        for (mf in commit.modifiedFiles){
            println("DIFFFFFFFFFF")
            println(mf.diff)
            println("diff_parseddiff_parseddiff_parseddiff_parsed")
            mf.diffParsed["added"]?.forEach { println(it) }
            println(mf.sourceCode)
            println(mf.sourceCodeBefore)
        }
    }

}