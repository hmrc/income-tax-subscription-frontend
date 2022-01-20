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
import org.scalatest.matchers.should.Matchers
import play.api.libs.ws.WSResponse
import utilities.UserMatchingSessionUtil._

trait UserMatchingIntegrationResultSupport extends SessionCookieCrumbler {
  matchers: Matchers =>

  implicit class UserMatchingResultUtil(result: WSResponse) {
    def verifyStoredUserDetailsIs(userDetails: Option[UserDetailsModel]): Unit = {
      val session = getSessionMap(result)

      userDetails match {
        case Some(detail) =>
          session.get(firstName) shouldBe Some(detail.firstName)
          session.get(lastName) shouldBe Some(detail.lastName)
          session.get(dobD) shouldBe Some(detail.dateOfBirth.day)
          session.get(dobM) shouldBe Some(detail.dateOfBirth.month)
          session.get(dobY) shouldBe Some(detail.dateOfBirth.year)
          session.get(nino) shouldBe Some(detail.nino)
        case _ =>
          session.get(firstName) shouldBe None
          session.get(lastName) shouldBe None
          session.get(dobD) shouldBe None
          session.get(dobM) shouldBe None
          session.get(dobY) shouldBe None
          session.get(nino) shouldBe None
      }
    }
  }

}
