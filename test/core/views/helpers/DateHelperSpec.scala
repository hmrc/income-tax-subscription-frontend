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

package core.views.helpers

import assets.MessageLookup
import core.forms.submapping.DateMapping._
import core.forms.validation.testutils.DataMap
import models.DateModel
import org.scalatest.Matchers._
import play.api.data.{Field, Form}
import play.api.i18n.Messages.Implicits.applicationMessages
import core.utils.UnitTestTrait

class DateHelperSpec extends UnitTestTrait {

  private def dateHelper(field: Field, label: Option[String])
  = core.views.html.helpers.dateHelper(field, label)(applicationMessages)

  val dateName = "testDate"
  val testForm = Form(
    dateName -> dateMapping.verifying(DataMap.alwaysFail)
  )

  val testLabel = "my test label text"

  "dateHelper" should {
    "populate the relevant content in the correct positions" in {
      val testField = testForm(dateName)

      val doc = dateHelper(testField, testLabel).doc
      doc.getElementsByTag("div").hasClass("form-group") shouldBe true
      doc.getElementsByTag("div").hasClass("form-field") shouldBe true
      doc.getElementsByTag("legend").text() should include(testLabel)

      val inputs = doc.getElementsByTag("input")

      inputs.size() shouldBe 3
      inputs.get(0).attr("type") shouldBe "text"
      inputs.get(0).attr("maxlength") shouldBe "2"
      inputs.get(1).attr("type") shouldBe "text"
      inputs.get(1).attr("maxlength") shouldBe "2"
      inputs.get(2).attr("type") shouldBe "text"
      inputs.get(2).attr("maxlength") shouldBe "4"

      val labels = doc.getElementsByTag("label")
      labels.get(0).text shouldBe MessageLookup.Base.day
      labels.get(1).text shouldBe MessageLookup.Base.month
      labels.get(2).text shouldBe MessageLookup.Base.year
    }

    "if the form is populated with true, then the checkbox is marked as checked" in {
      val testField = testForm.fill(DateModel("31", "01", "2017"))(dateName)
      val doc = dateHelper(testField, testLabel).doc

      val inputs = doc.getElementsByTag("input")

      inputs.size() shouldBe 3
      inputs.get(0).attr("value") shouldBe "31"
      inputs.get(1).attr("value") shouldBe "01"
      inputs.get(2).attr("value") shouldBe "2017"
    }

    "when there is error on the field, the errors needs to be displayed, but not otherwise" in {
      val testField = testForm(dateName)
      val doc = dateHelper(testField, testLabel).doc
      doc.getElementsByTag("div").hasClass("form-field--error") shouldBe false
      doc.getElementsByClass("error-notification").isEmpty shouldBe true

      val errorField = testForm.bind(DataMap.EmptyMap)(dateName)
      val errDoc = dateHelper(errorField, testLabel).doc
      errDoc.getElementsByTag("div").hasClass("form-field--error") shouldBe true
      errDoc.getElementsByClass("error-notification").isEmpty shouldBe false
    }
  }

}
