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

package core.auth

import play.api.mvc.Action
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole, Enrolments}
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.Future

trait UnauthorisedAgentSubscriptionController extends BaseFrontendController {

  object Authenticated extends AuthenticatedActions[IncomeTaxSAUser] {

    override def userApply: (Enrolments, Option[AffinityGroup], Option[CredentialRole], ConfidenceLevel) => IncomeTaxSAUser = IncomeTaxSAUser.apply

    private val unauthorisedAgentUnavailableMessage = "This page for unauthorised agents is not yet available to the public: "

    override def async: AuthenticatedAction[IncomeTaxSAUser] =
      if (applicationConfig.unauthorisedAgentEnabled)
        asyncInternal(unauthorisedAgentSubscriptionPredicates)
      else _ =>
        Action.async(request => Future.failed(new NotFoundException(unauthorisedAgentUnavailableMessage + request.uri)))
  }

}