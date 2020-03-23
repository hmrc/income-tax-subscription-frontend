/*
 * Copyright 2020 HM Revenue & Customs
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

package models.usermatching

import utilities.{TestModels, UnitTestTrait}


class UserMatchRequestModelSpec extends UnitTestTrait {

  "UserMatchRequestModel" should {

    "implicitly convert from UserDetailsModel" in {
      // nino is updated to add spaces, this is to test this conversion also removes all the spaces
      val input = TestModels.testUserDetails.copy(nino = " " + TestModels.testUserDetails.nino.toCharArray.mkString(" ") + " ")
      val converted: UserMatchRequestModel = UserMatchRequestModel(input)
      input.firstName mustBe converted.firstName
      input.lastName mustBe converted.lastName
      input.nino must not be converted.nino.toString
      input.nino.replace(" ", "") mustBe converted.nino.toString
      input.dateOfBirth.toLocalDate mustBe converted.dateOfBirth
    }

  }

}
