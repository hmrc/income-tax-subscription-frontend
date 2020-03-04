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

package forms.individual.subscription

import models.individual.subscription.ExitSurveyModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.data.FormError

class ExitSurveyFormSpec extends PlaySpec with GuiceOneAppPerTest {

  import forms.individual.subscription.ExitSurveyForm._

  "ExitSurveyForm" when {

    "Should validate maxlength of the how to improve this service question" should {
      val maxLen = "error.survey-feedback.maxLength"

      "throw a max length error" in {
        val maxLengthInput = Map[String, String](improvements -> "a" * (improvementsMaxLength + 1))
        val maxLengthTest = exitSurveyForm.bind(maxLengthInput)
        maxLengthTest.errors must contain(FormError(improvements, maxLen))
      }

      "Must not throw a max length error" in {
        val withinLimitInput = Map[String, String](improvements -> "a" * improvementsMaxLength)
        val withinLimitTest = exitSurveyForm.bind(withinLimitInput)
        withinLimitTest.errors mustNot contain(FormError(improvements, maxLen))
      }

      "allow null submission" in {
        val form = exitSurveyForm.bind(Map[String, String]())
        form.hasErrors mustBe false
        form.get mustBe ExitSurveyModel(None, None)
      }

      "allow full submission" in {
        val testSatisfaction = "test1"
        val testImprovements = "test2"
        val form = exitSurveyForm.bind(Map[String, String](
          satisfaction -> testSatisfaction,
          improvements -> testImprovements
        ))
        form.hasErrors mustBe false
        form.get mustBe ExitSurveyModel(Some(testSatisfaction), Some(testImprovements))
      }

    }
  }

}
