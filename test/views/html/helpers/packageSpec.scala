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

package views.html.helpers

import org.scalatest.Matchers._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class packageSpec extends PlaySpec with OneServerPerSuite {

  "the paramSeq function" should {

    "filter out the params that are not defined so that it cleans up the input to the play.ui helpers" in {
      val actualSeq = paramSeq(
        '_test -> None,
        '_test2 -> "value"
      )
      actualSeq shouldBe Seq('_test2 -> "value")
    }

  }

}
