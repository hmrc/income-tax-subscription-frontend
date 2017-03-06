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
    val signout = "Sign Out"
    val startDate = "Start date"
    val endDate = "End date"
    val day = "Day"
    val month = "Month"
    val year = "Year"
    val errorHeading = "Error Summary"
    val chage = "Change"
    val where_can_i_get_this_information = "Where can I get this information"
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
    val business = "My business"
    val property = "Property (for example, renting out a property)"
    val both = "Both business and property"
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
      val yes = "Yes - I’m a sole trader"
      val no = "No - I am a different type of business"
    }

  }

  object AccountingPeriod {
    val title = "Business accounting period"
    val heading = "Enter your accounting period dates"
    val line_1 = "This is the period your business accounts cover."
    val line_2 = "You will only be able to send digital updates to HMRC if your accounting period starts on or after 1 April 2017. If it starts before this date, you will still be able to register, but you will have to report your income and expenditure in a Self Assessment tax return."
    val hint = "Help me find my accounting period dates"

    object Hint {
      val line_1 = "Your accounting period start date is usually the day after the end of your previous accounting period, or 'year end'. For example, if you made your accounts up to 5 April 2017, your next period will start on 6 April 2017."
    }

  }

  object ContactEmail {
    val title = "Enter your email address"
    val heading: String = title
    val line_1 = "We’ll use this to get in touch with updates about your estimated Income Tax calculations."
  }

  object BusinessName {
    val title = "Business name"
    val heading: String = "What is your business name?"
    val line_1 = "This is the business name you gave when you registered for Self Assessment."
  }

  object BusinessIncomeType {
    val title = "Accounting method"
    val heading: String = "Which accounting method do you use?"
    val line_1 = "This is how you keep records of your business income and expenses for your tax return."
    val cash = "Cash basis accounting You only record income or expenses when you receive money or pay a bill"
    val accruals = "Traditional accounting (‘accruals basis’) You record income and expenses by the date you invoiced or were billed"
  }

  object Terms {
    val title = "Terms and conditions"
    val heading: String = title
    val line_1 = "To use this service you must accept the terms and conditions."
    val line_2 = "By accepting the terms and conditions, you agree:"
    val li_1 = "that HMRC can use the email address you have provided to contact you about your digital tax updates"
    val li_2 = "to use accounting software to send digital income and expenditure updates to HMRC at least every 3 months from the start of your accounting period"
    val li_3 = "to contact HMRC immediately if you start a new business or sell or close down your current business"
    val checkbox = "I accept the terms and conditions"
  }

  object Summary {
    val title = "Check your answers"
    val heading: String = title
    val line_1 = "You can edit your answers if you need to. When you're ready, submit your answers to sign up to send HMRC digital income and expenditure updates."
    val h2 = "Now send your application"
    val line_2 = "By submitting this notification you are confirming that, to the best of your knowledge, the details you are providing are correct."
    val income_source = "Income type"

    object IncomeSource {
      val business = "Business"
      val property = "Property"
      val both = "Business and property"
    }

    val accounting_period = "Accounting period"
    val accounting_period_month: Int => String = (month: Int) => s"$month month period"
    val business_name = "Business name"
    val income_type = "Accounting type"

    object IncomeType {
      val cash = "Cash basis"
      val accruals = "Accruals basis"
    }

    val contact_email = "Email"
    val terms = "Terms and conditions"
    val terms_agreed = "I agree"
  }

  object Timeout {
    val title = "Session closed due to inactivity"
    val heading = "You've been signed out due to inactivity."
    val returnToHome = """You can start again from the <a href="{0}" rel="external">subscription</a> page."""
  }

  object AlreadyEnrolled {
    val title = "You already have a subscription"
    val heading = "You already have a subscription"
  }

  object Confirmation {
    val title = "Submitted"
    val heading: String = title
    val subscriptionIdLabel = "Subscription number:"
    val submissionDateLabel = "Date:"
    val emailConfirmation = "You'll receive an email confirming that your application has been received."

    object whatHappensNext {
      val heading = "What happens next"
      val para1 = "Your process for HMRC."
      val bullet1 = "What correspondence user will expect to receive and within what time period"
      val bullet2 = "What implications and obligations on the user as a result of this subscription if any"
    }

    object registerForMoreTax {
      val heading = "Register for more tax"
      val link1 = "PAYE (Pay as you earn)"
      val link2 = "VAT (Value added tax)"
    }

    object guidanceSection {
      val heading = "Guidance"
      val link1 = "Quarterly filing instructions"
      val link2 = "Downloading software"
      val link3 = "Further reading"
    }

    object giveUsFeedback {
      val heading = "Give us feedback"
      val link1 = "What did you think of this service?"
      val feedbackDuration = "(takes 30 seconds)"
    }

  }

  object Error {

    object BackToPreferences {
      val empty = "Please select an option"
    }

    object Business {

      object SoleTrader {
        val empty = "Please select an option"
        val invalid = "Please select an option"
      }

    }

    object Property {

      object Income {
        val empty = "Please select an option"
        val invalid = "Please select an option"
      }

    }

    object Date {
      val empty = "Please enter a date"
      val invalid = "Please enter a valid date"
      val end_violation = "The end date must be after the start date"
    }

    object BusinessAccountingPeriod {
      val minStartDate = "You can't enter a start date that's before 1 April 2017"

      def maxEndDate(months: String, date: String): String =
        s"The end date must be within $months months of the start date, you can't enter a date that's after $date"
    }

    object BusinessName {
      val empty = "Please enter a business name"
      val maxLength = "The business name is too long"
      val invalid = "The business name contains invalid characters"
    }

    object ContactEmail {
      val empty = "Please enter a contact email"
      val maxLength = "The email is too long"
      val invalid = "The email is invalid"
    }

    object IncomeType {
      val empty = "Please select an income type"
      val invalid = "Please select an income type"
    }

    object NotEligible {
      val empty = "Please select an option"
      val invalid = "Please select an option"
    }

    object IncomeSource {
      val empty = "Please select an income source"
      val invalid = "Please select an income source"
    }

    object Terms {
      val empty = "Please accept the terms and conditions"
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
    val title = "You can't send digital updates yet"
    val heading = title
  }

  object MainIncomeError {
    val title = "Unable to register"
    val heading = "You can't send digital updates yet"
    val para1 = "At the moment, you can only register to send digital updates if you're either:"
    val para2 = "You can register later in the year when this service is available to you."
    val bullet1 = "a sole trader with income from one business"
    val bullet2 = "someone who lets UK property"
    val bullet3 = "a sole trader with income from one business and you let a UK property"
  }

}
