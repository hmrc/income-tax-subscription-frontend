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

import auth.individual.IncomeTaxUser
import common.Extractors
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil

class IncomeTaxAgentUser(val enrolments: Enrolments,
                         val affinityGroup: Option[AffinityGroup],
                         val confidenceLevel: ConfidenceLevel)
  extends IncomeTaxUser with Extractors {

  lazy val arn: String = getArnFromEnrolments(enrolments).get

  def clientName(implicit request: Request[AnyContent]): String = {
    request.fetchClientName.getOrElse(
      throw new InternalServerException("[IncomeTaxAgentUser][clientName] - could not retrieve client name from request session")
    )
  }

}
