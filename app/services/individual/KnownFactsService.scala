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

import connectors.individual.subscription.TaxEnrolmentsConnector
import core.Constants
import core.Constants.GovernmentGateway._
import core.config.AppConfig
import javax.inject.{Inject, Singleton}
import models.individual.subscription._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class KnownFactsService @Inject()(taxEnrolmentsConnector: TaxEnrolmentsConnector,
                                   appConfig: AppConfig) {
  def addKnownFacts(mtditId: String, nino: String)(implicit hc: HeaderCarrier): Future[Either[KnownFactsFailure, KnownFactsSuccess.type]] = {
    val enrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> mtditId)
    val enrolmentVerifiers = EnrolmentVerifiers(NINO -> nino)

    taxEnrolmentsConnector.upsertEnrolment(enrolmentKey, enrolmentVerifiers)
  }

}
