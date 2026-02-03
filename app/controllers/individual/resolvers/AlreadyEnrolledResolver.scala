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

package controllers.individual.resolvers

import config.AppConfig
import config.featureswitch.FeatureSwitch.OptBackIn
import config.featureswitch.FeatureSwitching
import connectors.SubscriptionConnector
import models.common.subscription.SubscriptionSuccess
import models.status.GetITSAStatus
import models.{HmrcLedUnconfirmed, SessionData}
import play.api.mvc.Call
import services.GetITSAStatusService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AlreadyEnrolledResolver @Inject()(subscriptionConnector: SubscriptionConnector,
                                        getITSAStatusService: GetITSAStatusService,
                                        val appConfig: AppConfig)
                                       (implicit executionContext: ExecutionContext) extends FeatureSwitching {

  def resolve(nino: String, sessionData: SessionData)(implicit hc: HeaderCarrier): Future[Call] = {
    subscriptionConnector.getSubscription(nino) flatMap {
      case Right(Some(SubscriptionSuccess(_, Some(HmrcLedUnconfirmed)))) =>
        Future.successful(controllers.individual.handoffs.routes.CheckIncomeSourcesController.show)
      case Right(_) =>
        if (isEnabled(OptBackIn)) {
          getITSAStatusService.getITSAStatus(sessionData) map { getITSAStatus =>
            getITSAStatus.status match {
              case GetITSAStatus.Annual => controllers.individual.handoffs.routes.OptedOutController.show
              case _ => controllers.individual.matching.routes.AlreadyEnrolledController.show
            }
          }
        } else {
          Future.successful(controllers.individual.matching.routes.AlreadyEnrolledController.show)
        }

      case Left(_) => throw new InternalServerException("[AlreadyEnrolledResolver][resolve] - Unable to fetch the business details.")
    }
  }

}
