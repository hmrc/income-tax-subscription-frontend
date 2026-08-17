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

import models.JourneyStep
import uk.gov.hmrc.http.InternalServerException

object JourneyStep {
  val prefix = "I-"

  case object ClaimEnrolment extends JourneyStep {
    val key = s"${prefix}ClaimEnrolment"
  }

  case object PreSignUp extends JourneyStep {
    val key = s"${prefix}PreSignUp"
  }

  case object SignUp extends JourneyStep {
    val key = s"${prefix}SignUp"
  }

  case object Confirmation extends JourneyStep {
    val key = s"${prefix}Confirmation"
  }

  def fromString(key: String): JourneyStep = {
    key match {
      case PreSignUp.key => PreSignUp
      case SignUp.key => SignUp
      case ClaimEnrolment.key => ClaimEnrolment
      case Confirmation.key => Confirmation
      case _ => throw new InternalServerException(s"[Individual][JourneyStep] - Unsupported journey key - $key")
    }
  }
}