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


class LabelParamsSpec extends PlaySpec with OneServerPerSuite {

  "labelParams quickAccess class" should {

    "allow access to the LabelParams parameters from Option[LabelParams] instance directly" in {
      val label = Some(true)
      val labelAfter = Some(true)
      val labelHighlight = Some(true)
      val labelClass = Some("class")
      val labelDataAttributes = Some("attr")
      val labelTextClass = Some("tclass")
      val testData = Some(LabelParams(
        label = label,
        labelAfter = labelAfter,
        labelTextClass = labelTextClass,
        labelDataAttributes = labelDataAttributes,
        labelHighlight = labelHighlight,
        labelClass = labelClass
      ))

      testData.label shouldBe label
      testData.labelAfter shouldBe labelAfter
      testData.labelHighlight shouldBe labelHighlight
      testData.labelClass shouldBe labelClass
      testData.labelDataAttributes shouldBe labelDataAttributes
      testData.labelTextClass shouldBe labelTextClass
    }
  }

  "return None if Option[LabelParams] is None" in {
    val label = Some(true)
    val labelAfter = Some(true)
    val labelHighlight = Some(true)
    val labelClass = Some("class")
    val labelDataAttributes = Some("attr")
    val labelTextClass = Some("tclass")
    val testData :Option[LabelParams]= None

    testData.label shouldBe None
    testData.labelAfter shouldBe None
    testData.labelHighlight shouldBe None
    testData.labelClass shouldBe None
    testData.labelDataAttributes shouldBe None
    testData.labelTextClass shouldBe None
  }

}
