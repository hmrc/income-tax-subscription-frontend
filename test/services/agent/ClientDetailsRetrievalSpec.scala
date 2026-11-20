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

package services.agent

import models.{DateModel, SessionData}
import models.requests.agent.IdentifierRequest
import models.usermatching.UserDetailsModel
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.MockNinoService
import services.mocks.MockSessionDataService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.individual.TestConstants.testUserDetails
import org.mockito.Mockito.when

import scala.concurrent.ExecutionContext.Implicits.global

class ClientDetailsRetrievalSpec extends PlaySpec with Matchers with MockNinoService with MockSessionDataService {

  val testNino: String = testUserDetails.nino

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val request: FakeRequest[AnyContent] = FakeRequest()

  val firstName: String = testUserDetails.firstName
  val lastName: String = testUserDetails.lastName
  val clientDetails: ClientDetails = ClientDetails(s"$firstName $lastName", testNino)
  val sessionData: SessionData = mockSessionData
  implicit val identifierRequest: IdentifierRequest[AnyContent] = IdentifierRequest(request, "", sessionData)

  trait Setup {
    val service: ClientDetailsRetrieval = new ClientDetailsRetrieval(
      mockNinoService
    )
  }

  "getClientDetails" must {
    "return client details" when {
      "the clients name is present in session" in new Setup {
        mockGetNino(testNino)
        mockFetchUserDetails(Some(testUserDetails))

        await(service.getClientDetails(sessionData)(identifierRequest, implicitly)) mustBe clientDetails
      }
    }
    "return an exception" when {
      "the clients name is not present in session" in new Setup {
        mockGetNino(testNino)
        mockFetchUserDetails(None)

        intercept[InternalServerException](await(service.getClientDetails(sessionData)(identifierRequest, implicitly)))
          .message mustBe "[ClientDetailsRetrieval][getClientDetails] - Unable to retrieve name from session"
      }
    }
  }

}
