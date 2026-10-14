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

import _root_.common.Constants.ITSASessionKeys
import models.EligibilityStatus
import models.common.business.SelfEmploymentData
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel, SoleTraderBusinesses}
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.MandationStatusModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{Format, JsValue, Json, OFormat}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter}
import utilities.SubscriptionDataKeys.{OverseasProperty, Property, SelectedTaxYear, SoleTraderBusinessesKey}

class SubscriptionDataSpec extends PlaySpec with GuiceOneAppPerSuite {

  private val crypto: ApplicationCrypto = app.injector.instanceOf[ApplicationCrypto]

  implicit private val jsonCrypto: Encrypter with Decrypter = crypto.JsonCrypto
  implicit private val sensitiveFormat: Format[SensitiveString] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
  implicit private val soleTraderBusinessesFormat: OFormat[SoleTraderBusinesses] = SoleTraderBusinesses.encryptedFormat

  private val selfEmploymentJson: JsValue = Json.toJson(
    SoleTraderBusinesses(
      businesses = Seq(
        SelfEmploymentData(
          id = "123",
          confirmed = true,
          startDateBeforeLimit = Some(false),
          businessStartDate = Some(models.common.business.BusinessStartDate(DateModel("01", "01", "2020")))
        ).toSoleTraderBusiness
      )
    )
  )

  "SubscriptionData" should {
    "return empty Seq when fetchAllSelfEmployments is called with no data" in {
      SubscriptionData(Map.empty).fetchAllSelfEmployments mustBe Seq.empty
    }

    "return empty Seq when SoleTraderBusinesses key is not present" in {
      val subscriptionData = SubscriptionData(Map("other_key" -> Json.obj("field" -> "value")))

      subscriptionData.fetchAllSelfEmployments mustBe Seq.empty
    }

    "return empty Seq when SoleTraderBusinesses data is invalid" in {
      val subscriptionData = SubscriptionData(Map(SoleTraderBusinessesKey -> Json.obj("businesses" -> "invalid")))

      subscriptionData.fetchAllSelfEmployments mustBe Seq.empty
    }

    "return Seq of SelfEmploymentData when SoleTraderBusinesses data is present" in {
      val result = SubscriptionData(Map(SoleTraderBusinessesKey -> selfEmploymentJson)).fetchAllSelfEmployments

      result must have size 1
      result.head.id mustBe "123"
      result.head.confirmed mustBe true
      result.head.startDateBeforeLimit mustBe Some(false)
    }

    "return PropertyModel when Property data is present" in {
      val testData = Map(
        Property -> Json.obj(
          "startDateBeforeLimit" -> false,
          "startDate" -> Json.obj("day" -> "01", "month" -> "01", "year" -> "2020"),
          "confirmed" -> true
        )
      )

      SubscriptionData(testData).fetchProperty mustBe Some(
        PropertyModel(Some(false), Some(DateModel("01", "01", "2020")), confirmed = true)
      )
    }

    "return None when Property data is missing or invalid" in {
      SubscriptionData(Map.empty).fetchProperty mustBe None
      SubscriptionData(Map(Property -> Json.obj("startDate" -> "invalid"))).fetchProperty mustBe None
    }

    "return OverseasPropertyModel when OverseasProperty data is present" in {
      val testData = Map(
        OverseasProperty -> Json.obj(
          "startDateBeforeLimit" -> true,
          "confirmed" -> true
        )
      )

      SubscriptionData(testData).fetchOverseasProperty mustBe Some(
        OverseasPropertyModel(Some(true), confirmed = true)
      )
    }

    "return None when OverseasProperty data is missing or invalid" in {
      SubscriptionData(Map.empty).fetchOverseasProperty mustBe None
      SubscriptionData(Map(OverseasProperty -> Json.obj("startDateBeforeLimit" -> "invalid"))).fetchOverseasProperty mustBe None
    }

    "return AccountingYearModel when SelectedTaxYear data is present" in {
      val testData = Map(
        SelectedTaxYear -> Json.obj(
          "accountingYear" -> "CurrentYear"
        )
      )

      SubscriptionData(testData).fetchSelectedTaxYear mustBe Some(AccountingYearModel(Current))
    }

    "return None when SelectedTaxYear data is missing or invalid" in {
      SubscriptionData(Map.empty).fetchSelectedTaxYear mustBe None
      SubscriptionData(Map(SelectedTaxYear -> Json.obj("accountingYear" -> "InvalidYear"))).fetchSelectedTaxYear mustBe None
    }

    "return the next tax year when the session says the user is eligible next year only" in {
      val subscriptionData = SubscriptionData(Map(SelectedTaxYear -> Json.obj("accountingYear" -> "CurrentYear")))
      val sessionData = SessionData(Map(
        ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exemptionReason = None))
      ))

      subscriptionData.fetchSelectedTaxYear(sessionData) mustBe Some(AccountingYearModel(Next, confirmed = true, editable = false))
    }

    "return the current tax year when the session says the user is mandated for the current year" in {
      val subscriptionData = SubscriptionData(Map(SelectedTaxYear -> Json.obj("accountingYear" -> "NextYear")))
      val sessionData = SessionData(Map(
        ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Mandated, Voluntary))
      ))

      subscriptionData.fetchSelectedTaxYear(sessionData) mustBe Some(AccountingYearModel(Current, confirmed = true, editable = false))
    }

    "fall back to the stored selected tax year when session data does not override it" in {
      val subscriptionData = SubscriptionData(Map(SelectedTaxYear -> Json.obj("accountingYear" -> "CurrentYear")))

      subscriptionData.fetchSelectedTaxYear(SessionData(Map.empty)) mustBe Some(AccountingYearModel(Current))
    }
  }
}

