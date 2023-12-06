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

package auth.agent

import auth.individual.BaseFrontendController
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole, Enrolments}

import javax.inject.Inject

abstract class PreSignUpController @Inject()(implicit mcc: MessagesControllerComponents) extends BaseFrontendController {

  object Authenticated extends AuthenticatedActions[IncomeTaxAgentUser] {

    override def getUser: (Enrolments, Option[AffinityGroup], Option[CredentialRole], ConfidenceLevel, String) => IncomeTaxAgentUser =
      (enrolments, affinity, _, confidence, _) => new IncomeTaxAgentUser(enrolments, affinity, confidence)

    override val async: AuthenticatedAction[IncomeTaxAgentUser] = asyncInternal(auth.agent.AuthPredicates.preSignUpPredicates)
  }

}
