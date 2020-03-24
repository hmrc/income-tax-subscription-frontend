/*
 * Copyright 2020 HM Revenue & Customs
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

package views

import _root_.agent.assets.MessageLookup.IncomeSource
import config.{AppConfig, MockConfig}
import forms.agent.IncomeSourceForm
import models.individual.subscription.IncomeSourceType
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.{Assertion, MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.html.agent.income_source

import scala.collection.JavaConversions._

class InvestigationSpec extends ViewSpec {

  val incomeSourceForm: Form[IncomeSourceType] = IncomeSourceForm.incomeSourceForm

  class IncomeSourceSetup(form: Form[IncomeSourceType] = incomeSourceForm, editMode: Boolean = false) extends Setup(
    page = income_source(form, testCall, isEditMode = editMode, testBackUrl)
  )

  "income source" must {

    "have the correct title" in new IncomeSourceSetup {
      document.title mustBe IncomeSource.title
    }

    "have the correct heading" in new IncomeSourceSetup {
      content hasPageHeading IncomeSource.heading
    }

    "have a form" in new IncomeSourceSetup {
      content hasFormWith(
        testCall.method,
        testCall.url
      )
    }

  }

  "income source" when {
    "in edit mode" must {
      "have a back link" in new IncomeSourceSetup(editMode = true) {
        content hasBackLinkTo testBackUrl
      }
    }

    "not in edit mode" must {
      "not have a back link" in new IncomeSourceSetup {
        content doesNotHave Selectors.backLink
      }
    }

    "there is an error in the form" must {
      "display the error on the page" in new IncomeSourceSetup(
        form = incomeSourceForm.withError(IncomeSourceForm.incomeSource, "test error message")
      ) {
        content hasFormError(
          errorKey = IncomeSourceForm.incomeSource,
          errorMessage = "test error message"
        )
      }
    }

    "there is not an error in the form" must {
      "not display an error on the page" in new IncomeSourceSetup {
        content doesNotHave Selectors.summaryError
        content doesNotHave Selectors.inputError
      }
    }

  }

}

trait ViewSpec extends WordSpec with MustMatchers with GuiceOneAppPerSuite {

  val testCall: Call = Call("POST", "/test-url")
  val testBackUrl: String = "/test-url"

  implicit val request: Request[_] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val appConfig: AppConfig = MockConfig

  class Setup(page: Html) {
    val document: Document = Jsoup.parse(page.body)
    val content: Element = document.selectFirst("#content")
  }

  object Selectors {
    val h1: String = "h1"
    val backLink: String = ".back-link"
    val form: String = "form"
    val summaryError: String = "#error-summary-display ul a"
    val inputError: String = ".error-notification"
  }

  implicit class CustomSelectors(element: Element) {

    //noinspection ScalaStyle
    def h1: Element = {
      element.select(Selectors.h1).headOption getOrElse fail("h1 not found")
    }

    def backLink: Element = {
      element.select(Selectors.backLink).headOption getOrElse fail("back link not found")
    }

    def form: Element = {
      element.select(Selectors.form).headOption getOrElse fail("form not found")
    }

    def summaryError: Element = {
      element.select(Selectors.summaryError).headOption getOrElse fail("error summary list item not found")
    }

    def inputError: Element = {
      element.select(Selectors.inputError).headOption getOrElse fail(s"input error not found")
    }

  }

  implicit class ElementTests(element: Element) {

    def hasPageHeading(heading: String): Assertion = element.h1.text mustBe heading

    def hasBackLinkTo(href: String): Assertion = element.backLink.attr("href") mustBe href

    def hasFormWith(method: String, action: String): Assertion = {
      element.form.attr("method") mustBe method
      element.form.attr("action") mustBe action
    }

    def doesNotHave(selector: String): Assertion = {
      element.select(selector).headOption.fold(succeed)(_ => fail(s"$selector was found"))
    }

    def hasFormError(errorKey: String, errorMessage: String): Assertion = {
      element.summaryError.attr("href") mustBe s"#$errorKey"
      element.summaryError.text mustBe errorMessage
      element.inputError.text mustBe errorMessage
    }

  }

}
