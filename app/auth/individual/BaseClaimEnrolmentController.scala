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

import auth.individual.AuthPredicate.AuthPredicate
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole, Enrolments}

import javax.inject.Inject
import scala.concurrent.Future

abstract class BaseClaimEnrolmentController @Inject()(implicit mcc: MessagesControllerComponents) extends BaseFrontendController {

  protected def defaultClaimEnrolmentPredicates: AuthPredicate[IncomeTaxSAUser] = claimEnrolmentPredicates

  object Authenticated extends AuthenticatedActions[IncomeTaxSAUser] {

    override def userApply: (Enrolments, Option[AffinityGroup], Option[CredentialRole], ConfidenceLevel, String) => IncomeTaxSAUser = IncomeTaxSAUser.apply

    override def async: AuthenticatedAction[IncomeTaxSAUser] = asyncInternal(defaultClaimEnrolmentPredicates)

    def asyncUnrestricted: AuthenticatedAction[IncomeTaxSAUser] = asyncInternal(emptyPredicate)

    def unrestricted(action: Request[AnyContent] => IncomeTaxUser => Result): Action[AnyContent] = {
      asyncUnrestricted(action andThen (_ andThen Future.successful))
    }

  }

}
