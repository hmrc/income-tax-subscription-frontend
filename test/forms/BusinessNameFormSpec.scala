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

import models.BusinessNameModel
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import org.scalatest.Matchers._

class BusinessNameFormSpec extends PlaySpec with OneAppPerTest {

  import BusinessNameForm._

  "The BusinessNameForm" should {
    "transform the request to the form case class" in {
      val testBusinessName = "ABC"
      val testInput = Map(businessName -> testBusinessName)
      val expected = BusinessNameModel(testBusinessName)
      val actual = businessNameForm.bind(testInput).value

      actual shouldBe Some(expected)
    }
  }

}
