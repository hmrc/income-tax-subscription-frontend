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

package controllers.individual.incomesource

import config.MockConfig
import config.featureswitch.FeatureSwitch.{ForeignProperty, ReleaseFour}
import config.featureswitch.FeatureSwitching
import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.ControllerBaseSpec
import forms.individual.incomesource.IncomeSourceForm
import models.Current
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import models.common.{AccountingYearModel, IncomeSourceModel}
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey, IncomeSource}
import utilities.TestModels.{testAccountingMethod, testAccountingMethodProperty, testBusinessName, testCacheMap, testOverseasAccountingMethodProperty, testOverseasPropertyStartDateModel, testPropertyStartDateModel, testSelectedTaxYearCurrent, testSummaryDataSelfEmploymentData}

import scala.concurrent.Future

class IncomeSourceControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector
  with MockConfig
  with FeatureSwitching {

  class TestIncomeSourceController extends IncomeSourceController(
    mockAuthService,
    MockSubscriptionDetailsService,
    mockIncomeTaxSubscriptionConnector
  )

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
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
        subscriptionRequest.post(IncomeSourceForm.incomeSourceForm, incomeSourceModel)
      )
    }

    "When it is not edit mode" should {
      "self-employed is checked and rent UK property and foreign property are NOT checked" when {
        "redirect to BusinessName page if release four is disabled" in {
          setupMockSubscriptionDetailsSaveFunctions()

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.business.routes.BusinessNameController.show().url
          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
        "redirect to the start of the self employment journey if release four is enabled" in {
          setupMockSubscriptionDetailsSaveFunctions()
          enable(ReleaseFour)

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl
          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "self-employed is checked and rent UK property is checked but foreign property is NOT checked" when {
        "redirect to BusinessName page if release four is disabled" in {
          setupMockSubscriptionDetailsSaveFunctions()
          disable(ReleaseFour)
          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.business.routes.BusinessNameController.show().url
          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }

        "redirect to self-employments start date page if release four is enabled" in {
          setupMockSubscriptionDetailsSaveFunctions()
          enable(ReleaseFour)

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false), isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe "/report-quarterly/income-and-expenses/sign-up/self-employments/details"
          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "Rent UK property is checked and self-employed, foreign property are NOT checked" should {
        "Release Four feature switch is disabled" when {
          "redirect to the PropertyAccounting method page" in {
            setupMockSubscriptionDetailsSaveFunctions()
            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false), isEditMode = false)
            disable(ReleaseFour)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get must be(controllers.individual.business.routes.PropertyAccountingMethodController.show().url)

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }
        "Release Four feature switch is enabled" when {
          "redirect to the PropertyStartDate page" in {
            enable(ReleaseFour)
            setupMockSubscriptionDetailsSaveFunctions()
            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false), isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get must be(controllers.individual.business.routes.PropertyStartDateController.show().url)

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }
      }
      "Foreign property is checked and self-employed, UK property are NOT checked" should {
        "Release Four and Foreign property feature switch is enabled" when {
          "redirect to the overseas property start date page" in {
            enable(ReleaseFour)
            enable(ForeignProperty)
            setupMockSubscriptionDetailsSaveFunctions()

            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true), isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get must be(controllers.individual.business.routes.OverseasPropertyStartDateController.show().url)

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }
      }

      "All self-employed, rent UK property and foreign property are checked" when {
        "redirect to BusinessName page" in {
          setupMockSubscriptionDetailsSaveFunctions()
          disable(ReleaseFour)

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true), isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get must be(controllers.individual.business.routes.BusinessNameController.show().url)

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }
    }

    "When it is in edit mode" should {

      "the user selects self-employment and self-employment journey has not been completed before" when {
        "FS ReleaseFour is enabled " should {
          s"redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl}" in {
            enable(ReleaseFour)
            setupMockSubscriptionDetailsSaveFunctions()
            mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
            mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
            mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Current)),
              ukPropertyStartDate = testPropertyStartDateModel,
              accountingMethodProperty = testAccountingMethodProperty
            )))

            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }

        "FS ReleaseFour is disabled and the user has no uk property and no overseas property income" should {
          s"redirect to ${controllers.individual.business.routes.BusinessNameController.show().url}" in {
            setupMockSubscriptionDetailsSaveFunctions()
            mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
            mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
            mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Current)),
              ukPropertyStartDate = testPropertyStartDateModel,
              accountingMethodProperty = testAccountingMethodProperty
            )))

            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe controllers.individual.business.routes.BusinessNameController.show().url

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }

        "FS ReleaseFour is disabled and the user has no uk property and has an overseas property income" should {
          s" redirect to ${controllers.individual.business.routes.BusinessNameController.show().url}" in {
            setupMockSubscriptionDetailsSaveFunctions()
            mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
            mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
            mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)),
              selectedTaxYear = Some(AccountingYearModel(Current)),
              accountingMethodProperty = testAccountingMethodProperty
            )))

            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true), isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe controllers.individual.business.routes.BusinessNameController.show().url

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }

        "FS ReleaseFour is disabled and the user has a uk property and has no overseas property income" should {
          s" redirect to ${controllers.individual.business.routes.BusinessNameController.show().url}" in {
            setupMockSubscriptionDetailsSaveFunctions()
            mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
            mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
            mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Current)),
              accountingMethodProperty = testAccountingMethodProperty
            )))

            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false), isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe controllers.individual.business.routes.BusinessNameController.show().url

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }

        "FS ReleaseFour is enabled and the user has no uk property and no overseas property income" should {
          s" redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl}" in {
            enable(ReleaseFour)
            setupMockSubscriptionDetailsSaveFunctions()
            mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
            mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
            mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
              incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Current)),
              accountingMethodProperty = testAccountingMethodProperty
            )))

            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }
      }

      "the user selected UK property and UK property journey has not been completed before" when {
        "when ReleaseFour is enabled" should {
          s" redirect to ${controllers.individual.business.routes.PropertyStartDateController.show()}" in {
            enable(ReleaseFour)
            setupMockSubscriptionDetailsSaveFunctions()
            mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
            mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
            mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Current)),
              overseasPropertyAccountingMethod = testOverseasAccountingMethodProperty
            )))

            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false), isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe controllers.individual.business.routes.PropertyStartDateController.show().url

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }

        "when ReleaseFour is disabled" should {
          s" redirect to ${controllers.individual.business.routes.PropertyAccountingMethodController.show().url}" in {
            setupMockSubscriptionDetailsSaveFunctions()
            mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
            mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
            mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
              selectedTaxYear = Some(AccountingYearModel(Current)),
              overseasPropertyAccountingMethod = testOverseasAccountingMethodProperty
            )))

            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false), isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe controllers.individual.business.routes.PropertyAccountingMethodController.show().url

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }
      }


      "the user selected overseas property and overseas property journey has not been completed before" when {
        "when ReleaseFour is enabled" should {
          s" redirect to ${controllers.individual.business.routes.OverseasPropertyStartDateController.show().url}" in {
            enable(ReleaseFour)
            setupMockSubscriptionDetailsSaveFunctions()
            mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
            mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
            mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
              incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
              selectedTaxYear = testSelectedTaxYearCurrent,
              accountingMethodProperty = testAccountingMethodProperty
            )))

            val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true), isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe controllers.individual.business.routes.OverseasPropertyStartDateController.show().url

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }
      }

      "the user select self-employment and self-employment journey has completed before and ReleaseFour is enabled" should {
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          enable(ReleaseFour)
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(testSummaryDataSelfEmploymentData)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(testAccountingMethod)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
            selectedTaxYear = testSelectedTaxYearCurrent,
            accountingMethodProperty = testAccountingMethodProperty
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user select self-employment and UK property and both journeys have been completed before and ReleaseFour is enabled" should {
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(testSummaryDataSelfEmploymentData)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(testAccountingMethod)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)),
            ukPropertyStartDate = testPropertyStartDateModel,
            accountingMethodProperty = testAccountingMethodProperty
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "ReleaseFour is enabled and the user select self-employment and UK property and self-employment journeys has not been completed before" should {
        s" redirect to ${appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl}" in {
          enable(ReleaseFour)
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)),
            ukPropertyStartDate = testPropertyStartDateModel,
            accountingMethodProperty = testAccountingMethodProperty
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }


      "the user select self-employment and overseas property and both journeys have been completed before and ReleaseFour is enabled" should {
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(testSummaryDataSelfEmploymentData)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(testAccountingMethod)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true)),
            selectedTaxYear = Some(AccountingYearModel(Current)),
            overseasPropertyStartDate = Some(testOverseasPropertyStartDateModel),
            overseasPropertyAccountingMethod = Some(testOverseasAccountingMethodProperty)
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = true), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user select self-employment, UK property and overseas property and all three journeys have been completed before " +
        "and ReleaseFour is enabled" should {
        s"return an SEE OTHER (303)" + s"${controllers.individual.subscription.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(testSummaryDataSelfEmploymentData)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(testAccountingMethod)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)),
            selectedTaxYear = Some(AccountingYearModel(Current)),
            ukPropertyStartDate = testPropertyStartDateModel,
            accountingMethodProperty = testAccountingMethodProperty,
            overseasPropertyStartDate = testOverseasPropertyStartDateModel,
            overseasPropertyAccountingMethod = testOverseasAccountingMethodProperty
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user select UK property and UK property journeys has been completed before and ReleaseFour is enabled" should {
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          enable(ReleaseFour)
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)),
            ukPropertyStartDate = testPropertyStartDateModel,
            accountingMethodProperty = testAccountingMethodProperty
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user select overseas property and overseas property journeys has been completed before and ReleaseFour is enabled" should {
        s"return an SEE OTHER (303)" + s"${controllers.individual.subscription.routes.CheckYourAnswersController.submit()}" in {
          enable(ReleaseFour)
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)),
            overseasPropertyStartDate = testOverseasPropertyStartDateModel,
            overseasPropertyAccountingMethod = testOverseasAccountingMethodProperty
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user select UK property and overseas property and both journeys have been completed before and ReleaseFour is enabled" should {
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show()}" in {
          enable(ReleaseFour)
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true)),
            ukPropertyStartDate = testPropertyStartDateModel,
            accountingMethodProperty = testAccountingMethodProperty,
            overseasPropertyStartDate = testOverseasPropertyStartDateModel,
            overseasPropertyAccountingMethod = testOverseasAccountingMethodProperty
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = true), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "the user selects self-employment and no UK property or overseas property and self-employment journey has been completed before and FS Release four " +
        "is disabled" should {
        s" redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show()}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockGetSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchAllFromSubscriptionDetails(Some(testCacheMap(
            incomeSource = Some(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)),
            selectedTaxYear = Some(testSelectedTaxYearCurrent),
            businessName = Some(testBusinessName),
            accountingMethod = Some(testAccountingMethod)
          )))

          val goodRequest = callSubmit(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }
    }


    "Calling the submit action of the IncomeSource controller with an authorised user and invalid submission" should {
      lazy val badRequest = new TestIncomeSourceController().submit(isEditMode = true)(subscriptionRequest)

      "return a bad request status (400)" in {
        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsFetch(IncomeSource, 0)
        verifySubscriptionDetailsSave(IncomeSource, 0)
      }
    }


    "The back url" when {
      "in edit mode" should {
        "point to the check your answers page" in {
          new TestIncomeSourceController().backUrl(isEditMode = true) mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }
      }
      "not in edit mode" should {
        "point to the tax year page" in {
          new TestIncomeSourceController().backUrl(isEditMode = false) mustBe controllers.individual.business.routes.WhatYearToSignUpController.show().url
        }
      }
    }

    authorisationTests()

  }

}
