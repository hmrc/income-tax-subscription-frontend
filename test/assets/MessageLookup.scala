/*
 * Copyright 2021 HM Revenue & Customs
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

package assets

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

  object FrontPage {
    val title = "Sign up to report your income and expenses quarterly"
    val heading: String = title
    val subHeading_1 = "Sign up using Government Gateway"
    val subHeading_2 = "What happens after you’ve signed up"
    val bullet_1 = "using your accounting software to record your income and expenses"
    val bullet_2 = "sending details to us regularly from May 2017"
    val bullet_3 = "agreeing to go paperless"
    val bullet_4 = "a sole trader with income from one business"
    val bullet_5 = "you rent out a UK property"
    val bullet_6 = "a sole trader with income from one business and you rent out a UK property"
    val bullet_7 = "complete your 2016 to 2017 Self Assessment tax return and pay what you owe by 31 January 2018"
    val bullet_8 = "send your summary report for the 2017 to 2018 tax year by 31 January 2019"
    val bullet_9 = "use your business tax account from August 2017 to see your reports and what you might owe"
    val line_1 = "By signing up to HM Revenue and Customs secure service, you’re helping to test a new way of working out your tax. You’ll do this by:"
    val line_2 = "You can sign up if your current accounting period starts after 5 April 2017 and you’re one of the following:"
    val line_3 = "If you have income other than those listed above, you won’t be able to report it using this service yet."
    val line_4 = "You’ll need to enter the user ID and password you got when you signed up to the Self Assessment online service."
    val line_5 = "You’ll need to:"
    val line_6 = "You can:"
  }

  object PreferencesCallBack {
    val title = "Do you want to continue?"
    val heading: String = "You need to agree to go paperless"
    val legend: String = "To sign up for this service, you need to allow HM Revenue and Customs to send you electronic communications."
    val button: String = "Go back"
    val signOut: String = "Sign out"
  }

  object IncomeSource {
    val title = "Where does your income come from?"
    val heading: String = title
    val business = "Your sole trader business"
    val property = "Renting out a UK property"
    val both = "Your sole trader business and renting out a UK property"
    val other = "Other"
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
    val exampleStartDate = "For example, 1 4 2018"
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
    val accordionSummary = "Show me an example"
    val accordionContentPara = "You created an invoice for someone in March 2017, but did not receive the money until May 2017. If you tell HMRC you received this income in:"
    val accordionContentBullet1 = "May 2017, you use ‘cash accounting’"
    val accordionContentBullet2 = "March 2017, you use ‘standard accounting’"
    val radioCash = "Cash accounting"
    val radioCashDetail = "You record on the date you either receive a payment from your tenants into your bank account, or pay a bill, for example for repairs and maintenance. Most small businesses use this method."
    val radioAccruals = "Standard accounting"
    val radioAccrualsDetail = "You record on the date you send an invoice to, or request a payment from your tenants, even if you do not receive or pay any money. You record on the date you receive an invoice for repairs or maintenance. This is also called ‘accruals’ or ‘traditional accounting’."
  }

  object OverseasPropertyAccountingMethod {
    val title = "What accounting method do you use for your overseas property business?"
    val heading = "What accounting method do you use for your overseas property business?"
    val accordionSummary = "Show me an example"
    val accordionContentPara = "You created an invoice for someone in March 2017, but did not receive the money until May 2017. If you tell HMRC you received this income in:"
    val accordionContentBullet1 = "May 2017, you use ‘cash accounting’"
    val accordionContentBullet2 = "March 2017, you use ‘standard accounting’"
    val radioCash = "Cash accounting"
    val radioCashDetail = "You record on the date you either receive a payment from your tenants into your bank account, or pay a bill, for example for repairs and maintenance. Most small businesses use this method."
    val radioAccruals = "Standard accounting"
    val radioAccrualsDetail = "You record on the date you send an invoice to, or request a payment from your tenants, even if you do not receive or pay any money. You record on the date you receive an invoice for repairs or maintenance. This is also called ‘accruals’ or ‘traditional accounting’."
  }

  object Summary {
    val title = "Check your answers"
    val heading: String = title
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

    object IncomeSource {
      val business = "Sole trader business"
      val property = "Property"
      val both = "Sole trader business and property"
    }

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

  object ClaimSubscription {
    val title = "You’ve already signed up for quarterly reporting"
    val heading = "You’ve already signed up for quarterly reporting"
  }

  object ClaimEnrollmentConfirmation {
    val title = "You have added Making Tax Digital for Income Tax to your account"
    val heading = "What happens now"
    val bullet1 = "Find software that’s compatible (opens in new tab) and allow it to interact with HMRC, if you have not already done so."
    val bullet2 = "Send next quarterly updates using your software when required throughout the tax year."
    val bullet3 = "Submit your annual updates and declare for the tax year."
  }

  object ClaimEnrolmentAlreadySignedUp {
    val title = "You are already signed up for Making Tax Digital for Income Tax"
    val content = "If you cannot find your Self Assessment account details, they may be in your other HMRC online account."
    val link1 = "Check if your Self Assessment is in another account"
    val link2 = "Retrieve sign in details for your other account"
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
    val title = "Check your answers"
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
    val title = "Check and complete your business details - Use software to send Income Tax updates - GOV.UK"
    val heading = "Check and complete your business details"
    val subHeadingComplete = "Application complete"
    val subHeadingIncomplete = "Application incomplete"
    def contentSummary (numberComplete: Int,numberTotal: Int) = s"You have completed $numberComplete of $numberTotal sections."
    val item1 = "1. Choose a tax year to sign up"
    val item2 = "2. Tell us about your income"
    val item3 = "3. Sign up"
    val signUp = "3. Sign up"
    val signUpIncompleteText = "You need to complete the `Tell us about your income` section before you can confirm and sign up"
    val continue = "Continue"
    val selectTaxYear = "Select tax year"
    val complete = "Complete"
    val incomplete = "Incomplete"
    val notStarted = "Not started"
    val inProgress = "In progress"
    val addBusiness = "Add a business"
    val selfEmploymentsBusinessLink= "selfEmploymentsBusinessLink"
    val ukPropertyBusiness= "UK property business"
    val overseasPropertyBusiness= "Overseas property business"
  }

  object TaxYearCheckYourAnswers {
    val title = "Check your answers - Use software to send Income Tax updates - GOV.UK"
    val heading = "Check your answers"
    val question = "Tax year you are signing up for"
    def current(staringYear: Int, endYear: Int): String = s"Current tax year (6 April $staringYear to 5 April $endYear)"
    def next(staringYear: Int, endYear: Int): String = s"Next tax year (6 April $staringYear to 5 April $endYear)"
  }

  object ProgressSaved {
    val title = "Your progress has been saved - Use software to send Income Tax updates - GOV.UK"
    val heading = "Your progress has been saved"
    def contentSummary(expirationDate: String) = s"We will keep your information until $expirationDate."
    val subheading = "What happens next"
    val paragraph1 = "You can:"
    val bullet1 = "return to sign up your business for Making Tax Digital for Income Tax"
    val bullet2 = "sign out and come back later"
    val paragraph2 = "If you sign out, you will need to come back to your Government Gateway login to continue. We suggest you bookmark this to make it easier to find when you return."
  }

  object SignUpCompleteIndividual {
    val title = "Sign up complete"
    val heading = "You have signed up to use software to send Income Tax updates"

    object whatHappensNow {
      val heading = "What happens now"
      val linkText = "find software that’s compatible (opens in new tab)"
      val findSoftware = s"If you have not already done so, $linkText. and allow it interact with HMRC."
      val allowInteraction = "Allow your software to interact with HMRC."
      val addIncomeExpensesAlreadyReceived = "Add any income and expenses you have already received or paid out from the start of your tax year into your software."
      val sendQuarterlyBy = "You will need to send quarterly updates using your software by:"
      val nextTaxYearJulyUpdate = "5 July 2021"
      val nextTaxYearOcoberUpdate = "5 October 2021"
      val nextTaxYearJanuaryUpdate = "5 January 2022"
      val nextTaxYearAprilUpdate = "5 April 2022"
      val submitAnnualAndDeclare = "Submit your annual updates and declare for the tax year by 31 January 2023."
      val currentYaxYearQuarterlyUpdates = "Send next quarterly updates using your software by:"
      val currentTaxYearJulyUpdate = "5 July 2020"
      val currentTaxYearOctoberUpdate = "5 October 2020"
      val currentTaxYearJanuaryUpdate = "5 January 2021"
      val currentTaxYearAprilUpdate = "5 April 2021"
      val currentTaxYearAnnualUpdates = "Submit your annual updates and declare for the tax year by 31 January 2022."
      val currentTaxYearPreviousUpdates = "You need to add all income and expenses for the previous updates using your software for:"
      val btaLinkText = "Business Tax account (opens in new tab)"
      val loginToBTA = s"Log in to your $btaLinkText to find out when your first update is due."
      val para1 = s"After you have sent an update you will get an year-to-date Income Tax estimate. You can view your estimates and submission dates in your software or your $btaLinkText."
      val para2 = "It may take a few hours before new information is displayed."
      val signOut = "Sign out"
      val finishAndSignOut = "Finish and sign out"
    }

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

    object IncomeSource {
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

  object WhatYearToSignUp {
    val title = "Which tax year do you want to start using software to file updates for?"
    val heading: String = title
    val line1 = "You can start sending income tax updates for the current tax year or the next tax year. It will not affect the amount of tax you will pay."
    val line1_updated = "You can start sending income tax updates during the current tax year or the next tax year. It will not affect the amount of tax you will pay. Add all business income and expenses into your software from 6 April:"

    def option1ConditionalExample1: String = "You will need to add all business income and expenses into your software from the start of the current tax year, which is 6 April. You will need to send quarterly filing updates for:"

    def option1ConditionalExample1_updated: String = "You need to send a quarterly filing update for:"

    def option1ConditionalExample2(year: String): String = s"You need to submit a final declaration by the 31 January $year."

    def option1ConditionalExample2_updated(year: String): String = s"Send a final declaration by the 31 January $year."

    def option2ConditionalExample1: String = "You will need to send quarterly filing updates for:"

    def option2ConditionalExample1_updated: String = "You need to send a quarterly filing update for:"

    def option2ConditionalExample2(year: String): String = s"You will need to submit a final declaration by 31 January $year and will need to complete a Self Assessment return for the current tax year as normal."

    def option2ConditionalExample2_updated(year: String): String = s"Send a final declaration by 31 January $year and complete a Self Assessment return for the current tax year as normal."

    def option1(fromYear: String, toYear: String): String = s"Current tax year (6 April $fromYear to 5 April $toYear)"

    def option2(fromYear: String, toYear: String): String = s"Next tax year (6 April $fromYear to 5 April $toYear)"

    def conditionalDate1(year: String): String = s"5 July $year"

    def conditionalDate2(year: String): String = s"5 October $year"

    def conditionalDate3(year: String): String = s"5 January $year"

    def conditionalDate4(year: String): String = s"5 April $year"

  }

  object AddMTDITOverview {
    val title = "Add Making Tax Digital for Income Tax to your business tax account"
    val heading: String = title
    val subHeading = "Your responsibilities"
    val insetText = "You need to use your current Government Gateway ID for Self Assessment. You can check your existing account details on your business tax account (opens in new tab)."
    val paragraph1 = "Your agent has signed you up for Making Tax Digital for Income Tax (opens in new tab)."
    val paragraph2 = "Adding this to your business tax account lets you manage it online with your other taxes."
    val paragraph3 = "Making Tax Digital for Income Tax replaces Self Assessment. You can stop this tax at any time and go back to Self Assessment."
    val paragraph4 = "You’ll still need to send HMRC a Self Assessment tax return for the tax year before you signed up for Making Tax Digital for Income Tax."
    val paragraph5 = "To add this tax, you may need to provide documents that prove your identity."

  }

}
