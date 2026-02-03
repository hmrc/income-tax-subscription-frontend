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

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignedUpDateService @Inject()(sessionDataService: SessionDataService)
                                   (implicit ec: ExecutionContext) {

  def getSignedUpDate(sessionData: SessionData)(implicit hc: HeaderCarrier): Future[LocalDate] = {
    sessionData.fetchSignedUpDate match {
      case Some(date) => Future.successful(date)
      case None =>
        val date = LocalDate.now()
        sessionDataService.saveSignedUpDate(date) map {
          case Right(_) =>
            date
          case Left(error) => throw new SaveToSessionException(error.toString)
        }
    }
    
  }

  private class SaveToSessionException(error: String) extends InternalServerException(
    s"[SignedUpDateService][getSignedUpDate] - Failure when saving signed up date to session: $error"
  )

}
