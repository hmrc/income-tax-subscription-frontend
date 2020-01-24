/*
 * Copyright 2020 HM Revenue & Customs
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

import core.config.AppConfig
import connectors.individual.subscription.httpparsers.AllocateEnrolmentResponseHttpParser._
import connectors.individual.subscription.httpparsers.UpsertEnrolmentResponseHttpParser._
import incometax.subscription.models.{EmacEnrolmentRequest, EnrolmentKey, EnrolmentVerifiers}
import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentsConnector @Inject()(appConfig: AppConfig,
                                        httpClient: HttpClient)(implicit ec: ExecutionContext) {
  def upsertEnrolment(enrolmentKey: EnrolmentKey,
                      verifiers: EnrolmentVerifiers
                     )(implicit hc: HeaderCarrier): Future[UpsertEnrolmentResponse] = {
    val url = appConfig.upsertEnrolmentUrl(enrolmentKey.asString)
    httpClient.PUT[EnrolmentVerifiers, UpsertEnrolmentResponse](url, verifiers)
  }

  def allocateEnrolment(groupId: String,
                        enrolmentKey: EnrolmentKey,
                        enrolmentRequest: EmacEnrolmentRequest
                       )(implicit hc: HeaderCarrier): Future[AllocateEnrolmentResponse] = {
    val url = appConfig.allocateEnrolmentUrl(groupId, enrolmentKey.asString)
    httpClient.POST[EmacEnrolmentRequest, AllocateEnrolmentResponse](url, enrolmentRequest)
  }
}
