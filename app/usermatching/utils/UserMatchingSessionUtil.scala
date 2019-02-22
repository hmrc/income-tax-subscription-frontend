/*
 * Copyright 2019 HM Revenue & Customs
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

package usermatching.utils

import core.models.DateModel
import play.api.mvc.{AnyContent, Request, Result}
import usermatching.models.UserDetailsModel


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

    def clearUserDetails(implicit request: Request[AnyContent]): Result =
      result.removingFromSession(
        firstName,
        lastName,
        dobD,
        dobM,
        dobY,
        nino
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
  }

}
