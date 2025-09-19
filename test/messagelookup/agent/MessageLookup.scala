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

package messagelookup.agent

object MessageLookup {

  object Base {
    val continue = "Continue"
    val update = "Update"
    val signOut = "Sign out"
    val goBack = "Go back"
    val tryAgain = "Try again"
    val saveAndContinue = "Save and continue"
    val saveAndComeBackLater = "Save and come back later"
  }

  object Timeout {
    val title = "Your session has timed out"
    val heading = "Your session has timed out"
    val returnToHome = """To sign your client up for quarterly reporting, you’ll have to sign in using your Government Gateway ID."""
  }

  object ClientAlreadySubscribed {
    val title = "Your client has already signed up"
    val heading = "Your client has already signed up"
    val para1 = "This client’s details are already in use."
  }

  object NotEnrolledAgentServices {
    val title = "You can’t use this service yet"
    val heading: String = title
    val linkText = "set up an agent services account"
    val para1 = s"To use this service, you need to $linkText."
  }

  object ClientDetailsLockout {
    val title = "You’ve been locked out"
    val heading = "You’ve been locked out"

    def line1(testTime: String) = s"To sign your client up for quarterly reporting, you’ll have to try again in $testTime."
  }

}
