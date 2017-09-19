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

import auth.MockConfig
import connectors.models.address.AddressLookupInitRequest
import controllers.ControllerBaseSpec
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{status, _}
import services.mocks.{MockAddressLookupService, MockKeystoreService}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}

import scala.concurrent.Future


class BusinessAddressControllerSpec extends ControllerBaseSpec
  with MockAddressLookupService
  with MockKeystoreService {

  override val controllerName: String = "BusinessAddressLookupController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "init" -> TestBusinessAddressController.init(),
    "callBack" -> TestBusinessAddressController.callBack("")
  )

  def createTestBusinessAddressController(setEnableRegistration: Boolean) = new BusinessAddressController(
    mockBaseControllerConfig(new MockConfig {
      override val enableRegistration = setEnableRegistration
    }),
    messagesApi,
    mockAuthService,
    mockAddressLookupService,
    MockKeystoreService
  )

  lazy val TestBusinessAddressController: BusinessAddressController =
    createTestBusinessAddressController(setEnableRegistration = true)

  lazy val testContinueUrl = TestBusinessAddressController.continueUrl(fakeRequest)
  val testRedirectionUrl = "testRedirectionUrl"
  lazy val testRequest = AddressLookupInitRequest(testContinueUrl)


  "When registration is disabled" should {
    lazy val TestBusinessAddressController: BusinessAddressController =
      createTestBusinessAddressController(setEnableRegistration = false)

    "init" should {
      "return NOT FOUND" in {
        val result = TestBusinessAddressController.init()(fakeRequest)
        val ex = intercept[NotFoundException] {
          await(result)
        }
        ex.message must startWith("This page for registration is not yet avaiable to the public:")
      }
    }

    "callBack" should {
      "return NOT FOUND" in {
        val result = TestBusinessAddressController.callBack("")(fakeRequest)
        val ex = intercept[NotFoundException] {
          await(result)
        }
        ex.message must startWith("This page for registration is not yet avaiable to the public:")
      }
    }
  }

  "When registration is enabled" should {

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

    "TestBusinessAddressController.callback" should {
      def call(id: String): Future[Result] = TestBusinessAddressController.callBack(id)(fakeRequest)

      val testId = "1234567890"

      "fetch and persist the address if the call is successful" in {
        mockRetrieveAddressSuccess(testId)
        setupMockKeystoreSaveFunctions()

        val result = call(testId)

        //TODO redirect to business start date when it becomes available
        status(result) must be(Status.NOT_IMPLEMENTED)

        verifyKeystore(saveBusinessAddress = 1)
      }

      "return Technical difficulty if the fetch fails" in {
        MockRetrieveAddressFailure(testId)

        val result = call(testId)
        val ex = intercept[InternalServerException] {
          await(result)
        }
        ex.message mustBe s"BusinessAddressController.callBack failed unexpectedly, status=$BAD_REQUEST"

        verifyKeystore(saveBusinessAddress = 0)
      }
    }

  }

}
