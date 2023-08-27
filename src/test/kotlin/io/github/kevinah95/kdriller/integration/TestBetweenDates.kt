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

import io.github.kevinah95.kdriller.Git
import io.github.kevinah95.kdriller.Repository
import org.apache.commons.io.FileUtils
import java.io.File
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TestBetweenDates {

    private val testFolder: File = FileUtils.getFile("src", "test", "resources", "test-repos")

    val toZone = TimeZone.getTimeZone("GMT-04:00")
    val cal = Calendar.getInstance(toZone)
    val dt1: Date
        get() {
            cal.set(2016, Calendar.OCTOBER, 8, 17, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.time
        }
    val dt2: Date
        get() {
            cal.set(2016, Calendar.OCTOBER, 8, 17, 59, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.time
        }

    @Test
    fun testBetweenDates(){
        val pathToRepo = listOf(testFolder.resolve("different_files").path)

        val listCommits = Repository(pathToRepo, since = dt1, to = dt2).traverseCommits().toList()

        assertEquals(2, listCommits.size)
        assertEquals("a1b6136f978644ff1d89816bc0f2bd86f6d9d7f5", listCommits[0].hash)
        assertEquals("375de7a8275ecdc0b28dc8de2568f47241f443e9", listCommits[1].hash)
    }

    @Test
    fun testBetweenDatesWithoutTimezone() {

        val zoned1 = ZonedDateTime.of(2016, 10, 8, 21, 0, 0, 0, null)

        val pathToRepo = listOf(testFolder.resolve("different_files").path)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        //val cal = Calendar.getInstance()
        cal.set(2016, Calendar.OCTOBER, 8, 21, 0, 0)
        val dt1 = cal.time
        cal.set(2016, Calendar.OCTOBER, 8, 21, 59, 0)
        val dt2 = cal.time


        for (commit in Repository(pathToRepo, since = dt1, to = dt2).traverseCommits()){
            val dat = commit.committerDate
            val x = 0
        }

        val listCommits = Repository(pathToRepo, since = dt1, to = dt2).traverseCommits().toList()

        assertEquals(2, listCommits.size)
        assertEquals("a1b6136f978644ff1d89816bc0f2bd86f6d9d7f5", listCommits[0].hash)
        assertEquals("375de7a8275ecdc0b28dc8de2568f47241f443e9", listCommits[1].hash)
    }
}