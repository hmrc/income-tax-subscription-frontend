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

package core.views.helpers

import assets.MessageLookup.{Base => common}
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.models.SummaryError
import core.forms.validation.testutils.DataMap
import core.forms.validation.utils.MappingUtil._
import org.scalatest.Matchers._
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.Invalid
import play.api.i18n.Messages.Implicits.applicationMessages
import core.utils.UnitTestTrait
import core.views.html.helpers

class SummaryErrorHelperSpec extends UnitTestTrait {

  private def summaryErrorHelper(form: Form[_])
  = helpers.summaryErrorHelper(form)(applicationMessages)

  case class TestData(field1: String, field2: String, field3: String)

  import ErrorMessageFactory._

  val errorMessage: Invalid = DataMap.alwaysFailInvalid
  val summaryErrorMessage: SummaryError = errorMessage.errors.head.args(SummaryErrorLoc).asInstanceOf[SummaryError]

  val field1Name = "field1"
  val field2Name = "field2"
  val field3Name = "field3"

  val testForm = Form(
    mapping(
      field1Name -> oText.toText.verifying(DataMap.alwaysFail),
      field2Name -> oText.toText,
      field3Name -> oText.toText.verifying(DataMap.alwaysFail)
    )(TestData.apply)(TestData.unapply)
  )

  import scala.collection.JavaConversions._

  "SummaryErrorHelper" should {
    "if the form does not present an error, it should continue a successful flow" in {
      val page = summaryErrorHelper(testForm)
      val doc = page.doc
      val summaryDiv = doc.getElementById("error-summary-display")
      summaryDiv shouldBe null
    }

    "if the form does present an error, the error should be displayed back" in {
      val filledForm = testForm.bind(DataMap.EmptyMap)
      val page = summaryErrorHelper(filledForm)
      val doc = page.doc

      val summary = doc.getElementById("error-summary-display")
      summary should not be null
      summary.attr("class") shouldBe "flash error-summary error-summary--show"
      summary.attr("role") shouldBe "alert"
      summary.attr("aria-labelledby") shouldBe "error-summary-heading"
      summary.attr("tabindex") shouldBe "-1"

      val fieldUl = doc.getElementsByTag("ul")
      val fieldLi = fieldUl.get(0).getElementsByTag("li")
      fieldLi.size() shouldBe 2

      fieldLi.foreach(x => x.attr("role") shouldBe "tooltip")

      val aField1 = fieldLi.get(0).getElementsByTag("a")
      aField1.attr("href") shouldBe "#field1"
      aField1.text shouldBe summaryErrorMessage.toText
      val aField2 = fieldLi.get(1).getElementsByTag("a")
      aField2.attr("href") shouldBe "#field3"
      aField2.text shouldBe summaryErrorMessage.toText
      val summaryHeading = doc.getElementsByTag("h2")
      summaryHeading.text shouldBe common.errorHeading
    }
  }

}