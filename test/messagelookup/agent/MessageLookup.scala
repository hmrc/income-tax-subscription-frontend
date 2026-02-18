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
    val uList = "You can either:"
    val bullet1 = "manage your clients’ Self Assessment details"
    val bullet2 = "sign up another client"
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

  object Heading {
    val heading = "Signing up your clients for Making Tax Digital for Income Tax"
  }

  object BeforeSignUp {
    val heading = "Before you sign up your clients"
    val paraOne = "To sign up your clients, you must have their authorisation in your agent services account."
    val paraTwoLinkText = "software that’s compatible with Making Tax Digital for Income Tax (opens in new tab)"
    val paraTwo = s"Make sure you or your clients use $paraTwoLinkText."
    val paraThree = "Make sure your client is a sole trader or gets income from property (inside or outside the UK)."
  }

  object AccountingPeriod {
    val heading = "Accounting period"
    val paraOne = "Make sure your client uses either:"
    val bulletOne = "an accounting period that runs from 6 April to 5 April"
    val bulletTwo = "an accounting period that runs from 1 April to 31 March (and their compatible software supports calendar update periods)"
    val paraTwo = "From the 2026 to 2027 tax year, this service will be extended to people with any accounting period. You must submit your client’s Self Assessment tax return for the tax years up to 5 April 2025 as normal."
  }

  object CheckSignUp {
    val heading = "We will check if you can sign up each client"
    val paraOne = "When you continue, you’ll need to enter these details for one of your clients:"
    val bulletOne = "name"
    val bulletTwo = "National Insurance number"
    val bulletThree = "date of birth"
    val paraTwo = "We’ll check that client’s record and tell you if you can sign them up."
  }

}
