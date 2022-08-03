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

package services

import connectors.individual.subscription.httpparsers.CreateIncomeSourcesResponseHttpParser.PostCreateIncomeSourceResponse
import connectors.individual.subscription.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import connectors.individual.subscription.httpparsers.SignUpIncomeSourcesResponseHttpParser.PostSignUpIncomeSourcesResponse
import connectors.individual.subscription.{MultipleIncomeSourcesSubscriptionConnector, SubscriptionConnector}
import models.common.subscription.CreateIncomeSourcesModel
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SubscriptionService @Inject()(multipleIncomeSourcesSubscriptionConnector: MultipleIncomeSourcesSubscriptionConnector,
                                    subscriptionConnector: SubscriptionConnector
                                   ) extends Logging {

  def getSubscription(nino: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] = {
    logger.debug(s"Getting subscription for nino=$nino")
    subscriptionConnector.getSubscription(nino)
  }

  def signUpIncomeSources(nino: String)(implicit hc: HeaderCarrier): Future[PostSignUpIncomeSourcesResponse] = {
    logger.debug(s"SignUp IncomeSources request for nino:$nino")
    multipleIncomeSourcesSubscriptionConnector.signUp(nino)
  }

  def createIncomeSourcesFromTaskList(mtdbsa: String,
                                      createIncomeSourcesModel: CreateIncomeSourcesModel
                                     )
                                     (implicit hc: HeaderCarrier): Future[PostCreateIncomeSourceResponse] = {
    logger.debug(s"Create IncomeSources request for MTDSA Id:$mtdbsa from task list")

    multipleIncomeSourcesSubscriptionConnector.createIncomeSourcesFromTaskList(mtdbsa, createIncomeSourcesModel)
  }

}
