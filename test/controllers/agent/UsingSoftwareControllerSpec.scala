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

import config.AppConfig
import config.featureswitch.FeatureSwitch.PrePopulate
import config.featureswitch.FeatureSwitching
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataSuccessResponse
import connectors.httpparser.{GetSessionDataHttpParser, SaveSessionDataHttpParser}
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.UsingSoftwareForm
import forms.agent.UsingSoftwareForm.usingSoftwareForm
import forms.submapping.YesNoMapping
import models.{EligibilityStatus, No, Yes}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.{MockGetEligibilityStatusService, MockSessionDataService}
import uk.gov.hmrc.http.InternalServerException
import views.agent.mocks.MockUsingSoftware

import scala.concurrent.Future

class UsingSoftwareControllerSpec extends ControllerSpec
  with MockUsingSoftware
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with MockGetEligibilityStatusService
  with MockSessionDataService
  with FeatureSwitching {


  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(PrePopulate)
  }

  "show" must {
    "return OK with the page content" when {
      "the user is able to sign up for both tax years" in {
        mockFetchSoftwareStatus(Right(None))
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockView(
          usingSoftwareForm = usingSoftwareForm,
          postAction = routes.UsingSoftwareController.submit,
          clientName = clientDetails.name,
          clientNino = clientDetails.formattedNino,
          backUrl = controllers.agent.eligibility.routes.ClientCanSignUpController.show().url
        )

        val result: Future[Result] = TestUsingSoftwareController.show(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user is able to sign up for next year only" in {
        mockFetchSoftwareStatus(Right(None))
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))
        mockView(
          usingSoftwareForm = usingSoftwareForm,
          postAction = routes.UsingSoftwareController.submit,
          clientName = clientDetails.name,
          clientNino = clientDetails.formattedNino,
          backUrl = controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url
        )

        val result: Future[Result] = TestUsingSoftwareController.show(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user has answered 'Yes' on question previously" in {
        mockFetchSoftwareStatus(Right(Some(Yes)))
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockView(
          usingSoftwareForm = usingSoftwareForm.fill(Yes),
          postAction = routes.UsingSoftwareController.submit,
          clientName = clientDetails.name,
          clientNino = clientDetails.formattedNino,
          backUrl = controllers.agent.eligibility.routes.ClientCanSignUpController.show().url
        )

        val result: Future[Result] = TestUsingSoftwareController.show(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user has answered 'No' on question previously" in {
        mockFetchSoftwareStatus(Right(Some(No)))
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockView(
          usingSoftwareForm = usingSoftwareForm.fill(No),
          postAction = routes.UsingSoftwareController.submit,
          clientName = clientDetails.name,
          clientNino = clientDetails.formattedNino,
          backUrl = controllers.agent.eligibility.routes.ClientCanSignUpController.show().url
        )

        val result: Future[Result] = TestUsingSoftwareController.show(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
    "throw an InternalServerException" when {
      "there was a problem fetching the users previous answer" in {
        mockFetchSoftwareStatus(Left(GetSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))

        intercept[InternalServerException](await(TestUsingSoftwareController.show(request)))
          .message mustBe "[UsingSoftwareController][show] - Could not fetch software status"
      }
    }
  }

  "submit" must {
    "redirect to the what year to sign up page" when {
      "the user selects 'Yes' and the prepopulate feature switch is enabled" in {
        enable(PrePopulate)
        mockSaveSoftwareStatus(Yes)(Right(SaveSessionDataSuccessResponse))

        val result: Future[Result] = TestUsingSoftwareController.submit(
          request.withMethod("POST").withFormUrlEncodedBody(
            UsingSoftwareForm.fieldName -> YesNoMapping.option_yes
          )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
      }
    }
    "redirect to the no software page" when {
      "the user selects 'No' and the prepopulate feature switch is enabled" in {
        enable(PrePopulate)
        mockSaveSoftwareStatus(No)(Right(SaveSessionDataSuccessResponse))

        val result: Future[Result] = TestUsingSoftwareController.submit(
          request.withMethod("POST").withFormUrlEncodedBody(
            UsingSoftwareForm.fieldName -> YesNoMapping.option_no
          )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.NoSoftwareController.show().url)
      }
    }
    "redirect to the what you need to do page" when {
      "the user selects any of the radio options and the prepopulate feature switch is disabled" in {
        mockSaveSoftwareStatus(No)(Right(SaveSessionDataSuccessResponse))

        val result: Future[Result] = TestUsingSoftwareController.submit(
          request.withMethod("POST").withFormUrlEncodedBody(
            UsingSoftwareForm.fieldName -> YesNoMapping.option_no
          )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatYouNeedToDoController.show().url)
      }
    }
    "return a bad request with the page content" when {
      "the user does not select an option and is eligible to sign up for both tax years" in {
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockView(
          usingSoftwareForm = usingSoftwareForm.bind(Map.empty[String, String]),
          postAction = routes.UsingSoftwareController.submit,
          clientName = clientDetails.name,
          clientNino = clientDetails.formattedNino,
          backUrl = controllers.agent.eligibility.routes.ClientCanSignUpController.show().url
        )

        val result: Future[Result] = TestUsingSoftwareController.submit(
          request.withMethod("POST").withFormUrlEncodedBody()
        )

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
      "the user does not select an option and is eligible to sign up for next year only" in {
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))
        mockView(
          usingSoftwareForm = usingSoftwareForm.bind(Map.empty[String, String]),
          postAction = routes.UsingSoftwareController.submit,
          clientName = clientDetails.name,
          clientNino = clientDetails.formattedNino,
          backUrl = controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url
        )

        val result: Future[Result] = TestUsingSoftwareController.submit(
          request.withMethod("POST").withFormUrlEncodedBody()
        )

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
    }
    "throw an InternalServerException" when {
      "saving the users choice returns a failure" in {
        mockSaveSoftwareStatus(Yes)(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(TestUsingSoftwareController.submit(
          request.withMethod("POST").withFormUrlEncodedBody(
            UsingSoftwareForm.fieldName -> YesNoMapping.option_yes
          )
        ))).message mustBe "[UsingSoftwareController][submit] - Could not save using software answer"
      }
    }
  }

  val appConfig: AppConfig = mock[AppConfig]

  object TestUsingSoftwareController extends UsingSoftwareController(
    mockUsingSoftware,
    fakeIdentifierAction,
    fakeConfirmedClientJourneyRefiner,
    mockSessionDataService,
    mockGetEligibilityStatusService,
  )(appConfig)

}
