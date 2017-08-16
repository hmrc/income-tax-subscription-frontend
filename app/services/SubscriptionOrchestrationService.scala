/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import cats.data.EitherT
import cats.implicits._
import connectors.models.ConnectorError
import connectors.models.authenticator.RefreshProfileResult
import connectors.models.subscription.SubscriptionResponse.SubscriptionSuccess
import models.SummaryModel
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionOrchestrationService @Inject() (subscriptionService: SubscriptionService,
                                                  knownFactsService: KnownFactsService,
                                                  enrolmentService: EnrolmentService,
                                                  refreshProfileService: RefreshProfileService
                                                 ) (implicit ec: ExecutionContext){

  def createSubscription(nino: String, summaryModel: SummaryModel)(implicit hc: HeaderCarrier): Future[Either[ConnectorError, SubscriptionSuccess]] = {
    val res = for {
      subscriptionResponse <- EitherT(subscriptionService.submitSubscription(nino, summaryModel))
      mtditId = subscriptionResponse.mtditId
      _ <- EitherT(knownFactsService.addKnownFacts(mtditId, nino))
      _ <- EitherT(enrolAndRefresh(mtditId, nino))
    } yield subscriptionResponse

    res.value
  }

  def enrolAndRefresh(mtditId: String, nino: String)(implicit hc: HeaderCarrier): Future[Either[ConnectorError, String]] = {
    val res = for {
      _ <- EitherT(enrolmentService.enrol(mtditId, nino))
      _ <- EitherT[Future, ConnectorError, RefreshProfileResult](refreshProfileService.refreshProfile())
    } yield mtditId

    res.value
  }

}
