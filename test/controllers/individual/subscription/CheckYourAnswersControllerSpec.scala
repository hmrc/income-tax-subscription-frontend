/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.individual.subscription

import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import models.individual.subscription.{Both, Business, Property}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.individual.mocks.MockSubscriptionOrchestrationService
import services.mocks.MockKeystoreService
import uk.gov.hmrc.http.InternalServerException
import utilities.CacheUtil._
import utilities.TestModels._
import utilities.individual.TestConstants._

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockSubscriptionOrchestrationService
  with FeatureSwitching {

  override val controllerName: String = "CheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestCheckYourAnswersController.show,
    "submit" -> TestCheckYourAnswersController.submit
  )

  object TestCheckYourAnswersController extends CheckYourAnswersController(
    mockAuthService,
    MockKeystoreService,
    mockSubscriptionOrchestrationService
  )

  "Calling the show action of the CheckYourAnswersController with an authorised user" when {

    def result: Future[Result] = TestCheckYourAnswersController.show(subscriptionRequest)

    "return ok (200) for business journey" in {
      val testBusinessCacheMap = testCacheMapCustom(
        rentUkProperty = testRentUkProperty_no_property,
        areYouSelfEmployed = testAreYouSelfEmployed_yes
      )
      mockFetchAllFromKeyStore(testBusinessCacheMap)

      status(result) must be(Status.OK)
    }

    "return ok (200) for property only journey)" in {
      val testPropertyCacheMap = testCacheMap(
        rentUkProperty = testRentUkProperty_property_only,
        areYouSelfEmployed = None
      )
      mockFetchAllFromKeyStore(testPropertyCacheMap)

      status(result) must be(Status.OK)
    }

    "return ok (200) for property and other income but no sole trader journey" in {
      val testPropertyCacheMap = testCacheMap(
        rentUkProperty = testRentUkProperty_property_and_other,
        areYouSelfEmployed = testAreYouSelfEmployed_no
      )
      mockFetchAllFromKeyStore(testPropertyCacheMap)

      status(result) must be(Status.OK)
    }

    "return ok (200) for both journey" in {
      mockFetchAllFromKeyStore(testCacheMap)

      status(result) must be(Status.OK)
    }
  }

  "Calling the submit action of the CheckYourAnswersController with an authorised user" should {

    lazy val request = subscriptionRequest

    def call: Future[Result] = TestCheckYourAnswersController.submit(request)

    "When the submission is successful" should {
      lazy val result = call

      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()
        mockFetchAllFromKeyStore(testCacheMap)
        mockCreateSubscriptionSuccess(testNino, testCacheMap.getSummary())
        status(result) must be(Status.SEE_OTHER)
        await(result)
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 1)
      }

      s"redirect to '${controllers.individual.subscription.routes.ConfirmationController.show().url}'" in {
        redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.ConfirmationController.show().url)
      }
    }

    "When the submission is unsuccessful" should {
      lazy val result = call

      "return a internalServer error" in {
        mockFetchAllFromKeyStore(testCacheMap)
        mockCreateSubscriptionFailure(testNino, testCacheMap.getSummary())
        intercept[InternalServerException](await(result)).message must include("Successful response not received from submission")
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
      }
    }

  }

  "The back url" when {
    s"point to the ${controllers.individual.business.routes.BusinessAccountingMethodController.show().url} when the income source is Business" in {
      TestCheckYourAnswersController.backUrl(incomeSource = Business) mustBe
        controllers.individual.business.routes.BusinessAccountingMethodController.show().url
    }

    s"point to the ${controllers.individual.business.routes.PropertyAccountingMethodController.show().url} when the income source is Both" in {
      TestCheckYourAnswersController.backUrl(incomeSource = Both) mustBe
        controllers.individual.business.routes.PropertyAccountingMethodController.show().url
    }

    s"point to the ${controllers.individual.business.routes.PropertyAccountingMethodController.show().url} when the income source is Property" in {
      TestCheckYourAnswersController.backUrl(incomeSource = Property) mustBe
        controllers.individual.business.routes.PropertyAccountingMethodController.show().url
    }
  }

  authorisationTests()

}
