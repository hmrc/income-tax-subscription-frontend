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

package models

import models.common.business.SelfEmploymentData
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel, SoleTraderBusinesses}
import play.api.libs.json.{Format, JsValue, OFormat, Reads}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import utilities.SubscriptionDataKeys.{OverseasProperty, Property, SelectedTaxYear, SoleTraderBusinessesKey}

case class SubscriptionData(data: Map[String, JsValue]) {

  private def fetch[T](key: String)(implicit reads: Reads[T]): Option[T] = {
    data.get(key).flatMap(_.asOpt[T])
  }

  def fetchSelectedTaxYear: Option[AccountingYearModel] = {
    fetch[AccountingYearModel](SelectedTaxYear)
  }

  def fetchSelectedTaxYear(sessionData: SessionData): Option[AccountingYearModel] = {
    if (sessionData.fetchEligibilityStatus.exists(_.eligibleNextYearOnly)) {
      Some(AccountingYearModel(Next, confirmed = true, editable = false))
    } else if (sessionData.fetchMandationStatus.exists(_.currentYearStatus.isMandated)) {
      Some(AccountingYearModel(Current, confirmed = true, editable = false))
    } else {
      fetchSelectedTaxYear
    }
  }

  def fetchProperty: Option[PropertyModel] = {
    fetch[PropertyModel](Property)
  }

  def fetchOverseasProperty: Option[OverseasPropertyModel] = {
    fetch[OverseasPropertyModel](OverseasProperty)
  }

  def fetchAllSelfEmployments(implicit sensitiveFormat: Format[SensitiveString]): Seq[SelfEmploymentData] = {
    implicit val encryptedFormat: OFormat[SoleTraderBusinesses] = SoleTraderBusinesses.encryptedFormat

    fetch[SoleTraderBusinesses](SoleTraderBusinessesKey).fold(Seq.empty[SelfEmploymentData]) { soleTraderBusinesses =>
      soleTraderBusinesses.businesses.map(_.toSelfEmploymentData)
    }
  }

}


