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

package services

import org.scalatest.Matchers._
import services.mocks.MockThrottlingService
import utils.UnitTestTrait

class ThrottlingServiceSpec extends UnitTestTrait
  with MockThrottlingService {

  "ThrottlingService" should {

//    val userEnrolment =
//      Set(Enrolment(Constants.ninoEnrolmentName,
//        Seq(Identifier(Constants.ninoEnrolmentIdentifierKey, TestConstants.testNino)),
//        Enrolment.Activated))
//
//    implicit lazy val request = FakeRequest()
//    implicit lazy val hc = HeaderCarrier()
//
//    "if nino is present for the user, call the throttling connector.check access" in {
//      implicit val user = IncomeTaxSAUser(auth.ggUser.userCL200Context, userEnrolment)
//      setupMockCheckAccess(auth.nino)(OK)
//      await(TestThrottlingService.checkAccess)
//      verifyMockCheckAccess(auth.nino)(1)
//    }
//
//    "if nino is not present for the user, do not call the throttling connector.check access" in {
//      implicit val user = IncomeTaxSAUser(TestUser.noNinoUserContext, Set.empty[Enrolment])
//
//      setupMockCheckAccess(auth.nino)(OK)
//
//      val thrown = intercept[Exception] {
//        await(TestThrottlingService.checkAccess)
//      }
//
//      thrown.isInstanceOf[InternalServerException] shouldBe true
//
//      verifyMockCheckAccess(auth.nino)(0)
//    }

  }

}
