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

import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.MockNinoService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.UserMatchingSessionUtil
import utilities.UserMatchingSessionUtil.ClientDetails

import scala.concurrent.ExecutionContext.Implicits.global

class ClientDetailsRetrievalSpec extends PlaySpec with Matchers with MockNinoService {

  val testNino: String = "test-nino"

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val request: FakeRequest[AnyContent] = FakeRequest()

  val firstName: String = "FirstName"
  val lastName: String = "LastName"
  val clientDetails: ClientDetails = ClientDetails(s"$firstName $lastName", testNino)

  trait Setup {
    val service: ClientDetailsRetrieval = new ClientDetailsRetrieval(
      mockNinoService
    )
  }

  "getClientDetails" must {
    "return client details" when {
      "the clients name is present in session" in new Setup {
        mockGetNino(testNino)

        await(service.getClientDetails()(request.withSession(
          UserMatchingSessionUtil.firstName -> firstName,
          UserMatchingSessionUtil.lastName -> lastName
        ), implicitly)) mustBe clientDetails
      }

    }
    "return an exception" when {
      "the clients name is not present in session" in new Setup {
        mockGetNino(testNino)

        intercept[InternalServerException](await(service.getClientDetails()(request, implicitly)))
          .message mustBe "[ClientDetailsRetrieval][getClientDetails] - Unable to retrieve name from session"
      }
    }
  }

}
