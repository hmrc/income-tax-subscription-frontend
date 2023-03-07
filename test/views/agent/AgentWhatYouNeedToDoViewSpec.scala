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

package views.agent

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.WhatYouNeedToDo

import java.time.format.DateTimeFormatter

class AgentWhatYouNeedToDoViewSpec extends ViewSpec {

  val whatYouNeedToDo: WhatYouNeedToDo = app.injector.instanceOf[WhatYouNeedToDo]

  "WhatYouNeedToDo" when {
    "the user is mandated for the current year" should {
      def mainContent: Element = document(eligibleNextYearOnly = false, mandatedCurrentYear = true, mandatedNextYear = false).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = false, mandatedCurrentYear = true, mandatedNextYear = false),
        title = WhatYouNeedToDoMessages.heading,
        isAgent = true,
        backLink = None,
        hasSignOutLink = true
      )

      "have a first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.paraOne
      }

      "have a second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.paraTwo
      }

      "have a notification banner" which {
        def notificationBanner: Element = mainContent.selectHead(".govuk-notification-banner")

        "has a heading" in {
          notificationBanner.selectHead(".govuk-notification-banner__header").text mustBe WhatYouNeedToDoMessages.NotificationBanner.heading
        }

        "has a bullet list" which {
          def bulletList: Element = notificationBanner.selectHead("ul")

          "has a first bullet" in {
            bulletList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.NotificationBanner.bulletOne
          }

          "has a second bullet" in {
            bulletList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.NotificationBanner.bulletTwo
          }

          "has a third bullet" in {
            bulletList.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.NotificationBanner.bulletThree
          }
        }
      }

      "have a warning text" in {
        mainContent.selectHead(".govuk-warning-text__text").text mustBe WhatYouNeedToDoMessages.MandatedCurrentYear.WarningText.para
      }

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has an accept and continue button to submit the form" in {
          form.selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue
        }
      }
    }
    "the user is mandated for next tax year and only eligible for the next tax year" should {
      def mainContent: Element = document(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = true).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = true),
        title = WhatYouNeedToDoMessages.heading,
        isAgent = true,
        backLink = None,
        hasSignOutLink = true
      )

      // rest of the tests here for mandated next year and eligible next year content

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has an accept and continue button to submit the form" in {
          form.selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue
        }
      }
    }
    "the user is voluntary for both years but only eligible for next year" should {
      def mainContent: Element = document(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = false).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = true, mandatedCurrentYear = false, mandatedNextYear = false),
        title = WhatYouNeedToDoMessages.heading,
        isAgent = true,
        backLink = None,
        hasSignOutLink = true
      )

      "have a first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.paraOne
      }

      "have a second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.paraTwo
      }

      "have a notification banner" which {
        def notificationBanner: Element = mainContent.selectHead(".govuk-notification-banner")

        "has a heading" in {
          notificationBanner.selectHead(".govuk-notification-banner__header").text mustBe WhatYouNeedToDoMessages.NotificationBanner.heading
        }

        "has a bullet list" which {
          def bulletList: Element = notificationBanner.selectHead("ul")

          "has a first bullet" in {
            bulletList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.NotificationBanner.bulletOne
          }

          "has a second bullet" in {
            bulletList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.NotificationBanner.bulletTwo
          }

          "has a third bullet" in {
            bulletList.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.NotificationBanner.bulletThree
          }

          "has a forth bullet" in {
            bulletList.selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.NotificationBanner.bulletFour
          }
        }

      }

      "have an inset text" in {
        mainContent.selectHead(".govuk-inset-text").text mustBe WhatYouNeedToDoMessages.EligibleNextYearOnly.InsetText.para
      }

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has an accept and continue button to submit the form" in {
          form.selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue
        }
      }
    }
    "the user is voluntary and eligible for both years" should {
      def mainContent: Element = document(eligibleNextYearOnly = false, mandatedCurrentYear = false, mandatedNextYear = false).mainContent

      "use the correct template details" in new TemplateViewTest(
        view = page(eligibleNextYearOnly = false, mandatedCurrentYear = false, mandatedNextYear = false),
        title = WhatYouNeedToDoMessages.heading,
        isAgent = true,
        backLink = None,
        hasSignOutLink = true
      )

      "have a first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.paraOne
      }

      "have a notification banner" which {
        def notificationBanner: Element = mainContent.selectHead(".govuk-notification-banner")

        "has a heading" in {
          notificationBanner.selectHead(".govuk-notification-banner__header").text mustBe WhatYouNeedToDoMessages.NotificationBanner.heading
        }

        "has a bullet list" which {
          def bulletList: Element = notificationBanner.selectHead("ul")

          "has a first bullet" in {
            bulletList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.NotificationBanner.bulletOne
          }

          "has a second bullet" in {
            bulletList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.NotificationBanner.bulletTwo
          }

          "has a third bullet" in {
            bulletList.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.NotificationBanner.bulletThree
          }

          "has a forth bullet" in {
            bulletList.selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.NotificationBanner.bulletFour
          }

          "has a fifth bullet" in {
            bulletList.selectNth("li", 5).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.NotificationBanner.bulletFive
          }
        }
      }

      "have an inset text" in {
        mainContent.selectHead(".govuk-inset-text").text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.InsetText.para
      }

      "have a second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMessages.VoluntaryAndEligible.paraTwo
      }

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has the correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has an accept and continue button to submit the form" in {
          form.selectHead("button").text mustBe WhatYouNeedToDoMessages.acceptAndContinue
        }
      }
    }
  }

  def page(eligibleNextYearOnly: Boolean, mandatedCurrentYear: Boolean, mandatedNextYear: Boolean): HtmlFormat.Appendable = {
    whatYouNeedToDo(
      postAction = testCall,
      eligibleNextYearOnly = eligibleNextYearOnly,
      mandatedCurrentYear = mandatedCurrentYear,
      mandatedNextYear = mandatedNextYear
    )
  }

  def document(eligibleNextYearOnly: Boolean, mandatedCurrentYear: Boolean, mandatedNextYear: Boolean): Document = {
    Jsoup.parse(page(eligibleNextYearOnly, mandatedCurrentYear, mandatedNextYear).body)
  }

  object WhatYouNeedToDoMessages {
    val heading: String = "What you need to do"

    object NotificationBanner {
      val heading: String = "Important"
    }

    object MandatedCurrentYear {
      val paraOne: String = "Based on your client’s previous tax returns, they must use Making Tax Digital for Income Tax."
      val paraTwo: String = "Either you or your client must:"

      object NotificationBanner {
        val bulletOne: String = "record income and expenses using compatible software"
        val bulletTwo: String = "use software to send us quarterly updates"
        val date = AccountingPeriodUtil.getFinalDeclarationDate(false).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
        val bulletThree: String = s"send an end of period statement and submit a final declaration by ${date}"
      }
      object WarningText {
        val para: String = "Your client may be penalised if they do not use Making Tax Digital for Income Tax."
      }

    }

    object MandatedAndEligibleNextYearOnly {
      // add messages here
    }

    object EligibleNextYearOnly {
      val paraOne: String = {
        val year: Int = AccountingPeriodUtil.getCurrentTaxEndYear
        s"Your client can sign up to use Making Tax Digital for Income Tax from 6 April $year."
      }
      val paraTwo: String = "By signing up you agree that either you or your client will:"

      object NotificationBanner {
        val bulletOne: String = "record income and expenses using compatible software"
        val bulletTwo: String = "use software to send us quarterly update"
        val bulletThree: String = {
          val date: String = AccountingPeriodUtil.getFinalDeclarationDate(true).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
          s"send an end of period statement and submit a final declaration by $date"
        }
        val bulletFour: String = "tell HMRC if they stop trading or start a new business"
      }

      object InsetText {
        val para: String = {
          val year: Int = AccountingPeriodUtil.getCurrentTaxEndYear
          s"Your client’s Self Assessment tax return must be submitted at the end of the $year tax year as normal."
        }
      }
    }

    object VoluntaryAndEligible {
      val paraOne: String = "By taking part in this pilot you agree that either you or your client will:"

      object NotificationBanner {
        val bulletOne: String = "record income and expenses using compatible software"
        val bulletTwo: String = "use software to send us quarterly updates"
        val bulletThree: String = "complete any missing quarterly updates (if you’ve chosen to sign up for the current tax year)"
        val bulletFour: String = "send an end of period statement and submit a final declaration by 31 January following the end of the tax year"
        val bulletFive: String = "tell HMRC if they stop trading or start a new business"
      }

      object InsetText {
        val para: String = "Using Making Tax Digital for Income Tax is currently voluntary. Your client can opt out and go back to Self Assessment at any time."
      }

      val paraTwo: String = "It will be compulsory for some people to use Making Tax Digital for Income Tax from April 2026," +
        " depending on their total qualifying income. If this applies to your client, we’ll send them a letter."
    }

    val acceptAndContinue: String = "Accept and continue"

  }

}