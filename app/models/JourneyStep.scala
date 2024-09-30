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

package models

import uk.gov.hmrc.http.InternalServerException

sealed trait JourneyStep {
  val key: String
}

object JourneyStep {

  case object ClientDetails extends JourneyStep {
    val key = "ClientDetails"
  }

  case object ConfirmedClient extends JourneyStep {
    val key = "ConfirmedClient"
  }

  case object Confirmation extends JourneyStep {
    val key = "Confirmation"
  }

  def fromString(key: String): JourneyStep = {
    key match {
      case ConfirmedClient.key => ConfirmedClient
      case Confirmation.key => Confirmation
      case _ => throw new InternalServerException(s"[JourneyStep][fromString] - Unsupported journey key - $key")
    }
  }

}