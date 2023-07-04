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

import kdriller.domain.Developer
import org.junit.jupiter.api.Test

internal class TestDeveloper {
    @Test fun testEqDev() {
        val d1 = Developer("Davide", "s.d@gmail.com")
        val d2 = Developer("Davide", "s.d@gmail.com")
        val d3 = Developer("Davide", "s.d@gmail.eu")
        val d4 = null

        assert(d1 == d1)
        assert(d1 == d2)
        assert(d1 != d3)
        assert(d1 != d4)
    }
}