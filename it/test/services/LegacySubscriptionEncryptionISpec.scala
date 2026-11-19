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

package services

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestModels.testBusiness
import models.common.{AccountingYearModel, SoleTraderBusinesses}
import models.{Current, SessionData}
import play.api.http.Status.OK
import play.api.libs.json.{Format, Json}
import services.GetCompleteDetailsService.{CompleteDetails, IncomeSources, SoleTraderBusiness, SoleTraderBusinesses as CompleteSoleTraderBusinesses}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.http.HeaderCarrier
import utilities.SubscriptionDataKeys.{SelectedTaxYear, SoleTraderBusinessesKey}

// TODO: Remove these temporary legacy compatibility tests once ApplicationCrypto is fully removed.
class LegacySubscriptionEncryptionISpec extends ComponentSpecBase {

  private lazy val subscriptionDetailsService = app.injector.instanceOf[SubscriptionDetailsService]
  private lazy val getCompleteDetailsService = app.injector.instanceOf[GetCompleteDetailsService]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private lazy val legacySensitiveFormat: Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)(
      implicitly[Format[String]],
      app.injector.instanceOf[ApplicationCrypto].JsonCrypto
    )

  private val selfEmployment = testBusiness("legacy-business", confirmed = true).copy(
    startDateBeforeLimit = Some(true),
    businessDisplayName = Some("Legacy business display name")
  )
  private val businesses = SoleTraderBusinesses(Seq(selfEmployment.toSoleTraderBusiness))

  private def legacyEncryptedBusinesses = Json.toJson(businesses)(
    SoleTraderBusinesses.encryptedFormat(legacySensitiveFormat)
  )

  "SubscriptionDetailsService" must {
    "read legacy AES-encrypted sole trader businesses through the real connector" in {
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
        SoleTraderBusinessesKey, OK, legacyEncryptedBusinesses
      )

      subscriptionDetailsService.fetchAllSoleTraderBusinesses(reference).futureValue mustBe businesses
      IncomeTaxSubscriptionConnectorStub.verifyGet(SoleTraderBusinessesKey)()
    }

    "read legacy AES-encrypted self-employment details through the real connector" in {
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
        SoleTraderBusinessesKey, OK, legacyEncryptedBusinesses
      )

      subscriptionDetailsService.fetchAllSelfEmployments(reference).futureValue mustBe Seq(selfEmployment)
      IncomeTaxSubscriptionConnectorStub.verifyGet(SoleTraderBusinessesKey)()
    }
  }

  "GetCompleteDetailsService" must {
    "build complete sign-up details from legacy AES-encrypted subscription data through the real services and connector" in {
      val taxYear = AccountingYearModel(Current)
      IncomeTaxSubscriptionConnectorStub.stubGetAllSubscriptionDetails(
        OK,
        Json.obj(
          SoleTraderBusinessesKey -> legacyEncryptedBusinesses,
          SelectedTaxYear -> Json.toJson(taxYear)
        )
      )
      val expected = CompleteDetails(
        incomeSources = IncomeSources(
          soleTraderBusinesses = Some(CompleteSoleTraderBusinesses(Seq(SoleTraderBusiness(
            id = selfEmployment.id,
            name = selfEmployment.businessName.get.businessName,
            displayName = selfEmployment.businessDisplayName,
            trade = selfEmployment.businessTradeName.get.businessTradeName,
            startDate = None,
            address = selfEmployment.businessAddress.get.address
          )))),
          ukProperty = None,
          foreignProperty = None
        ),
        taxYear = taxYear
      )

      getCompleteDetailsService.getCompleteSignUpDetails(reference, SessionData()).futureValue mustBe Right(expected)
      helpers.WiremockHelper.verifyGet(IncomeTaxSubscriptionConnectorStub.allSubscriptionDataUri(), Some(1))
    }
  }
}
