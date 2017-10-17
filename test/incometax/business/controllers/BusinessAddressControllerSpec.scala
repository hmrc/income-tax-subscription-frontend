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

package incometax.business.controllers

import controllers.ITSASessionKeys
import core.auth.{MockConfig, Registration}
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import core.utils.TestModels._
import incometax.business.services.mocks.MockAddressLookupService
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}

import scala.concurrent.Future


class BusinessAddressControllerSpec extends ControllerBaseSpec
  with MockAddressLookupService
  with MockKeystoreService {

  override val controllerName: String = "BusinessAddressLookupController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "init" -> TestBusinessAddressController.init(editMode = false),
    "callBack" -> TestBusinessAddressController.callBack(editMode = false, "")
  )

  lazy val request: FakeRequest[AnyContentAsEmpty.type] = subscriptionRequest.withSession(ITSASessionKeys.JourneyStateKey -> Registration.name)

  def createTestBusinessAddressController(setEnableRegistration: Boolean): BusinessAddressController = new BusinessAddressController(
    mockBaseControllerConfig(new MockConfig {
      override val enableRegistration: Boolean = setEnableRegistration
    }),
    messagesApi,
    mockAuthService,
    mockAddressLookupService,
    MockKeystoreService
  )

  lazy val TestBusinessAddressController: BusinessAddressController =
    createTestBusinessAddressController(setEnableRegistration = true)


  "When registration is disabled" should {
    lazy val TestBusinessAddressController: BusinessAddressController =
      createTestBusinessAddressController(setEnableRegistration = false)

    "BusinessAddressController.init" when {
      for (editMode <- Seq(true, false)) {
        s"Edit mode is $editMode" should {
          "return NOT FOUND" in {
            val result = TestBusinessAddressController.init(editMode)(request)
            val ex = intercept[NotFoundException] {
              await(result)
            }
            ex.message must startWith("This page for registration is not yet available to the public:")
          }

          "callBack" should {
            "return NOT FOUND" in {
              val result = TestBusinessAddressController.callBack(editMode, "")(request)
              val ex = intercept[NotFoundException] {
                await(result)
              }
              ex.message must startWith("This page for registration is not yet available to the public:")
            }
          }
        }
      }
    }
  }

  "When registration is enabled" should {

    "BusinessAddressController.show" when {
      def call(editMode: Boolean): Future[Result] = TestBusinessAddressController.show(editMode = editMode)(request)

      for (editMode <- Seq(true, false)) {
        s"Edit mode is $editMode" when {
          "There is no address saved in keystore redirect to init" in {
            setupMockKeystore(fetchBusinessAddress = None)

            val result = call(editMode)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(incometax.business.controllers.routes.BusinessAddressController.init().url)

            await(result)
            verifyKeystore(fetchBusinessAddress = 1, saveBusinessAddress = 0)
          }

          "There is an address in keystore" in {
            setupMockKeystore(fetchBusinessAddress = testAddress)

            val result = call(editMode)

            status(result) mustBe OK
            await(result)
            verifyKeystore(fetchBusinessAddress = 1, saveBusinessAddress = 0)
          }
        }
      }


    }

    "BusinessAddressController.submit" should {
      def call(editMode: Boolean): Future[Result] = TestBusinessAddressController.submit(editMode = editMode)(request)

      "when editMode is true" in {
        val result = call(editMode = true)
        status(result)
        redirectLocation(result) mustBe Some(incometax.business.controllers.routes.BusinessAddressController.init(editMode = true).url)
      }

      "when editMode is false" in {
        val result = call(editMode = false)
        status(result)
        redirectLocation(result) mustBe Some(incometax.business.controllers.routes.BusinessStartDateController.show().url)
      }
    }

    "BusinessAddressController.init" should {
      val testRedirectionUrl = "testRedirectionUrl"
      lazy val testRequest = TestBusinessAddressController.initConfig(editMode = false)(request)

      def call: Future[Result] = TestBusinessAddressController.init(editMode = false)(request)

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

    "TestBusinessAddressController.callback" when {
      def call(editMode: Boolean, id: String): Future[Result] = TestBusinessAddressController.callBack(editMode = editMode, id)(request)

      val testId = "1234567890"

      "an UK address is returned" when {
        for (editMode <- Seq(true, false)) {
          s"Edit mode is $editMode" should {
            "fetch and persist the address if the call is successful" in {
              mockRetrieveAddressSuccess(testId)
              setupMockKeystoreSaveFunctions()

              val result = call(editMode, testId)

              status(result) must be(Status.SEE_OTHER)
              if (editMode)
                redirectLocation(result).get mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
              else
                redirectLocation(result).get mustBe incometax.business.controllers.routes.BusinessStartDateController.show().url

              verifyKeystore(saveBusinessAddress = 1)
            }
          }
        }

        "return Technical difficulty if the fetch fails" in {
          MockRetrieveAddressFailure(testId)

          val result = call(editMode = false, testId)
          val ex = intercept[InternalServerException] {
            await(result)
          }
          ex.message mustBe s"BusinessAddressController.callBack failed unexpectedly, status=$BAD_REQUEST"

          verifyKeystore(saveBusinessAddress = 0)
        }
      }

      "a non UK address is returend" should {
        // TODO goto the validation error page
        "return Not implemented" in {
          mockRetrieveAddressNoneUK(testId)

          val result = call(editMode = false, testId)

          status(result) mustBe NOT_IMPLEMENTED

          verifyKeystore(saveBusinessAddress = 0)
        }
      }

    }
  }

  "the address lookup core.config" when {
    import assets.MessageLookup.Base._
    import assets.MessageLookup.BusinessAddress._

    for (editMode <- Seq(true, false)) {
      lazy val conf = TestBusinessAddressController.initConfig(editMode)(request)

      s"Edit mode is $editMode" should {
        "should have the correct parameters" in {
          conf.continueUrl mustBe TestBusinessAddressController.callbackUrl(editMode)(request)
          conf.showBackButtons mustBe Some(true)

          val lookup = conf.lookupPage.get
          lookup.heading mustBe Some(Lookup.heading)
          lookup.filterLabel mustBe Some(Lookup.nameOrNimber)
          lookup.submitLabel mustBe Some(Lookup.submit)
          lookup.manualAddressLinkText mustBe Some(Lookup.enterManually)

          val select = conf.selectPage.get
          select.title mustBe Some(Select.title)
          select.heading mustBe Some(Select.heading)
          select.showSearchAgainLink mustBe Some(true)
          select.editAddressLinkText mustBe Some(Select.edit)

          val confirm = conf.confirmPage.get
          confirm.heading mustBe Some(Confirm.heading)
          confirm.showChangeLink mustBe Some(false)
          confirm.showSearchAgainLink mustBe Some(true)
          confirm.searchAgainLinkText mustBe Some(Confirm.change)
          if (editMode)
            confirm.submitLabel mustBe Some(update)
          else
            confirm.submitLabel mustBe None

          val edit = conf.editPage.get
          edit.heading mustBe Some(Edit.heading)
          edit.line1Label mustBe Some(Edit.addLine1)
          edit.line2Label mustBe Some(Edit.addLine2)
          edit.line3Label mustBe Some(Edit.addLine3)
          edit.showSearchAgainLink mustBe Some(true)
        }
      }
    }

  }

}
