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

package forms

import models.ExitSurveyModel
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest


class ExitSurveyFormSpec extends PlaySpec with GuiceOneAppPerTest {

  import ExitSurveyForm._

  "ExitSurveyForm" should {

    "allow null submission" in {
      val form = exitSurveyForm.bind(Map[String, String]())
      form.hasErrors mustBe false
      form.get mustBe ExitSurveyModel(None, None, None, None)
    }

    "allow full submission" in {
      val testAboutToQuery = "test1"
      val testAdditionalTasks1 = "test2a"
      val testAdditionalTasks2 = "test2b"
      val testAdditionalTasks3 = "test2c"
      val testExperience = "test3"
      val testRecommendation = "test4"
      val form = exitSurveyForm.bind(Map[String, String](
        aboutToQuery -> testAboutToQuery,
        additionalTasks + "[0]" -> testAdditionalTasks1, // expected format for items in a list
        additionalTasks + "[1]" -> testAdditionalTasks2,
        additionalTasks + "[2]" -> testAdditionalTasks3,
        recommendation -> testRecommendation,
        experience -> testExperience
      ))
      form.hasErrors mustBe false
      form.get mustBe ExitSurveyModel(Some(testAboutToQuery), Some(List(testAdditionalTasks1, testAdditionalTasks2, testAdditionalTasks3)), Some(testRecommendation), Some(testExperience))
    }

  }

}
