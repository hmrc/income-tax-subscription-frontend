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

package models.common.business

import models.common.business.BusinessTradeNameModel.MaximumLengthOfBusinessTradeName
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.play.PlaySpec

class BusinessTradeNameModelSpec extends PlaySpec {

  private val shortEnough = MaximumLengthOfBusinessTradeName
  private val tooLong = shortEnough + 1
  private val oneChar = "x"

  "toCleanOption" when {
    "the string is short enough" should {
      "return the value, wrapped up" in {
        val acceptableTradeName = oneChar * shortEnough
        BusinessTradeNameModel(acceptableTradeName).toCleanOption shouldBe Some(BusinessTradeNameModel(acceptableTradeName))
      }
    }
    "the string is too long" should {
      "return None" in {
        val unacceptableTradeName = oneChar * tooLong
        BusinessTradeNameModel(unacceptableTradeName).toCleanOption shouldBe None
      }
    }

    "the string has some bad characters, adjacent to spaces, which should be reduced to a single space" should {
      "return None" in {
        val unacceptableTradeName = s"this    is    good    +    that plus    is    bad"
        BusinessTradeNameModel(unacceptableTradeName).toCleanOption shouldBe Some(BusinessTradeNameModel("this is good that plus is bad"))
      }
    }
    "the string has some bad characters, and only one letter" should {
      "return None" in {
        val unacceptableTradeName = s"!!!!!!!!!!$oneChar!!!!!!!!!"
        BusinessTradeNameModel(unacceptableTradeName).toCleanOption shouldBe None
      }
    }
    "the string has all good characters, but only one letter" should {
      "return None" in {
        val unacceptableTradeName = s"1 2 3 4 5 $oneChar 6 7 8 9 0"
        BusinessTradeNameModel(unacceptableTradeName).toCleanOption shouldBe None
      }
    }
    "the string has some bad characters, and two letters" should {
      "return None" in {
        val acceptableTradeName = s"!!!!!!!!!!$oneChar!!!!!!!!!!$oneChar!!!!!!!!!"
        BusinessTradeNameModel(acceptableTradeName).toCleanOption shouldBe Some(BusinessTradeNameModel(s"$oneChar $oneChar"))
      }
    }
  }

}
