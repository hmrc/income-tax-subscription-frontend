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

package services.agent

import cats.data.EitherT
import cats.implicits._
import config.featureswitch.FeatureSwitch.SPSEnabled
import config.featureswitch.FeatureSwitching
import connectors.agent.AgentSPSConnector

import javax.inject.{Inject, Singleton}
import models.common.subscription.{CreateIncomeSourcesSuccess, SubscriptionSuccess}
import models.{AgentSummary, ConnectorError, SummaryModel}
import services.SubscriptionService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionOrchestrationService @Inject()(subscriptionService: SubscriptionService,
                                                 autoEnrolmentService: AutoEnrolmentService,
                                                 agentSPSConnector: AgentSPSConnector)
                                                (implicit ec: ExecutionContext) extends FeatureSwitching {

  def createSubscription(arn: String, nino: String, utr: String, summaryModel: SummaryModel, isReleaseFourEnabled: Boolean = false)
                        (implicit hc: HeaderCarrier): Future[Either[ConnectorError, SubscriptionSuccess]] = {

    if (isReleaseFourEnabled) {
      signUpAndCreateIncomeSources(nino, summaryModel.asInstanceOf[AgentSummary]) flatMap {
        case right@Right(subscriptionSuccess) => {
          autoEnrolmentService.autoClaimEnrolment(utr, nino, subscriptionSuccess.mtditId) map {
            case Right(_) =>
              if(isEnabled(SPSEnabled)){
              confirmAgentEnrollmentToSps(arn, nino, utr, right.value.mtditId)
              }
              right
            case Left(_) =>
              right
          }
        }
        case left => Future.successful(left)
      }
    }
    else {
      subscriptionService.submitSubscription(nino, summaryModel, Some(arn)) flatMap {
        case right@Right(subscriptionSuccess) => {
          autoEnrolmentService.autoClaimEnrolment(utr, nino, subscriptionSuccess.mtditId) map { _ =>
            right
          }
        }
        case left => Future.successful(left)
      }
    }
  }

  private[services] def signUpAndCreateIncomeSources(nino: String, agentSummary: AgentSummary)
                                                    (implicit hc: HeaderCarrier): Future[Either[ConnectorError, SubscriptionSuccess]] = {

    val res = for {
      signUpResponse <- EitherT(subscriptionService.signUpIncomeSources(nino))
      mtdbsa = signUpResponse.mtdbsa
      _ <- EitherT[Future, ConnectorError, CreateIncomeSourcesSuccess](subscriptionService.createIncomeSources(nino, mtdbsa, agentSummary))
    } yield SubscriptionSuccess(mtdbsa)

    res.value
  }

  private[services] def confirmAgentEnrollmentToSps(arn: String, nino: String, sautr: String, mtditId: String)
                                                   (implicit hc: HeaderCarrier): Future[HttpResponse] = {
    agentSPSConnector.postSpsConfirm(arn, nino, sautr, mtditId)
  }

}