/*
 * Copyright 2025 HM Revenue & Customs
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

import forms.individual.accountingperiod.AccountingPeriodForm
import forms.individual.accountingperiod.AccountingPeriodForm._
import models.common.BusinessAccountingPeriod
import models.common.BusinessAccountingPeriod._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.data.FormError


class AccountingPeriodFormSpec extends PlaySpec with GuiceOneAppPerTest {

  val testInputs = Seq(SixthAprilToFifthApril, FirstAprilToThirtyFirstMarch, OtherAccountingPeriod)

  def boundForm(testInput: Map[String, String]): Option[BusinessAccountingPeriod] = accountingPeriodForm.bind(testInput).value

  "AccountingPeriodForm" should {

    "return valid accounting period" when {
      testInputs.foreach { accountingPeriod =>

        s"${accountingPeriod.key} is provided to the form" in {
          val testInput: Map[String, String] = Map(AccountingPeriodForm.fieldName -> accountingPeriod.key)

          boundForm(testInput).value mustBe accountingPeriod
        }
      }
    }
    "return a form error" when {
      "an invalid accounting period is provided to the form" in {
        val testInput: Map[String, String] = Map(AccountingPeriodForm.fieldName -> "invalid")

        accountingPeriodForm.bind(testInput).errors must contain(
          FormError(AccountingPeriodForm.fieldName, "accounting-period.error")
        )
      }
      "no accounting period is provided to the form" in {
        val testInput: Map[String, String] = Map.empty

        accountingPeriodForm.bind(testInput).errors must contain(FormError(AccountingPeriodForm.fieldName, "accounting-period.error"))
      }
    }
  }

}
