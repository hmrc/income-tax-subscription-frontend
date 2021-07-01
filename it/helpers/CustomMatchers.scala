/*
 * Copyright 2018 HM Revenue & Customs
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

import models.DateModel
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

      def apply(response: WSResponse): HavePropertyMatchResult[String] = {
        val body = Jsoup.parse(response.body)

        HavePropertyMatchResult(
          body.title == expectedValue,
          "pageTitle",
          expectedValue,
          body.title
        )
      }
    }

  def mainHeading(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {

      def apply(response: WSResponse): HavePropertyMatchResult[String] = {
        val body = Jsoup.parse(response.body)
        val h1 = body.select("h1").first().text()
        HavePropertyMatchResult(
          h1 == expectedValue,
          "h1",
          expectedValue,
          h1
        )
      }
    }

  def dateField(id: String, expectedValue: DateModel): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {

      def apply(response: WSResponse): HavePropertyMatchResult[String] = {
        val body = Jsoup.parse(response.body)
        val day = body.getElementById(id + ".dateDay").`val`()
        val month = body.getElementById(id + ".dateMonth").`val`()
        val year = body.getElementById(id + ".dateYear").`val`()
        HavePropertyMatchResult(
          day == expectedValue.day && month == expectedValue.month && year == expectedValue.year,
          "day",
          expectedValue.toString,
          day + " / " + month + " / " + year
        )
      }
    }

  def textField(id: String, expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {

      def apply(response: WSResponse): HavePropertyMatchResult[String] = {
        val body = Jsoup.parse(response.body)
        val text = body.getElementById(id).`val`()
        HavePropertyMatchResult(
          text == expectedValue,
          "text field",
          expectedValue,
          text
        )
      }
    }

  def errorDisplayed(): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {
      def apply(response: WSResponse): HavePropertyMatchResult[String] = {
        val body = Jsoup.parse(response.body)
        val errorHeader = Option(body.getElementById("error-summary-title")).getOrElse(body.getElementById("error-summary-heading"))

        HavePropertyMatchResult(
          errorHeader != null,
          "errorDisplayed",
          "error heading found",
          "no error heading found"
        )
      }
    }

  def elementValueByID(id: String)(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {

      def apply(response: WSResponse): HavePropertyMatchResult[String] = {
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

      def apply(response: WSResponse): HavePropertyMatchResult[String] = {
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
    def apply(response: WSResponse): HavePropertyMatchResult[String] = {
      val redirectLocation: Option[String] = response.header("Location")

      val matchCondition = redirectLocation.exists(_.contains(expectedValue))
      HavePropertyMatchResult(
        matchCondition,
        "redirectUri",
        expectedValue,
        redirectLocation.getOrElse("")
      )
    }
  }

  def checkboxSet(id: String, selectedCheckbox: Option[String]): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {
      def apply(response: WSResponse): HavePropertyMatchResult[String] = {
        val body = Jsoup.parse(response.body)
        val checkboxes = body.select(s"input[id^=$id]")
        val checkedAttr = "checked"

        def textForSelectedCheckbox(idForSelectedCheckbox: String) =
          if (idForSelectedCheckbox.isEmpty) ""
          else body.select(s"label[for=$idForSelectedCheckbox]").text()

        val matchCondition = selectedCheckbox match {
          case Some(expectedOption) =>
            val idForSelectedCheckbox = checkboxes.select(s"input[checked]").attr("id")
            textForSelectedCheckbox(idForSelectedCheckbox) == expectedOption
          case None => !checkboxes.hasAttr(checkedAttr)
        }

        HavePropertyMatchResult(
          matches = matchCondition,
          propertyName = "checkbox",
          expectedValue = selectedCheckbox.fold("")(identity),
          actualValue = {
            val selected = checkboxes.select("input[checked]")
            selected.size() match {
              case 0 =>
                "no checkbox is selected"
              case 1 =>
                val idForSelectedCheckbox = selected.attr("id")
                s"""The "${textForSelectedCheckbox(idForSelectedCheckbox)}" selected"""
              case _ =>
                s"multiple checkbox are selected: [$checkboxes]"
            }
          }
        )
      }
    }

  def radioButtonSet(id: String, selectedRadioButton: Option[String]): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {
      def apply(response: WSResponse): HavePropertyMatchResult[String] = {
        val body = Jsoup.parse(response.body)
        val radios = body.select(s"input[id^=$id]")
        val checkedAttr = "checked"

        def textForSelectedButton(idForSelectedRadio: String) =
          if (idForSelectedRadio.isEmpty) ""
          else body.select(s"label[for=$idForSelectedRadio]").text()

        val matchCondition = selectedRadioButton match {
          case Some(expectedOption) =>
            val idForSelectedRadio = radios.select(s"input[checked]").attr("id")
            textForSelectedButton(idForSelectedRadio) == expectedOption
          case None => !radios.hasAttr(checkedAttr)
        }

        HavePropertyMatchResult(
          matches = matchCondition,
          propertyName = "radioButton",
          expectedValue = selectedRadioButton.fold("")(identity),
          actualValue = {
            val selected = radios.select("input[checked]")
            selected.size() match {
              case 0 =>
                "no radio button is selected"
              case 1 =>
                val idForSelectedRadio = selected.attr("id")
                s"""The "${textForSelectedButton(idForSelectedRadio)}" selected"""
              case _ =>
                s"multiple radio buttons are selected: [$radios]"
            }
          }
        )
      }
    }
}