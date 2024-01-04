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

import models.DateModel

object MessageLookup {

  object Base {
    val continue = "Continue"
    val saveAndContinue = "Save and continue"
    val saveAndComeBackLater = "Save and come back later"
    val continueToSignUp = "Continue to sign up"
    val submit = "Submit"
    val update = "Update"
    val signOut = "Sign out"
    val startDate = "Start date"
    val endDate = "End date"
    val day = "Day"
    val month = "Month"
    val year = "Year"
    val errorHeading = "There is a problem"
    val change = "Change"
    val where_can_i_get_this_information = "Where can I get this information"
    val signUp = "Sign up"
    val dateOfBirth = "Date of birth"
    val goBack = "Go back"
    val navTitle = "Report your income and expenses quarterly"
    val titleError = "Error: "
    val yes = "Yes"
    val no = "No"
    val back = "Back"
  }

  object PreferencesCallBack {
    val title = "Do you want to continue?"
    val heading: String = "You need to agree to go paperless"
    val signOut: String = "Sign out"
  }

  object IndividualIncomeSource {
    val title = "What are your sources of income?"
    val heading: String = title
    val line_1: String = "Renting out a property includes using a letting agency."
    val business = "Sole trader with one or more businesses"
    val ukProperty = "Rent out UK properties"
    val foreignProperty = "Rent out overseas properties"
    val errorHeading = "There is a problem"
    val errorSummary = "Select your sources of income"
  }

  object Property {

    object Income {
      val title = "How much was your income from property this year?"
      val heading: String = title
      val lt10k = "Less than £10,000"
      val ge10k = "£10,000 or more"
    }

  }

  object CannotReportYet {
    val title = "You can’t use software to report your Income Tax yet"
    val heading: String = title
    val linkText = "Self Assessment tax return"
    val para2 = s"You need to send a $linkText instead."

    def para1(startDate: DateModel): String = s"You can sign up and use software to record your income and expenses, but you can’t send any reports until ${startDate.toOutputDateFormat}."
  }

  object CanReportBusinessButNotPropertyYet {
    val title = "You can’t use software to report your property income yet"
    val heading: String = title
    val para1 = "You can use software to report the work you do for yourself."
    val linkText = "send a Self Assessment tax return"
    val para2 = s"You can’t use software to submit a report for your property income until 6 April 2018. You need to $linkText instead."
  }

  object CannotReportYetBothMisaligned {
    val title = "You can’t use software to report your Income Tax yet"
    val heading: String = title
    val linkText = "send a Self Assessment tax return"
    val para1 = "You can sign up and use software to record your income and expenses, but you won’t be able to submit a report for:"
    val bullet1 = "property income until 6 April 2018"
    val para2 = s"You need to $linkText instead."

    def bullet2(startDate: DateModel): String = s"sole trader income until ${startDate.toOutputDateFormat}"
  }

  object AgentCannotReportYetBothMisaligned {
    val title = "Your client can’t use software to report their Income Tax yet"
    val heading: String = title
    val linkText = "send a Self Assessment tax return"
    val para1 = "You can still sign this client up and use software to record their income and expenses, but they won’t be able to submit a report for their:"
    val bullet1 = "property income until 6 April 2018"
    val para2 = s"Your client still needs to $linkText."

    def bullet2(businessStartDate: DateModel): String = s"sole trader income until ${businessStartDate.toOutputDateFormat}"
  }

  object BusinessName {
    val title = "Business name"
    val heading: String = "What’s the name of your business?"

    object SignUp {
      val line_1 = "This is the business name you used to register for Self Assessment. If your business doesn’t have a name, enter your own name."
    }

  }

  object PropertyStartDateMessages {
    val title = "When did your UK property business start trading?"
    val heading: String = title
    val hint = "For example, 1 4 2018"
    val continue = "Continue"
    val backLink = "Back"
    val update = "Update"
  }


  object AccountingMethod {
    val title = "How do you record your income and expenses for your self-employed business?"
    val heading: String = "How do you record your income and expenses for your self-employed business?"
    val accordion = "Show me an example"
    val accordion_line_1 = "You invoiced someone in March 2017 but didn’t receive the money until May 2017. If you would tell HM Revenue and Customs you received this income in:"
    val accordion_bullet_1 = "May 2017, then you use ‘cash basis’ accounting"
    val accordion_bullet_2 = "March 2017, then you use ‘accruals basis’"
    val cash = "Cash accounting You record on the date you receive money or pay a bill. Most sole traders and small businesses use this method."
    val accruals = "Standard accounting You record on the date you send or receive an invoice, even if you do not receive or pay any money. This is also called ‘accruals’ or ‘traditional accounting’."
  }

  object PropertyAccountingMethod {
    val title = "What accounting method do you use for your UK property business?"
    val heading = "What accounting method do you use for your UK property business?"
    val captionHidden = "This section is"
    val captionVisible = "UK property"
    val accordionSummary = "Show me an example"
    val accordionContentPara = "You created an invoice for someone in March 2017, but did not receive the money until May 2017. If you tell HMRC you received this income in:"
    val accordionContentBullet1 = "May 2017, you use ‘cash basis accounting’"
    val accordionContentBullet2 = "March 2017, you use ‘traditional accounting’"
    val radioCash = "Cash basis accounting"
    val radioCashDetail = "You record on the date you either receive a payment from your tenants into your bank account, or pay a bill, for example for repairs and maintenance. Most small businesses use this method."
    val radioAccruals = "Traditional accounting"
    val radioAccrualsDetail = "You record on the date you send an invoice to, or request a payment from your tenants, even if you do not receive or pay any money. You record on the date you receive an invoice for repairs or maintenance. This is also called ‘accruals’ or ‘standard accounting’."
  }

  object OverseasPropertyAccountingMethod {
    val title = "What accounting method do you use for your foreign property business?"
    val heading = "What accounting method do you use for your foreign property business?"
    val captionHidden = "This section is"
    val captionVisible = "Foreign property"
    val accordionSummary = "Show me an example"
    val accordionContentPara = "You created an invoice for someone in March 2017, but did not receive the money until May 2017. If you tell HMRC you received this income in:"
    val accordionContentBullet1 = "May 2017, you use ‘cash basis accounting’"
    val accordionContentBullet2 = "March 2017, you use ‘traditional accounting’"
    val radioCash = "Cash basis accounting"
    val radioCashDetail = "You record on the date you either receive a payment from your tenants into your bank account, or pay a bill, for example for repairs and maintenance. Most small businesses use this method."
    val radioAccruals = "Traditional accounting"
    val radioAccrualsDetail = "You record on the date you send an invoice to, or request a payment from your tenants, even if you do not receive or pay any money. You record on the date you receive an invoice for repairs or maintenance. This is also called ‘accruals’ or ‘standard accounting’."
  }

  object Summary {
    val title = "Check your answers"
    val heading: String = "Check your answers"
    val heading_hidden = "before signing up"
    val income_source = "Income received from"
    val selfEmployment = "Self-employment"
    val ukProperty = "UK property"
    val accountingMethodProperty = "Accounting method for UK property income"
    val propertyStart = "Trading start date of UK property business"
    val selfEmployments = "Number of businesses signed up"
    val overseasPropertyStartDate = "Trading start date of overseas property business"
    val accountingMethodForeignProperty = "Accounting method for overseas property income"
    val business_name = "Your business name"
    val selected_tax_year = "Tax year to start filing updates"
    val business_accountingmethod = "Accounting method for your self-employment income"
    val contact_email = "Do you want to receive electronic communications from HMRC?"
    val confirm_and_sign_up = "Confirm and sign up"

    object AccountingMethod {
      val cash = "Cash accounting"
      val accruals = "Standard accounting"
    }

    object SelectedTaxYear {
      def current(staringYear: Int, endYear: Int): String = s"Current tax year (6 April $staringYear to 5 April $endYear)"

      def next(staringYear: Int, endYear: Int): String = s"Next tax year (6 April $staringYear to 5 April $endYear)"
    }

  }

  object Timeout {
    val title = "Your session has timed out"
    val heading: String = title
    val returnToHome = """To sign up for quarterly reporting, you’ll have to sign in using your Government Gateway ID."""
  }

  object AlreadyEnrolled {
    val title = "You’ve already signed up"
    val heading = "You’ve already signed up"
    val line1 = "Your sign in details are already in use."
  }

  object ClaimEnrollmentConfirmation {
    val title = "You have added Making Tax Digital for Income Tax to your account"
    val heading = "What you must do now"
    val para1 = "You will now use Making Tax Digital for Income Tax to submit your income and expenses to HMRC."
    val para2 = "Read more about Making Tax Digital for Income Tax (opens in new tab)."
    val para3 = "We advise contacting your appointed tax agent, if you have one."
    val ContinueToOnlineServicesButton = "Continue to Online Services account"
  }

  object ClaimEnrolmentAlreadySignedUp {
    val title = "You are already signed up for Making Tax Digital for Income Tax"
    val content = "If you cannot find your Self Assessment account details, they may be in your other HMRC online services account. You can:"
    val link1 = "check if your Self Assessment is in another account"
    val link2 = "retrieve sign in details for your other account"
  }

  object UserDetailsError {
    val title = "User match error"
    val heading = "We couldn’t confirm your details"
    val line1 = "The details you’ve entered are not on our system."
  }

  object UserDetailsLockout {
    val title = "You’ve been locked out"
    val heading = "You’ve been locked out"

    def line1(testTime: String): String = s"To sign up for quarterly reporting, you’ll have to try again in $testTime."
  }

  object UserDetails {
    val title = "Confirm your details"
    val heading = "Confirm your details"
    val line1 = "We will check these details with information we currently have."
    val field1 = "First name"
    val field2 = "Last name"
    val field3 = "National Insurance number"
    val field4 = "Date of birth"
    val formhint1_line1 = "It’s on your National Insurance card, benefit letter, payslip or P60. For example, ‘QQ 12 34 56 C’."
    val formhint2 = "For example, 10 12 1990"
  }

  object ConfirmUser {
    val title = "Check your user details"
    val heading = "Check your answers"
    val firstName = "First name"
    val lastName = "Last name"
    val nino = "National Insurance number"
    val dob = "Date of birth"
  }


  object Confirmation {
    val title = "Confirmation page"
    val heading = "Sign up complete"
    val signOut = "Sign out"

    object whatHappensNext {
      val heading = "What happens next"
      val para1 = "You must use accounting software to record your income and expenses and send a report to HM Revenue and Customs at least every quarter."
      val para2 = "After you send a report you’ll get an Income Tax estimate. You can view your estimate and report deadlines in your:"
      val bul1 = "accounting software"
      val bul2 = "business tax account"
      val para4 = "It may take a few hours for them all to appear."
    }

  }

  object TaskList {
    val agentHeading = "Sign up your client for Making Tax Digital for Income Tax"
    val agentTitle = s"$agentHeading - Use software to report your client’s Income Tax - GOV.UK"
    val heading = "Sign up for Making Tax Digital for Income Tax"
    val title = s"$heading - Use software to send Income Tax updates - GOV.UK"

    val userInfoHeading = "Your information"
    val userInfoContent = "Full name: individualUserFullName National Insurance number: individualUserNino Unique Taxpayer Reference (UTR): individualUTRNumber"
    val userInfoPartialContent = "National Insurance number: individualUserNino Unique Taxpayer Reference (UTR): individualUTRNumber"

    val subHeadingComplete = "Application complete"
    val subHeadingIncomplete = "Application incomplete"

    def contentSummary(numberComplete: Int, numberTotal: Int) = s"You have completed $numberComplete of $numberTotal sections."

    val item1 = "1. Information about you"
    val item2 = "2. Your income sources"
    val item3 = "3. Choose a tax year"
    val item2Para = "Add any sole trader or UK and foreign property income sources you have."
    val agentItem1 = "1. Your client’s information"
    val agentItem2 = "2. Tell us about your client’s income"
    val agentItem3 = "3. Choose a tax year to sign up"
    val signUpIncompleteText = "You need to complete all sections above before you can sign up."
    val signUpReadyText = "You can now sign up."
    val agentSignUpReadyText = "You can now sign up your client."
    val agentSignUpIncompleteText = "You need to complete all sections above before you can confirm and sign up your client."
    val item3Para = "Select when you want to start using Making Tax Digital for Income Tax."
    val continue = "Submit and continue"
    val submitContinue = "Submit and continue"
    val checkAndContinue = "Check and continue"
    val selectTaxYear = "Select tax year"
    val complete = "Completed"
    val incomplete = "Incomplete"
    val notStarted = "Not started"
    val inProgress = "In progress"
    val addBusiness = "Add an income source"
    val addYourIncomeSources = "Add your income sources"
    val addYourClientsIncomeSource = "Add your client’s income sources"
    val viewYourIncomeSources = "View your income sources"
    val viewYourClientsIncomeSources = "View your client’s income sources"
    val agentAddBusiness = "Add a business"
    val selfEmploymentsBusinessLink = "selfEmploymentsBusinessLink"
    val ukPropertyBusiness = "UK property business"
    val overseasPropertyBusiness = "Overseas property business"
    val saveAndComeBackLater = "Save and come back later"
  }

  object TaxYearCheckYourAnswers {
    val title = "Check your answers - tax year - Use software to send Income Tax updates - GOV.UK"
    val agentTitle = "Check your answers - tax year - Use software to report your client’s Income Tax - GOV.UK"
    val heading = "Check your answers"
    val caption = "This section is Tax year you are signing up for"
    val agentCaption = "FirstName LastName | ZZ 11 11 11 Z"
    val question = "Tax year you are signing up for"
    val hiddenQuestion = "Change tax year you are signing up for"

    def current(staringYear: Int, endYear: Int): String = s"Current tax year (6 April $staringYear to 5 April $endYear)"

    def next(staringYear: Int, endYear: Int): String = s"Next tax year (6 April $staringYear to 5 April $endYear)"
  }

  object ProgressSaved {
    val title = "Your progress has been saved - Use software to send Income Tax updates - GOV.UK"
    val heading = "Your progress has been saved"

    def contentSummary(expirationDate: String) = s"We will keep your information until $expirationDate."

    val subheading = "What happens next"
    val paragraph1 = "You can:"
    val bullet1 = "continue signing up for Making Tax Digital for Income Tax"
    val bullet2 = "sign out and come back later"
    val paragraph2 = "If you sign out, you will need to come back to your Government Gateway login to continue. We suggest you bookmark this to make it easier to find when you return."
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

  object Error {

    object AccountingYear {
      val empty = "You must select an option to continue"
      val invalid = "You must select an option to continue"
    }

    object AgentAccountingYear {
      val empty = "Select the tax year you want to sign your client up for"
      val invalid = "Select the tax year you want to sign your client up for"
    }

    object UserDetails {
      val firstNameEmpty = "Enter your first name"
      val lastNameEmpty = "Enter your last name"
      val firstNameInvalid = "Enter a valid first name"
      val lastNameInvalid = "Enter a valid last name"
      val firstNameMaxLength = "Enter a first name that’s 105 characters or less"
      val lastNameMaxLength = "Enter a last name that’s 105 characters or less"
    }

    object Nino {
      val empty = "Enter your National Insurance number"
      val invalid = "Enter a valid National Insurance number"
    }

    object DOBDate {
      val empty = "Enter a date of birth"
      val invalid_chars = "Enter a date of birth using numbers 0 to 9"
      val invalid = "Enter a real date of birth"
    }

    object BackToPreferences {
      val empty = "You must select an option to continue"
    }

    object Business {

      object SoleTrader {
        val empty = "You must select an option to continue"
        val invalid = "You must select an option to continue"
      }

    }

    object Property {

      object Income {
        val empty = "You must select an option to continue"
        val invalid = "You must select an option to continue"
      }

    }

    object Date {
      val empty = "You must enter a date"
      val invalid = "You must enter a valid date"
      val end_violation = "You must enter a date greater than the start date"
    }

    object StartDate {
      val empty = "Enter a start date"
      val invalid_chars = "Enter a start date using numbers 0 to 9"
      val invalid = "Enter a real start date"
    }

    object EndDate {
      val empty = "Enter an end date"
      val invalid_chars = "Enter an end date using numbers 0 to 9"
      val invalid = "Enter a real end date"
      val end_violation = "Enter an end date that’s after the start date"
      val end_past = "Enter an end date that’s the present date or a future date"
    }

    object BusinessAccountingPeriod {
      val minStartDate = "The start date of your accounting period must be in the future."
      val maxEndDate = "You must provide an end date that is not more than 24 months after your start date"
    }

    object BusinessName {
      val empty = "You must enter your Business name"
      val maxLength = "You can’t enter more than 105 characters for your Business name"
      val invalid = "The business name contains invalid characters"
    }

    object ContactEmail {
      val empty = "Please enter a contact email"
      val maxLength = "The email is too long"
      val invalid = "The email is invalid"
    }

    object AccountingMethod {
      val empty = "Select how you record your income and expenses for your self-employed business"
      val invalid = "Select how you record your income and expenses for your self-employed business"
    }

    object NotEligible {
      val empty = "You must select an option to continue"
      val invalid = "You must select an option to continue"
    }

    object UnauthroisedAgent {

      object ConfirmAgent {
        val empty = "You must select an option to continue"
        val invalid = "You must select an option to continue"
      }

    }

  }

  object Eligible {
    val title = "You can send digital updates"
    val heading: String = title
    val line_1 = "Your answers mean you should be able to start sending HMRC digital updates after you sign up."
    val line_2 = "You just need to enter a few more details."
  }

  object NoSA {

    object Agent {
      val title = "Your client is not registered for Self Assessment"
      val heading: String = title
      val linkText = "register for Self Assessment"
      val line1 = s"To use this service, your client needs to $linkText."
    }

  }

  object MainIncomeError {
    val title = "You can’t sign up for quarterly reporting yet"
    val heading = "You can’t sign up for quarterly reporting yet"
    val para1 = "At the moment, you can only sign up if you’re one of the following:"
    val para2 = "You’ll be able to send quarterly reports for other income later in the year."
    val bullet1 = "a sole trader with income from one business"
    val bullet2 = "someone who rents out a UK property"
    val bullet3 = "a sole trader with income from one business and you rent out a UK property"
  }

  object ThankYou {
    val title = "Thank you"
    val heading = "Thank you"
    val line_1 = "Your feedback will help us improve this service."
    val gotoGovUk = "Go to the GOV.UK page"
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

  object NotEligibleForIncomeTax {
    val title = "You cannot use this service yet"
    val heading: String = "You cannot use this service yet"
    val para1 = "This is a trial service. You may be able to use the ‘Report your income and expenses quarterly’ service in the future."
  }

  object AddMTDITOverview {
    val title = "Add Making Tax Digital for Income Tax to your business tax account"
    val heading: String = title
    val paragraph1 = "Your agent has signed you up for Making Tax Digital for Income Tax (opens in new tab)"
    val paragraph2 = "You can now add it to your HMRC online services account and manage it with other taxes."
    val paragraph3 = "You may need to provide documents to prove your identity to add Making Tax Digital for Income Tax to your HMRC online services account."
    val insetText = "You need to use your current Government Gateway ID for Self Assessment to add Making Tax Digital for Income Tax. You can check your existing account details on your business tax account (opens in new tab)."
  }

  object ThrottleStartOfJourneyAgent {
    val title = "We are currently experiencing high levels of demand for this service - Use software to report your client’s Income Tax - GOV.UK"
    val heading = "We are currently experiencing high levels of demand for this service"
    val line1 = "You can not sign up your client to the Making Tax Digital for Income Tax service at the moment. If you would still like to access this service, try again shortly or sign out and come back later."
    val tryAgain = "Try again"
    val signOutText = "Sign out"
  }

  object ThrottleStartOfJourneyIndividual {
    val title = "There are currently too many people trying to sign up - Use software to send Income Tax updates - GOV.UK"
    val heading = "There are currently too many people trying to sign up"
    val line1 = "You can not finish signing up to the Making Tax Digital for Income Tax at the moment."
    val line2 = "If you want to continue, try again or sign out and come back later."
    val tryAgain = "Try again"
    val signOutText = "Sign out"
  }

  object ThrottleEndofJourney {
    val title = "You cannot complete signing-up at the moment - Use software to send Income Tax updates - GOV.UK"
    val heading = "You cannot complete signing-up at the moment"
    val line_1 = "There are currently too many people using this service."
    val line_2 = "To finish signing up, try again or sign out and come back later."
    val line_3 = "The details you’ve already entered have been saved for 30 days."
    val continueButton = "Try again"
    val signOutText = "Sign out"
  }

  object ThrottleEndofJourneyAgent {
    val title = "Your sign up cannot be completed right now - Use software to report your client’s Income Tax - GOV.UK"
    val heading = "Your sign up cannot be completed right now"
    val line_1 = "We are experiencing high levels of applications for this service and cannot complete your client’s sign up at the moment. Don’t worry, the details you have entered will be saved for 30 days."
    val line_2 = "To finish and complete signing up your client, try again or sign out and come back later."
    val continueButton = "Try again"
    val signOutText = "Sign out"
  }

}
