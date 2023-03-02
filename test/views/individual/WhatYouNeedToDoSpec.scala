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

package views.individual

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.WhatYouNeedToDo

import java.time.format.DateTimeFormatter

class WhatYouNeedToDoSpec extends ViewSpec {

  val whatYouNeedToDo: WhatYouNeedToDo = app.injector.instanceOf[WhatYouNeedToDo]

  def page(onlyNextYear : Boolean): HtmlFormat.Appendable = whatYouNeedToDo(testCall, onlyNextYear, mandatedCurrentYear = false)

  def document(onlyNextYear : Boolean): Document = Jsoup.parse(page(onlyNextYear).body)


  def pageCurrentMandated(currentYearMandated: Boolean): HtmlFormat.Appendable = whatYouNeedToDo(testCall, onlyNextYear = false, mandatedCurrentYear = currentYearMandated)

  def documentCurrentMandated(currentYearMandated : Boolean): Document = Jsoup.parse(pageCurrentMandated(currentYearMandated).body)

  object WhatYouNeedToDoMessages {
    val heading: String = "What you need to do"
    val paraOne: String = "By taking part in this pilot you agree that you’ll:"

    object NotificationBanner {
      val heading: String = "Important"
      val bulletOne: String = "get compatible software to record your income and expenses"
      val bulletTwo: String = "complete any missing quarterly updates (if you’ve chosen to sign up for the current tax year)"
      val bulletThree: String = "send an end of period statement using your software by 31 January following the tax year you’ve chosen to start using Making Tax Digital For Income Tax"
      val bulletFour: String = "submit your final declaration by 31 January following the tax year you’ve started using Making Tax Digital for Income Tax"
      val bulletFive: String = "tell HMRC if you stop trading or start a new business"
    }

    object InsetText {
      val para: String = "You can stop using Making Tax Digital for Income Tax at any time until 6 April 2026. If you stop, you must send your Self Assessment tax return at the end of the tax year as normal."
    }
  }

  object NextYearOnlyWhatYouNeedToDoMessages {
    val heading: String = "What you need to do"
    val paraOne: String = s"You can sign up to use Making Tax Digital for Income Tax to submit your records from 6 April ${AccountingPeriodUtil.getCurrentTaxEndYear}"
    val paraTwo: String = "By taking part in this pilot you agree to:"

    object NotificationBanner {
      val heading: String = "Important"
      val bulletOne: String = s"get compatible software to record your income and expenses from 6 April ${AccountingPeriodUtil.getCurrentTaxEndYear}"
      val bulletTwo: String = "use your compatible software to send us quarterly updates"
      val bulletThree: String = {
        val date = AccountingPeriodUtil.getFinalDeclarationDate(true).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
        s"send an end of period statement using your software and send your final declaration by $date"
      }
      val bulletFour: String = "tell HMRC if you stop trading or start a new business"
    }

    object InsetText {
      val para: String = "You must submit your normal Self Assessment tax return at the end of this tax year as normal."
    }

  }


  object WhatYouNeedToDoMandatedCurrent {
    val heading: String = "What you need to do"
    val paraOne: String = "Based on your previous returns, you need to sign up for Making Tax Digital for Income Tax to submit your records."
    val paraTwo: String = "By signing up you agree that you will:"

    object NotificationBanner {
      val heading: String = "Important"
      val bulletOne: String = "get compatible software to record your income and expenses"
      val bulletTwo: String = "use your compatible software to send us quarterly updates"
      val bulletThree: String = {
        val date = AccountingPeriodUtil.getFinalDeclarationDate(false).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
        s"send an end of period statement using your software and submit your final declaration by $date"
      }
    }

    object WarningText {
      val para: String = "You may be penalised if you don’t meet the requirements for Making Tax Digital for Income tax."
    }

  }


  "WhatYouNeedToDo" must {

    "use the correct template details" in new TemplateViewTest(
      view = page(false),
      title = WhatYouNeedToDoMessages.heading,
      isAgent = false,
      backLink = None,
      hasSignOutLink = true
    )

    "have a page heading" in {
      document(false).mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMessages.heading
    }

    "have a first paragraph" in {
      document(false).mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMessages.paraOne
    }

    "has a notification banner" which {
      def notificationBanner: Element = document(false).mainContent.selectHead(".govuk-notification-banner")

      "has a heading" in {
        notificationBanner.selectHead(".govuk-notification-banner__header").text mustBe WhatYouNeedToDoMessages.NotificationBanner.heading
      }

      "has a bullet list" which {
        def bulletList: Element = notificationBanner.selectHead("ul")

        "has a first bullet" in {
          bulletList.selectNth("li", 1).text mustBe WhatYouNeedToDoMessages.NotificationBanner.bulletOne
        }

        "has a second bullet" in {
          bulletList.selectNth("li", 2).text mustBe WhatYouNeedToDoMessages.NotificationBanner.bulletTwo
        }

        "has a third bullet" in {
          bulletList.selectNth("li", 3).text mustBe WhatYouNeedToDoMessages.NotificationBanner.bulletThree
        }

        "has a forth bullet" in {
          bulletList.selectNth("li", 4).text mustBe WhatYouNeedToDoMessages.NotificationBanner.bulletFour
        }

        "has a fifth bullet" in {
          bulletList.selectNth("li", 5).text mustBe WhatYouNeedToDoMessages.NotificationBanner.bulletFive
        }
      }
    }

    "has an inset text" in {
      document(false).selectHead(".govuk-inset-text").text mustBe WhatYouNeedToDoMessages.InsetText.para
    }

  }



  "NextYearOnlyWhatYouNeedToDo" must {

    "use the correct template details" in new TemplateViewTest(
      view = page(true),
      title = NextYearOnlyWhatYouNeedToDoMessages.heading,
      isAgent = false,
      backLink = None,
      hasSignOutLink = true
    )

    "have a page heading" in {
      document(true).mainContent.selectHead("h1").text mustBe NextYearOnlyWhatYouNeedToDoMessages.heading
    }

    "have a first paragraph" in {
      document(true).mainContent.selectNth("p", 1).text mustBe NextYearOnlyWhatYouNeedToDoMessages.paraOne
    }

    "have a second paragraph" in {
      document(true).mainContent.selectNth("p", 2).text mustBe NextYearOnlyWhatYouNeedToDoMessages.paraTwo
    }

    "has a notification banner" which {
      def notificationBanner: Element = document(true).mainContent.selectHead(".govuk-notification-banner")

      "has a heading" in {
        notificationBanner.selectHead(".govuk-notification-banner__header").text mustBe NextYearOnlyWhatYouNeedToDoMessages.NotificationBanner.heading
      }

      "has a bullet list" which {
        def bulletList: Element = notificationBanner.selectHead("ul")

        "has a first bullet" in {
          bulletList.selectNth("li", 1).text mustBe NextYearOnlyWhatYouNeedToDoMessages.NotificationBanner.bulletOne
        }

        "has a second bullet" in {
          bulletList.selectNth("li", 2).text mustBe NextYearOnlyWhatYouNeedToDoMessages.NotificationBanner.bulletTwo
        }

        "has a third bullet" in {
          bulletList.selectNth("li", 3).text mustBe NextYearOnlyWhatYouNeedToDoMessages.NotificationBanner.bulletThree
        }

        "has a forth bullet" in {
          bulletList.selectNth("li", 4).text mustBe NextYearOnlyWhatYouNeedToDoMessages.NotificationBanner.bulletFour
        }
      }
    }

    "has an inset text" in {
      document(true).selectHead(".govuk-inset-text").text mustBe NextYearOnlyWhatYouNeedToDoMessages.InsetText.para
    }

  }

  "WhatYouNeedToDoMandatedCurrent" must {

    "use the correct template details" in new TemplateViewTest(
      view = pageCurrentMandated(true),
      title = WhatYouNeedToDoMandatedCurrent.heading,
      isAgent = false,
      backLink = None,
      hasSignOutLink = true
    )

    "have a page heading" in {
      documentCurrentMandated(true).mainContent.selectHead("h1").text mustBe WhatYouNeedToDoMandatedCurrent.heading
    }

    "have a first paragraph" in {
      documentCurrentMandated(true).mainContent.selectNth("p", 1).text mustBe WhatYouNeedToDoMandatedCurrent.paraOne
    }

    "have a second paragraph" in {
      documentCurrentMandated(true).mainContent.selectNth("p", 2).text mustBe WhatYouNeedToDoMandatedCurrent.paraTwo
    }

    "has a notification banner" which {
      def notificationBanner: Element = documentCurrentMandated(true).mainContent.selectHead(".govuk-notification-banner")

      "has a heading" in {
        notificationBanner.selectHead(".govuk-notification-banner__header").text mustBe WhatYouNeedToDoMandatedCurrent.NotificationBanner.heading
      }

      "has a bullet list" which {
        def bulletList: Element = notificationBanner.selectHead("ul")

        "has a first bullet" in {
          bulletList.selectNth("li", 1).text mustBe WhatYouNeedToDoMandatedCurrent.NotificationBanner.bulletOne
        }

        "has a second bullet" in {
          bulletList.selectNth("li", 2).text mustBe WhatYouNeedToDoMandatedCurrent.NotificationBanner.bulletTwo
        }

        "has a third bullet" in {
          bulletList.selectNth("li", 3).text mustBe WhatYouNeedToDoMandatedCurrent.NotificationBanner.bulletThree
        }
        }
      }
    }

    "has an warning text" in {
      documentCurrentMandated(true).selectHead(".govuk-warning-text__text").text mustBe WhatYouNeedToDoMandatedCurrent.WarningText.para
    }


}
