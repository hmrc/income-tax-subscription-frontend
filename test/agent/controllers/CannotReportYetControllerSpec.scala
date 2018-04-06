/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.controllers

import assets.{MessageLookup => messages}
import core.Constants.crystallisationTaxYearStart
import core.audit.Logging
import core.config.featureswitch._
import core.models.DateModel
import agent.services.mocks.MockKeystoreService
import core.config.MockConfig
import core.utils.TestModels
import core.utils.TestModels._
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

class CannotReportYetControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching {

  override val controllerName: String = "CannotReportYetController"

  object AuthTestCannotReportYetController extends CannotReportYetController(
    mockBaseControllerConfig(
      new MockConfig {
        override val taxYearDeferralEnabled = true
      }
    ),
    messagesApi,
    MockKeystoreService,
    app.injector.instanceOf[Logging],
    mockAuthService
  )

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> AuthTestCannotReportYetController.show(false),
    "submit" -> AuthTestCannotReportYetController.submit(false)
  )

  object TestCannotReportYetController extends CannotReportYetController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    app.injector.instanceOf[Logging],
    mockAuthService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(TaxYearDeferralFeature)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(TaxYearDeferralFeature)
  }

  val testCannotCrystalliseDate = DateModel("4", "4", "2018")
  val testCanCrystalliseDate = DateModel("6", "4", "2018")

  "Calling the show action of the CannotReportYetController" when {
    implicit lazy val request: Request[AnyContent] = subscriptionRequest

    def call = TestCannotReportYetController.show(isEditMode = false)(request)

    "Property only" should {
      "return OK and display the CannotReport page with until 6 April 2018" in {
        setupMockKeystore(fetchAll = testCacheMapCustom(
          incomeSource = TestModels.testIncomeSourceProperty,
          accountingPeriodDate = None
        ))

        val result = call
        status(result) must be(Status.OK)

        val expectedView = agent.views.html.client_cannot_report_yet(
          postAction = routes.CannotReportYetController.submit(),
          backUrl = agent.controllers.routes.IncomeSourceController.show().url,
          dateModel = crystallisationTaxYearStart
        ).body

        contentAsString(result) mustBe expectedView

        verifyKeystore(fetchAll = 1)
      }
    }

    "Business only and" when {
      "the accounting period does not match the tax year" should {
        "return OK and display the CannotReport page with until a the next tax year" in {
          val testAccountingPeriod = TestModels.testAccountingPeriod.copy(endDate = testCannotCrystalliseDate)

          setupMockKeystore(fetchAll = testCacheMapCustom(
            incomeSource = TestModels.testIncomeSourceBusiness,
            accountingPeriodDate = testAccountingPeriod
          ))

          val result = call
          status(result) must be(Status.OK)

          val expectedView = agent.views.html.client_cannot_report_yet(
            postAction = routes.CannotReportYetController.submit(),
            backUrl = agent.controllers.business.routes.BusinessAccountingPeriodDateController.show().url,
            dateModel = testAccountingPeriod.endDate plusDays 1
          ).body

          contentAsString(result) mustBe expectedView
          verifyKeystore(fetchAll = 1)
        }
      }
    }

    "Both business and property income" when {
      "their end date is before the 6th April 2018" should {
        "return Ok with cannot report yet both misaligned page" in {
          val testAccountingPeriod = TestModels.testAccountingPeriod.copy(endDate = testCannotCrystalliseDate)

          setupMockKeystore(fetchAll = testCacheMapCustom(
            incomeSource = TestModels.testIncomeSourceBoth,
            accountingPeriodDate = testAccountingPeriod
          ))

          val result = call
          status(result) must be(Status.OK)

          val expectedView = agent.views.html.client_cannot_report_yet_both_misaligned(
            postAction = routes.CannotReportYetController.submit(),
            backUrl = agent.controllers.business.routes.BusinessAccountingPeriodDateController.show().url,
            businessStartDate = testAccountingPeriod.endDate plusDays 1
          ).body

          contentAsString(result) mustBe expectedView
          verifyKeystore(fetchAll = 1)
        }
      }
      "their end date is after the 6th April 2018" should {
        "return Ok with can report business but not property yet page" in {
          val testAccountingPeriod = TestModels.testAccountingPeriod.copy(endDate = testCanCrystalliseDate)
          setupMockKeystore(fetchAll = testCacheMapCustom(
            incomeSource = TestModels.testIncomeSourceBoth,
            accountingPeriodDate = testAccountingPeriod
          ))

          val result = call
          status(result) must be(Status.OK)

          val expectedView = agent.views.html.client_cannot_report_property_yet(
            postAction = routes.CannotReportYetController.submit(),
            backUrl = agent.controllers.business.routes.BusinessAccountingPeriodDateController.show().url
          ).body

          contentAsString(result) mustBe expectedView

          verifyKeystore(fetchAll = 1)
        }
      }
    }
    "the accounting period data is in an invalid state" should {
      "throw an InternalServerException" in {
        setupMockKeystore(fetchAll = testCacheMapCustom(
          incomeSource = TestModels.testIncomeSourceBoth,
          accountingPeriodDate = None
        ))

        intercept[InternalServerException](await(call))
      }
    }
  }

  "Calling the submit action of the CannotReportYetController with an authorised user" when {

    def callSubmit(isEditMode: Boolean = false) = TestCannotReportYetController.submit(isEditMode = isEditMode)(subscriptionRequest)

    "not in edit mode" should {
      s"redirect to '${
        agent.controllers.business.routes.BusinessNameController.show().url
      }' on the business journey" in {
        setupMockKeystore(fetchAll = testCacheMapCustom(incomeSource = TestModels.testIncomeSourceBusiness))

        val goodRequest = callSubmit()

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessNameController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAll = 1)
      }

      s"redirect to '${
        agent.controllers.routes.TermsController.show().url
      }' on the property journey" in {
        setupMockKeystore(fetchAll = testCacheMapCustom(incomeSource = TestModels.testIncomeSourceProperty))

        val goodRequest = callSubmit()

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.OtherIncomeController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAll = 1)
      }

      s"redirect to '${
        agent.controllers.business.routes.BusinessNameController.show().url
      }' on the both journey" in {
        setupMockKeystore(fetchAll = testCacheMapCustom(incomeSource = TestModels.testIncomeSourceBoth))

        val goodRequest = callSubmit()

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessNameController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAll = 1)
      }
    }

    "in edit mode" should {
      s"redirect to '${
        agent.controllers.routes.CheckYourAnswersController.show().url
      }'" in {
        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAll = 0)
      }
    }
  }

  authorisationTests()
}
