/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.auth

import agent.common.Constants
import agent.controllers.ITSASessionKeys
import core.auth.IncomeTaxUser
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core._

import scala.collection.immutable.::

case class IncomeTaxAgentUser(enrolments: Enrolments, affinityGroup: Option[AffinityGroup], confidenceLevel: ConfidenceLevel) extends IncomeTaxUser {
  lazy val arn: Option[String] = getEnrolment(Constants.agentServiceEnrolmentName)

  def clientNino(implicit request: Request[AnyContent]): Option[String] =
    request.session.get(ITSASessionKeys.NINO)

  def clientUtr(implicit request: Request[AnyContent]): Option[String] =
    request.session.get(ITSASessionKeys.UTR)

  private def getEnrolment(key: String) = enrolments.enrolments.collectFirst {
    case Enrolment(`key`, EnrolmentIdentifier(_, value) :: _, _, _) => value
  }
}
