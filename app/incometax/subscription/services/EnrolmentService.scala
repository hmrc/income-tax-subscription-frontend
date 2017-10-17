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

package incometax.subscription.services

import javax.inject.{Inject, Singleton}

import core.Constants.GovernmentGateway._
import incometax.subscription.connectors.GGConnector
import incometax.subscription.models.{EnrolFailure, EnrolRequest, EnrolSuccess}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentService @Inject()(ggConnector: GGConnector) {
  def enrol(mtditId: String, nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[EnrolFailure, EnrolSuccess.type]] = {
    val enrolRequest = EnrolRequest(
      portalId = ggPortalId,
      serviceName = ggServiceName,
      friendlyName = ggFriendlyName,
      knownFacts = List(mtditId, nino)
    )

    ggConnector.enrol(enrolRequest)
  }
}

