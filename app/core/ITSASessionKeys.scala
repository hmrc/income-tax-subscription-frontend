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

package core

object ITSASessionKeys {
  val StartTime = "StartTime"
  val RequestURI = "Request-URI"
  val NINO = "NINO"
  val UTR = "UTR"
  val FailedUserMatching = "Failed-User-Matching"
  val JourneyStateKey = "Journey-State"
  val PreferencesRedirectUrl = "Preferences-Redirect-Url"
  val AgentReferenceNumber = "Agent-Reference-Number"
  val ConfirmedAgent = "Confirmed-Agent"
}
