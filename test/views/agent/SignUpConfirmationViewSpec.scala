/*
 * Copyright 2022 HM Revenue & Customs
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

import models.UpdateDeadline
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{ImplicitDateFormatter, ImplicitDateFormatterImpl, ViewSpec}
import views.html.agent.SignUpConfirmation

import java.time.LocalDate
import java.time.Month._
import scala.util.Random

class SignUpConfirmationViewSpec extends ViewSpec {

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  import implicitDateFormatter.LongDate

  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  val testName = "Lisa Khan"
  val testNino = "QQ123456L"

  def page(selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String]): Html = signUpConfirmation(selectedTaxYearIsNext, userNameMaybe, testNino)

  def document(selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String] = Some(testName)): Document =
    Jsoup.parse(page(selectedTaxYearIsNext, userNameMaybe).body)

  "The sign up confirmation view" when {

    for (yearIsNext <- Seq(true, false)) {
      val testMainContent = document(yearIsNext).mainContent
      s"nextYear flag is $yearIsNext" must {
        "have a section 1" which {
          "contains a heading" in {
            testMainContent.selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.section1heading
          }
        }
      }
    }


  }

  private object SignUpConfirmationMessages {
    val section1heading = "What you will have to do"
  }
}
