/*
 * Copyright 2023 HM Revenue & Customs
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

import config.AppConfig
import config.featureswitch.FeatureSwitch.SignalControlGatewayEligibility
import config.featureswitch.FeatureSwitching
import connectors.individual.eligibility.GetEligibilityStatusConnector
import models.{EligibilityStatus, SessionData}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetEligibilityStatusService @Inject()(getEligibilityStatusConnector: GetEligibilityStatusConnector,
                                            ninoService: NinoService,
                                            utrService: UTRService,
                                            sessionDataService: SessionDataService)
                                           (val appConfig: AppConfig)
                                           (implicit ec: ExecutionContext) extends FeatureSwitching {

  def getEligibilityStatus(sessionData: SessionData = SessionData())(implicit hc: HeaderCarrier): Future[EligibilityStatus] = {
    sessionData.fetchEligibilityStatus match {
      case Some(value) => Future.successful(value)
      case None =>
        if (isEnabled(SignalControlGatewayEligibility)) {
          getAndSaveSignalControlGatewayEligibilityResults(sessionData)
        } else {
          getAndSaveControlListEligibilityResults(sessionData)
        }
    }
  }

  private def getAndSaveSignalControlGatewayEligibilityResults(sessionData: SessionData)(implicit hc: HeaderCarrier): Future[EligibilityStatus] = {
    ninoService.getNino(sessionData) flatMap { nino =>
      utrService.getUTR(sessionData) flatMap { utr =>
        getEligibilityStatusConnector.getEligibilityStatus(nino, utr) flatMap {
          case Right(value) =>
            sessionDataService.saveEligibilityStatus(value) map {
              case Right(_) => value
              case Left(error) => throw new SaveToSessionException(error.toString)
            }
          case Left(error) => throw new FetchFromAPIException(s"status = ${error.httpResponse.status}, body = ${error.httpResponse.body}")
        }
      }
    }
  }

  private def getAndSaveControlListEligibilityResults(sessionData: SessionData)(implicit hc: HeaderCarrier): Future[EligibilityStatus] = {
    utrService.getUTR(sessionData) flatMap { utr =>
      getEligibilityStatusConnector.getEligibilityStatus(utr) flatMap {
        case Right(value) =>
          sessionDataService.saveEligibilityStatus(value) map {
            case Right(_) => value
            case Left(error) => throw new SaveToSessionException(error.toString)
          }
        case Left(error) => throw new FetchFromAPIException(s"status = ${error.httpResponse.status}, body = ${error.httpResponse.body}")
      }
    }
  }

  private class FetchFromAPIException(error: String) extends InternalServerException(
    s"[GetEligibilityStatusService][getEligibilityStatus] - failure fetching eligibility status from API: $error"
  )

  private class SaveToSessionException(error: String) extends InternalServerException(
    s"[GetEligibilityStatusService][getEligibilityStatus] - failure saving eligibility status to session: $error"
  )
}
