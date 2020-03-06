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

package controllers.individual.business

import controllers.ControllerBaseSpec
import core.config.MockConfig
import core.config.featureswitch._
import services.individual.mocks.MockKeystoreService
import core.utils.TestModels._
import forms.individual.business.AccountingMethodForm
import models.individual.business.{AccountingMethodModel, AccountingPeriodModel, MatchTaxYearModel}
import models.individual.incomesource.RentUkPropertyModel
import models.individual.subscription.{Both, Business, IncomeSourceType, Property}
import models.{Cash, DateModel, No}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class BusinessAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching {

  override val controllerName: String = "BusinessAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessAccountingMethodController.show(isEditMode = false),
    "submit" -> TestBusinessAccountingMethodController.submit(isEditMode = false)
  )

  object TestBusinessAccountingMethodController extends BusinessAccountingMethodController(
    mockBaseControllerConfig(
      new MockConfig {}),
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  private def fetchAllCacheMap(matchTaxYear: Option[MatchTaxYearModel] = None,
                               accountingPeriod: Option[AccountingPeriodModel] = None,
                               incomeSourceType: IncomeSourceType = Business) =
    testCacheMap(
      incomeSource = incomeSourceType match {
        case Business => testIncomeSourceBusiness
        case Property => None
        case Both => testIncomeSourceBoth
      },
      rentUkProperty = incomeSourceType match {
        case Business => testRentUkProperty_no_property
        case Property => None
        case Both => testRentUkProperty_property_and_other
      },
      areYouSelfEmployed = testAreYouSelfEmployed_yes,
      matchTaxYear = matchTaxYear,
      accountingPeriodDate = accountingPeriod
    )

  val taxYear2018AccountingPeriod = AccountingPeriodModel(DateModel("6", "4", "2017"), DateModel("5", "4", "2018"))
  val taxYear2019AccountingPeriod = AccountingPeriodModel(DateModel("6", "4", "2017"), DateModel("5", "4", "2019"))

  def matchTaxYearCacheMap(incomeSourceType: IncomeSourceType = Business): CacheMap = fetchAllCacheMap(
    matchTaxYear = testMatchTaxYearYes, incomeSourceType = incomeSourceType)

  def matchTaxYearYesIncomeSourceBoth(incomeSourceType: IncomeSourceType = Both): CacheMap = fetchAllCacheMap(
    matchTaxYear = testMatchTaxYearYes, incomeSourceType = incomeSourceType)

  def matchTaxYearNoIncomeSourceBoth(incomeSourceType: IncomeSourceType = Both): CacheMap = fetchAllCacheMap(
    matchTaxYear = testMatchTaxYearNo, incomeSourceType = incomeSourceType)

  def taxYear2018CacheMap(incomeSourceType: IncomeSourceType = Business): CacheMap = fetchAllCacheMap(
    matchTaxYear = testMatchTaxYearNo, accountingPeriod = taxYear2018AccountingPeriod, incomeSourceType = incomeSourceType)

  def taxYear2019CacheMap(incomeSourceType: IncomeSourceType = Business): CacheMap = fetchAllCacheMap(
    matchTaxYear = testMatchTaxYearNo, accountingPeriod = taxYear2019AccountingPeriod, incomeSourceType = incomeSourceType)


  "Calling the show action of the BusinessAccountingMethod with an authorised user" should {

    lazy val result = TestBusinessAccountingMethodController.show(isEditMode = false)(subscriptionRequest)

    "return ok (200)" in {
      setupMockKeystore(
        fetchAccountingMethod = None,
        fetchAll = matchTaxYearCacheMap() // for the back url
      )

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchAccountingMethod = 1, saveAccountingMethod = 0, fetchAll = 1)
    }
  }

  "Calling the submit action of the BusinessAccountingMethod with an authorised user and valid submission" should {

    def callSubmit(isEditMode: Boolean): Future[Result] = TestBusinessAccountingMethodController.submit(isEditMode = isEditMode)(subscriptionRequest
      .post(AccountingMethodForm.accountingMethodForm, AccountingMethodModel(Cash)))

    "When it is not in edit mode" should {
      s"redirect to '${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}'" in {
        setupMockKeystoreSaveFunctions()
        setupMockKeystore(fetchRentUkProperty = RentUkPropertyModel(No, None))

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAll = 0, saveAccountingMethod = 1)
      }

    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()
        setupMockKeystore(fetchRentUkProperty = RentUkPropertyModel(No, None))

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchAll = 0, saveAccountingMethod = 1)
      }

      s"redirect to '${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}'" in {
        setupMockKeystoreSaveFunctions()
        setupMockKeystore(fetchRentUkProperty = RentUkPropertyModel(No, None))

        val goodRequest = callSubmit(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAll = 0, saveAccountingMethod = 1)
      }
    }
  }

  "Calling the submit action of the BusinessAccountingMethod with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessAccountingMethodController.submit(isEditMode = false)(subscriptionRequest)

    "return a bad request status (400)" in {
      // for the back url
      setupMockKeystore(fetchAll = matchTaxYearCacheMap())

      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(saveAccountingMethod = 0, fetchAll = 1)
    }
  }

  "The back url not in edit mode" when {
    "income source type is business" should {
      object TestBusinessAccountingMethodController2 extends BusinessAccountingMethodController(
        mockBaseControllerConfig(
          new MockConfig {}),
        messagesApi,
        MockKeystoreService,
        mockAuthService
      )
      s"point to ${controllers.individual.business.routes.WhatYearToSignUpController.show().url}" in {
        setupMockKeystore(fetchAll = matchTaxYearCacheMap())
        await(TestBusinessAccountingMethodController2.backUrl(isEditMode = false)) mustBe
          controllers.individual.business.routes.WhatYearToSignUpController.show().url
      }

      "income source type is not business and match tax year is answered with no" should {
        s"point to ${controllers.individual.business.routes.BusinessAccountingPeriodDateController.show().url}" in {
          setupMockKeystore(fetchAll = matchTaxYearNoIncomeSourceBoth())
          await(TestBusinessAccountingMethodController2.backUrl(isEditMode = false)) mustBe
            controllers.individual.business.routes.BusinessAccountingPeriodDateController.show().url
        }
      }

      "income source type is not business and match tax year is answered with yes" should {
        s"point to ${controllers.individual.business.routes.WhatYearToSignUpController.show().url}" in {
          setupMockKeystore(fetchAll = matchTaxYearYesIncomeSourceBoth())
          await(TestBusinessAccountingMethodController2.backUrl(isEditMode = false)) mustBe
            controllers.individual.business.routes.MatchTaxYearController.show().url
        }
      }
    }

    "The back url in edit mode" should {
      s"point to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
        await(TestBusinessAccountingMethodController.backUrl(isEditMode = true)) mustBe
          controllers.individual.subscription.routes.CheckYourAnswersController.show().url
      }
    }
  }
  authorisationTests()

}
