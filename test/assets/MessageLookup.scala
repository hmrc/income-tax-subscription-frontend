/*
 * Copyright 2019 HM Revenue & Customs
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

import core.models.DateModel

object MessageLookup {

  object Base {
    val continue = "Continue"
    val continueToSignUp = "Continue to sign up"
    val submit = "Submit"
    val update = "Update"
    val signOut = "Sign out"
    val startDate = "Start date"
    val endDate = "End date"
    val day = "Day"
    val month = "Month"
    val year = "Year"
    val errorHeading = "There's a problem"
    val change = "Change"
    val where_can_i_get_this_information = "Where can I get this information"
    val signUp = "Sign up"
    val dateOfBirth = "Date of birth"
    val goBack = "Go back"
    val navTitle = "Report your income and expenses quarterly"
    val titleError = "Error: "
    val yes = "Yes"
    val no = "No"
  }

  object FrontPage {
    val title = "Sign up to report your income and expenses quarterly"
    val heading = title
    val subHeading_1 = "Sign up using Government Gateway"
    val subHeading_2 = "What happens after you've signed up"
    val bullet_1 = "using your accounting software to record your income and expenses"
    val bullet_2 = "sending details to us regularly from May 2017"
    val bullet_3 = "agreeing to go paperless"
    val bullet_4 = "a sole trader with income from one business"
    val bullet_5 = "you rent out a UK property"
    val bullet_6 = "a sole trader with income from one business and you rent out a UK property"
    val bullet_7 = "complete your 2016 to 2017 Self Assessment tax return and pay what you owe by 31 January 2018"
    val bullet_8 = "send your summary report for the 2017 to 2018 tax year by 31 January 2019"
    val bullet_9 = "use your business tax account from August 2017 to see your reports and what you might owe"
    val line_1 = "By signing up to HM Revenue and Customs secure service, you're helping to test a new way of working out your tax. You'll do this by:"
    val line_2 = "You can sign up if your current accounting period starts after 5 April 2017 and you're one of the following:"
    val line_3 = "If you have income other than those listed above, you won't be able to report it using this service yet."
    val line_4 = "You'll need to enter the user ID and password you got when you signed up to the Self Assessment online service."
    val line_5 = "You'll need to:"
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

  object RentUkProperty {
    val title = "Do you rent out a UK property?"
    val heading: String = title
    val line_1 = "This includes if you use a letting agency."
    val question = "Is this your only source of self-employed income?"
    val yes = "Yes"
    val no = "No"
  }

  object WorkForYourself {
    val title = "Do you work for yourself?"
    val heading = "Do you work for yourself?"
    val para1 = "This does not include if your business is a limited company or partnership."
  }

  object Property {

    object Income {
      val title = "How much was your income from property this year?"
      val heading: String = title
      val lt10k = "Less than £10,000"
      val ge10k = "£10,000 or more"
    }

  }

  object Business {

    object SoleTrader {
      val title = "Are you a sole trader?"
      val heading: String = title
      val line_1 = "As a sole trader, you run your own business as an individual. You can keep all your business's profits after you've paid tax on them. 'Sole trader' means you're responsible for the business, not that you have to work alone."
      val yes = "Yes - I'm a sole trader"
      val no = "No - I am a different type of business"
    }

    object MatchTaxYear {

      object SignUp {
        val title = "Does your accounting period match the tax year?"
        val heading: String = "Does your accounting period match the tax year?"
        val line1 = "The tax year runs from 6 April to 5 April. Your accounting period is usually the same, unless you chose different dates when you registered for Self Assessment."
      }

      object Registration {
        val title = "Do you want to match your accounting period to the tax year?"
        val heading: String = "Do you want to match your accounting period to the tax year?"
        val line1 = "The tax year runs from 6 April to 5 April. Most sole traders match their accounting period to the tax year."
        val line2 = "You can do this even if the tax year has already started."
      }

    }

    object AccountingPeriodPrior {
      val title = "Business accounting period"
      val heading: String = "Did your current accounting period start before 6 April 2017?"
      val accordion = "What's an accounting period?"
      val accordion_line1 = "This is the period that your latest income and expense records cover for Self Assessment."
      val accordion_line2 = "Your start date is usually 6 April (the same as the tax year), unless you selected a different date when you registered for Self Assessment."
      val yes = "Yes"
      val no = "No"
    }

    object RegisterNextAccountingPeriod {
      val title = "You can't use software to report your income yet"
      val heading = title
      val line_1 = "To report your income for your current accounting period, you'll have to submit a Self Assessment tax return."
      val line_2 = "You can sign up now and use software to record your income and expenses, but you won't be able to submit a report until your next accounting period."
      val button = "Continue to sign up"
      val signOut = "Sign out"
    }

  }

  object BusinessStartDate {
    val title = "Business start date"
    val heading = "When did your business start trading?"
    val exampleStartDate = "For example, 1 4 2018"
  }

  object AccountingPeriod {
    val title = "Business accounting period"
    val heading_signup = "Enter your accounting period dates"
    val heading_registration = "When is your current accounting period?"
    val heading_editMode = "Change your accounting period dates"
    val line_1_signup = "Your accounting period is usually 12 months. For example, 1 May 2017 to 30 April 2018."
    val line_1_registration = "Your accounting period is usually 12 months. For example, 6 April 2017 to 5 April 2018"
    val exampleStartDate_signup = "For example, 1 4 2018"
    val exampleEndDate_signup = "For example, 31 3 2019"
    val exampleStartDate_registration = "For example, 6 4 2017"
    val exampleEndDate_registration = "For example, 5 4 2018"
  }

  object CannotReportYet {
    val title = "You can't use software to report your Income Tax yet"
    val heading: String = title
    val linkText = "Self Assessment tax return"
    def para1(startDate: DateModel) = s"You can sign up and use software to record your income and expenses, but you can't send any reports until ${startDate.toOutputDateFormat}."
    val para2 = s"You need to send a $linkText instead."
  }

  object CanReportBusinessButNotPropertyYet {
    val title = "You can't use software to report your property income yet"
    val heading: String = title
    val para1 = "You can use software to report the work you do for yourself."
    val linkText = "send a Self Assessment tax return"
    val para2 = s"You can't use software to submit a report for your property income until 6 April 2018. You need to $linkText instead."
  }

  object CannotReportYetBothMisaligned {
    val title = "You can't use software to report your Income Tax yet"
    val heading: String = title
    val linkText = "send a Self Assessment tax return"
    val para1 = "You can sign up and use software to record your income and expenses, but you won't be able to submit a report for:"
    val bullet1 = "property income until 6 April 2018"
    def bullet2(startDate: DateModel) = s"sole trader income until ${startDate.toOutputDateFormat}"
    val para2 = s"You need to $linkText instead."
  }

  object AgentCannotReportYetBothMisaligned {
    val title = "Your client can't use software to report their Income Tax yet"
    val heading: String = title
    val linkText = "send a Self Assessment tax return"
    val para1 = "You can still sign this client up and use software to record their income and expenses, but they won't be able to submit a report for their:"
    val bullet1 = "property income until 6 April 2018"
    def bullet2(businessStartDate: DateModel) = s"sole trader income until ${businessStartDate.toOutputDateFormat}"
    val para2 = s"Your client still needs to $linkText."
  }

  object BusinessName {
    val title = "Business name"
    val heading: String = "What's the name of your business?"

    object SignUp {
      val line_1 = "This is the business name you used to register for Self Assessment. If your business doesn't have a name, enter your own name."
    }

    object Registration {
      val line_1 = "If your business doesn't have a name, enter your own name."
    }

  }

  object BusinessPhoneNumber {
    val title = "Business phone number"
    val heading: String = "What's your business telephone number?"
  }

  object BusinessAddress {

    object Lookup {
      val heading = "What's your business address?"
      val nameOrNimber = "House name or number"
      val submit = "Search address"
      val enterManually = "Enter UK address manually"
    }

    object Select {
      val title = "Choose an address"
      val heading = "Choose an address"
      val edit = "Edit address"

    }

    object Confirm {
      val heading = "What's your business address?"
      val change = "Change"
    }

    object Edit {
      val heading = "Enter your address"
      val addLine1 = "Address line 1"
      val addLine2 = "Address line 2"
      val addLine3 = "Address line 3"
    }

  }

  object AccountingMethod {
    val title = "Accounting method"
    val heading: String = "How do you record your income and expenses?"
    val accordion = "Show me an example"
    val accordion_line_1 = "You invoiced someone in March 2017 but didn't receive the money until May 2017. If you would tell HM Revenue and Customs you received this income in:"
    val accordion_bullet_1 = "May 2017, then you use 'cash basis' accounting"
    val accordion_bullet_2 = "March 2017, then you use 'accruals basis'"
    val cash = "Cash basis You record on the date you receive money or pay a bill. Many sole traders and small businesses use this method."
    val accruals = "Accruals basis You record on the date you send or receive an invoice, even if you don't receive or pay any money. This method is also called 'traditional accounting'."
  }

  object Terms {
    val title = "Terms of participation"
    val heading: String = title
    val line_1 = "By taking part in this trial, you agree to:"
    val bullet_1 = "use relevant software to record your income and expenses"
    val bullet_2 = "submit a report at least once every 3 months from the start of your accounting period"
    def bullet_3(taxStartYear: Int, taxEndYear: Int, taxDueYear: Int) = s"send your final report for the $taxStartYear to $taxEndYear tax year by 31 January $taxDueYear"
    val bullet_4 = "declare any other income sources and reliefs"
    val bullet_5 = "authorise any third party you use (such as your accountant) and have responsibility for any information they give to HMRC on your behalf"
    val bullet_6 = "tell HMRC if you want to stop trading or start a new business"
    val bullet_7 = "tell HMRC if you want to leave this trial"
    val line_2 = "These terms aren't contractual and you can stop taking part in the trial at any time."
    val button = "Accept and continue"
  }

  object Summary {
    val title = "Check your answers"
    val heading: String = title
    val heading_hidden = "before signing up"
    val h2 = "You've told us"
    val income_source = "Where does your income come from?"
    val rentUkProperty = "Do you rent out a UK property?"
    val onlySourceOfIncome = "Is this your only source of self-employed income?"
    val workForYourself = "Do you work for yourself?"

    object IncomeSource {
      val business = "Sole trader business"
      val property = "Property"
      val both = "Sole trader business and property"
    }

    val match_tax_year = "Does your accounting period match the tax year?"
    val accounting_period = s"Your accounting period dates"
    val accounting_period_registration = s"When's your current accounting period?"
    val accounting_period_month: Int => String = (month: Int) => s"$month month period"
    val business_name = "What's the name of your business?"
    val business_phone_number = "What's your business telephone number?"
    val business_address = "What's your business address?"
    val business_start_date = "When did your business start trading?"
    val income_type = "What method do you use for your accounting?"
    val other_income = "Do you have any other sources of income?"

    object AccountingMethod {
      val cash = "Cash basis"
      val accruals = "Accruals basis"
    }

    val contact_email = "Do you want to receive electronic communications from HMRC?"
    val confirm_and_sign_up = "Confirm and sign up"
  }

  object Timeout {
    val title = "Your session has timed out"
    val heading: String = title
    val returnToHome = """To sign up for quarterly reporting, you'll have to sign in using your Government Gateway ID."""
  }

  object AlreadyEnrolled {
    val title = "You've already signed up"
    val heading = "You've already signed up"
    val line1 = "Your sign in details are already in use."
  }

  object ClaimSubscription {
    val title = "You've already signed up for quarterly reporting"
    val heading = "You've already signed up for quarterly reporting"
  }

  object UserDetailsError {
    val title = "User match error"
    val heading = "We couldn't confirm your details"
    val line1 = "The details you've entered are not on our system."
  }

  object UserDetailsLockout {
    val title = "You've been locked out"
    val heading = "You've been locked out"

    def line1(testTime: String) = s"To sign up for quarterly reporting, you'll have to try again in $testTime."
  }

  object UserDetails {
    val title = "Enter your details"
    val heading = "Enter your details"
    val line1 = "We will attempt to match these details against information we currently hold."
    val field1 = "First name"
    val field2 = "Last name"
    val field3 = "National Insurance number"
    val field4 = "Date of birth"
    val formhint1_line1 = "For example, 'QQ 12 34 56 C'."
    val formhint2 = "For example, 10 12 1990"
  }

  object ConfirmUser {
    val title = "Confirm your details"
    val heading = "Check your answers"
    val heading_hidden = "before looking up your details"
    val h2 = "You've told us"
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
      val para2 = "After you send a report you'll get an Income Tax estimate. You can view your estimate and report deadlines in your:"
      val bul1 = "accounting software"
      val bul2 = "business tax account"
      val para4 = "It may take a few hours for them all to appear."
    }

    object Unauthorised {
      val title = "Sign up complete"
      val heading = "Sign up complete"
      val signOut = "Sign out"

      object whatHappensNext {
        val heading = "What happens next"
        val para1 = "Either you or your agent need to complete the steps below."
        val para2 = "It's important to choose a software package that can interact with your agent's."
        val list1 = "Choose accounting software if you haven't already."
        val list2 = "Sign in to the software with your Government Gateway details and authorise it to interact with HMRC."
        val list3 = "Add any income and expenses that you've already received or paid out."
        val list4 = "Record your future income and expenses using the software, then send HMRC a report at least every quarter."
        val list5 = "Add any other income sources in your final report, you need to send this report by 31 January."
        val para3 = "After you've sent a report you'll get an Income Tax estimate. You can view your estimate and report deadlines in your:"
        val bul1 = "accounting software"
        val bul2 = "business tax account"
        val para4 = "It may take a few hours for them all to appear."
      }

    }

  }

  object SignUpComplete {
    val title = "Sign up complete"
    val heading: String = title

    object whatHappensNext {
      val heading = "What happens next"
      val linkText = "Choose accounting software"
      val number1 = s"$linkText if you have not already."
      val number2 = "Sign in to the software with your Government Gateway details and authorise it to interact with HMRC."
      val number3 = "Add any income and expenses that you have already received or paid out."
      val number4 = "Record your future income and expenses using software, then send HMRC a report at least every quarter."
      val number5 = "Add any other income sources in your final report, you need to send this report by 31 January."
      val para1 = "After you have sent a report you will get an Income Tax estimate. You can view your estimate and report deadlines in your:"
      val bullet1 = "accounting software"
      val bullet2 = "business tax account"
      val para2 = "It may take a few hours for your information to appear."
    }

  }


  object AffinityGroup {
    val title = "You can't use this service"
    val heading = "You can't use this service"
    val line1 = "You can only use this service if you have an individual Government Gateway account."
    val line2 = """To sign up for quarterly reporting, you'll need to sign in using a different type of account."""

    object Agent {
      val linkId: String = "agent-service"
      val linkText = "use our agent service."
      val line1 = s"To sign up for quarterly reporting with these sign in details, you need to $linkText"
    }

  }

  object Error {

    object UserDetails {
      val firstNameEmpty = "Enter your first name"
      val lastNameEmpty = "Enter your last name"
      val firstNameInvalid = "Enter a valid first name"
      val lastNameInvalid = "Enter a valid last name"
      val firstNameMaxLength = "Enter a first name that's 105 characters or less"
      val lastNameMaxLength = "Enter a last name that's 105 characters or less"
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

      object MatchTaxYear {
        val empty = "You must select an option to continue"
        val invalid = "You must select an option to continue"
      }

      object AccountingPeriodPrior {
        val empty = "You must select an option to continue"
        val invalid = "You must select an option to continue"
      }

      object RegisterNextAccountingPeriod {
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
      val end_violation = "Enter an end date that's after the start date"
      val end_past = "Enter an end date that's the present date or a future date"
    }

    object BusinessAccountingPeriod {
      val minStartDate = "Enter the start date of your next accounting period."
      val maxEndDate = "You must provide an end date that is not more than 24 months after your start date"
    }

    object BusinessName {
      val empty = "You must enter your Business name"
      val maxLength = "You can't enter more than 105 characters for your Business name"
      val invalid = "The business name contains invalid characters"
    }

    object BusinessPhoneNumber {
      val empty = "You must enter your business phone number"
      val maxLength = "You can't enter more than 24 characters for your business phone number"
      val invalid = "The business phone number contains invalid characters"
    }

    object ContactEmail {
      val empty = "Please enter a contact email"
      val maxLength = "The email is too long"
      val invalid = "The email is invalid"
    }

    object AccountingMethod {
      val empty = "You must select an option to continue"
      val invalid = "You must select an option to continue"
    }

    object NotEligible {
      val empty = "You must select an option to continue"
      val invalid = "You must select an option to continue"
    }

    object OtherIncome {
      val empty = "You must select an option to continue"
      val invalid = "You must select an option to continue"
    }

    object IncomeSource {
      val empty = "You must select an option to continue"
      val invalid = "You must select an option to continue"
    }

    object WorkForYourself {
      val empty = "You must select an option to continue"
      val invalid = "You must select an option to continue"
    }

    object RentUkProperty {
      val emptyRentUkProperty = "Select yes if you rent out a UK property"
      val invalidRentUkProperty = "Select yes if you rent out a UK property"
      val emptyOnlyIncomeSource = "Select yes if this is your only source of self-employed income"
      val invalidOnlyIncomeSource = "Select yes if this is your only source of self-employed income"
    }

    object Terms {
      val empty = "You must accept the terms of participation to continue"
    }

    object ExitSurvey {
      val maxLength = "You can't enter more than 1200 characters for your feedback"
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
    val title = "You need to register for Self Assessment"
    val heading = title
    val linkText = "register for Self Assessment"
    val line1 = s"Before you can sign up to use software to report your Income Tax, you need to $linkText."

    object Agent {
      val title = "Your client is not registered for Self Assessment"
      val heading = title
      val linkText = "register for Self Assessment"
      val line1 = s"To use this service, your client needs to $linkText."
    }

  }

  object MainIncomeError {
    val title = "You can't sign up for quarterly reporting yet"
    val heading = "You can't sign up for quarterly reporting yet"
    val para1 = "At the moment, you can only sign up if you're one of the following:"
    val para2 = "You'll be able to send quarterly reports for other income later in the year."
    val bullet1 = "a sole trader with income from one business"
    val bullet2 = "someone who rents out a UK property"
    val bullet3 = "a sole trader with income from one business and you rent out a UK property"
  }

  object OtherIncomeError {
    val title = "Tell us about your other income sources in your final report"
    val heading: String = title
    val para1 = "You only need to send quarterly reports on income you earn from either:"
    val bullet1 = "working for yourself"
    val bullet2 = "renting out a UK property"
    val bullet3 = "working for yourself and renting out a UK property"
    val para2 = "You need to submit your final report by 31 January."
  }

  object OtherIncome {
    val title = "Do you have any other sources of income?"
    val heading = "Do you have any other sources of income?"
    val para1 = "This could include:"
    val bullet1 = "employment that isn't your sole trader business"
    val bullet2 = "UK pensions or annuities"
    val bullet3 = "taxable state benefits"
    val bullet4 = "employment or investments from outside the UK"
    val bullet5 = "capital gains"
    val yes = "Yes"
    val no = "No"
  }

  object ExitSurvey {
    val title = "Give feedback"
    val heading = "Give feedback"
    val line_1 = "Please don't include any personal or financial information, for example your National Insurance or credit card numbers."
    val line_2 = "We use your feedback to make our services better."
    val submit = "Send feedback"

    object Q1 {
      val question = "Overall, how did you feel about the service you received today?"
      val option_1 = "Very satisfied"
      val option_2 = "Satisfied"
      val option_3 = "Neither satisfied or dissatisfied"
      val option_4 = "Dissatisfied"
      val option_5 = "Very dissatisfied"
    }

    object Q2 {
      val question = "How could we improve this service?"
    }

  }

  object ThankYou {
    val title = "Thank you"
    val heading = "Thank you"
    val line_1 = "Your feedback will help us improve this service."
    val gotoGovUk = "Go to the GOV.UK page"
  }

  object IvFailed {
    val title = "We're unable to confirm your identity"
    val heading = title
    val line_1 = "To help protect your data, you can only sign up to report your income and expenses quarterly once we've confirmed who you are."
    val hmrcLink = "HM Revenue and Customs (opens in new window)"
    val line_2 = s"If you can't confirm your identity and you have a query you can contact $hmrcLink to get help."
    val tryAgainLink = "Try to confirm your identity again."
  }

  object UnauthorisedAgent {

    object AgentNotAuthorised {
      def title = s"You have not authorised your agent"

      def heading = title

      val line_1 = "We've deleted any information they've asked to submit for you."
    }

    object AuthoriseAgent {
      def title = s"Do you authorise your agent to use software to report your Income Tax?"

      def heading = s"Do you authorise your agent to use software to report your Income Tax?"

      val yes = "Yes"
      val no = "No"
    }

  }

  object CannotSignUp {
    val title = "You can't use this service"
    val heading: String = title
    val linktext = "send a Self Assessment tax return"
    val line1 = "You can only use software to report your Income Tax if you either:"
    val bullet1 = "work for yourself"
    val bullet2 = "rent out UK property"
    val bullet3 = "work for yourself and rent out UK property"
    val line2 = s"You need to $linktext instead."
  }

  object CannotUseService {
    val title = "You cannot use this service"
    val heading: String = title
    val line1 = "You can only sign up if you are an administrator."
  }

  object UnplannedOutage {
    val title = "Sorry, there is a problem with the service"
    val heading: String = title
    val line1 = "Try again later."
    val line2 = "In the meantime:"
    val link1 = "Income Tax main page"
    val link2 = "Income Tax related content"
    val bullet1 = s"go to the $link1"
    val bullet2 = s"get $link2"
  }

}
