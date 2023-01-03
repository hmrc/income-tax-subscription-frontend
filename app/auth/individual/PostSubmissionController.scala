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

import javax.inject.Inject
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole, Enrolments}

abstract class PostSubmissionController @Inject()(implicit mcc: MessagesControllerComponents) extends BaseFrontendController {

  object Authenticated extends AuthenticatedActions[IncomeTaxSAUser] {
    override def getUser: (Enrolments, Option[AffinityGroup], Option[CredentialRole], ConfidenceLevel, String) => IncomeTaxSAUser =
      (enrolments, affinity, credentials, confidence, userId) => new IncomeTaxSAUser(enrolments, affinity, credentials, confidence, userId)

    override val async: AuthenticatedAction[IncomeTaxSAUser] = asyncInternal(enrolledPredicates)
  }

}
