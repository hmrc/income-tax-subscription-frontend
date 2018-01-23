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

package incometax.incomesource.controllers

import core.audit.Logging
import core.config.featureswitch._
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import core.utils.TestModels
import core.utils.TestModels.testCacheMap
import incometax.incomesource.forms.IncomeSourceForm
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class CannotReportYetControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching {

  override val controllerName: String = "CannotReportYetController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

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

  "Calling the show action of the CannotReportYetController" should {

    lazy val result = TestCannotReportYetController.show(isEditMode = false)(subscriptionRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return OK and display the page" in {
      setupMockKeystore(fetchAll = testCacheMap)

      status(result) must be(Status.OK)

      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))

      verifyKeystore(fetchIncomeSource = 0, fetchAll = 1)
    }

  }

  "Calling the submit action of the CannotReportYetController with an authorised user" when {

    def callSubmit(isEditMode: Boolean = false) = TestCannotReportYetController.submit(isEditMode = isEditMode)(subscriptionRequest)

    "not in edit mode" should {

      s"redirect to '${incometax.business.controllers.routes.BusinessAccountingMethodController.show().url}' on the business journey" in {

        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

        val goodRequest = callSubmit()

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, fetchAll = 0)
      }

      s"redirect to '${incometax.subscription.controllers.routes.TermsController.show().url}' on the property journey" in {

        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceProperty)

        val goodRequest = callSubmit()

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(incometax.incomesource.controllers.routes.OtherIncomeController.show().url)

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, fetchAll = 0)
      }

      s"redirect to '${incometax.business.controllers.routes.BusinessAccountingMethodController.show().url}' on the both journey" in {

        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callSubmit()

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, fetchAll = 0)
      }
    }

    "in edit mode" should {
      s"redirect to '${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}'" in {
        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, fetchAll = 0)
      }
    }

  }

  "backUrl" when {
    def evalBackUrl(incomeSource: String, matchTaxYear: Option[Boolean], isEditMode: Boolean) =
      TestCannotReportYetController.backUrl(incomeSource, matchTaxYear, isEditMode)

    "income source is property" should {
      "return income source" in {
        val result = evalBackUrl(IncomeSourceForm.option_property, None, isEditMode = false)
        result mustBe incometax.incomesource.controllers.routes.IncomeSourceController.show().url
      }
    }

    "income source is business" when {
      "not in edit mode and " when {
        "match tax year is answered no, return other income" in {
          val result = evalBackUrl(IncomeSourceForm.option_business, Some(false), isEditMode = false)
          result mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url
        }
        "match tax year is answered yes, return other income error" in {
          val result = evalBackUrl(IncomeSourceForm.option_business, Some(true), isEditMode = false)
          result mustBe incometax.business.controllers.routes.MatchTaxYearController.show().url
        }
      }
      "in edit mode and " when {
        "match tax year is answered no, return other income" in {
          val result = evalBackUrl(IncomeSourceForm.option_business, Some(false), isEditMode = true)
          result mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true).url
        }
        "match tax year is answered yes, return other income error" in {
          val result = evalBackUrl(IncomeSourceForm.option_business, Some(true), isEditMode = true)
          result mustBe incometax.business.controllers.routes.MatchTaxYearController.show(editMode = true).url
        }
      }
    }

    "income source is both" when {
      "not in edit mode and " when {
        "match tax year is answered no, return other income" in {
          val result = evalBackUrl(IncomeSourceForm.option_both, Some(false), isEditMode = false)
          result mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url
        }
        "match tax year is answered yes, return other income error" in {
          val result = evalBackUrl(IncomeSourceForm.option_both, Some(true), isEditMode = false)
          result mustBe incometax.business.controllers.routes.MatchTaxYearController.show().url
        }
      }
      "in edit mode and " when {
        "match tax year is answered no, return other income" in {
          val result = evalBackUrl(IncomeSourceForm.option_both, Some(false), isEditMode = true)
          result mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true).url
        }
        "match tax year is answered yes, return other income error" in {
          val result = evalBackUrl(IncomeSourceForm.option_both, Some(true), isEditMode = true)
          result mustBe incometax.business.controllers.routes.MatchTaxYearController.show(editMode = true).url
        }
      }
    }
  }


  authorisationTests()
}
