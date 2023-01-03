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

package models

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateModelSpec extends PlaySpec with GuiceOneServerPerSuite{

  "the DateModel" should {

    val date = DateModel("01", "02", "2017")

    "convert correctly to java.time.LocalDate" in {
      date.toLocalDate mustBe LocalDate.parse("01/02/2017", DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    "correctly format a date output to a view into d MMMMM uuuu" in {
      date.toOutputDateFormat mustBe "1 February 2017"
    }

    "correctly format a date for check your answers into dd/MM/uuuu" in {
      date.toOutputDateFormat mustBe "1 February 2017"
    }

    "plusDays should return the correct date" in {
      val testDate = DateModel("28", "02", "2018")
      val increment = 1
      val expectedDate = DateModel("1", "3", "2018")
      testDate.plusDays(increment) mustBe expectedDate
    }
    "should display the date in the user's chosen language" when {
     lazy val messagesApi = app.injector.instanceOf[MessagesApi]
     lazy val messagesEnglish = MessagesImpl(Lang("en"), messagesApi)
     lazy val messagesWelsh = MessagesImpl(Lang("cy"), messagesApi)
      "locale is in English" in {
        date.toCheckYourAnswersDateFormat(messagesEnglish) mustBe "1 February 2017"
      }
      "locale is in Welsh" in {
        date.toCheckYourAnswersDateFormat(messagesWelsh) mustBe "1 Chwefror 2017"
      }
    }
  }
}
