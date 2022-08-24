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

package models.common.business

import models.common.business.BusinessTradeNameModel.{MaximumLengthOfBusinessTradeName, getBusinessTradeNameModelMaybe}
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.play.PlaySpec

class BusinessTradeNameModelSpec extends PlaySpec {

  private val shortEnough = MaximumLengthOfBusinessTradeName
  private val tooLong = shortEnough + 1
  private val oneChar = "x"

  "getBusinessTradeNameModelMaybe" when {

    "the string is short enough" should {
      "return the value, wrapped up" in {
        val acceptableTradeName = oneChar * shortEnough
        getBusinessTradeNameModelMaybe(acceptableTradeName) should be(Some(BusinessTradeNameModel(acceptableTradeName)))
      }
    }
    "the string is too long" should {
      "return None" in {
        val unacceptableTradeName = oneChar * tooLong
        getBusinessTradeNameModelMaybe(unacceptableTradeName) should be(None)
      }
    }
  }

}
