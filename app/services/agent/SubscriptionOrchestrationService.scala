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

package services.agent

import cats.data.EitherT
import cats.implicits._
import connectors.agent.AgentSPSConnector
import models.ConnectorError
import models.common.subscription.{CreateIncomeSourcesModel, CreateIncomeSourcesSuccess, SubscriptionSuccess}
import services.SubscriptionService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionOrchestrationService @Inject()(subscriptionService: SubscriptionService,
                                                 autoEnrolmentService: AutoEnrolmentService,
                                                 agentSPSConnector: AgentSPSConnector)
                                                (implicit ec: ExecutionContext) {

  def createSubscriptionFromTaskList(arn: String,
                                     nino: String,
                                     utr: String,
                                     createIncomeSourcesModel: CreateIncomeSourcesModel)
                                    (implicit hc: HeaderCarrier): Future[Either[ConnectorError, SubscriptionSuccess]] = {

    signUpAndCreateIncomeSourcesFromTaskList(nino, createIncomeSourcesModel) flatMap {
      case right@Right(subscriptionSuccess) => {
        autoEnrolmentService.autoClaimEnrolment(utr, nino, subscriptionSuccess.mtditId) flatMap {
          case Right(_) =>
            confirmAgentEnrollmentToSps(arn, nino, utr, right.value.mtditId)
              .map(_ => right)
          case Left(_) =>
            Future.successful(right)
        }
      }
      case left => Future.successful(left)
    }

  }

  private[services] def signUpAndCreateIncomeSourcesFromTaskList(nino: String, createIncomeSourcesModel: CreateIncomeSourcesModel)
                                                                (implicit hc: HeaderCarrier): Future[Either[ConnectorError, SubscriptionSuccess]] = {

    val taxYear: String = {
      createIncomeSourcesModel.ukProperty.map(_.accountingPeriod.toLongTaxYear) orElse
        createIncomeSourcesModel.overseasProperty.map(_.accountingPeriod.toLongTaxYear) orElse
        createIncomeSourcesModel.soleTraderBusinesses.map(_.accountingPeriod.toLongTaxYear)
    }.getOrElse(throw new InternalServerException(
      "[SubscriptionOrchestrationService][signUpAndCreateIncomeSourcesFromTaskList] - Unable to retrieve any tax year from income sources"
    ))

    val res = for {
      signUpResponse <- EitherT(subscriptionService.signUpIncomeSources(nino, taxYear))
      mtdbsa = signUpResponse.mtdbsa
      _ <- EitherT[Future, ConnectorError, CreateIncomeSourcesSuccess](subscriptionService.createIncomeSourcesFromTaskList(mtdbsa, createIncomeSourcesModel))
    } yield SubscriptionSuccess(mtdbsa)

    res.value
  }

  private[services] def confirmAgentEnrollmentToSps(arn: String, nino: String, sautr: String, mtditId: String)
                                                   (implicit hc: HeaderCarrier): Future[Unit] = {
    agentSPSConnector.postSpsConfirm(arn, nino, sautr, mtditId)
  }

}
