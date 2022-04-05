/*
 * Copyright 2022 HM Revenue & Customs
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
import config.featureswitch.FeatureSwitch.{ForeignProperty, SaveAndRetrieve}
import controllers.ControllerBaseSpec
import forms.individual.business.AccountingMethodPropertyForm
import models.common.PropertyModel
import models.{AccountingMethod, Accruals, Cash}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.TestModels._
import views.html.individual.incometax.business.PropertyAccountingMethod

import scala.concurrent.Future

class PropertyAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService  {

  override val controllerName: String = "PropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  private def withController(testCode: PropertyAccountingMethodController => Any): Unit = {
    val propertyAccountingMethodView = mock[PropertyAccountingMethod]

    when(propertyAccountingMethodView(any(), any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new PropertyAccountingMethodController(
      mockAuditingService,
      propertyAccountingMethodView,
      mockAuthService,
      MockSubscriptionDetailsService
    )

    testCode(controller)
  }

  def propertyOnlyIncomeSourceType: CacheMap = testCacheMap(incomeSource = Some(testIncomeSourceProperty))

  def allThreeIncomeSourcesType: CacheMap = testCacheMap(incomeSource = Some(testIncomeSourceAll))

  def bothIncomeSourceType: CacheMap = testCacheMap(incomeSource = Some(testIncomeSourceBoth))

  "show" should {
    "display the property accounting method view and return OK (200)" in withController { controller =>
      mockFetchProperty(None)
      mockFetchAllFromSubscriptionDetails(Some(propertyOnlyIncomeSourceType))

      lazy val result = await(controller.show(isEditMode = false)(subscriptionRequest))

      status(result) must be(Status.OK)
    }
  }

  "submit" should withController { controller =>

    def callShow(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Cash)
    )

    def callShowWithErrorForm(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    val testAccountingMethod: AccountingMethod = Cash

    "When it is not in edit mode" when {

      "save and retrieve is enabled" should {
        "redirect to uk property check your answers page" in {
          enable(SaveAndRetrieve)
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchProperty(None)
          val goodRequest = callShow(isEditMode = false)

          await(goodRequest)
          redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.PropertyCheckYourAnswersController.show().url)


          verifyPropertySave(Some(PropertyModel(accountingMethod = Some(testAccountingMethod))))
        }
      }
      "save and retrieve is disabled" when {
        "there is an overseas income source" should {
          "redirect to overseas accounting method page" in {
            enable(ForeignProperty)
            setupMockSubscriptionDetailsSaveFunctions()
            mockFetchAllFromSubscriptionDetails(Some(testCacheMap(incomeSource = Some(testIncomeSourceAll))))
            mockFetchProperty(None)

            val goodRequest = callShow(isEditMode = false)
            await(goodRequest)
            redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.OverseasPropertyStartDateController.show().url)


            verifyPropertySave(Some(PropertyModel(accountingMethod = Some(testAccountingMethod))))
          }
        }

        "there is no overseas income source" should {
          "redirect to final check your answers page" in {
            setupMockSubscriptionDetailsSaveFunctions()
            mockFetchProperty(None)
            val goodRequest = callShow(isEditMode = false)
            await(goodRequest)
            redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show.url)


            verifyPropertySave(Some(PropertyModel(accountingMethod = Some(testAccountingMethod))))
          }
        }
      }
    }


    "When it is in edit mode" when {
      "save and retrieve is enabled" should {
        "redirect to uk property check your answers page " in {
          enable(SaveAndRetrieve)
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchProperty(Some(testFullPropertyModel))

          val goodRequest = controller.submit(isEditMode = true)(
            subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Accruals)
          )

          redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.PropertyCheckYourAnswersController.show(true).url)


          verifyPropertySave(Some(testFullPropertyModel.copy(accountingMethod = Some(Accruals), confirmed = false)))
        }
      }

      "save and retrieve is disabled" should {
        "redirect to final check your answers page" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchProperty(Some(testFullPropertyModel))

          val goodRequest = controller.submit(isEditMode = true)(
            subscriptionRequest.post(AccountingMethodPropertyForm.accountingMethodPropertyForm, Accruals)
          )

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show.url)

          await(goodRequest)
          verifyPropertySave(Some(testFullPropertyModel.copy(accountingMethod = Some(Accruals), confirmed = false)))


        }
      }
    }


    "when there is an invalid submission with an error form" should {
      "return bad request status (400)" in {
        mockFetchAllFromSubscriptionDetails(Some(propertyOnlyIncomeSourceType))

        val badRequest = callShowWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyPropertySave(None)
      }
    }

    "The back url" when {
      "is not in edit mode" when {
        "save and retrieve is enabled" should {
          "redirect back to uk property start date page" in withController { controller =>
            enable(SaveAndRetrieve)
            mockFetchAllFromSubscriptionDetails(Some(propertyOnlyIncomeSourceType))
            await(controller.backUrl(isEditMode = false)) mustBe
              controllers.individual.business.routes.PropertyStartDateController.show().url
          }
        }

        "save and retrieve is disabled" should {
          "redirect back to uk property start date page" in withController { controller =>
            mockFetchAllFromSubscriptionDetails(Some(propertyOnlyIncomeSourceType))
            await(controller.backUrl(isEditMode = false)) mustBe
              controllers.individual.business.routes.PropertyStartDateController.show().url
          }
        }

      }
      "is in edit mode" when {
        "save and retrieve is enabled" should {
          "redirect back to uk property check your answers page" in withController { controller =>
            enable(SaveAndRetrieve)
            setupMockSubscriptionDetailsSaveFunctions()
            await(controller.backUrl(isEditMode = true)) mustBe
              controllers.individual.business.routes.PropertyCheckYourAnswersController.show(true).url
          }
        }


        "save and retrieve is disabled" should {
          "redirect back to final check your answers page" in withController { controller =>
            setupMockSubscriptionDetailsSaveFunctions()
            await(controller.backUrl(isEditMode = true)) mustBe
              controllers.individual.subscription.routes.CheckYourAnswersController.show.url
          }
        }
      }

    }
  }
}
