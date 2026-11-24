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

import crypto.ComposeCryptoService
import models.SubscriptionData
import models.common.business.SelfEmploymentData
import play.api.libs.json.Format
import services.MigrateEncryptedDataService.{MigrateEncryptedDataResponse, MigrationNotRequired, SuccessfullyMigratedEncryptedData}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.SubscriptionDataKeys.SoleTraderBusinessesKey

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MigrateEncryptedDataService @Inject()(crypto: ComposeCryptoService,
                                            subscriptionDetailsService: SubscriptionDetailsService)
                                           (implicit executionContext: ExecutionContext) {

  implicit val composedCrypto: Encrypter with Decrypter = crypto.composedJsonCrypto

  implicit val sensitiveFormat: Format[SensitiveString] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

  def migrateEncryptedData(reference: String)
                          (implicit hc: HeaderCarrier): Future[MigrateEncryptedDataResponse] = {
    subscriptionDetailsService.getAllSubscriptionData(reference).flatMap(migrateBusinesses(reference, _))
  }

  private def migrateBusinesses(reference: String, subscriptionData: Option[SubscriptionData])
                               (implicit hc: HeaderCarrier): Future[MigrateEncryptedDataResponse] = {
    subscriptionData match {
      case Some(subscriptionData) if subscriptionData.hasNewEncryptionFlag =>
        Future.successful(MigrationNotRequired)
      case Some(subscriptionData) =>
        subscriptionData.data.get(SoleTraderBusinessesKey)
          .fold(Future.unit)(_ => saveBusinesses(reference, subscriptionData.fetchAllSelfEmployments))
          .flatMap(_ => saveNewEncryptionFlag(reference, SuccessfullyMigratedEncryptedData))
      case None =>
        saveNewEncryptionFlag(reference, MigrationNotRequired)
    }
  }

  private def saveBusinesses(reference: String, selfEmploymentData: Seq[SelfEmploymentData])
                            (implicit hc: HeaderCarrier): Future[Unit] = {
    subscriptionDetailsService.saveBusinessesFromSelfEmploymentData(reference, selfEmploymentData).map {
      case Right(_) => ()
      case Left(_) => throw new InternalServerException("Failed to save newly encrypted self employment data")
    }
  }

  private def saveNewEncryptionFlag(reference: String, successResult: MigrateEncryptedDataResponse)
                                   (implicit hc: HeaderCarrier): Future[MigrateEncryptedDataResponse] = {
    subscriptionDetailsService.saveNewEncryptionFlag(reference) map {
      case Right(_) => successResult
      case Left(_) => throw new InternalServerException("Failed to save new encryption flag")
    }
  }

}

object MigrateEncryptedDataService {

  sealed trait MigrateEncryptedDataResponse

  case object SuccessfullyMigratedEncryptedData extends MigrateEncryptedDataResponse

  case object MigrationNotRequired extends MigrateEncryptedDataResponse

}
