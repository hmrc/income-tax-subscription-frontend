/*
 * Copyright 2017 HM Revenue & Customs
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

package digitalcontact.services

import java.util.UUID
import javax.inject.Inject

import digitalcontact.connectors.PaperlessPreferenceTokenConnector
import digitalcontact.models.PaperlessPreferenceTokenResult.PaperlessPreferenceTokenSuccess
import services.KeystoreService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class PaperlessPreferenceTokenService @Inject()(keystoreService: KeystoreService,
                                                paperlessPreferenceTokenConnector: PaperlessPreferenceTokenConnector) {
  def storeNino(nino: String)(implicit hc: HeaderCarrier): Future[String] =
    keystoreService.fetchPaperlessPreferenceToken() flatMap {
      case Some(token) => Future.successful(token)
      case None =>
        val token = s"${UUID.randomUUID()}"
        paperlessPreferenceTokenConnector.storeNino(token, nino) flatMap {
          case Right(PaperlessPreferenceTokenSuccess) =>
            keystoreService.savePaperlessPreferenceToken(token) map (_ => token)
          case _ =>
            Future.failed(new InternalServerException("Failed to store paperless preferences token"))
        }
    }
}
