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

package connectors.individual.subscription

import config.AppConfig
import connectors.individual.subscription.httpparsers.CreateIncomeSourcesResponseHttpParser._
import connectors.individual.subscription.httpparsers.SignUpIncomeSourcesResponseHttpParser._
import models.common.business.BusinessSubscriptionDetailsModel
import models.common.subscription.CreateIncomeSourcesModel
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MultipleIncomeSourcesSubscriptionConnector @Inject()(val appConfig: AppConfig,
                                                           val http: HttpClient)
                                                          (implicit ec: ExecutionContext) {

  def signUpUrl(nino: String): String = appConfig.signUpIncomeSourcesUrl + MultipleIncomeSourcesSubscriptionConnector.signUpUri(nino)

  def createIncomeSourcesUrl(mtdbsa: String): String = appConfig.createIncomeSourcesUrl +
    MultipleIncomeSourcesSubscriptionConnector.createIncomeSourcesUri(mtdbsa)

  def signUp(nino: String)(implicit hc: HeaderCarrier): Future[PostSignUpIncomeSourcesResponse] =
    http.POSTEmpty[PostSignUpIncomeSourcesResponse](signUpUrl(nino))

  def createIncomeSources(mtdbsa: String, request: BusinessSubscriptionDetailsModel)
                         (implicit hc: HeaderCarrier): Future[PostCreateIncomeSourceResponse] =
    http.POST[BusinessSubscriptionDetailsModel, PostCreateIncomeSourceResponse](createIncomeSourcesUrl(mtdbsa), request)

  def createIncomeSourcesFromTaskList(mtdbsa: String, request: CreateIncomeSourcesModel)
                                     (implicit hc: HeaderCarrier): Future[PostCreateIncomeSourceResponse] =
    http.POST[CreateIncomeSourcesModel, PostCreateIncomeSourceResponse](createIncomeSourcesUrl(mtdbsa), request)

}

object MultipleIncomeSourcesSubscriptionConnector {

  def signUpUri(nino: String): String = s"/$nino"

  def createIncomeSourcesUri(mtdbsa: String): String = s"/$mtdbsa"

}
