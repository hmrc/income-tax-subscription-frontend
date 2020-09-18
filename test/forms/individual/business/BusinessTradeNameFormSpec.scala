/*
 * Copyright 2020 HM Revenue & Customs
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

package forms.individual.business

import forms.individual.business.BusinessTradeNameForm.{businessTradeName, businessTradeNameValidationForm}
import forms.validation.testutils.DataMap.DataMap
import models.DateModel
import models.common.BusinessNameModel
import models.individual.business.{BusinessTradeNameModel, SelfEmploymentData, BusinessStartDate}
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.{Form, FormError}


class BusinessTradeNameFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  val testValidBusinessTradeName: String = "Plumbing"
  val testInvalidBusinessTradeName: String = "!()+{}?^~"
  val testValidBusinessTradeNameModel: BusinessTradeNameModel = BusinessTradeNameModel(testValidBusinessTradeName)
  val testInvalidBusinessTradeNameModel: BusinessTradeNameModel = BusinessTradeNameModel(testInvalidBusinessTradeName)

  def businessTradeForm(excludedBusinessTradeNames: Seq[BusinessTradeNameModel] = Nil): Form[BusinessTradeNameModel] = {
    businessTradeNameValidationForm(excludedBusinessTradeNames)
  }

  def testSelfEmploymentData(id: String, businessName: String, businessTrade: String): SelfEmploymentData = SelfEmploymentData(
    id,
    Some(BusinessStartDate(DateModel("1", "1", "1"))),
    Some(BusinessNameModel(businessName)),
    Some(BusinessTradeNameModel(businessTrade))
  )

  "The BusinessTradeNameForm" should {
    "transform a valid request to the case class" in {

      val testInput = Map(businessTradeName -> testValidBusinessTradeName)

      val expected = BusinessTradeNameModel(testValidBusinessTradeName)

      val actual = businessTradeForm().bind(testInput).value

      actual shouldBe Some(expected)
    }

    "when testing the validation" should {

      val maxLength = 160

      val empty = "error.business_trade_name.empty"
      val maxLen = "error.business_trade_name.maxLength"
      val invalid = "error.business_trade_name.invalid"
      val duplicate = "error.business_trade_name.duplicate"

      "the map be empty" in {
        val emptyInput0 = DataMap.EmptyMap
        val emptyTest0 = businessTradeForm().bind(emptyInput0)
        emptyTest0.errors must contain(FormError(businessTradeName, empty))
      }

      "the name be empty" in {
        val emptyInput = DataMap.businessTradeNameMap("")
        val emptyTest = businessTradeForm().bind(emptyInput)
        emptyTest.errors must contain(FormError(businessTradeName, empty))
      }

      "the name is too long" in {
        val maxLengthInput = DataMap.businessTradeNameMap("a" * maxLength + 1)
        val maxLengthTest = businessTradeForm().bind(maxLengthInput)
        maxLengthTest.errors must contain(FormError(businessTradeName, maxLen))
      }

      "the name should be invalid" in {
        val invalidInput = DataMap.businessTradeNameMap("!()+{}?^~")
        val invalidTest = businessTradeForm().bind(invalidInput)
        invalidTest.errors must contain(FormError(businessTradeName, invalid))
      }

      "the name should not allow just a space" in {
        val emptyInput = DataMap.businessTradeNameMap(" ")
        val invalidTest = businessTradeForm().bind(emptyInput)
        invalidTest.errors must contain(FormError(businessTradeName, empty))
      }

      "the name be max characters and acceptable" in {
        val withinLimitInput = DataMap.businessTradeNameMap("a" * maxLength)
        val withinLimitTest = businessTradeForm().bind(withinLimitInput)
        withinLimitTest.value mustNot contain(maxLen)
      }

      "invalidate a business trade which is in the list of excluded business trade" in {
        val testInput = Map(businessTradeName -> "nameOne")
        val expected = BusinessNameModel(testValidBusinessTradeName)
        val actual = businessTradeForm(excludedBusinessTradeNames = Seq(
          BusinessTradeNameModel("nameOne"), BusinessTradeNameModel("nameTwo")
        )).bind(testInput)
        actual.errors must contain(FormError(businessTradeName, "error.business_trade_name.duplicate"))
      }

      "The following submission should be valid" when {
        "there are no other businesses" in {
          val valid = DataMap.businessTradeNameMap("Test business")
          val result = businessTradeForm().bind(valid)
          result.hasErrors shouldBe false
          result.hasGlobalErrors shouldBe false
        }
      }
    }
  }
}

