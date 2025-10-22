/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.agent

import common.Constants.ITSASessionKeys
import config.AppConfig
import config.featureswitch.FeatureSwitching
import connectors.httpparser.SaveSessionDataHttpParser.{SaveSessionDataSuccessResponse, UnexpectedStatusFailure}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.UsingSoftwareForm
import forms.agent.UsingSoftwareForm.usingSoftwareForm
import forms.submapping.YesNoMapping
import models.No.NO
import models.Yes.YES
import models.status.MandationStatus.{Mandated, Voluntary}
import models.{EligibilityStatus, No, SessionData, Yes}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.JsString
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.{MockGetEligibilityStatusService, MockMandationStatusService, MockSessionDataService}
import uk.gov.hmrc.http.InternalServerException
import views.agent.mocks.MockUsingSoftware

import scala.concurrent.Future

class UsingSoftwareControllerSpec extends ControllerSpec
  with MockUsingSoftware
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockGetEligibilityStatusService
  with MockMandationStatusService
  with MockSessionDataService
  with FeatureSwitching {

  "show" must {
    "return OK with the page content" when {
      "the user is able to sign up for both tax years" in {
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockView(
          usingSoftwareForm = usingSoftwareForm,
          postAction = routes.UsingSoftwareController.submit(),
          clientName = clientDetails.name,
          clientNino = clientDetails.formattedNino,
          backUrl = controllers.agent.eligibility.routes.ClientCanSignUpController.show().url
        )

        val result: Future[Result] = testUsingSoftwareController().show(false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user is able to sign up for next year only" in {
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))
        mockView(
          usingSoftwareForm = usingSoftwareForm,
          postAction = routes.UsingSoftwareController.submit(),
          clientName = clientDetails.name,
          clientNino = clientDetails.formattedNino,
          backUrl = controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url
        )

        val result: Future[Result] = testUsingSoftwareController().show(false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user has answered 'Yes' on question previously" in {
        val sessionData = SessionData(Map(
          ITSASessionKeys.HAS_SOFTWARE -> JsString(YES)
        ))
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockView(
          usingSoftwareForm = usingSoftwareForm.fill(Yes),
          postAction = routes.UsingSoftwareController.submit(),
          clientName = clientDetails.name,
          clientNino = clientDetails.formattedNino,
          backUrl = controllers.agent.eligibility.routes.ClientCanSignUpController.show().url
        )

        val result: Future[Result] = testUsingSoftwareController(sessionData).show(false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user has answered 'No' on question previously" in {
        val sessionData = SessionData(Map(
          ITSASessionKeys.HAS_SOFTWARE -> JsString(NO)
        ))
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockView(
          usingSoftwareForm = usingSoftwareForm.fill(No),
          postAction = routes.UsingSoftwareController.submit(),
          clientName = clientDetails.name,
          clientNino = clientDetails.formattedNino,
          backUrl = controllers.agent.eligibility.routes.ClientCanSignUpController.show().url
        )

        val result: Future[Result] = testUsingSoftwareController(sessionData).show(false)(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" when {
    "the users submission is invalid" should {
      "return a bad request with the page content" in {
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockView(
          usingSoftwareForm = usingSoftwareForm.bind(Map.empty[String, String]),
          postAction = routes.UsingSoftwareController.submit(),
          clientName = clientDetails.name,
          clientNino = clientDetails.formattedNino,
          backUrl = controllers.agent.eligibility.routes.ClientCanSignUpController.show().url
        )

        val result: Future[Result] = testUsingSoftwareController().submit(false)(request)

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
    }
    "the user submits 'Yes'" should {
      "redirect to the what you need to do page" when {
        "the user is eligible for next year only" in {

          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))
          mockSaveSoftwareStatus(Yes)(Right(SaveSessionDataSuccessResponse))

          val result: Future[Result] = testUsingSoftwareController().submit(false)(
            request.withMethod("POST").withFormUrlEncodedBody(
              UsingSoftwareForm.fieldName -> YesNoMapping.option_yes
            )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)
        }
        "the user is mandated for the current tax year" in {

          mockGetMandationService(Mandated, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockSaveSoftwareStatus(Yes)(Right(SaveSessionDataSuccessResponse))

          val result: Future[Result] = testUsingSoftwareController().submit(false)(
            request.withMethod("POST").withFormUrlEncodedBody(
              UsingSoftwareForm.fieldName -> YesNoMapping.option_yes
            )
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.WhatYouNeedToDoController.show().url)
        }
      }
      "redirect to the what year to sign up page" when {
        "the user is able to sign up for both tax years" in {
          mockGetMandationService(Voluntary, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          mockSaveSoftwareStatus(Yes)(Right(SaveSessionDataSuccessResponse))

          Seq(false, true).foreach { editMode =>
            val result: Future[Result] = testUsingSoftwareController().submit(editMode)(
              request.withMethod("POST").withFormUrlEncodedBody(
                UsingSoftwareForm.fieldName -> YesNoMapping.option_yes
              )
            )

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(editMode match {
              case false => controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
              case true => controllers.agent.routes.GlobalCheckYourAnswersController.show.url
            })
          }
        }
      }
    }
    "the user submits 'No'" should {
      "redirect to the no software page" in {
        mockSaveSoftwareStatus(No)(Right(SaveSessionDataSuccessResponse))
        mockGetMandationService(Voluntary, Mandated)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = false))
        val result: Future[Result] = testUsingSoftwareController().submit(false)(
          request.withMethod("POST").withFormUrlEncodedBody(
            UsingSoftwareForm.fieldName -> YesNoMapping.option_no
          )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.NoSoftwareController.show().url)
      }
    }
    "an error occurs when saving the software status" should {
      "throw an internal server exception" in {
        mockGetMandationService(Voluntary, Mandated)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockSaveSoftwareStatus(Yes)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[Result] = testUsingSoftwareController().submit(false)(
          request.withMethod("POST").withFormUrlEncodedBody(
            UsingSoftwareForm.fieldName -> YesNoMapping.option_yes
          )
        )

        intercept[InternalServerException](await(result))
          .message mustBe "[UsingSoftwareController][submit] - Could not save using software answer"
      }
    }
  }

  val appConfig: AppConfig = mock[AppConfig]

  private def testUsingSoftwareController(sessionData: SessionData = SessionData()) = new UsingSoftwareController(
    mockUsingSoftware,
    fakeIdentifierActionWithSessionData(sessionData),
    fakeConfirmedClientJourneyRefiner,
    mockSessionDataService,
    mockGetEligibilityStatusService,
    mockMandationStatusService
  )(appConfig)

}
