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

import mu.KotlinLogging
import org.eclipse.jgit.lib.TextProgressMonitor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import org.eclipse.jgit.api.Git as Repo

private val logger = KotlinLogging.logger {}

class TestMemory {
    fun cloneTempRepo(tmpdir: Path): String {
        val repoFolder = tmpdir.toFile()
        Repo.cloneRepository().setURI("https://github.com/ishepard/pydriller.git").setDirectory(repoFolder).setProgressMonitor(
            TextProgressMonitor()
        ).call().use {

        }
        return repoFolder.path
    }

    @Test
    fun testMemory(@TempDir tmpdir: Path){
        val repoFolder = cloneTempRepo(tmpdir)

        val (allCommitsWithNothing, elapsedWithNothing) = measureTimedValue {
            mine(repoFolder, 0)
        }

        val (allCommitsWithEverything, elapsedWithEverything) = measureTimedValue {
            mine(repoFolder, 1)
        }

        val (allCommitsWithMetrics, elapsedWithMetrics) = measureTimedValue {
            mine(repoFolder, 2)
        }
        val maxValues = listOf(
            allCommitsWithNothing.maxOrNull(),
            allCommitsWithEverything.maxOrNull(),
            allCommitsWithMetrics.maxOrNull()
        )

        logger.warn("Max values are: $maxValues")

        val minutesWithEverything = elapsedWithEverything.truncate(DurationUnit.MINUTES)
        val minutesWithMetrics = elapsedWithMetrics.truncate(DurationUnit.MINUTES)

        logger.warn(
            "TIME: With nothing: ${elapsedWithNothing.truncate(DurationUnit.SECONDS)} (.. commits/sec), " +
            "with everything: ${elapsedWithEverything.truncate(DurationUnit.SECONDS)} (.. commits/sec), " +
            "with metrics: ${elapsedWithMetrics.truncate(DurationUnit.SECONDS)} (.. commits/sec)",
        )
    }

    fun mine(repo: String, _type: Int): MutableList<Long> {
        val dt2 = Calendar.getInstance()
        dt2.set(2021, 12, 1)
        val allCommits = mutableListOf<Long>()

        for (commit in Repository(listOf(repo)).traverseCommits()){
            val memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            allCommits.add(memory)
            val h = commit.author.name  // noqa

            if (_type == 0){
                continue
            }

            for (mod in commit.modifiedFiles){
                val dd = mod.diff
                if (_type == 1) {
                    continue
                }

                if (mod.filename.endsWith(".py")){
                    val cc = mod.complexity // noqa
                }
            }
        }
        return allCommits
    }

    /**
     * Truncates the duration to the specified [unit].
     *
     * Truncating the duration removes any time units smaller than the specified unit
     * and returns a new [Duration] with the truncated value.
     *
     * @param unit The duration unit to truncate to.
     * @returns a new [Duration] truncated to the specified [unit].
     */
    private fun Duration.truncate(unit: DurationUnit): Duration {
        return toComponents { days: Long, hours: Int, minutes: Int, seconds: Int, nanoseconds: Int ->
            when (unit) {
                DurationUnit.NANOSECONDS -> this // there's no smaller unit than NANOSECONDS, so just return the current Duration
                DurationUnit.MICROSECONDS -> days.days + hours.hours + minutes.minutes + seconds.seconds + nanoseconds.nanoseconds.inWholeMicroseconds.microseconds
                DurationUnit.MILLISECONDS -> days.days + hours.hours + minutes.minutes + seconds.seconds + nanoseconds.nanoseconds.inWholeMilliseconds.milliseconds
                DurationUnit.SECONDS      -> days.days + hours.hours + minutes.minutes + seconds.seconds
                DurationUnit.MINUTES      -> days.days + hours.hours + minutes.minutes
                DurationUnit.HOURS        -> days.days + hours.hours
                DurationUnit.DAYS         -> days.days
            }
        }
    }
}