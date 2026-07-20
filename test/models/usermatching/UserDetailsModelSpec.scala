/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.usermatching

import models.DateModel
import utilities.UnitTestTrait

class UserDetailsModelSpec extends UnitTestTrait {

  private def check(firstName: String, lastName: String, expectedFirst: String, expectedLast: String) = {
    val model = UserDetailsModel(firstName, lastName, "nino", DateModel("1", "1", "2001"))

    model.firstName mustBe expectedFirst
    model.lastName mustBe expectedLast
  }

  "UserDetailsModel" should {
    "capitalise first and last names" when {
      "for simple names" in {
        check(
          firstName = "joe",
          lastName = "bloggs",
          expectedFirst = "Joe",
          expectedLast = "Bloggs"
        )
      }

      "for hyphenated names" in {
        check(
          firstName = "mary-anne",
          lastName = "double-barrel",
          expectedFirst = "Mary-Anne",
          expectedLast = "Double-Barrel"
        )
      }

      "for multiple names" in {
        check(
          firstName = "mary anne",
          lastName = "double barrel",
          expectedFirst = "Mary Anne",
          expectedLast = "Double Barrel"
        )
      }

      "for multiple hyphenated names" in {
        check(
          firstName = "mary-anne sarah-jane",
          lastName = "double-barrel blazer-jones",
          expectedFirst = "Mary-Anne Sarah-Jane",
          expectedLast = "Double-Barrel Blazer-Jones"
        )
      }
    }
  }
}
