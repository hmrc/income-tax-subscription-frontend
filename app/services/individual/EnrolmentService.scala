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

package services.individual

import config.AppConfig
import connectors.individual.subscription.TaxEnrolmentsConnector
import javax.inject.{Inject, Singleton}
import models.individual.subscription.{EmacEnrolmentRequest, EnrolFailure, EnrolSuccess, EnrolmentKey}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.individual.Constants
import utilities.individual.Constants.GovernmentGateway._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentService @Inject()(config: AppConfig,
                                 enrolmentStoreConnector: TaxEnrolmentsConnector,
                                 authConnector: AuthConnector)(implicit ec: ExecutionContext) {

  def enrol(mtditId: String, nino: String)(implicit hc: HeaderCarrier): Future[Either[EnrolFailure, EnrolSuccess.type]] = {
    authConnector.authorise(EmptyPredicate, credentials and groupIdentifier) flatMap {
      case Some(Credentials(ggCred, GGProviderId)) ~ Some(groupId) =>
        val enrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> mtditId)
        val enrolmentRequest = EmacEnrolmentRequest(ggCred, nino)
        enrolmentStoreConnector.allocateEnrolment(groupId, enrolmentKey, enrolmentRequest)
      case Some(_) ~ None =>
        Future.failed(new InternalServerException("Failed to enrol - user did not have a group identifier (not a valid GG user)"))
      case _ ~ _ =>
        Future.failed(new InternalServerException("Failed to enrol - user had a different auth provider ID (not a valid GG user)"))
    }
  }

}

