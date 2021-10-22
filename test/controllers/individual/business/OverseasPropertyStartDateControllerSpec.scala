/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.individual.business

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.{ReleaseFour, SaveAndRetrieve}
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import forms.individual.business.OverseasPropertyStartDateForm
import models.DateModel
import models.common.{IncomeSourceModel, OverseasPropertyStartDateModel}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.individual.mocks.MockAuthService
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.OverseasPropertyStartDate
import utilities.TestModels.{testCacheMap, testIncomeSourceBoth, testIncomeSourceOverseasProperty}
import views.individual.mocks.MockOverseasPropertyStartDate

import java.time.LocalDate
import scala.concurrent.Future

class OverseasPropertyStartDateControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuthService with MockAuditingService with FeatureSwitching with MockOverseasPropertyStartDate {

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  override val controllerName: String = "OverseasPropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOverseasPropertyStartDateController.show(isEditMode = false),
    "submit" -> TestOverseasPropertyStartDateController.submit(isEditMode = false)
  )

  object TestOverseasPropertyStartDateController extends OverseasPropertyStartDateController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockLanguageUtils,
    overseasPropertyStartDate
  )

  val incomeSourceAllTypes: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)

  val incomeSourceSelfEmployAndOverseasProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)

  val incomeSourceUkAndOverseasProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)

  val incomeSourceOverseasPropertyOnly: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)

  def foreignPropertyIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceOverseasProperty)

  def bothIncomeSourceType: CacheMap = testCacheMap(incomeSource = testIncomeSourceBoth)


  "show" should {
    "display the foreign property start date view and return OK (200)" in withController { controller =>
      disable(SaveAndRetrieve)
      mockOverseasPropertyStartDate()

      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockIndividualWithNoEnrolments()
      mockFetchAllFromSubscriptionDetails(testCacheMap(
        incomeSource = Some(incomeSourceAllTypes)
      ))

      status(result) must be(Status.OK)
      verifySubscriptionDetailsSave(OverseasPropertyStartDate, 0)
      verifySubscriptionDetailsFetchAll(1)
    }

    "display the foreign property start date view and return OK (200) when Save & Retrieve feature is enabled" in withController { controller =>
      enable(SaveAndRetrieve)
      mockOverseasPropertyStartDate()

      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockIndividualWithNoEnrolments()
      mockFetchAllFromSubscriptionDetails(testCacheMap())

      status(result) must be(Status.OK)
      verifySubscriptionDetailsSave(OverseasPropertyStartDate, 0)
      verifySubscriptionDetailsFetchAll(1)
    }
  }

  "submit" should {

    val testValidMaxStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
    val testValidMinStartDate: DateModel = DateModel.dateConvert(LocalDate.of(1900, 1, 1))

    val testOverseasPropertyStartDateModel: OverseasPropertyStartDateModel = OverseasPropertyStartDateModel(testValidMaxStartDate)

    def callShow(controller: OverseasPropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequest.post(OverseasPropertyStartDateForm.overseasPropertyStartDateForm(testValidMinStartDate.toString, testValidMaxStartDate.toString),
          testOverseasPropertyStartDateModel)
      )

    def callShowWithErrorForm(controller: OverseasPropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequest
      )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in withController { controller =>
        mockIndividualWithNoEnrolments()
        setupMockSubscriptionDetailsSaveFunctions()
        val goodRequest = callShow(controller, isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyStartDate, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

      "redirect to foreign property accounting method page" in withController { controller =>
        mockIndividualWithNoEnrolments()
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(controller, isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyStartDate, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in withController { controller =>
        mockIndividualWithNoEnrolments()
        setupMockSubscriptionDetailsSaveFunctions()


        val goodRequest = callShow(controller, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyStartDate, 1)
        verifySubscriptionDetailsFetchAll(1)
      }

      "redirect to checkYourAnswer page" in withController { controller =>
        disable(SaveAndRetrieve)
        mockIndividualWithNoEnrolments()
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(controller, isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyStartDate, 1)
        verifySubscriptionDetailsFetchAll(1)

      }

      "redirect to taskList page" in withController { controller =>
        enable(SaveAndRetrieve)
        mockIndividualWithNoEnrolments()
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(controller, isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsSave(OverseasPropertyStartDate, 1)
        verifySubscriptionDetailsFetchAll(1)
      }
    }

    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in withController { controller =>
        disable(SaveAndRetrieve)
        mockOverseasPropertyStartDate()
        mockIndividualWithNoEnrolments()
        mockFetchIndividualIncomeSourceFromSubscriptionDetails(Some(testIncomeSourceOverseasProperty))

        val badRequest = callShowWithErrorForm(controller, isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(OverseasPropertyStartDate, 0)
        verifySubscriptionDetailsFetchAll(1)
      }

      "return bad request status (400) when Save & Retrieve feature is enabled" in withController { controller =>
        enable(SaveAndRetrieve)
        mockOverseasPropertyStartDate()

        val badRequest = callShowWithErrorForm(controller, isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(OverseasPropertyStartDate, 0)
        verifySubscriptionDetailsFetchAll(0)
      }
    }
  }

  "backUrl" when {
    "in edit mode" when {
      "save and retrieve is enabled" should {
        "redirect to the task list page" in withController { controller =>
          enable(SaveAndRetrieve)
          controller.backUrl(isEditMode = true, maybeIncomeSourceModel = None) mustBe
            controllers.individual.business.routes.TaskListController.show().url
        }
      }
      "save and retrieve is disabled" should {
        "redirect to the check your answer page" in withController { controller =>
          controller.backUrl(isEditMode = true, maybeIncomeSourceModel = None) mustBe
            controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }
      }
    }

    "not in edit mode" when {
      "save and retrieve is enabled" should {
        "redirect to add business page" in withController { controller =>
          enable(SaveAndRetrieve)

          controller.backUrl(isEditMode = false, maybeIncomeSourceModel = None) mustBe
            controllers.individual.incomesource.routes.IncomeSourceController.show().url
        }
      }
      "save and retrieve is disabled and the user has uk property income" should {
        "redirect to uk property accounting method page" in withController { controller =>

          val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)
          controller.backUrl(isEditMode = false, maybeIncomeSourceModel = Some(incomeSourceModel)) mustBe
            controllers.individual.business.routes.PropertyAccountingMethodController.show().url
        }
      }

      "save and retrieve is disabled and the user has self-employment income but no uk property" should {
        "redirect to uk business accounting method page" in withController { controller =>

          val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)
          controller.backUrl(isEditMode = false, maybeIncomeSourceModel = Some(incomeSourceModel)) mustBe
            appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/details/business-accounting-method"
        }
      }

      "save and retrieve is disabled and the user only has overseas property" should {
        "redirect to income source page" in withController { controller =>

          val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)
          controller.backUrl(isEditMode = false, maybeIncomeSourceModel = Some(incomeSourceModel)) mustBe
            controllers.individual.incomesource.routes.IncomeSourceController.show().url
        }
      }
    }
  }

  private def withController(testCode: OverseasPropertyStartDateController => Any) = {
    val controller = new OverseasPropertyStartDateController(
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockLanguageUtils,
      overseasPropertyStartDate
    )

    testCode(controller)
  }

}
