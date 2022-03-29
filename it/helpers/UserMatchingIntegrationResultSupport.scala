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

import models.usermatching.UserDetailsModel
import org.scalatest.matchers.must.Matchers
import play.api.libs.ws.WSResponse
import utilities.UserMatchingSessionUtil._

trait UserMatchingIntegrationResultSupport extends SessionCookieCrumbler {
  matchers: Matchers =>

  implicit class UserMatchingResultUtil(result: WSResponse) {
    def verifyStoredUserDetailsIs(userDetails: Option[UserDetailsModel]): Unit = {
      val session = getSessionMap(result)

      userDetails match {
        case Some(detail) =>
          session.get(firstName) mustBe Some(detail.firstName)
          session.get(lastName) mustBe Some(detail.lastName)
          session.get(dobD) mustBe Some(detail.dateOfBirth.day)
          session.get(dobM) mustBe Some(detail.dateOfBirth.month)
          session.get(dobY) mustBe Some(detail.dateOfBirth.year)
          session.get(nino) mustBe Some(detail.nino)
        case _ =>
          session.get(firstName) mustBe None
          session.get(lastName) mustBe None
          session.get(dobD) mustBe None
          session.get(dobM) mustBe None
          session.get(dobY) mustBe None
          session.get(nino) mustBe None
      }
    }
  }

}
