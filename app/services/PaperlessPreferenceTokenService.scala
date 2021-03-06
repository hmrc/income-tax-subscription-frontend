/*
 * Copyright 2021 HM Revenue & Customs
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

import java.util.UUID

import connectors.PaperlessPreferenceTokenConnector
import javax.inject.Inject
import models.PaperlessPreferenceTokenResult.PaperlessPreferenceTokenSuccess
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

class PaperlessPreferenceTokenService @Inject()(subscriptionDetailsService: SubscriptionDetailsService,
                                                paperlessPreferenceTokenConnector: PaperlessPreferenceTokenConnector)
                                               (implicit ec: ExecutionContext) {
  def storeNino(nino: String)(implicit hc: HeaderCarrier): Future[String] = {


    subscriptionDetailsService.fetchPaperlessPreferenceToken() flatMap {
      case Some(token) => Future.successful(token)
      case None =>
        val token = s"${UUID.randomUUID()}"
        paperlessPreferenceTokenConnector.storeNino(token, nino) flatMap {
          case Right(PaperlessPreferenceTokenSuccess) =>
            subscriptionDetailsService.savePaperlessPreferenceToken(token) map (_ => token)
          case _ =>
            Future.failed(new InternalServerException("Failed to store paperless preferences token"))
        }
    }
  }
}
