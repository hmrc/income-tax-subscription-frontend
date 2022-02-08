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

package utilities

import models.usermatching.UserDetailsModel
import org.scalatest.MustMatchers
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest

trait UserMatchingTestSupport {
  matcher: MustMatchers =>

  import utilities.UserMatchingSessionUtil._

  implicit class UserMatchingRequestUtil[A](request: FakeRequest[A]) {
    def buildRequest(userDetails: Option[UserDetailsModel]): FakeRequest[A] = {
      userDetails match {
        case Some(model) =>
          request.withSession(
            firstName -> model.firstName,
            lastName -> model.lastName,
            dobD -> model.dateOfBirth.day,
            dobM -> model.dateOfBirth.month,
            dobY -> model.dateOfBirth.year,
            nino -> model.nino
          )
        case _ => request
//          .withHeaders(play.api.http.HeaderNames.COOKIE ->
//          Cookies.mergeCookieHeader(request.headers.get(play.api.http.HeaderNames.COOKIE).getOrElse(""),
//            Seq(Session.encodeAsCookie(new Session(request.session.data - (firstName, lastName, dobD, dobM, dobY, nino))))
//          )
//        )
      }
    }

  }

  implicit class UserMatchingResultUtil(result: Result) {
    def verifyStoredUserDetailsIs[A](userDetails: Option[UserDetailsModel])(request: Request[A]): Unit = {
      val session = result.session(request)

      userDetails match {
        case Some(detail) =>
          withClue("first name") {
            session.get(firstName) mustBe Some(detail.firstName)
          }
          withClue("last name") {
            session.get(lastName) mustBe Some(detail.lastName)
          }
          withClue("dobD") {
            session.get(dobD) mustBe Some(detail.dateOfBirth.day)
          }
          withClue("dobM") {
            session.get(dobM) mustBe Some(detail.dateOfBirth.month)
          }
          withClue("dobY") {
            session.get(dobY) mustBe Some(detail.dateOfBirth.year)
          }
          withClue("nino") {
            session.get(nino) mustBe Some(detail.nino)
          }
        case _ =>
          withClue("first name") {
            session.get(firstName) mustBe None
          }
          withClue("last name") {
            session.get(lastName) mustBe None
          }
          withClue("dobD") {
            session.get(dobD) mustBe None
          }
          withClue("dobM") {
            session.get(dobM) mustBe None
          }
          withClue("dobY") {
            session.get(dobY) mustBe None
          }
          withClue("nino") {
            session.get(nino) mustBe None
          }
      }
    }
  }

}
