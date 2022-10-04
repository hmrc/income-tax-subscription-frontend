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
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DateModelMappingTest extends AnyWordSpec with Matchers {

  "DateModelMapping dateModelFormatter" when {
    val formatter = DateModelMapping.DateModelFormatter(isAgent = true, "context", None, None, None)
    "date is all fine" should {
      "give a good result" in {
        val result = formatter.bind("key", Map(
          "key-dateDay" -> "1",
          "key-dateMonth" -> "6",
          "key-dateYear" -> "2001"
        ))
        result.isRight must be (true)
        result.toOption.get must be (DateModel("1", "6", "2001"))
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
        val maybeFormError = result.swap.toOption.get.headOption
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
        val maybeFormError = result.swap.toOption.get.headOption
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
        val maybeFormError = result.swap.toOption.get.headOption
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
        val maybeFormError = result.swap.toOption.get.headOption
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
        val maybeFormError = result.swap.toOption.get.headOption
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
        val maybeFormError = result.swap.toOption.get.headOption
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
        val maybeFormError = result.swap.toOption.get.headOption
        maybeFormError.isDefined must be (true)
        maybeFormError.get.key must be ("key-dateDay")
      }
    }
  }

}
