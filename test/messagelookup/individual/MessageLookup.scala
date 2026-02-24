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

package messagelookup.individual

object MessageLookup {

  object Base {
    val continue = "Continue"
    val continueToSignUp = "Continue to sign up"
    val update = "Update"
    val signOut = "Sign out"
    val day = "Day"
    val month = "Month"
    val year = "Year"
    val goBack = "Go back"
    val titleError = "Error: "
  }

  object Timeout {
    val title = "Your session has timed out"
    val heading: String = title
    val returnToHome = """To sign up for quarterly reporting, you’ll have to sign in using your Government Gateway ID."""
  }

  object AlreadyEnrolled {
    val title = "You’re already signed up to use Making Tax Digital for Income Tax"
    val heading = "You’re already signed up to use Making Tax Digital for Income Tax"
    val line1 = "This could be because:"
    val b1 = "you previously signed up"
    val b2 = "someone who helps you with your tax, like an agent, has already signed you up"
    val h2 = "Next steps"
    val line2 = "You can manage your Self Assessment details from HMRC online services."
    val line3 = "You’ll need to sign in again using the same sign in details that you use for your Self Assessment tax return."
  }

  object ClaimEnrollmentConfirmation {
    def title(origin: String) =
      "You have added Making Tax Digital for Income Tax to your " +
        getAccountType(origin)

    val heading = "What to do next"
    val para1 = "Check you have completed all the steps to use Making Tax Digital for Income Tax (opens in new tab)."
    val para2 = "You, or your agent if you have one, will need to use software that works with Making Tax Digital for Income Tax to:"
    val para3 = "We advise contacting your appointed tax agent, if you have one."

    val b1 = "create, store and correct digital records of your self-employment and property income and expenses"
    val b2 = "send your quarterly updates to HMRC"
    val b3 = "submit your tax return and pay tax due by 31 January the following year"

    def ContinueToOnlineServicesButton(origin: String) =
      "Continue to " +
        getAccountType(origin)

    private def getAccountType(origin: String) =
      origin match {
        case "bta" => "business tax account"
        case "pta" => "personal tax account"
        case "sign-up" => "online services account"
        case _ => ""
      }
  }

  object ClaimEnrolmentAlreadySignedUp {
    val title = "You are already signed up for Making Tax Digital for Income Tax"
    val content = "If you cannot find your Self Assessment account details, they may be in your other HMRC online services account. You can:"
    val link1 = "check if your Self Assessment is in another account"
    val link2 = "retrieve sign in details for your other account"
  }

  object Confirmation {
    val title = "Confirmation page"
  }

  object AffinityGroup {
    val title = "You can’t use this service"
    val heading = "You can’t use this service"
    val line1 = "You can only use this service if you have an individual Government Gateway account."
    val line2 = """To sign up for quarterly reporting, you’ll need to sign in using a different type of account."""

    object Agent {
      val linkId: String = "agent-service"
      val linkText = "use our agent service."
      val line1 = s"To sign up for quarterly reporting with these sign in details, you need to $linkText"
    }

  }

  object NoSA {

    object Agent {
      val title = "Your client is not registered for Self Assessment"
      val heading: String = title
      val linkText = "register for Self Assessment"
      val line1 = s"To use this service, your client needs to $linkText."
    }

  }

  object CannotSignUp {
    val title = "You cannot use this service"
    val heading: String = title
    val linktext = "send a Self Assessment tax return"
    val line1 = "You can only sign up if you are an administrator."
    val bullet1 = "are you self-employed"
    val bullet2 = "rent out UK property"
    val bullet3 = "are you self-employed and rent out UK property"
    val line2 = s"You need to $linktext instead."
  }

  object CannotUseService {
    val title = "You cannot use this service"
    val heading: String = title
    val line1 = "You can only sign up if you are an administrator."
  }

  object ThrottleStartOfJourneyAgent {
    val title = "We are currently experiencing high levels of demand for this service - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
    val heading = "We are currently experiencing high levels of demand for this service"
    val line1 = "You can not sign up your client to the Making Tax Digital for Income Tax service at the moment. If you would still like to access this service, try again shortly or sign out and come back later."
    val tryAgain = "Try again"
    val signOutText = "Sign out"
  }

  object ThrottleStartOfJourneyIndividual {
    val title = "There are currently too many people trying to sign up - Sign up for Making Tax Digital for Income Tax - GOV.UK"
    val heading = "There are currently too many people trying to sign up"
    val line1 = "You can not finish signing up to the Making Tax Digital for Income Tax at the moment."
    val line2 = "If you want to continue, try again or sign out and come back later."
    val tryAgain = "Try again"
    val signOutText = "Sign out"
  }

  object ThrottleEndofJourney {
    val title = "You cannot complete signing-up at the moment - Sign up for Making Tax Digital for Income Tax - GOV.UK"
    val heading = "You cannot complete signing-up at the moment"
    val line_1 = "There are currently too many people using this service."
    val line_2 = "To finish signing up, try again or sign out and come back later."
    val line_3 = "The details you’ve already entered have been saved for 30 days."
    val continueButton = "Try again"
    val signOutText = "Sign out"
  }

  object ThrottleEndofJourneyAgent {
    val title = "There is a problem - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
    val heading = "There is a problem"
    val line_1 = "We are experiencing high levels of applications for Making Tax Digital for Income Tax and cannot complete your client’s sign up at the moment."
    def line_2(date: String) = s"We’ll save the data you have entered until $date."
    val continueButton = "Try again"
    val signOutText = "Sign out"
  }

  object IndividualSignUpTerms {
    val heading = "Signing up for Making Tax Digital for Income Tax"
    val subheading = "How to sign up"

    object Heading {
      val paraOne: String = "Making Tax Digital for Income Tax is a new way of reporting income to HMRC. " +
        "It’s currently in a voluntary phase for selected self-employed businesses and landlords."
    }

    object beforeYouSignUp {
      val heading = "Before you sign up"
      val paraOne = "To sign up, you must be a sole trader or get income from either a UK or foreign property."
      val paraTwoLinkText = "software that works with Making Tax Digital for Income Tax (opens in new tab)"
      val paraTwo = s"You must also use $paraTwoLinkText."
    }

    object soleTrader {
      val heading = "Sole trader"
      val paraOne = "You’re a sole trader if you run your own business as an individual and work for yourself. This is also known as being self-employed."
      val paraTwo = "You’re not a sole trader if your only business income is from a limited company."
    }

    object incomeProperty {
      val heading = "Income from property"
      val paraOne = "You can sign up if you get income from property in the UK or from property in another country. For example, letting houses, flats or holiday homes, on a long or short term basis."
    }

    object identityVerification {
      val heading = "We may need to check your identity"
      val paraOne = "To confirm your identity, you can either:"
      val bulletOne = "use an app to match you to your photo ID"
      val bulletTwo = "answer questions about information we hold about you"
    }

  }

}
