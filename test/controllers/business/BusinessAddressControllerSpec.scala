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

package controllers.business

import connectors.models.address.AddressLookupRequest
import controllers.ControllerBaseSpec
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{status, _}
import services.mocks.MockAddressLookupService
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future


class BusinessAddressControllerSpec extends ControllerBaseSpec
  with MockAddressLookupService {

  override val controllerName: String = "BusinessAddressLookupController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "init" -> TestBusinessAddressController.init()
  )

  object TestBusinessAddressController extends BusinessAddressController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService,
    mockAddressLookupService
  )

  lazy val testContinueUrl = TestBusinessAddressController.continueUrl(fakeRequest)
  val testRedirectionUrl = "testRedirectionUrl"
  lazy val testRequest = AddressLookupRequest(testContinueUrl)


  "TestBusinessAddressController.init" should {
    def call: Future[Result] = TestBusinessAddressController.init()(fakeRequest)

    "return SEE_OTHER if calls to init was successful" in {
      mockInitSuccess(testRequest)(testRedirectionUrl)

      val result = call
      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result) mustBe Some(testRedirectionUrl)
    }

    "return Technical difficulty if calls to init was unsuccessful" in {
      mockInitFailure(testRequest)

      val result = call
      val ex = intercept[InternalServerException] {
        await(result)
      }
      ex.message mustBe s"BusinessAddressController.init failed unexpectedly, status=$BAD_REQUEST"
    }

  }


}
