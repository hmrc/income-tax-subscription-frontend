/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.formatters

import models.DateModel
import org.scalatest.{MustMatchers, WordSpec}

class NewDateModelMappingTest extends WordSpec with MustMatchers {

  "NewDateModelMapping max day for month" when {
    "month is february, in a leap year" should {
      "give 29" in {
        NewDateModelMapping.maxDayForMonth(2, 2000) must be(29)
      }
    }
    "month is february, not in a leap year" should {
      "give 29" in {
        NewDateModelMapping.maxDayForMonth(2, 2001) must be(28)
      }
    }
    "month is thermidor" should {
      "give maxint" in {
        NewDateModelMapping.maxDayForMonth(13, 2001) must be(Integer.MAX_VALUE)
      }
    }
    "crazy corner case of 1900 not being a leap year" should {
      "give 28" in {
        NewDateModelMapping.maxDayForMonth(2, 1900) must be(28)
      }
    }
  }

  "NewDateModelMapping dateModelFormatter" when {
    val formatter = NewDateModelMapping.DateModelFormatter(isAgent = true, "context", None, None, None)
    "date is all fine" should {
      "give a good result" in {
        val result = formatter.bind("key", Map(
          "key-dateDay" -> "1",
          "key-dateMonth" -> "6",
          "key-dateYear" -> "2001"
        ))
        result.isRight must be (true)
        result.right.get must be (DateModel("1", "6", "2001"))
      }
    }
    "day only is numeric but rubbish" should {
      "give a key saying day is rubbish" in {
        val result = formatter.bind("key", Map(
          "key-dateDay" -> "33",
          "key-dateMonth" -> "6",
          "key-dateYear" -> "2001"
        ))
        result.isLeft must be (true)
        val maybeFormError = result.left.get.headOption
        maybeFormError.isDefined must be (true)
        maybeFormError.get.key must be ("key-dateDay")
      }
    }
    "day only is non-numeric rubbish" should {
      "give a key saying day is rubbish" in {
        val result = formatter.bind("key", Map(
          "key-dateDay" -> "x",
          "key-dateMonth" -> "6",
          "key-dateYear" -> "2001"
        ))
        result.isLeft must be (true)
        val maybeFormError = result.left.get.headOption
        maybeFormError.isDefined must be (true)
        maybeFormError.get.key must be ("key-dateDay")
      }
    }
    "month only is numeric but rubbish" should {
      "give a key saying month is rubbish" in {
        val result = formatter.bind("key", Map(
          "key-dateDay" -> "1",
          "key-dateMonth" -> "13",
          "key-dateYear" -> "2001"
        ))
        result.isLeft must be (true)
        val maybeFormError = result.left.get.headOption
        maybeFormError.isDefined must be (true)
        maybeFormError.get.key must be ("key-dateMonth")
      }
    }
    "month only is non-numeric rubbish" should {
      "give a key saying month is rubbish" in {
        val result = formatter.bind("key", Map(
          "key-dateDay" -> "1",
          "key-dateMonth" -> "x",
          "key-dateYear" -> "2001"
        ))
        result.isLeft must be (true)
        val maybeFormError = result.left.get.headOption
        maybeFormError.isDefined must be (true)
        maybeFormError.get.key must be ("key-dateMonth")
      }
    }
    "year only is numeric but rubbish" should {
      "give a key saying year is rubbish" in {
        val result = formatter.bind("key", Map(
          "key-dateDay" -> "1",
          "key-dateMonth" -> "12",
          "key-dateYear" -> "1"
        ))
        result.isLeft must be (true)
        val maybeFormError = result.left.get.headOption
        maybeFormError.isDefined must be (true)
        maybeFormError.get.key must be ("key-dateYear")
      }
    }
    "year only is non-numeric rubbish" should {
      "give a key saying year is rubbish" in {
        val result = formatter.bind("key", Map(
          "key-dateDay" -> "1",
          "key-dateMonth" -> "12",
          "key-dateYear" -> "x"
        ))
        result.isLeft must be (true)
        val maybeFormError = result.left.get.headOption
        maybeFormError.isDefined must be (true)
        maybeFormError.get.key must be ("key-dateYear")
      }
    }
    "corner case NOT leap year provided" should {
      "give a key saying day is rubbish" in {
        val result = formatter.bind("key", Map(
          "key-dateDay" -> "29",
          "key-dateMonth" -> "2",
          "key-dateYear" -> "1900"
        ))
        result.isLeft must be (true)
        val maybeFormError = result.left.get.headOption
        maybeFormError.isDefined must be (true)
        maybeFormError.get.key must be ("key-dateDay")
      }
    }
  }

}
