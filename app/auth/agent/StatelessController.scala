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

package auth.agent

import auth.individual.AuthPredicate.AuthPredicate
import auth.individual.BaseFrontendController
import javax.inject.Inject
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole, Enrolments}

abstract class StatelessController @Inject()(implicit mcc: MessagesControllerComponents) extends BaseFrontendController {

  protected val statelessDefaultPredicate: AuthPredicate[IncomeTaxAgentUser] = auth.agent.AuthPredicates.homePredicates

  object Authenticated extends AuthenticatedActions[IncomeTaxAgentUser] {

    override def userApply: (Enrolments, Option[AffinityGroup], Option[CredentialRole], ConfidenceLevel) => IncomeTaxAgentUser =
      (enrolments, affinity, _, confidence) => IncomeTaxAgentUser.apply(enrolments, affinity, confidence)

    override val async: AuthenticatedAction[IncomeTaxAgentUser] = asyncInternal(statelessDefaultPredicate)
  }

}