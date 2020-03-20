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

package forms.validation.utils

import org.scalatest.Matchers._
import utilities.UnitTestTrait

class PatternsSpec extends UnitTestTrait {

  "ISO 8859-1" should {
    "valid for" in {
      val testData = List[String](
        32.toChar.toString,
        33.toChar.toString,
        125.toChar.toString,
        126.toChar.toString,
        160.toChar.toString,
        161.toChar.toString,
        254.toChar.toString,
        255.toChar.toString
      )
      testData.foreach(data => Patterns.validText(data) shouldBe true)
    }

    "invalid for" in {
      val testData = List[String](
        31.toChar.toString,
        127.toChar.toString,
        159.toChar.toString,
        256.toChar.toString
      )
      testData.foreach(data => Patterns.validText(data) shouldBe false)
    }
  }

  "email" should {
    "valid for" in {
      val testData = List[String](
        "a@a.a",
        "a@a.a.a",
        "aa.a@a.a.a"
      )
      testData.foreach(data => Patterns.validEmail(data) shouldBe true)
    }

    "invalid for" in {
      val testData = List[String](
        "a",
        "a@a",
        "a.a@a"
      )
      testData.foreach(data => Patterns.validEmail(data) shouldBe false)
    }
  }
}
