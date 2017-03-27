/*
 * Copyright 2017 HM Revenue & Customs
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

object MessageLookup {

  object Base {
    val continue = "Continue"
    val submit = "Submit"
    val update = "Update"
    val signOut = "Sign Out"
    val startDate = "Start date"
    val endDate = "End date"
    val day = "Day"
    val month = "Month"
    val year = "Year"
    val errorHeading = "Error Summary"
    val change = "Change"
    val where_can_i_get_this_information = "Where can I get this information"
    val signUp = "Sign up"
  }

  object FrontPage {
    val title = "Sign up to report your income and expenses quarterly"
    val heading = title
    val h2 = "What happens next"
    val bullet_1 = "using your accounting software to record your income and expenses"
    val bullet_2 = "sending details to us regularly from May 2017"
    val bullet_3 = "you're a sole trader"
    val bullet_4 = "you rent out a UK property"
    val bullet_5 = "you're a sole trader and rent out a UK property"
    val bullet_6 = "you're willing to go paperless"
    val bullet_7 = "your current accounting period starts on or after 1 April 2017"
    val bullet_8 = "keep using accounting software to keep your records and send regular reports"
    val bullet_9 = "send your summary report for the tax year 2017 to 2018 by 31 January 2019 at the latest"
    val bullet_10 = "a reference number to you may need if you contact us"
    val bullet_11 = "an estimate of what you owe every time you send a report"
    val bullet_12 = "use your business tax account to look at your reports from June 2017"
    val bullet_13 = "choose to pay as you go"
    val line_1 = "Thank you for agreeing to take part in this HM Revenue and Customs pilot."
    val line_2 = "By signing up you're helping us to test a new way of working out your tax. You'll do this by:"
    val line_3 = "Soon this will replace your annual tax return but you'll still need to send us your 2016 to 2017 tax return and pay any tax you owe by 31 January 2018."
    val line_4 = "Your information will be secure. You'll need to go through security checks first to make sure no one else can access your personal details."
    val line_5 = "If at any time during the pilot you decide not to take part you can stop but you may need to start using this way of working out your tax from April 2018."
    val line_6 = "You can sign up if:"
    val line_7 = "Use the user ID and password you got when you signed up to the Self Assessment online service."
    val line_8 = "You'll need to:"
    val line_9 = "You'll get:"
    val line_10 = "You can"

  }

  object PreferencesCallBack {
    val title = "Do you want to continue with your registration?"
    val heading: String = title
    val line_1: String = "In order to continue with your registration, you must set up a contact email address and agree to go paperless with HMRC."
    val legend: String = title
    val yes: String = "Yes - sign up to go paperless"
    val no: String = "No - sign me out"
  }

  object IncomeSource {
    val title = "Where does your income come from?"
    val heading: String = title
    val business = "Your sole trader business"
    val property = "Renting out a UK property"
    val both = "Your sole trader business and renting out a UK property"
    val other = "Other"
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
      val title = "Did your current accounting period start before 1 April 2017?"
      val heading: String = title
      val line_1 = "1 April 2017 is the date this service started."
      val accordion = "What is an accounting period?"
      val accordion_line1 = "This is the period that your latest income and expense records cover for Self Assessment."
      val accordion_line2 = "Your start date is usually 6 April (the same as the tax year), unless you selected a different date when you registered for Self Assessment."
      val yes = "Yes"
      val no = "No"
    }

    object RegisterNextAccountingPeriod {
      val title = "Do you want to sign up for your next accounting period?"
      val heading = title
      val line_1 = "You can't send quarterly reports yet, as your current accounting period started before 1 April 2017."
      val line_2 = "You can sign up for your next accounting period, but can't send quarterly reports until this begins."
      val yes = "Yes"
      val no = "No - sign out"
    }

  }

  object AccountingPeriod {
    val title = "Business accounting period"
    val heading_current = "What are the dates of your current accounting period?"
    val heading_next = "When is your next accounting period?"
    val line_1 = "Your accounting period is usually 6 April to 5 April (the same as the tax year), unless you selected different dates when you registered for Self Assessment."
  }

  object ContactEmail {
    val title = "Enter your email address"
    val heading: String = title
    val line_1 = "We'll use this to get in touch with updates about your estimated Income Tax calculations."
  }

  object BusinessName {
    val title = "Business name"
    val heading: String = "What's the name of your business?"
    val line_1 = "This is the business name you used to register for Self Assessment. If your business doesn't have a name, enter your own name."
  }

  object AccountingMethod {
    val title = "Accounting method"
    val heading: String = "What method do you use for your accounting?"
    val accordion = "Show me an example"
    val accordion_line_1 = "Your accounting period is 6 April 2016 to 5 April 2017. You invoiced someone in March 2017 but didn't receive the money until May 2017."
    val accordion_line_2 = "If you would record this income in:"
    val accordion_bullet_1 = "May 2017, then the method you use is ‘cash basis' accounting"
    val accordion_bullet_2 = "March 2017, then the method you use is ‘accruals basis' accounting"
    val cash = "Cash basis Your end-of-year records show the money you've received or paid out in the tax year. Most sole traders and small businesses use this method."
    val accruals = "Accruals basis Your end-of-year accounts record income and expenses in the tax year that they occur, regardless of when you receive or make a payment."
  }

  object Terms {
    val title = "Terms of participation"
    val heading: String = title
    val line_1 = "By accepting the terms of participation, you confirm that you've read and understood the terms of the trial. You'll have been sent these terms separately."
    val checkbox = "I accept the terms of participation"
  }

  object Summary {
    val title = "Check your answers"
    val heading: String = title
    val heading_hidden = "before signing up"
    val h2 = "You've told us"
    val income_source = "Where does your income come from?"

    object IncomeSource {
      val business = "Business"
      val property = "Property"
      val both = "Business and property"
    }

    val accounting_period_prior = "Did your current accounting period start before 1 April 2017?"
    val accounting_period =  s"Your accounting period dates:"
    val accounting_period_month: Int => String = (month: Int) => s"$month month period"
    val business_name = "What's the name of your business?"
    val income_type = "What method do you use for your accounting?"

    object AccountingMethod {
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
    val returnToHome = """To register to send digital tax updates, you'll have to sign back in using your Government Gateway ID."""
  }

  object AlreadyEnrolled {
    val title = "You've already signed up"
    val heading = "You've already signed up"
    val para1 = "Your Government Gateway ID is already signed up for regular reporting."
  }

  object Confirmation {
    val title = "Confirmation page"
    val heading = "Success"
    val banner_line1_1 = "You've successfully signed up for quarterly reporting."
    val banner_line1_2 = "Your reference number is:"

    object whatHappensNext {
      val heading = "What happens next"
      val para1 = "Now that you've signed up to trial this service, you must:"
      val bullet1 = "make a note of your reference number. If you forget your Government Gateway account details, we can use this reference number to help you retrieve them"
      val bullet2 = "download accounting software. You'll need to use this software to send HMRC reports about your income and expenditure"
      val bullet3 = "activate your Business Tax Account"
      val bullet4 = "report at least once every 3 months from the start of your accounting period"
      val bullet5 = "report any outstanding records you may have for your current accounting period"
      val para2 = "After you send a report, HMRC will calculate an estimate of your Income Tax. You can see these estimates from your accounting software after your first report, or through your Business Tax Account from July 2017"
    }

  }

  object Error {

    object BackToPreferences {
      val empty = "You must select an option to continue"
    }

    object Business {

      object SoleTrader {
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

    object BusinessAccountingPeriod {
      val minStartDate = "You can't enter a start date before 1 April 2017"
      val maxEndDate = "You must provide an end date that is not more than 24 months after your start date"
    }

    object BusinessName {
      val empty = "You must enter your Business name"
      val maxLength = "You can't enter more than 105 characters for your Business name"
      val invalid = "The business name contains invalid characters"
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

    object Terms {
      val empty = "You must confirm T&C's to continue"
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
    val title = "You can't sign up for quarterly reporting yet"
    val heading = title
    val line1 = "You can sign up later in the year when this service is available to you."
  }

  object ThrottleLimit {
    val title = "Service is unavailable due to maintenance"
    val heading = title
    val line1 = "Please try again later."
  }

  object MainIncomeError {
    val title = "You can't sign up for quarterly reporting yet"
    val heading = "You can't sign up for quarterly reporting yet"
    val para1 = "At the moment, you can only sign up if you're one of the following:"
    val para2 = "You can sign up later in the year when this service is available to you."
    val bullet1 = "a sole trader with income from one business"
    val bullet2 = "someone who rents out a UK property"
    val bullet3 = "a sole trader with income from one business and you rent out a UK property"
  }

  object OtherIncomeError {
    val title = "You can only send quarterly reports on part of your income"
    val heading: String = title
    val para1 = "As this service is currently a trial, you can only send quarterly reports on income from your sole trader business, renting out a UK property, or both."
    val para2 = "You can send quarterly reports including your other income later in the year, when this service becomes available."
  }

  object OtherIncome {
    val title = "Do you have any other sources of income?"
    val heading = "Do you have any other sources of income?"
    val para1 = "This could include:"
    val bullet1 = "employment that isn't your sole trader business"
    val bullet2 = "UK pensions or annuities"
    val bullet3 = "state benefits"
    val bullet4 = "employment or investments from outside the UK"
    val bullet5 = "capital gains"
    val bullet6 = "the 'Rent a Room Scheme'"
    val yes = "Yes"
    val no = "No"
  }

}
