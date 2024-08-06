/*
 * Copyright 2023 HM Revenue & Customs
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

import common.Constants
import common.Constants.ITSASessionKeys
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core._

trait IncomeTaxUser {
  val enrolments: Enrolments
  val affinityGroup: Option[AffinityGroup]
}

case class UserIdentifiers(nameMaybe: Option[String], entityIdMaybe: Option[String])

class IncomeTaxSAUser(val enrolments: Enrolments,
                      val affinityGroup: Option[AffinityGroup],
                      val credentialRole: Option[CredentialRole],
                      val confidenceLevel: ConfidenceLevel,
                      val userId: String) extends IncomeTaxUser {

  lazy val isAssistant: Boolean = credentialRole match {
    case Some(Assistant) => true
    case None => throw new IllegalArgumentException("Non GGW credential found")
    case _ => false
  }

  private def getEnrolment(key: String) = {
    enrolments.getEnrolment(key).flatMap { enrolment =>
      enrolment.identifiers.headOption map { identifier =>
        identifier.value
      }
    }
  }

  def getSPSEntityId(implicit request: Request[AnyContent]): Option[String] = {
    request.session.get(ITSASessionKeys.SPSEntityId)
  }

  lazy val mtdItsaRef: Option[String] = getEnrolment(Constants.mtdItsaEnrolmentName)
}

object IncomeTaxSAUser {
  def fullName(implicit request: Request[AnyContent]): Option[String] =
    request.session.get(ITSASessionKeys.FULLNAME)
}
