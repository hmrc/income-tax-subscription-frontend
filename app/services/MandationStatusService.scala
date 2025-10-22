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

package services

import connectors.MandationStatusConnector
import models.SessionData
import models.status.MandationStatusModel
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MandationStatusService @Inject()(mandationStatusConnector: MandationStatusConnector,
                                       ninoService: NinoService,
                                       utrService: UTRService,
                                       sessionDataService: SessionDataService)
                                      (implicit ec: ExecutionContext) {

  def getMandationStatus(sessionData: SessionData = SessionData())(implicit hc: HeaderCarrier): Future[MandationStatusModel] = {
    sessionData.fetchMandationStatus match {
      case Some(mandationStatus) => Future.successful(mandationStatus)
      case None =>
        ninoService.getNino(sessionData) flatMap { nino =>
          utrService.getUTR(sessionData) flatMap { utr =>
            mandationStatusConnector.getMandationStatus(nino = nino, utr = utr) flatMap {
              case Right(mandationStatus) =>
                sessionDataService.saveMandationStatus(mandationStatus) map {
                  case Right(_) => mandationStatus
                  case Left(error) => throw new SaveToSessionException(error.toString)
                }
              case Left(error) => throw new FetchFromAPIException(error.toString)
            }
          }
        }
    }
  }

  private class FetchFromAPIException(error: String) extends InternalServerException(
    s"[MandationStatusService][getMandationStatus] - Failure when fetching mandation status from API: $error"
  )

  private class SaveToSessionException(error: String) extends InternalServerException(
    s"[MandationStatusService][getMandationStatus] - Failure when saving mandation status to session: $error"
  )

}
