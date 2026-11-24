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

import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsResponse, PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import crypto.ComposeCryptoService
import models.{DateModel, SubscriptionData}
import models.common.{SoleTraderBusiness, SoleTraderBusinesses}
import models.common.business.{Address, Country}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{Format, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.MigrateEncryptedDataService.{MigrationNotRequired, SuccessfullyMigratedEncryptedData}
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.SubscriptionDataKeys.SoleTraderBusinessesKey

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MigrateEncryptedDataServiceSpec extends PlaySpec with GuiceOneAppPerSuite with MockSubscriptionDetailsService {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val reference = "test-reference"
  private val service = new MigrateEncryptedDataService(app.injector.instanceOf[ComposeCryptoService], mockSubscriptionDetailsService)
  private val legacySensitiveFormat: Format[SensitiveString] = {
    implicit val legacyCrypto: Encrypter with Decrypter = app.injector.instanceOf[ApplicationCrypto].JsonCrypto
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
  }
  private val businesses = SoleTraderBusinesses(Seq(SoleTraderBusiness(
    id = "business-1",
    confirmed = true,
    startDateBeforeLimit = Some(false),
    startDate = Some(DateModel("1", "1", "2020")),
    name = Some("Business one"),
    displayName = Some("Display one"),
    trade = Some("Business trade"),
    address = Some(Address(Seq("Address line one", "Address line two"), Some("ZZ1 1ZZ"),
      Some(Country("GB", "United Kingdom")), Some("123456789")))
  )))
  private val subscriptionData = SubscriptionData(Map(
    SoleTraderBusinessesKey -> Json.toJson(businesses)(SoleTraderBusinesses.encryptedFormat(legacySensitiveFormat))
  ))
  private val selfEmployments = businesses.businesses.map(_.toSelfEmploymentData)

  private def mockSaveFlag(result: PostSubscriptionDetailsResponse): Unit = {
    when(mockSubscriptionDetailsService.saveNewEncryptionFlag(reference)(hc)).thenReturn(Future.successful(result))
  }

  "migrateEncryptedData" must {
    "skip all writes when the new encryption flag is already present" in {
      mockGetAllSubscriptionData(Some(subscriptionData.copy(data = subscriptionData.data + ("newEncryption" -> Json.toJson(true)))))

      await(service.migrateEncryptedData(reference)) mustBe MigrationNotRequired

      verify(mockSubscriptionDetailsService).getAllSubscriptionData(reference)(hc)
    }

    "migrate businesses and save the new encryption flag" in {
      mockGetAllSubscriptionData(Some(subscriptionData))
      mockSaveBusinesses(selfEmployments)(Right(PostSubscriptionDetailsSuccessResponse))
      mockSaveFlag(Right(PostSubscriptionDetailsSuccessResponse))

      await(service.migrateEncryptedData(reference)) mustBe SuccessfullyMigratedEncryptedData

      verify(mockSubscriptionDetailsService).getAllSubscriptionData(reference)(hc)
      verify(mockSubscriptionDetailsService).saveBusinessesFromSelfEmploymentData(reference, selfEmployments)(hc)
      verify(mockSubscriptionDetailsService).saveNewEncryptionFlag(reference)(hc)
    }

    "migrate businesses already encrypted with composed crypto when the flag is absent" in {
      val composedSubscriptionData = SubscriptionData(Map(
        SoleTraderBusinessesKey -> Json.toJson(businesses)(SoleTraderBusinesses.encryptedFormat(service.sensitiveFormat))
      ))
      mockGetAllSubscriptionData(Some(composedSubscriptionData))
      mockSaveBusinesses(selfEmployments)(Right(PostSubscriptionDetailsSuccessResponse))
      mockSaveFlag(Right(PostSubscriptionDetailsSuccessResponse))

      await(service.migrateEncryptedData(reference)) mustBe SuccessfullyMigratedEncryptedData

      verify(mockSubscriptionDetailsService).saveBusinessesFromSelfEmploymentData(reference, selfEmployments)(hc)
      verify(mockSubscriptionDetailsService).saveNewEncryptionFlag(reference)(hc)
    }

    "replace stored business data that fails validation with an empty list" in {
      val businessWithoutConfirmation = Json.toJsObject(businesses.businesses.head)(
        SoleTraderBusiness.encryptedFormat(legacySensitiveFormat)
      ) - "confirmed"
      mockGetAllSubscriptionData(Some(SubscriptionData(Map(
        SoleTraderBusinessesKey -> Json.obj("businesses" -> Json.arr(businessWithoutConfirmation))
      ))))
      mockSaveBusinesses(Seq.empty)(Right(PostSubscriptionDetailsSuccessResponse))
      mockSaveFlag(Right(PostSubscriptionDetailsSuccessResponse))

      await(service.migrateEncryptedData(reference)) mustBe SuccessfullyMigratedEncryptedData

      verify(mockSubscriptionDetailsService).saveBusinessesFromSelfEmploymentData(reference, Seq.empty)(hc)
      verify(mockSubscriptionDetailsService).saveNewEncryptionFlag(reference)(hc)
    }

    "fail without writing businesses or the flag when decryption fails" in {
      mockGetAllSubscriptionData(Some(SubscriptionData(Map(
        SoleTraderBusinessesKey -> Json.obj("businesses" -> Json.arr(
          Json.obj("id" -> "business-1", "confirmed" -> true, "name" -> "not-valid-ciphertext!")
        ))
      ))))

      intercept[SecurityException](await(service.migrateEncryptedData(reference))).getMessage mustBe
        "Unable to decrypt value"

      verify(mockSubscriptionDetailsService).getAllSubscriptionData(reference)(hc)
      verifyNoMoreInteractions(mockSubscriptionDetailsService)
    }

    "save only the flag when subscription data has no businesses" in {
      mockGetAllSubscriptionData(Some(SubscriptionData(Map.empty)))
      mockSaveFlag(Right(PostSubscriptionDetailsSuccessResponse))

      await(service.migrateEncryptedData(reference)) mustBe SuccessfullyMigratedEncryptedData

      verify(mockSubscriptionDetailsService).getAllSubscriptionData(reference)(hc)
      verify(mockSubscriptionDetailsService).saveNewEncryptionFlag(reference)(hc)
      verifyNoMoreInteractions(mockSubscriptionDetailsService)
    }

    "save only the new encryption flag when subscription data is absent" in {
      mockGetAllSubscriptionData(None)
      mockSaveFlag(Right(PostSubscriptionDetailsSuccessResponse))

      await(service.migrateEncryptedData(reference)) mustBe MigrationNotRequired

      verify(mockSubscriptionDetailsService).getAllSubscriptionData(reference)(hc)
      verify(mockSubscriptionDetailsService).saveNewEncryptionFlag(reference)(hc)
    }

    "fail without saving the flag when saving businesses fails" in {
      mockGetAllSubscriptionData(Some(subscriptionData))
      mockSaveBusinesses(selfEmployments)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

      intercept[InternalServerException](await(service.migrateEncryptedData(reference))).getMessage mustBe
        "Failed to save newly encrypted self employment data"

      verify(mockSubscriptionDetailsService).getAllSubscriptionData(reference)(hc)
      verify(mockSubscriptionDetailsService).saveBusinessesFromSelfEmploymentData(reference, selfEmployments)(hc)
    }

    "fail when saving the flag after migrating businesses fails" in {
      mockGetAllSubscriptionData(Some(subscriptionData))
      mockSaveBusinesses(selfEmployments)(Right(PostSubscriptionDetailsSuccessResponse))
      mockSaveFlag(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

      intercept[InternalServerException](await(service.migrateEncryptedData(reference))).getMessage mustBe
        "Failed to save new encryption flag"
    }

  }
}
