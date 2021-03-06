/*
 * Copyright 2021 HM Revenue & Customs
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

package auth.individual

import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core._
import utilities.ITSASessionKeys
import utilities.individual.Constants

import scala.collection.immutable.::

trait IncomeTaxUser {
  val enrolments: Enrolments
  val affinityGroup: Option[AffinityGroup]
}

case class IncomeTaxSAUser(enrolments: Enrolments,
                           affinityGroup: Option[AffinityGroup],
                           credentialRole: Option[CredentialRole],
                           confidenceLevel: ConfidenceLevel,
                           userId: String) extends IncomeTaxUser {

  def nino(implicit request: Request[AnyContent]): Option[String] =
    getEnrolment(Constants.ninoEnrolmentName) match {
      case None => request.session.get(ITSASessionKeys.NINO)
      case x => x
    }

  def ninoFromEnrolmentsOnly: Option[String] = {
    getEnrolment(Constants.ninoEnrolmentName)
  }

  def utr(implicit request: Request[AnyContent]): Option[String] =
    getEnrolment(Constants.utrEnrolmentName) match {
      case None => request.session.get(ITSASessionKeys.UTR)
      case x => x
    }

  lazy val mtdItsaRef: Option[String] = getEnrolment(Constants.mtdItsaEnrolmentName)

  lazy val isAssistant: Boolean = credentialRole match {
    case Some(Assistant) => true
    case None => throw new IllegalArgumentException("Non GGW credential found")
    case _ => false
  }

  private def getEnrolment(key: String) = enrolments.enrolments.collectFirst {
    case Enrolment(`key`, EnrolmentIdentifier(_, value) :: _, _, _) => value
  }
}
