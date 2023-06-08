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

package utilities

import common.Constants.ITSASessionKeys
import models.DateModel
import models.usermatching.UserDetailsModel
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.http.InternalServerException

import scala.util.matching.Regex

object UserMatchingSessionUtil {

  val firstName = "FirstName"
  val lastName = "LastName"
  val dobD = "DOBD"
  val dobM = "DOBM"
  val dobY = "DOBY"
  val nino = "MatchingNino"

  implicit class UserMatchingSessionResultUtil(result: Result) {
    def saveUserDetails(userDetails: UserDetailsModel)(implicit request: Request[AnyContent]): Result =
      result.addingToSession(
        firstName -> userDetails.firstName,
        lastName -> userDetails.lastName,
        // this is stored seperately in order to preserve the user's exact input, otherwise leading zeros may be lost
        dobD -> userDetails.dateOfBirth.day,
        dobM -> userDetails.dateOfBirth.month,
        dobY -> userDetails.dateOfBirth.year,
        nino -> userDetails.nino
      )

    def clearAllUserDetails(implicit request: Request[AnyContent]): Result =
      result.removingFromSession(
        firstName,
        lastName,
        dobD,
        dobM,
        dobY,
        nino
      )

    def clearUserDetailsExceptName(implicit request: Request[AnyContent]): Result =
      result.removingFromSession(
        dobD,
        dobM,
        dobY,
        nino
      )

    def clearUserName(implicit request: Request[AnyContent]): Result =
      result.removingFromSession(
        firstName,
        lastName
      )
  }

  implicit class UserMatchingSessionRequestUtil(request: Request[AnyContent]) {
    def fetchUserDetails: Option[UserDetailsModel] =
      (request.session.get(firstName),
        request.session.get(lastName),
        request.session.get(dobD),
        request.session.get(dobM),
        request.session.get(dobY),
        request.session.get(nino)) match {
        case (Some(f), Some(l), Some(dd), Some(dm), Some(dy), Some(n)) =>
          Some(UserDetailsModel(firstName = f, lastName = l, nino = n, dateOfBirth = DateModel(dd, dm, dy)))
        case _ => None
      }

    def fetchClientName: Option[String] =
      (request.session.get(firstName),
        request.session.get(lastName)) match {
        case (Some(f), Some(l)) =>
          Some(f + " " + l)
        case _ => None
      }

    def fetchClientNino: Option[String] =
      request.session.get(nino)

    def fetchConfirmedClientNino: Option[String] =
      request.session.get(ITSASessionKeys.NINO)

    def clientDetails: ClientDetails = {
      (request.fetchClientName, request.fetchConfirmedClientNino) match {
        case (Some(name), Some(nino)) => ClientDetails(name, nino)
        case _ => throw new InternalServerException("[IncomeTaxAgentUser][clientDetails] - could not retrieve client details from session")
      }
    }

  }


  case class ClientDetails(name: String, nino: String) {

    private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

    val formattedNino: String = nino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }

  }

}
