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

package agent.services

import javax.inject.{Inject, Singleton}

import cats.data.EitherT
import cats.implicits._
import agent.connectors.models.ConnectorError
import agent.connectors.models.gg.KnownFactsSuccess
import agent.connectors.models.subscription.SubscriptionSuccess
import agent.models.SummaryModel

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class SubscriptionOrchestrationService @Inject()(subscriptionService: SubscriptionService,
                                                 knownFactsService: KnownFactsService
                                                )(implicit ec: ExecutionContext) {

  def createSubscription(arn: String,
                         nino: String,
                         summaryModel: SummaryModel)(implicit hc: HeaderCarrier): Future[Either[ConnectorError, SubscriptionSuccess]] = {
    val res = for {
      subscriptionResponse <- EitherT(subscriptionService.submitSubscription(arn, nino, summaryModel))
      mtditId = subscriptionResponse.mtditId
      _ <- EitherT[Future, ConnectorError, KnownFactsSuccess.type](knownFactsService.addKnownFacts(mtditId, nino))
    } yield subscriptionResponse

    res.value
  }

}
