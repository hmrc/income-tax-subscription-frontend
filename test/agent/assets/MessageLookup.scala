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

package agent.assets

object MessageLookup {

  object Base {
    val continue = "Continue"
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
    val startNow = "Start now"
    val dateOfBirth = "Date of birth"
    val goBack = "Go back"
    val tryAgain = "Try again"
    val addAnother = "Sign up another client"
    val back = "Back"
  }

  object FrontPage {
    val title = "Sign up to report your client's income and expenses quarterly"
    val heading = title
    val subHeading_1 = "Sign up using Government Gateway"
    val subHeading_2 = "Before you start"

    val linkText_1 = "create an Agent Services account"
    val linkText_2 = "authorise you as their agent"

    val bullet_1 = "using your accounting software to record your client's income and expenses"
    val bullet_2 = "sending details to us regularly from July 2017"
    val bullet_3 = "a sole trader with income from one business"
    val bullet_4 = "someone who rents out a UK property"
    val bullet_5 = "a sole trader with income from one business and they rent out a UK property"
    val bullet_6 = "full name"
    val bullet_7 = "date of birth"
    val bullet_8 = "National Insurance Number"
    val bullet_9 = s"you need to $linkText_1"
    val bullet_10 = s"your client needs to $linkText_2"

    val para_1 = "By signing up to HM Revenue and Customs secure service, you're helping to test a new way of working out your client's tax. You'll do this by:"
    val para_2 = "You can sign up your client if their current accounting period starts after 5 April 2017 and they're one of the following:"
    val para_3 = "If your client has any income other than those listed above, you won't be able to report it using this service yet."
    val para_4 = "You'll need to enter the user ID and password you got when you created your Agent Services account."
    val para_5 = "You'll need your client's:"
    val para_6 = "To use this service:"
  }

  object PreferencesCallBack {
    val title = "Do you want to continue?"
    val heading: String = "To sign up, you must allow HMRC to send you electronic communications"
    val legend: String = "To sign up for quarterly reporting, you must allow HMRC to send you electronic communications"
    val yes: String = "Continue to sign up"
    val no: String = "Sign out"
  }

  object IncomeSource {
    val title = "Select your client's income type"
    val heading: String = title
    val business = "Sole trader business"
    val property = "Renting out a UK property"
    val both = "Their sole trader business and renting out a UK property"
    val other = "Other income sources"
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

    object AccountingPeriodPrior {
      val title = "Business accounting period"
      val heading: String = "Did your client's current accounting period start before 6 April 2017?"
      val yes = "Yes"
      val no = "No"
    }

    object RegisterNextAccountingPeriod {
      val title = "You can't send quarterly reports yet"
      val heading = title
      val line_1 = "You can sign your client up now, but you won't be able to send any reports until their next accounting period begins."
      val button = "Continue to sign up"
    }

    object MatchTaxYear {
      val heading = "Does your client's accounting period match the tax year?"
      val para1 = "The tax year runs from 6 April to 5 April."
      val para2 = "Your client's accounting period is the same unless you have previously chosen different dates."
      val yes = "Yes"
      val no = "No"
    }

    object WhatYearToSignUp {
      val heading = "What tax year do you want to sign your client up for?"
      val line1 = "You can sign your client up for the current year or the next year."
      def example1(taxYearStart: String, taxYearEnd: String) = s"For example, if your client's accounting period runs from 6 April $taxYearStart to 5 April $taxYearEnd, you will sign your client up for the current year $taxYearStart to $taxYearEnd."
      def example2(taxYearStart: String, taxYearEnd: String) = s"If your client's accounting period runs from 6 April $taxYearStart to 5 April $taxYearEnd, you will sign your client up for next year $taxYearStart to $taxYearEnd."
      def option1(taxYearStart: String, taxYearEnd: String) = s"Current tax year ($taxYearStart to $taxYearEnd)"
      def option2(taxYearStart: String, taxYearEnd: String) = s"Next tax year ($taxYearStart to $taxYearEnd)"
    }
  }

  object AccountingPeriod {
    val title = "What accounting period are you signing your client up for?"
    val heading = "What accounting period are you signing your client up for?"
    val line1 = "For example, if your accounting period is 1 August 2019 to 31 July 2020, you will be signing up for the 2020 to 2021 tax year."
    def exampleStartDate(year: Int): String = s"For example, 1 4 ${year.toString}"
    def exampleEndDate(year: Int): String = s"For example, 31 3 ${year.toString}"
  }

  object ContactEmail {
    val title = "Enter your email address"
    val heading: String = title
    val line_1 = "We'll use this to get in touch with updates about your estimated Income Tax calculations."
  }

  object BusinessName {
    val title = "Business name"
    val heading: String = "What's the name of your client's business?"
    val line_1 = "This is the business name they used to register for Self Assessment. If their business doesn't have a name, enter your client's name."
  }

  object AccountingMethod {
    val title = "What accounting method does your client use for their self-employed business?"
    val heading: String = "What accounting method does your client use for their self-employed business?"
    val cash = "Cash basis"
    val accruals = "Accruals basis"
  }

  object PropertyAccountingMethod {
    val title = "What accounting method does your client use for their property business?"
    val heading: String = "What accounting method does your client use for their property business?"
    val cash = "Cash basis"
    val accruals = "Accruals basis"
  }

  object Terms {
    val title = "Terms of participation"
    val heading: String = title
    val line_1 = "By taking part in this trial, you agree that either you or your client will:"
    val point_1 = "use relevant software to record your client's income and expenses"
    val point_2 = "submit a report at least once every 3 months from the start of your client's accounting period"

    def point_3(taxStartYear: Int, taxEndYear: Int, taxDueYear: Int) = s"send their final report for the $taxStartYear to $taxEndYear tax year by 31 January $taxDueYear"

    val point_4 = "declare any other income sources and reliefs"
    val point_5 = "tell HMRC if your client stops trading or starts a new business"
    val point_6 = "tell HMRC if you want to leave this trial"
    val line_2 = "These terms are not contractual and your client can stop taking part in this trial at any time."
    val button = "Accept and continue"
  }

  object Summary {
    val title = "Check your answers"
    val heading: String = title
    val heading_hidden = "before signing up"
    val h2 = "You've told us"
    val income_source = "Client's income type"

    object IncomeSource {
      val business = "Sole trader business"
      val property = "Property"
      val both = "Sole trader business and property"
    }

    val accounting_period_prior = "Did your current accounting period start before 1 April 2017?"
    val accounting_period = s"Your client's accounting period dates"
    val accounting_period_month: Int => String = (month: Int) => s"$month month period"
    val business_name = "Business name"
    val income_type = "Accounting method for their self-employed business"
    val income_type_property = "Accounting method for their property business"
    val other_income = "Does your client have any other sources of income?"
    val match_tax_year = "Accounting period matches tax year"

    object AccountingMethod {
      val cash = "Cash basis"
      val accruals = "Traditional accounting"
    }

    object AccountingMethodProperty {
      val cash = "Cash basis"
      val accruals = "Accruals basis"
    }

    val contact_email = "Do you want to receive electronic communications from HMRC?"
    val terms = "Terms of participation"
    val terms_agreed = "I agree"
    val confirm_and_sign_up = "Confirm and sign up"
  }

  object Timeout {
    val title = "Your session has timed out"
    val heading = "Your session has timed out"
    val returnToHome = """To sign your client up for quarterly reporting, you'll have to sign in using your Government Gateway ID."""
  }

  object ClientAlreadySubscribed {
    val title = "Your client has already signed up"
    val heading = "Your client has already signed up"
    val para1 = "This client's details are already in use."
  }

  object NoClientRelationship {
    val title = "You're not authorised for this client"
    val heading = "You're not authorised for this client"
    val para1 = "To send quarterly reports, your client must authorise you as their agent."
  }

  object Confirmation {
    val title = "Sign up complete"
    val heading: String = title

    object whatHappensNext {
      val heading = "What happens next"
      val para1 = "Either you or your client need to complete the steps below."
      val para2 = "It's important for your client to choose a software package that can interact with yours."
      val linkText = "Choose accounting software"
      val number1 = s"$linkText if you haven't already."
      val number2 = "Sign in to the software with your Government Gateway details and authorise it to interact with HMRC."
      val number3 = "Add any income and expenses that your client has already received or paid out."
      val number4 = "Record your client's future income and expenses using the software, then send HMRC a report at least every quarter."
      val number5 = "Add any other income sources in their final report, your client needs to send this report by 31 January."
      val para3 = "After you've sent a report your client will get an Income Tax estimate. They can view their estimate and report deadlines in their:"
      val bullet1 = "accounting software"
      val bullet2 = "business tax account"
    }

    object giveUsFeedback {
      val heading = "Give us feedback"
      val para1 = "Your feedback helps us improve."
      val link = "What did you think of this service?"
    }

  }

  object SignUpComplete {
    val title = "You have signed up your client for the Use software to send Income Tax updates pilot"
    val heading: String = title

    object whatNext {
      val heading = "What you need to do next"
      val para1 = "Either you or your client must complete the steps below. If you both use software, it's important to choose packages that can interact with each other."
      val linkText = "Choose relevant software (opens in a new window)"
      val number1 = s"$linkText if you haven't already."
      val number2 = "Allow your software to interact with HMRC."
      val number3 = "Add all income and expenses your client has already received or paid out from the start of their tax year into the software."
      val number4 = "Continue to record your client's income and expenses using software and send a quarterly report to HMRC."
      val number5 = "Send your client's final report by 31 January after the end of the tax year you're signing them up for."
      val para2 = "After you've sent a report, your client will get an Income Tax estimate. They can see what they owe for the tax year after you’ve sent their final report."
    }

  }

  object UnauthorisedAgentConfirmation {
    val title = "Send client link"
    val heading = "Send your client this link"
    val url = "www.tax.service.gov.uk/report-quarterly/income-and-expenses/sign-up/"

    object whatHappensNext {
      val heading = "What happens next"
      val para1 = "You must send your client this link for them to authorise you as their agent. They must do this within 10 days. You still need to:"
      val bullet1 = "Choose accounting software if you haven't already."
      val bullet2 = "Sign in to the software with your Government Gateway details and authorise it to interact with HMRC."
      val para2 = "If your client doesn't authorise you within 10 days, you'll need to invite them again."
    }

    object whenAuthorised {
      val heading = "When you're authorised"
      val linkText = "business tax account"
      val para1 = "Either you or your client need to complete the steps below."
      val number1 = "Add any income and expenses that your client has already received or paid out"
      val number2 = "Record their future income and expenses using the software, then send HMRC a report at least every quarter"
      val number3 = "Add any other income sources in your client's final report, they need to send this report by 31 January"
    }

  }

  object Error {

    object BackToPreferences {
      val empty = "You must select an option to continue"
    }

    object Business {

      object AccountingPeriodPrior {
        val empty = "Select yes if their accounting period started before 6 April 2017"
        val invalid = "Select yes if their accounting period started before 6 April 2017"
      }

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

    object DOBDate {
      val empty = "Enter a date of birth"
      val invalid_chars = "Enter a date of birth using numbers 0 to 9"
      val invalid = "Enter a real date of birth"
    }


    object BusinessAccountingPeriod {
      val minStartDate = "The start date of your accounting period must be in the future."
      val maxEndDate = "Enter an end date that's less than 24 months after your start date"
    }

    object BusinessName {
      val empty = "Enter your client's business name"
      val maxLength = "Enter a business name that's 105 characters or less"
      val invalid = "Enter a valid business name"
    }

    object ContactEmail {
      val empty = "Please enter a contact email"
      val maxLength = "The email is too long"
      val invalid = "The email is invalid"
    }

    object AccountingMethod {
      val empty = "Select how your client records their income and expenses for their self-employed business"
      val invalid = "Select how your client records their income and expenses for their self-employed business"
    }

    object NotEligible {
      val empty = "You must select an option to continue"
      val invalid = "You must select an option to continue"
    }

    object OtherIncome {
      val empty = "Select yes if your client has other income sources"
      val invalid = "Select yes if your client has other income sources"
    }

    object IncomeSource {
      val empty = "Select an income type"
      val invalid = "Select an income type"
    }

    object Terms {
      val empty = "You must accept the terms of participation to continue"
    }

    object ClientDetails {
      val firstNameEmpty = "Enter your client's first name"
      val lastNameEmpty = "Enter your client's last name"
      val firstNameInvalid = "Enter a valid first name"
      val lastNameInvalid = "Enter a valid last name"
      val firstNameMaxLength = "Enter a first name that's 105 characters or less"
      val lastNameMaxLength = "Enter a last name that's 105 characters or less"
    }

    object Nino {
      val empty = "Enter your client's National Insurance number"
      val invalid = "Enter a valid National Insurance number"
    }

  }

  object Eligible {
    val title = "You can send digital updates"
    val heading: String = title
    val line_1 = "Your answers mean you should be able to start sending HMRC digital updates after you sign up."
    val line_2 = "You just need to enter a few more details."
  }

  object Not_Eligible {
    val title = "You can't send digital updates yet"
    val heading: String = title
    val line_1 = "Your answers mean you're not eligible to send digital updates to HMRC right now."
    val line_2 = "You can still sign up for the service. HMRC are working to make digital updates available for all sole traders in the coming months. If you sign up now, you will be able to send digital updates to HMRC when you become eligible."
    val line_3 = "If you choose to sign up, we'll ask you a few questions about your income and contact details."
    val signUp = "I want to sign up"
    val signOut = "I don't want to sign up - sign me out"
    val question = "Choose what you would like to do next"
  }

  object NoNino {
    val title = "Your client can't use this service yet"
    val heading = title
    val line1 = "This client can sign up later in the year."
  }

  object MainIncomeError {
    val title = "You can't sign up your client yet"
    val heading = "You can't sign up your client yet"
    val para1 = "At the moment, you can only sign up if your client is one of the following:"
    val para2 = "You'll be able to use this reporting method for your client later in the year."
    val bullet1 = "a sole trader with income from one business"
    val bullet2 = "someone who rents out a UK property"
    val bullet3 = "a sole trader with income from one business and they rent out a UK property"
  }

  object OtherIncomeError {
    val title = "You can use software to report your client's Income Tax"
    val heading: String = title
    val para1 = "You only need to send quarterly reports on your client's income from either:"
    val para2 = "You can tell us about your client's other income sources in their final summary report. Your client needs to submit this report by 31 January."
    val bullet1 = "their sole trader business"
    val bullet2 = "renting out a UK property"
    val bullet3 = "their sole trader business and renting out a UK property"
  }

  object OtherIncome {
    val title = "Does your client have any other sources of income?"
    val heading = "Does your client have any other sources of income?"
    val para1 = "This could include:"
    val bullet1Default = "employment that isn't their sole trader business"
    val bullet1Property = "employment"
    val bullet2 = "UK pensions or annuities"
    val bullet3 = "taxable state benefits"
    val bullet4 = "employment or investments from outside the UK"
    val bullet5 = "capital gains"
    val yes = "Yes"
    val no = "No"
  }

  object ClientCannotReportYet {
    val title = "Your client can't use software to report their Income Tax yet"
    val heading: String = title
    val linkText = "send a Self Assessment tax return"
    val para1 = "You can still sign this client up and use software to record their income and expenses, but they won't be able to submit a report until 6 April 2018."
    val para2 = s"Your client still needs to $linkText."
  }

  object ClientCannotReportPropertyYet {
    val title = "You can't use software to report your client's property income yet"
    val heading: String = title
    val linkText = "send a Self Assessment tax return"
    val para1 = "You can use software to report your client's sole trader income."
    val para2 = s"They won't be able to submit a report for their property income until 6 April 2018. Your client still needs to $linkText."
  }


  object NotEnrolledAgentServices {
    val title = "You can't use this service yet"
    val heading = title
    val linkText = "set up an agent services account"
    val para1 = s"To use this service, you need to $linkText."
  }

  object ClientDetailsError {
    val title = "There's a problem"
    val heading = "There's a problem"
    val line1 = "The details you've entered are not on our system."
  }

  object ClientDetailsLockout {
    val title = "You've been locked out"
    val heading = "You've been locked out"

    def line1(testTime: String) = s"To sign your client up for quarterly reporting, you'll have to try again in $testTime."
  }

  object ClientDetails {
    val title = "Enter your client's details"
    val heading = "Enter your client's details"
    val line1 = "We will attempt to match these details against information we currently hold."
    val field1 = "First name"
    val field2 = "Last name"
    val field3 = "National Insurance number"
    val field4 = "Date of birth"
    val formhint1_line1 = "For example, 'QQ 12 34 56 C'."
    val formhint2 = "For example, 10 12 1990"
  }

  object AgentNotAuthorisedError {
    val title = "You're not authorised for this client"
    val heading: String = title
    val para1 = "You can still sign up your client, but we'll hold their information until they've authorised you as their agent."
  }

  object ConfirmClient {
    val title = "Confirm your client"
    val heading = "Check your answers"
    val heading_hidden = "before looking up your client"
    val h2 = "You've told us"
    val firstName = "First name"
    val lastName = "Last name"
    val nino = "National Insurance number"
    val dob = "Date of birth"
  }

  object ExitSurvey {
    val title = "Give feedback"
    val heading = "Give feedback"
    val line_1 = "Please don't include any personal or financial information, for example your National Insurance or credit card numbers."
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

}
