/*
 * Copyright 2021 HM Revenue & Customs
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

import assets.MessageLookup
import forms.formatters.DateModelMapping.dateMapping
import forms.validation.testutils.DataMap.DataMap
import models.DateModel
import org.scalatest.Matchers._
import play.api.data.Form
import play.api.i18n.{Lang, Messages, MessagesApi}
import utilities.UnitTestTrait
import views.html.helpers.dateHelper

class DateHelperSpec extends UnitTestTrait {

  val dateName = "testDate"
  val testForm = Form(
    dateName -> dateMapping.verifying(DataMap.alwaysFail)
  )

  val welshMessages: Messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("cy")))

  val testLabel = "my test label text"

  "dateHelper" should {
    "populate the relevant content in the correct positions" in {
      val testField = testForm(dateName)

      val doc = dateHelper(testField, testLabel, None, testForm).doc
      doc.getElementsByTag("div").hasClass("form-group") shouldBe true
      doc.getElementsByTag("div").hasClass("form-field") shouldBe true
      doc.getElementsByTag("legend").text() should include(testLabel)

      val inputs = doc.getElementsByTag("input")

      inputs.size() shouldBe 3
      inputs.get(0).attr("type") shouldBe "text"
      inputs.get(0).attr("maxlength") shouldBe "2"
      inputs.get(0).attr("autocomplete") shouldBe ""
      inputs.get(0).parent().classNames() should contain("form-group-day")
      inputs.get(1).attr("type") shouldBe "text"
      inputs.get(1).attr("maxlength") shouldBe "2"
      inputs.get(1).attr("autocomplete") shouldBe ""
      inputs.get(1).parent().classNames() should contain("form-group-month")
      inputs.get(2).attr("type") shouldBe "text"
      inputs.get(2).attr("maxlength") shouldBe "4"
      inputs.get(2).attr("autocomplete") shouldBe ""
      inputs.get(2).parent().classNames() should contain("form-group-year")

      val labels = doc.getElementsByTag("label")
      labels.get(0).text shouldBe MessageLookup.Base.day
      labels.get(1).text shouldBe MessageLookup.Base.month
      labels.get(2).text shouldBe MessageLookup.Base.year
    }

    "the day & month class changes for the Welsh language" in {
      val testField = testForm(dateName)

      val doc = dateHelper(testField, testLabel, None, testForm)(welshMessages).doc

      val inputs = doc.getElementsByTag("input")

      inputs.get(0).parent().classNames() should contain("form-group-year")
      inputs.get(1).parent().classNames() should contain("form-group-year")
      inputs.get(2).parent().classNames() should contain("form-group-year")
    }

    "if the form is populated with true, then the checkbox is marked as checked" in {
      val testField = testForm.fill(DateModel("31", "01", "2017"))(dateName)
      val doc = dateHelper(testField, testLabel, None, testForm).doc

      val inputs = doc.getElementsByTag("input")

      inputs.size() shouldBe 3
      inputs.get(0).attr("value") shouldBe "31"
      inputs.get(1).attr("value") shouldBe "01"
      inputs.get(2).attr("value") shouldBe "2017"
    }

    "when there is error on the field, the errors needs to be displayed, but not otherwise" in {
      val testField = testForm(dateName)
      val doc = dateHelper(testField, testLabel, None, testForm).doc
      doc.getElementsByTag("div").hasClass("form-field--error") shouldBe false
      doc.getElementsByClass("error-notification").isEmpty shouldBe true

      val errorForm = testForm.bind(DataMap.EmptyMap)
      val errorField = errorForm(dateName)
      val errDoc = dateHelper(errorField, testLabel, None, errorForm).doc
      errDoc.getElementsByTag("div").hasClass("form-field--error") shouldBe true
      errDoc.getElementsByClass("error-notification").isEmpty shouldBe false
    }
  }
  "dateHelper with hint" should {
    "have aria-describedBy and input Id linked " in {
      val testField = testForm(dateName)

      val doc = dateHelper(testField, testLabel, Some("Hint text"), testForm).doc
      doc.getElementsByAttributeValue("aria-describedby", "date-helper-hint").hasText mustBe true
      doc.getElementById("date-helper-hint").hasText mustBe true
    }
  }
  "dateHelper" when {
    "is a date of birth selected" in {
      val testField = testForm(dateName)

      val doc = dateHelper(testField, testLabel, None, testForm, isDateOfBirth = true).doc
      val inputs = doc.getElementsByTag("input")

      inputs.get(0).attr("autocomplete") mustBe "bday-day"
      inputs.get(1).attr("autocomplete") mustBe "bday-month"
      inputs.get(2).attr("autocomplete") mustBe "bday-year"
    }
  }
}
