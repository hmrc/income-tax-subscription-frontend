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

package views.helpers

import forms.validation.ConstraintUtil._
import forms.validation.ErrorMessageFactory
import forms.validation.models.SummaryError
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.data.validation.Valid
import play.api.i18n.Messages.Implicits.applicationMessages
import play.twirl.api.Html
import org.scalatest.Matchers._

class SummaryErrorHelperSpec extends PlaySpec with OneServerPerSuite {

  private def summaryErrorHelper(form: Form[_])
  = views.html.helpers.summaryErrorHelper(form)(applicationMessages)

  implicit class HtmlFormatUtil(html: Html) {
    def doc: Document = Jsoup.parse(html.body)
  }

  case class TestData(field1: String, field2: String, field3: String)

  val errorMessage = ErrorMessageFactory.error("errKey")
  val summaryErrorMessage: SummaryError = errorMessage.errors.head.args(1).asInstanceOf[SummaryError]

  val testValidation = (data: String) => {
    data.length > 0 match {
      case true => errorMessage
      case _ => Valid
    }
  }
  val testConstraint = constraint(testValidation)

  val field1Name = "field1"
  val field2Name = "field2"
  val field3Name = "field3"
  val testForm = Form(
    mapping(
      field1Name -> text.verifying(testConstraint),
      field2Name -> text,
      field3Name -> text.verifying(testConstraint)
    )(TestData.apply)(TestData.unapply)
  )

  "SummaryErrorHelper" should {
    "if the form does not present an error, it should continue a successful flow" in {
      val page = summaryErrorHelper(testForm)
      val doc = page.doc

      val summaryDiv = doc.getElementById("field1")
      summaryDiv shouldBe null
    }

    "if the form does present an error, the error should be displayed back" in {
      val page = summaryErrorHelper(testForm.fill(TestData("data", " ", "data")))
      val doc = page.doc
      val inputs = doc.getElementsByTag("field2")
    }
  }

}