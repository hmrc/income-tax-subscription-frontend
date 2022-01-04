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

package services

import connectors.PaperlessPreferenceTokenConnector
import models.PaperlessPreferenceTokenResult.PaperlessPreferenceTokenSuccess
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaperlessPreferenceTokenService @Inject()(subscriptionDetailsService: SubscriptionDetailsService,
                                                paperlessPreferenceTokenConnector: PaperlessPreferenceTokenConnector)
                                               (implicit ec: ExecutionContext) {
  def storeNino(nino: String, reference: String)(implicit hc: HeaderCarrier): Future[String] = {
    subscriptionDetailsService.fetchPaperlessPreferenceToken(reference) flatMap {
      case Some(token) => Future.successful(token)
      case None =>
        val token = UUID.randomUUID().toString
        paperlessPreferenceTokenConnector.storeNino(token, nino) flatMap {
          case Right(PaperlessPreferenceTokenSuccess) =>
            subscriptionDetailsService.savePaperlessPreferenceToken(reference, token) map (_ => token)
          case _ =>
            Future.failed(new InternalServerException("Failed to store paperless preferences token"))
        }
    }
  }
}
