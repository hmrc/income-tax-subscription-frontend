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

package controllers.individual.tasklist.ukproperty

import config.{AppConfig, MockConfig}
import config.featureswitch.FeatureSwitch.RemoveAccountingMethod
import config.featureswitch.FeatureSwitching
import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.ControllerSpec
import controllers.individual.ControllerBaseSpec
import controllers.individual.actions.mocks.{MockIdentifierAction, MockSignUpJourneyRefiner}
import models.common.PropertyModel
import models.{Cash, DateModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.{MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.individual.mocks.MockPropertyCheckYourAnswers

import scala.concurrent.Future

class PropertyCheckYourAnswersControllerSpec extends ControllerSpec
  with MockIdentifierAction
  with MockSignUpJourneyRefiner
  with MockSubscriptionDetailsService
  with MockPropertyCheckYourAnswers
  with FeatureSwitching {

  override val appConfig: AppConfig = MockConfig

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(RemoveAccountingMethod)
  }

  "Show" should {
    "return ok and display the page" when {
      "property data is available" when {
        "remove accounting method feature switch is enabled and start date before limit is false" in {
          enable(RemoveAccountingMethod)
          val propertyModel = PropertyModel(startDateBeforeLimit = Some(false))
          mockFetchProperty(Some(propertyModel))
          mockPropertyCheckYourAnswersView(
            viewModel = propertyModel,
            postAction = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(),
            backUrl = controllers.individual.tasklist.ukproperty.routes.PropertyStartDateController.show().url,
            isGlobalEdit = false
          )
          val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = false)(request)
          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }

        "remove accounting method feature switch is disabled" in {
          val propertyModel = PropertyModel()
          mockFetchProperty(Some(propertyModel))
          mockPropertyCheckYourAnswersView(
            viewModel = propertyModel,
            postAction = controllers.individual.tasklist.ukproperty.routes.PropertyCheckYourAnswersController.submit(),
            backUrl = controllers.individual.tasklist.ukproperty.routes.PropertyAccountingMethodController.show().url,
            isGlobalEdit = false
          )
          val result: Future[Result] = TestPropertyCheckYourAnswersController.show(isEditMode = false, isGlobalEdit = false)(request)
          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
      }
    }
    "throw an internal server exception" when {
      "property data is not available" in {

      }
    }
  }
  object TestPropertyCheckYourAnswersController extends PropertyCheckYourAnswersController(
    fakeIdentifierAction,
    fakeSignUpJourneyRefiner,
    mockSubscriptionDetailsService,
    mockView
  )(MockConfig)

}
