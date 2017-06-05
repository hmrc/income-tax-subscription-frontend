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

package connectors

import audit.Logging
import auth.{authenticatedFakeRequest, mockAuthorisedUserIdCL200, mockMtdItSaEnrolled}
import connectors.mocks.MockHttp
import connectors.models.Enrolment
import org.scalatest.Matchers._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import utils.UnitTestTrait
import utils.TestConstants._

class EnrolmentConnectorSpec extends UnitTestTrait with MockHttp {

  object TestEnrolmentConnector extends EnrolmentConnector(appConfig, mockHttpGet, app.injector.instanceOf[Logging])

  def setupMockEnrolmentGet(status: Int, response: JsValue)(implicit request: Request[AnyContent]): Unit =
    setupMockHttpGet("enrol/enrolments")(status, response)

  "EnrolmentConnector" should {

    def call = await(TestEnrolmentConnector.getEnrolments("enrol"))

    "return an enrolment for an enrolled user" in {
      implicit val request = authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockMtdItSaEnrolled)
      val enrolment = Enrolment(ggServiceName, Seq(), "Activated")
      setupMockEnrolmentGet(OK, Json.toJson(Seq(enrolment)))
      call shouldBe Some(Seq(enrolment))
    }

    "return not enrolled for a user without enrolment" in {
      implicit val request = authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockAuthorisedUserIdCL200)
      setupMockEnrolmentGet(OK, Json.parse("[]"))
      call shouldBe None
    }

    "return None when call to enrolments in unsuccessful" in {
      implicit val request = authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockAuthorisedUserIdCL200)
      setupMockEnrolmentGet(BAD_REQUEST, Json.parse("[]"))
      call shouldBe None
    }
  }

}
