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

package services.individual

import cats.data.EitherT
import cats.implicits._
import models.ConnectorError
import models.common.subscription.{CreateIncomeSourcesModel, SignUpSuccessResponse, SubscriptionSuccess}
import services.{SPSService, SubscriptionService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionOrchestrationService @Inject()(subscriptionService: SubscriptionService,
                                                 knownFactsService: KnownFactsService,
                                                 enrolmentService: EnrolmentService,
                                                 spsService: SPSService
                                                )(implicit ec: ExecutionContext) {

  def enrolAndRefresh(mtditId: String, nino: String)(implicit hc: HeaderCarrier): Future[Either[ConnectorError, String]] = {
    val res = for {
      _ <- EitherT(enrolmentService.enrol(mtditId, nino))
    } yield mtditId

    res.value
  }

  def signUpAndCreateIncomeSourcesFromTaskList(createIncomeSourceModel: CreateIncomeSourcesModel,
                                               maybeSpsEntityId: Option[String] = None)
                                              (implicit hc: HeaderCarrier): Future[Either[ConnectorError, Option[SubscriptionSuccess]]] = {
    val taxYear: String = {
      createIncomeSourceModel.ukProperty.map(_.accountingPeriod.toLongTaxYear) orElse
        createIncomeSourceModel.overseasProperty.map(_.accountingPeriod.toLongTaxYear) orElse
        createIncomeSourceModel.soleTraderBusinesses.map(_.accountingPeriod.toLongTaxYear)
    }.getOrElse(throw new InternalServerException(
      "[SubscriptionOrchestrationService][signUpAndCreateIncomeSourcesFromTaskList] - Unable to retrieve any tax year from income sources"
    ))

    val result: EitherT[Future, ConnectorError, Option[SubscriptionSuccess]] = {
      EitherT(subscriptionService.signUpIncomeSources(createIncomeSourceModel.nino, taxYear)).flatMap {
        case SignUpSuccessResponse.SignUpSuccessful(mtdbsa) =>
          for {
            _ <- EitherT(subscriptionService.createIncomeSourcesFromTaskList(mtdbsa, createIncomeSourceModel))
            _ <- EitherT(knownFactsService.addKnownFacts(mtdbsa, createIncomeSourceModel.nino))
            _ <- EitherT(enrolAndRefresh(mtdbsa, createIncomeSourceModel.nino))
            _ = spsService.confirmPreferences(mtdbsa, maybeSpsEntityId)
          } yield {
            Some(SubscriptionSuccess(mtdbsa))
          }
        case SignUpSuccessResponse.AlreadySignedUp => EitherT[Future, ConnectorError, Option[SubscriptionSuccess]](Future.successful(Right(None)))
      }
    }

    result.value

  }
}
