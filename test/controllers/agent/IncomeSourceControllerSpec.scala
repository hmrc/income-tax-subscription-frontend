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

package controllers.agent

import agent.audit.mocks.MockAuditingService
import config.MockConfig
import config.featureswitch.FeatureSwitch.ForeignProperty
import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import forms.agent.IncomeSourceForm
import models.Current
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import models.common.{AccountingYearModel, IncomeSourceModel, OverseasPropertyModel, PropertyModel}
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey, IncomeSource}
import utilities.TestModels.{testAccountingMethod, testAccountingMethodProperty, testCacheMap, testPropertyStartDateModel, testSelectedTaxYearCurrent, testSummaryDataSelfEmploymentData}
import utilities.individual.TestConstants.testSelfEmploymentData
import views.agent.mocks.MockIncomeSource

import scala.concurrent.Future

class IncomeSourceControllerSpec extends AgentControllerBaseSpec
  with MockIncomeSource
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector
  with MockConfig
  with MockAuditingService
   {

  class TestIncomeSourceController extends IncomeSourceController(
    incomeSource,
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockIncomeTaxSubscriptionConnector
  )

  override def beforeEach(): Unit = {
    disable(ForeignProperty)
    super.beforeEach()
  }

  override val controllerName: String = "IncomeSourceController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> new TestIncomeSourceController().show(isEditMode = true),
    "submit" -> new TestIncomeSourceController().submit(isEditMode = true)
  )

  "test" should {
    "en" in {
      val m: Messages = messagesApi.preferred(subscriptionRequest)
      m must not be None
      m.apply("base.back") must be("Back")
    }
  }

  "Calling the show action of the IncomeSourceController with an authorised user" when {

    def call: Future[Result] = new TestIncomeSourceController().show(isEditMode = true)(subscriptionRequest)

    "the new income source flow" should {
      "return ok (200)" in {
        mockFetchIncomeSourceFromSubscriptionDetails(None)
        mockIncomeSource()

        val result = call
        status(result) must be(Status.OK)

        await(result)
        verifySubscriptionDetailsFetch(IncomeSource, 1)
        verifySubscriptionDetailsSave(IncomeSource, 0)
      }
    }

  }

  "Calling the submit action of the IncomeSource controller with an authorised user and valid submission" should {

    def callSubmit(incomeSourceModel: IncomeSourceModel,
                   isEditMode: Boolean
                  ): Future[Result] = {
      new TestIncomeSourceController().submit(isEditMode = isEditMode)(
        subscriptionRequest.post(IncomeSourceForm.incomeSourceForm(true), incomeSourceModel)
      )
    }

    "When it is not edit mode" when {
      "self-employed is checked and rent UK property and foreign property are NOT checked" should {
        "redirect to the income tax self employment initialise route" in {
          setupMockSubscriptionDetailsSaveFunctions()

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl
          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "Rent UK property is checked and self-employed, foreign property are NOT checked" should {
        "redirect to the Property Start Date page" in {
          setupMockSubscriptionDetailsSaveFunctions()
          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false), isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get must be(controllers.agent.business.routes.PropertyStartDateController.show().url)

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }

        "Self-employed, rent UK property are checked" should {
          "redirect to income tax self employment initialise route" in {
            setupMockSubscriptionDetailsSaveFunctions()

            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false), isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get must be(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }

        "Calling the submit action of the IncomeSource controller with an authorised user and invalid submission" should {
          lazy val badRequest = new TestIncomeSourceController().submit(isEditMode = true)(subscriptionRequest)

          "return a bad request status (400)" in {
            mockIncomeSource()
            status(badRequest) must be(Status.BAD_REQUEST)

            await(badRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 0)
            verifySubscriptionDetailsSave(IncomeSource, 0)
          }
        }

        authorisationTests()

      }
    }

    "When it is in edit mode" should {

      "the user selects self-employment and self-employment journey has not been completed before" when {
        "and selected tax year page has not been completed before" should {
          s"redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl}" in {
            setupMockSubscriptionDetailsSaveFunctions()
            mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
            mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
            mockFetchProperty(None)
            mockFetchOverseasProperty(None)
            mockFetchAllFromSubscriptionDetails(Some(testCacheMap(incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)))))

            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }
      }


      "the user selected UK property and UK property journey has not been completed before" should {
        s"redirect to ${controllers.agent.business.routes.PropertyStartDateController.show().url}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
            selectedTaxYear = Some(AccountingYearModel(Current))
          )))
          mockFetchProperty(None)
          mockFetchOverseasProperty(None)

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.business.routes.PropertyStartDateController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }


      "the user selected overseas property and overseas property journey has not been completed before" should {
        s"redirect to ${controllers.agent.business.routes.OverseasPropertyStartDateController.show().url}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchProperty(None)
          mockFetchOverseasProperty(None)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
            selectedTaxYear = testSelectedTaxYearCurrent
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.business.routes.OverseasPropertyStartDateController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }


      "the user select self-employment and self-employment journey ( include tax year to sign up) has completed before" should {
        s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show.url}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(testSummaryDataSelfEmploymentData)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(testAccountingMethod)
          mockFetchProperty(None)
          mockFetchOverseasProperty(None)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
            selectedTaxYear = testSelectedTaxYearCurrent
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show.url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user select self-employment and UK property and both journeys have been completed before" should {
        s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(testSummaryDataSelfEmploymentData)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(testAccountingMethod)
          mockFetchProperty(Some(PropertyModel(
            accountingMethod = testAccountingMethodProperty.propertyAccountingMethod,
            startDate = testPropertyStartDateModel.startDate
          )))
          mockFetchOverseasProperty(None)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false))
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show.url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user select self-employment and UK property and self-employment journeys has not been completed before" should {
        s"redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchProperty(Some(PropertyModel(
            accountingMethod = testAccountingMethodProperty.propertyAccountingMethod,
            startDate = testPropertyStartDateModel.startDate
          )))
          mockFetchOverseasProperty(None)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false))
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }


      "the user select self-employment and overseas property and both journeys have been completed before" should {
        s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(testSummaryDataSelfEmploymentData)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(testAccountingMethod)
          mockFetchProperty(None)
          mockFetchOverseasProperty(Some(OverseasPropertyModel(
            accountingMethod = testAccountingMethodProperty.propertyAccountingMethod,
            startDate = testPropertyStartDateModel.startDate
          )))
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)),
            selectedTaxYear = Some(AccountingYearModel(Current))
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show.url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user select self-employment, UK property and overseas property and all three journeys have been completed before" should {
        s"return an SEE OTHER (303)" + s" ${controllers.agent.routes.CheckYourAnswersController.show}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(testSummaryDataSelfEmploymentData)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(testAccountingMethod)
          mockFetchProperty(Some(PropertyModel(
            accountingMethod = testAccountingMethodProperty.propertyAccountingMethod,
            startDate = testPropertyStartDateModel.startDate
          )))
          mockFetchOverseasProperty(Some(OverseasPropertyModel(
            accountingMethod = testAccountingMethodProperty.propertyAccountingMethod,
            startDate = testPropertyStartDateModel.startDate
          )))
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
            selectedTaxYear = Some(AccountingYearModel(Current))
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show.url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user select UK property and UK property journeys has been completed before" should {
        s" redirect to ${controllers.agent.routes.CheckYourAnswersController.show.url}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchProperty(Some(PropertyModel(
            accountingMethod = testAccountingMethodProperty.propertyAccountingMethod,
            startDate = testPropertyStartDateModel.startDate
          )))
          mockFetchOverseasProperty(None)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false))
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show.url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user select overseas property and overseas property journeys has been completed before" should {
        s"return an SEE OTHER (303)" + s"${controllers.agent.routes.CheckYourAnswersController.submit}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchProperty(None)
          mockFetchOverseasProperty(Some(OverseasPropertyModel(
            accountingMethod = testAccountingMethodProperty.propertyAccountingMethod,
            startDate = testPropertyStartDateModel.startDate
          )))
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true))
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show.url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user select UK property and overseas property and both journeys have been completed before" should {
        s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchProperty(Some(PropertyModel(
            accountingMethod = testAccountingMethodProperty.propertyAccountingMethod,
            startDate = testPropertyStartDateModel.startDate
          )))
          mockFetchOverseasProperty(Some(OverseasPropertyModel(
            accountingMethod = testAccountingMethodProperty.propertyAccountingMethod,
            startDate = testPropertyStartDateModel.startDate
          )))
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true))
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show.url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user selects self-employment and no UK property or overseas property and self-employment journey has been completed before" should {
        s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(testSelfEmploymentData)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(testAccountingMethod)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
            selectedTaxYear = Some(testSelectedTaxYearCurrent)
          )))
          mockFetchProperty(None)
          mockFetchOverseasProperty(None)

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show.url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }
    }

    "Calling the submit action of the IncomeSource controller with an authorised user and invalid submission" should {
      lazy val badRequest = new TestIncomeSourceController().submit(isEditMode = true)(subscriptionRequest)

      "return a bad request status (400)" in {
        mockIncomeSource()
        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsFetch(IncomeSource, 0)
        verifySubscriptionDetailsSave(IncomeSource, 0)
      }
    }

    "The back url is not in edit mode" when {
      "the user click back url" should {
        "redirect to Match Tax Year page" in {
          new TestIncomeSourceController().backUrl(isEditMode = false) mustBe
            controllers.agent.routes.WhatYearToSignUpController.show().url
        }
      }
    }

    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to check your answer page" in {
          new TestIncomeSourceController().backUrl(isEditMode = true) mustBe
            controllers.agent.routes.CheckYourAnswersController.show.url
        }
      }
    }
    authorisationTests()
  }
}
