/*
 * Copyright 2024 HM Revenue & Customs
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

package models.individual

import auth.individual.{ClaimEnrolment => OldClaimEnrolment, SignUp => OldSignUp}
import play.api.Logging
import uk.gov.hmrc.http.InternalServerException

sealed trait JourneyStep {
  val key: String
}

object JourneyStep extends Logging {

  case object PreSignUp extends JourneyStep {
    val key: String = "PreSignUp"
  }

  case object SignUp extends JourneyStep {
    val key: String = "SignUp"
  }

  case object Confirmation extends JourneyStep {
    val key: String = "Confirmation"
  }

  def fromString(key: String): JourneyStep = {
    key match {
      // if the user is in the old state, pretend it's the new sign up state
      case OldSignUp.name => SignUp
      case OldClaimEnrolment.name => PreSignUp
      case PreSignUp.key => PreSignUp
      case SignUp.key => SignUp
      case Confirmation.key => Confirmation
      case _ => throw new InternalServerException(s"[Individual][JourneyStep] - Unsupported journey key - $key")
    }
  }

}