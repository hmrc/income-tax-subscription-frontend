/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.agent

import forms.formatters.DateModelMapping.dateModelMapping
import forms.submapping.AccountingMethodMapping.{option_accruals, option_cash}
import forms.submapping.{AccountingMethodMapping, YesNoMapping}
import models._
import models.common.PropertyModel
import play.api.data.Forms.tuple
import play.api.data.validation.Invalid
import play.api.data.{Form, Mapping}
import utilities.AccountingPeriodUtil

import java.time.LocalDate

object UkPropertyIncomeSourcesForm {

  val startDate: String = "startDate"
  val startDateBeforeLimit = "start-date-before-limit"
  val accountingMethodProperty: String = "accountingMethodProperty"
  val errorContext: String = "property"

  def maxStartDate: LocalDate = LocalDate.now().plusDays(6)

  def minStartDate: LocalDate = LocalDate.of(1900, 1, 1)

  private def ukStartDate(f: LocalDate => String): Mapping[DateModel] = dateModelMapping(
    isAgent = true,
    errorContext = errorContext,
    minDate = Some(minStartDate),
    maxDate = Some(maxStartDate),
    Some(f)
  )

  private val ukAccountingMethod: Mapping[AccountingMethod] = AccountingMethodMapping(
    errInvalid = Invalid("agent.error.accounting-method-property.invalid"),
    errEmpty = Some(Invalid("agent.error.accounting-method-property.invalid"))
  )

  def ukPropertyIncomeSourcesForm(f: LocalDate => String): Form[(DateModel, AccountingMethod)] = Form(
    tuple(
      startDate -> ukStartDate(f),
      accountingMethodProperty -> ukAccountingMethod
    )
  )

  def ukPropertyIncomeSourcesFormNoDate: Form[(YesNo, AccountingMethod)] = Form(
    tuple(
      startDateBeforeLimit -> YesNoMapping.yesNoMapping(
        yesNoInvalid = Invalid(s"agent.error.$errorContext.income-source.$startDateBeforeLimit.invalid", AccountingPeriodUtil.getStartDateLimit.getYear.toString)
      ),
      accountingMethodProperty -> ukAccountingMethod
    )
  )

  def createPropertyMapData(property: Option[PropertyModel]): Map[String, String] = {

    val maybeStartDate: Option[DateModel] = property.flatMap(_.startDate)
    val maybeStartDateBeforeLimit: Option[Boolean] = property.flatMap(_.startDateBeforeLimit)
    val maybeAccountingMethod: Option[AccountingMethod] = property.flatMap(_.accountingMethod)

    val dateMap: Map[String, String] = maybeStartDate.fold(Map.empty[String, String]) { date =>
      Map(
        s"$startDate-dateDay" -> date.day,
        s"$startDate-dateMonth" -> date.month,
        s"$startDate-dateYear" -> date.year
      )
    }

    val startDateBeforeLimitMap: Map[String, String] = {
      if (maybeStartDate.exists(_.toLocalDate.isBefore(AccountingPeriodUtil.getStartDateLimit))) {
        Map(startDateBeforeLimit -> YesNoMapping.option_yes)
      } else {
        maybeStartDateBeforeLimit.fold(
          if (maybeStartDate.exists(_.toLocalDate.isAfter(AccountingPeriodUtil.getStartDateLimit.minusDays(1)))) {
            Map(startDateBeforeLimit -> YesNoMapping.option_no)
          } else {
            Map.empty[String, String]
          }
        ) {
          case true => Map(startDateBeforeLimit -> YesNoMapping.option_yes)
          case false => Map(startDateBeforeLimit -> YesNoMapping.option_no)
        }
      }
    }

    val accountingMethodMap: Map[String, String] = maybeAccountingMethod.fold(Map.empty[String, String]) {
      case Cash => Map(accountingMethodProperty -> option_cash)
      case Accruals => Map(accountingMethodProperty -> option_accruals)
    }

    dateMap ++ startDateBeforeLimitMap ++ accountingMethodMap

  }
}