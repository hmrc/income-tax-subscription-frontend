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

package services.individual

import cats.data.EitherT
import cats.implicits._
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionSuccess}
import models.{ConnectorError, IndividualSummary, SummaryModel}
import services.{SPSService, SubscriptionService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionOrchestrationService @Inject()(subscriptionService: SubscriptionService,
                                                 knownFactsService: KnownFactsService,
                                                 enrolmentService: EnrolmentService,
                                                 spsService: SPSService
                                                )(implicit ec: ExecutionContext) {

  def createSubscription(nino: String, summaryModel: SummaryModel, isReleaseFourEnabled: Boolean = false, maybeSpsEntityId: Option[String] = None)
                        (implicit hc: HeaderCarrier): Future[Either[ConnectorError, SubscriptionSuccess]] = {
    if (isReleaseFourEnabled) {
      signUpAndCreateIncomeSources(nino, summaryModel.asInstanceOf[IndividualSummary], maybeSpsEntityId)
    } else {
      createSubscriptionCore(nino, summaryModel)
    }
  }

  private[services] def createSubscriptionCore(nino: String, summaryModel: SummaryModel)
                                              (implicit hc: HeaderCarrier): Future[Either[ConnectorError, SubscriptionSuccess]] = {
    val res = for {
      subscriptionResponse <- EitherT(subscriptionService.submitSubscription(nino, summaryModel, arn = None))
      mtditId = subscriptionResponse.mtditId
      _ <- EitherT(knownFactsService.addKnownFacts(mtditId, nino))
      _ <- EitherT(enrolAndRefresh(mtditId, nino))
    } yield subscriptionResponse

    res.value
  }

  def enrolAndRefresh(mtditId: String, nino: String)(implicit hc: HeaderCarrier): Future[Either[ConnectorError, String]] = {
    val res = for {
      _ <- EitherT(enrolmentService.enrol(mtditId, nino))
    } yield mtditId

    res.value
  }

  private[services] def signUpAndCreateIncomeSources(nino: String, individualSummary: IndividualSummary, maybeSpsEntityId: Option[String])
                                                    (implicit hc: HeaderCarrier): Future[Either[ConnectorError, SubscriptionSuccess]] = {
    val res = for {
      signUpResponse <- EitherT(subscriptionService.signUpIncomeSources(nino))
      mtdbsa = signUpResponse.mtdbsa
      _ <- EitherT(subscriptionService.createIncomeSources(nino, mtdbsa, individualSummary))
      _ <- EitherT(knownFactsService.addKnownFacts(mtdbsa, nino))
      _ <- EitherT(enrolAndRefresh(mtdbsa, nino))
      _ = spsService.confirmPreferences(mtdbsa, maybeSpsEntityId)
    } yield {
      SubscriptionSuccess(mtdbsa)
    }

    res.value
  }

  def signUpAndCreateIncomeSourcesFromTaskList(nino: String,
                                               createIncomeSourceModel: CreateIncomeSourcesModel,
                                               maybeSpsEntityId: Option[String] = None)
                                              (implicit hc: HeaderCarrier): Future[Either[ConnectorError, SubscriptionSuccess]] = {
    val res = for {
      signUpResponse <- EitherT(subscriptionService.signUpIncomeSources(nino))
      mtdbsa = signUpResponse.mtdbsa
      _ <- EitherT(subscriptionService.createIncomeSourcesFromTaskList(mtdbsa, createIncomeSourceModel))
      _ <- EitherT(knownFactsService.addKnownFacts(mtdbsa, nino))
      _ <- EitherT(enrolAndRefresh(mtdbsa, nino))
      _ = spsService.confirmPreferences(mtdbsa, maybeSpsEntityId)
    } yield {
      SubscriptionSuccess(mtdbsa)
    }

    res.value
  }

}
