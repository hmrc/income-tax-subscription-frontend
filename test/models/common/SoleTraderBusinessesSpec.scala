/*
 * Copyright 2026 HM Revenue & Customs
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

package models.common

import org.scalatestplus.play.PlaySpec

class SoleTraderBusinessesSpec extends PlaySpec {

  "getBusinessesWithDuplicateNames" should {
    "return businesses whose names match another business" in {
      val duplicateOne = SoleTraderBusiness(id = "1", name = Some("Business"))
      val duplicateTwo = SoleTraderBusiness(id = "2", name = Some("Business"))
      val unique = SoleTraderBusiness(id = "3", name = Some("Another business"))
      val businesses = SoleTraderBusinesses(Seq(duplicateOne, unique, duplicateTwo))

      businesses.getBusinessesWithDuplicateNames mustBe Seq(duplicateOne, duplicateTwo)
    }

    "ignore businesses with unique names" in {
      val unnamedOne = SoleTraderBusiness(id = "1", name = Some("Business"))
      val unnamedTwo = SoleTraderBusiness(id = "2", name = Some("Another business"))

      SoleTraderBusinesses(Seq(unnamedOne, unnamedTwo)).getBusinessesWithDuplicateNames mustBe empty
    }

    "addDisplayNames" should {
      "update only businesses with duplicate names, and preserve the original order" in {
        val duplicateOne = SoleTraderBusiness(id = "1", name = Some("Business"))
        val unique = SoleTraderBusiness(id = "2", name = Some("Another business"))
        val duplicateTwo = SoleTraderBusiness(id = "3", name = Some("Business"))
        val businesses = SoleTraderBusinesses(Seq(duplicateOne, unique, duplicateTwo))

        businesses.addDisplayNames(Seq("First label", "Second label")).businesses mustBe Seq(
          duplicateOne.copy(displayName = Some("First label")),
          unique,
          duplicateTwo.copy(displayName = Some("Second label"))
        )
      }
    }
  }
}
