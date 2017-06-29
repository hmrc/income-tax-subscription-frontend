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

import audit.Logging
import common.Constants
import connectors.EnrolmentConnector
import connectors.models.Enrolment._
import play.api.mvc.Result
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import common.Constants._
import connectors.models.Enrolment

@Singleton
class EnrolmentService @Inject()(val authConnector: AuthConnector,
                                 val enrolmentConnector: EnrolmentConnector,
                                 logging: Logging) {

  def getEnrolments(implicit hc: HeaderCarrier): Future[Option[Seq[Enrolment]]] = {
    logging.debug(s"getEnrolments")
    for {
      authority <- authConnector.currentAuthority
        .collect { case Some(auth) => auth }
      enrolments <- enrolmentConnector.getEnrolments(authority.uri)
    } yield enrolments
  }

}

