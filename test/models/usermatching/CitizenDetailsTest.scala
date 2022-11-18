/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import utilities.UnitTestTrait

class CitizenDetailsTest extends UnitTestTrait {

  private val firstName = Some(Math.random().toString)
  private val lastName = Some(Math.random().toString)
  private val fullName: Option[String] = Some((firstName ++ lastName).mkString(" "))

  "CitizenDetailsSuccess" must {
    "construct no name for no names" in {
      CitizenDetails.buildFullName(None, None) shouldBe (None)
    }
    "construct a name for first name only" in {
      CitizenDetails.buildFullName(firstName, None) shouldBe firstName
    }
    "construct a name for second name only" in {
      CitizenDetails.buildFullName(None, lastName) shouldBe lastName
    }
    "construct a name for both names" in {
      CitizenDetails.buildFullName(firstName, lastName) shouldBe fullName
    }
  }

}
