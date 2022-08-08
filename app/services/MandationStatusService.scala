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

import connectors.MandationStatusConnector
import connectors.httpparser.PostMandationStatusParser.PostMandationStatusResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MandationStatusService @Inject()(val mandationStatusConnector: MandationStatusConnector, val subscriptionDetailsService: SubscriptionDetailsService) {
  def retrieveMandationStatus(reference: String, nino: String, utr: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    for {
      mandationStatus <- mandationStatusConnector.getMandationStatus(nino, utr)
      _ = saveMandationStatus(reference, mandationStatus)
    } yield ()
  }

  private def saveMandationStatus(reference:String, mandationStatusResponse: PostMandationStatusResponse)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    mandationStatusResponse match {
      case Right(mandationStatus) => subscriptionDetailsService.saveMandationStatus(reference, mandationStatus).map(_ => ())
      case Left(_) => Future.successful(())
    }
  }
}
