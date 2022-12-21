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

package models.common.business

import models.common.business.BusinessTradeNameModel.{MaximumLengthOfBusinessTradeName, MinimumNumberOfLettersInBusinessTradeName, businessTradeNameRegex}
import play.api.libs.json.{Json, OFormat}

case class BusinessTradeNameModel(businessTradeName: String) {

  def toCleanOption: Option[BusinessTradeNameModel] = {

    val trimmedName = businessTradeNameRegex
      .findAllIn(businessTradeName)
      .matchData
      .mkString(" ")

    val hasFewerThanTwoLetters: Boolean = trimmedName.count(_.isLetter) < MinimumNumberOfLettersInBusinessTradeName
    val isLongerThanMaximumLength: Boolean = trimmedName.length > MaximumLengthOfBusinessTradeName

    if (hasFewerThanTwoLetters || isLongerThanMaximumLength)
      None
    else
      Some(BusinessTradeNameModel(trimmedName))
  }

}

object BusinessTradeNameModel {

  implicit val format: OFormat[BusinessTradeNameModel] = Json.format[BusinessTradeNameModel]

  //business trade name should
  //   follow business rules as of min. 2 characters and max 35 characters
  //   only allow characters of upper case letters, lower case letters, full stops, commas, digits, &, ', \, /, -
  private val businessTradeNameRegex = """[A-Za-z0-9,.&'\\/-]+""".r
  val MaximumLengthOfBusinessTradeName = 35
  val MinimumNumberOfLettersInBusinessTradeName = 2

}
