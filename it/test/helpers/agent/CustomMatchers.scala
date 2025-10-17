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

package helpers.agent

import models.DateModel
import org.jsoup.Jsoup
import org.scalatest.matchers._
import play.api.libs.ws.WSResponse

import scala.jdk.CollectionConverters.CollectionHasAsScala

trait CustomMatchers {

  def httpStatus(expectedValue: Int): HavePropertyMatcher[WSResponse, Int] =
    (response: WSResponse) => HavePropertyMatchResult(
      response.status == expectedValue,
      "httpStatus",
      expectedValue,
      response.status
    )

  def httpContentType(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    (response: WSResponse) => HavePropertyMatchResult(
      response.contentType.contains(expectedValue),
      "httpContentType",
      expectedValue = expectedValue,
      actualValue = response.contentType
    )

  def pageTitle(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    (response: WSResponse) => {
      val body = Jsoup.parse(response.body)

      HavePropertyMatchResult(
        body.title == expectedValue,
        "pageTitle",
        expectedValue,
        body.title
      )
    }


  def govukDateField(id: String, expectedValue: DateModel): HavePropertyMatcher[WSResponse, String] = (response: WSResponse) => {
    val body = Jsoup.parse(response.body)
    val day = body.getElementById(id + "-dateDay").`val`()
    val month = body.getElementById(id + "-dateMonth").`val`()
    val year = body.getElementById(id + "-dateYear").`val`()
    HavePropertyMatchResult(
      day == expectedValue.day && month == expectedValue.month && year == expectedValue.year,
      "day",
      expectedValue.toString,
      day + " / " + month + " / " + year
    )
  }

  def errorDisplayed(): HavePropertyMatcher[WSResponse, String] =
    (response: WSResponse) => {
      val body = Jsoup.parse(response.body)
      val errorHeaderPresent = Option(body.getElementsByClass("govuk-error-message")).isDefined

      HavePropertyMatchResult(
        errorHeaderPresent,
        "errorDisplayed",
        "error heading found",
        "no error heading found"
      )
    }

  def elementTextBySelector(selector: String)(expectedValue: String): HavePropertyMatcher[WSResponse, String] = (response: WSResponse) => {
    val body = Jsoup.parse(response.body)

    HavePropertyMatchResult(
      body.select(selector).asScala.headOption.exists(_.text == expectedValue),
      s"select($selector)",
      expectedValue,
      body.select(selector).asScala.headOption.map(_.text).getOrElse("")
    )
  }

  def backUrl(expectedValue: String): HavePropertyMatcher[WSResponse, String] = (response: WSResponse) => {
    val body = Jsoup.parse(response.body)
    val backButton = body.select(".govuk-back-link").asScala.headOption

    HavePropertyMatchResult(
      backButton.exists(_.attr("href") == expectedValue),
      "backUrl",
      expectedValue,
      backButton.map(_.attr("href")).getOrElse("")
    )
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

  def radioButtonSet(id: String, selectedRadioButton: Option[String]): HavePropertyMatcher[WSResponse, String] =
    (response: WSResponse) => {
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
