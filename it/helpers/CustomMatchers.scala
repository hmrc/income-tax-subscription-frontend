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

package helpers

import org.jsoup.Jsoup
import org.scalatest.matchers._
import play.api.libs.json.Reads
import play.api.libs.ws.WSResponse

trait CustomMatchers {
  def httpStatus(expectedValue: Int): HavePropertyMatcher[WSResponse, Int] =
    new HavePropertyMatcher[WSResponse, Int] {
      def apply(response: WSResponse) =
        HavePropertyMatchResult(
          response.status == expectedValue,
          "httpStatus",
          expectedValue,
          response.status
        )
    }

  def jsonBodyAs[T](expectedValue: T)(implicit reads: Reads[T]): HavePropertyMatcher[WSResponse, T] =
    new HavePropertyMatcher[WSResponse, T] {
      def apply(response: WSResponse) =
        HavePropertyMatchResult(
          response.json.as[T] == expectedValue,
          "jsonBodyAs",
          expectedValue,
          response.json.as[T]
        )
    }

  def emptyBody: HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {
      def apply(response: WSResponse) =
        HavePropertyMatchResult(
          response.body == "",
          "emptyBody",
          "",
          response.body
        )
    }

  def pageTitle(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {

      def apply(response: WSResponse) = {
        val body = Jsoup.parse(response.body)

        HavePropertyMatchResult(
          body.title == expectedValue,
          "pageTitle",
          expectedValue,
          body.title
        )
      }
    }

  def elementValueByID(id: String)(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {

      def apply(response: WSResponse) = {
        val body = Jsoup.parse(response.body)

        HavePropertyMatchResult(
          body.getElementById(id).`val` == expectedValue,
          s"elementByID($id)",
          expectedValue,
          body.getElementById(id).`val`
        )
      }
    }

  def elementTextByID(id: String)(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {

      def apply(response: WSResponse) = {
        val body = Jsoup.parse(response.body)

        HavePropertyMatchResult(
          body.getElementById(id).text == expectedValue,
          s"elementByID($id)",
          expectedValue,
          body.getElementById(id).text
        )
      }
    }

  def redirectURI(expectedValue: String): HavePropertyMatcher[WSResponse, String] = new HavePropertyMatcher[WSResponse, String] {
    def apply(response: WSResponse) = {
      val redirectLocation: Option[String] = response.header("Location")

      HavePropertyMatchResult(
        redirectLocation.contains(expectedValue),
        "httpStatus",
        expectedValue,
        redirectLocation.getOrElse("")
      )
    }
  }

  def radioButton(id: String, selectedValue: Option[String]): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {
      def apply(response: WSResponse) = {
        val body = Jsoup.parse(response.body)
        val radio = body.getElementById(id)
        val checkedAttr = "checked"
        val matchCondition = selectedValue match {
          case Some(expectedOption) => radio.attr(checkedAttr) == expectedOption
          case None => !radio.hasAttr(checkedAttr)
        }
        HavePropertyMatchResult(
          matches = matchCondition,
          propertyName = "radioButton",
          expectedValue = "",
          actualValue = radio.attr(checkedAttr)
        )
      }
    }
}