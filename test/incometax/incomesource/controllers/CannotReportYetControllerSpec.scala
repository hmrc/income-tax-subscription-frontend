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

import assets.{MessageLookup => messages}
import core.Constants.crystallisationTaxYearStart
import core.audit.Logging
import core.config.featureswitch._
import core.controllers.ControllerBaseSpec
import core.models.DateModel
import core.services.mocks.MockKeystoreService
import core.utils.TestModels
import core.utils.TestModels._
import incometax.subscription.models.{Both, Business, IncomeSourceType, Property}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

class CannotReportYetControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching {

  override val controllerName: String = "CannotReportYetController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestCannotReportYetController.show(false),
    "submit" -> TestCannotReportYetController.submit(false)
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
    disable(NewIncomeSourceFlowFeature)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(TaxYearDeferralFeature)
    disable(NewIncomeSourceFlowFeature)
  }

  val testCannotCrystalliseDate = DateModel("4", "4", "2018")
  val testCanCrystalliseDate = DateModel("6", "4", "2018")

  "Calling the show action of the CannotReportYetController" when {
    def call = TestCannotReportYetController.show(isEditMode = false)(subscriptionRequest)

    "NewIncomeSourceFlowFeature is disabled" should {
      "Property only" should {
        "return OK and display the CannotReport page with until 6 April 2018" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(incomeSource = TestModels.testIncomeSourceProperty))

          val result = call
          status(result) must be(Status.OK)
          lazy val document = Jsoup.parse(contentAsString(result))

          contentType(result) must be(Some("text/html"))
          charset(result) must be(Some("utf-8"))

          document.title mustBe messages.CannotReportYet.title
          document.getElementsByTag("p") text() must include(messages.CannotReportYet.para1(crystallisationTaxYearStart))

          verifyKeystore(fetchAll = 1)
        }
      }

      "Business only and" when {
        "matches the tax year" should {
          "return OK and display the CannotReport page with until 6 April 2018" in {
            setupMockKeystore(fetchAll = testCacheMapCustom(
              incomeSource = TestModels.testIncomeSourceBusiness,
              matchTaxYear = TestModels.testMatchTaxYearYes
            ))

            val result = call
            status(result) must be(Status.OK)
            lazy val document = Jsoup.parse(contentAsString(result))

            contentType(result) must be(Some("text/html"))
            charset(result) must be(Some("utf-8"))

            document.title mustBe messages.CannotReportYet.title
            document.getElementsByTag("p") text() must include(messages.CannotReportYet.para1(crystallisationTaxYearStart))

            verifyKeystore(fetchAll = 1)
          }
        }
        "does not matches the tax year and accounting period end year is after 2018" should {
          "return OK and display the CannotReport page with until a the next tax year" in {
            setupMockKeystore(fetchAll = testCacheMapCustom(
              incomeSource = TestModels.testIncomeSourceBusiness,
              matchTaxYear = TestModels.testMatchTaxYearNo,
              accountingPeriodDate = TestModels.testAccountingPeriod.copy(endDate = testCannotCrystalliseDate)
            ))

            val result = call
            status(result) must be(Status.OK)
            lazy val document = Jsoup.parse(contentAsString(result))

            contentType(result) must be(Some("text/html"))
            charset(result) must be(Some("utf-8"))

            document.title mustBe messages.CannotReportYet.title
            document.getElementsByTag("p") text() must include(messages.CannotReportYet.para1(testCannotCrystalliseDate.plusDays(1)))

            verifyKeystore(fetchAll = 1)
          }
        }
      }

      "Both business and property income" when {
        "matches the tax year" should {
          "return OK and display the CannotReport page with until 6 April 2018" in {
            setupMockKeystore(fetchAll = testCacheMapCustom(
              incomeSource = TestModels.testIncomeSourceBoth,
              matchTaxYear = TestModels.testMatchTaxYearYes
            ))

            val result = call
            status(result) must be(Status.OK)
            lazy val document = Jsoup.parse(contentAsString(result))

            contentType(result) must be(Some("text/html"))
            charset(result) must be(Some("utf-8"))

            document.title mustBe messages.CannotReportYet.title
            document.getElementsByTag("p") text() must include(messages.CannotReportYet.para1(crystallisationTaxYearStart))

            verifyKeystore(fetchAll = 1)
          }
        }
        "the entered accounting period matches the tax year" should {
          "return OK and display the CannotReport page with until 6 April 2018" in {
            setupMockKeystore(fetchAll = testCacheMapCustom(
              incomeSource = TestModels.testIncomeSourceBoth,
              accountingPeriodDate = TestModels.testAccountingPeriodMatched
            ))

            val result = call
            status(result) must be(Status.OK)
            lazy val document = Jsoup.parse(contentAsString(result))

            contentType(result) must be(Some("text/html"))
            charset(result) must be(Some("utf-8"))

            document.title mustBe messages.CannotReportYet.title
            document.getElementsByTag("p") text() must include(messages.CannotReportYet.para1(crystallisationTaxYearStart))

            verifyKeystore(fetchAll = 1)
          }
        }
        "doesn't match the tax year" when {
          "their end date is before the 6th April 2018" should {
            "return Ok with cannot report yet both misaligned page" in {
              setupMockKeystore(fetchAll = testCacheMapCustom(
                incomeSource = TestModels.testIncomeSourceBoth,
                matchTaxYear = TestModels.testMatchTaxYearNo,
                accountingPeriodDate = TestModels.testAccountingPeriod.copy(endDate = testCannotCrystalliseDate)
              ))

              val result = call
              status(result) must be(Status.OK)
              lazy val document = Jsoup.parse(contentAsString(result))

              contentType(result) must be(Some("text/html"))
              charset(result) must be(Some("utf-8"))

              document.title mustBe messages.CannotReportYetBothMisaligned.title
              document.getElementsByTag("li") text() must include(messages.CannotReportYetBothMisaligned.bullet2(testCannotCrystalliseDate.plusDays(1)))

              verifyKeystore(fetchAll = 1)
            }
          }
          "their end date is after the 6th April 2018" should {
            "return Ok with can report business but not property yet page" in {
              setupMockKeystore(fetchAll = testCacheMapCustom(
                incomeSource = TestModels.testIncomeSourceBoth,
                matchTaxYear = TestModels.testMatchTaxYearNo,
                accountingPeriodDate = TestModels.testAccountingPeriod.copy(endDate = testCanCrystalliseDate)
              ))

              val result = call
              status(result) must be(Status.OK)
              lazy val document = Jsoup.parse(contentAsString(result))

              contentType(result) must be(Some("text/html"))
              charset(result) must be(Some("utf-8"))

              document.title mustBe messages.CanReportBusinessButNotPropertyYet.title
              document.getElementsByTag("p") text() must include(messages.CanReportBusinessButNotPropertyYet.para1)

              verifyKeystore(fetchAll = 1)
            }
          }
        }
      }
      "the accounting period data is in an invalid state" should {
        "throw an InternalServerException" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(
            incomeSource = TestModels.testIncomeSourceBoth,
            matchTaxYear = None
          ))

          intercept[InternalServerException](await(call))
        }
      }
    }

    "NewIncomeSourceFlowFeature is enable" when {
      "Property only" should {
        "return OK and display the CannotReport page with until 6 April 2018" in {
          enable(NewIncomeSourceFlowFeature)
          setupMockKeystore(fetchAll = testCacheMapCustom(
            rentUkProperty = testRentUkProperty_property_and_other,
            workForYourself = testWorkForYourself_no
          ))

          val result = call
          status(result) must be(Status.OK)
          lazy val document = Jsoup.parse(contentAsString(result))

          contentType(result) must be(Some("text/html"))
          charset(result) must be(Some("utf-8"))

          document.title mustBe messages.CannotReportYet.title
          document.getElementsByTag("p") text() must include(messages.CannotReportYet.para1(crystallisationTaxYearStart))

          verifyKeystore(fetchAll = 1)
        }
      }

      "Business only and" when {
        "matches the tax year" should {
          "return OK and display the CannotReport page with until 6 April 2018" in {
            enable(NewIncomeSourceFlowFeature)
            setupMockKeystore(fetchAll = testCacheMapCustom(
              rentUkProperty = testRentUkProperty_no_property,
              workForYourself = testWorkForYourself_yes,
              matchTaxYear = TestModels.testMatchTaxYearYes
            ))

            val result = call
            status(result) must be(Status.OK)
            lazy val document = Jsoup.parse(contentAsString(result))

            contentType(result) must be(Some("text/html"))
            charset(result) must be(Some("utf-8"))

            document.title mustBe messages.CannotReportYet.title
            document.getElementsByTag("p") text() must include(messages.CannotReportYet.para1(crystallisationTaxYearStart))

            verifyKeystore(fetchAll = 1)
          }
        }
        "does not matches the tax year and accounting period end year is after 2018" should {
          "return OK and display the CannotReport page with until a the next tax year" in {
            enable(NewIncomeSourceFlowFeature)
            setupMockKeystore(fetchAll = testCacheMapCustom(
              rentUkProperty = testRentUkProperty_no_property,
              workForYourself = testWorkForYourself_yes,
              matchTaxYear = TestModels.testMatchTaxYearNo,
              accountingPeriodDate = TestModels.testAccountingPeriod.copy(endDate = testCannotCrystalliseDate)
            ))

            val result = call
            status(result) must be(Status.OK)
            lazy val document = Jsoup.parse(contentAsString(result))

            contentType(result) must be(Some("text/html"))
            charset(result) must be(Some("utf-8"))

            document.title mustBe messages.CannotReportYet.title
            document.getElementsByTag("p") text() must include(messages.CannotReportYet.para1(testCannotCrystalliseDate.plusDays(1)))

            verifyKeystore(fetchAll = 1)
          }
        }
      }

      "Both business and property income" when {
        "matches the tax year" should {
          "return OK and display the CannotReport page with until 6 April 2018" in {
            enable(NewIncomeSourceFlowFeature)

            setupMockKeystore(fetchAll = testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other,
              workForYourself = testWorkForYourself_yes,
              matchTaxYear = TestModels.testMatchTaxYearYes
            ))

            val result = call
            status(result) must be(Status.OK)
            lazy val document = Jsoup.parse(contentAsString(result))

            contentType(result) must be(Some("text/html"))
            charset(result) must be(Some("utf-8"))

            document.title mustBe messages.CannotReportYet.title
            document.getElementsByTag("p") text() must include(messages.CannotReportYet.para1(crystallisationTaxYearStart))

            verifyKeystore(fetchAll = 1)
          }
        }
        "the entered accounting period matches the tax year" should {
          "return OK and display the CannotReport page with until 6 April 2018" in {
            enable(NewIncomeSourceFlowFeature)
            setupMockKeystore(fetchAll = testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other,
              workForYourself = testWorkForYourself_yes,
              accountingPeriodDate = TestModels.testAccountingPeriodMatched
            ))

            val result = call
            status(result) must be(Status.OK)
            lazy val document = Jsoup.parse(contentAsString(result))

            contentType(result) must be(Some("text/html"))
            charset(result) must be(Some("utf-8"))

            document.title mustBe messages.CannotReportYet.title
            document.getElementsByTag("p") text() must include(messages.CannotReportYet.para1(crystallisationTaxYearStart))

            verifyKeystore(fetchAll = 1)
          }
        }

        "doesn't match the tax year" when {
          "their end date is before the 6th April 2018" should {
            "return Ok with cannot report yet both misaligned page" in {
              enable(NewIncomeSourceFlowFeature)

              setupMockKeystore(fetchAll = testCacheMapCustom(
                rentUkProperty = testRentUkProperty_property_and_other,
                workForYourself = testWorkForYourself_yes,
                matchTaxYear = TestModels.testMatchTaxYearNo,
                accountingPeriodDate = TestModels.testAccountingPeriod.copy(endDate = testCannotCrystalliseDate)
              ))

              val result = call
              status(result) must be(Status.OK)
              lazy val document = Jsoup.parse(contentAsString(result))

              contentType(result) must be(Some("text/html"))
              charset(result) must be(Some("utf-8"))

              document.title mustBe messages.CannotReportYetBothMisaligned.title
              document.getElementsByTag("li") text() must include(messages.CannotReportYetBothMisaligned.bullet2(testCannotCrystalliseDate.plusDays(1)))

              verifyKeystore(fetchAll = 1)
            }
          }
          "their end date is after the 6th April 2018" should {
            "return Ok with can report business but not property yet page" in {
              enable(NewIncomeSourceFlowFeature)

              setupMockKeystore(fetchAll = testCacheMapCustom(
                rentUkProperty = testRentUkProperty_property_and_other,
                workForYourself = testWorkForYourself_yes,
                matchTaxYear = TestModels.testMatchTaxYearNo,
                accountingPeriodDate = TestModels.testAccountingPeriod.copy(endDate = testCanCrystalliseDate)
              ))

              val result = call
              status(result) must be(Status.OK)
              lazy val document = Jsoup.parse(contentAsString(result))

              contentType(result) must be(Some("text/html"))
              charset(result) must be(Some("utf-8"))

              document.title mustBe messages.CanReportBusinessButNotPropertyYet.title
              document.getElementsByTag("p") text() must include(messages.CanReportBusinessButNotPropertyYet.para1)

              verifyKeystore(fetchAll = 1)
            }
          }
        }
      }
      "the accounting period data is in an invalid state" should {
        "throw an InternalServerException" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(
            incomeSource = TestModels.testIncomeSourceBoth,
            matchTaxYear = None
          ))

          intercept[InternalServerException](await(call))
        }
      }
    }

  }

  "Calling the submit action of the CannotReportYetController with an authorised user" when {

    def callSubmit(isEditMode: Boolean = false) = TestCannotReportYetController.submit(isEditMode = isEditMode)(subscriptionRequest)

    "NewIncomeSourceFlowFeature is disabled" should {
      "not in edit mode" should {
        s"redirect to '${
          incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        }' on the business journey" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(incomeSource = TestModels.testIncomeSourceBusiness))

          val goodRequest = callSubmit()

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAll = 1)
        }

        s"redirect to '${
          incometax.subscription.controllers.routes.TermsController.show().url
        }' on the property journey" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(incomeSource = TestModels.testIncomeSourceProperty))

          val goodRequest = callSubmit()

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.incomesource.controllers.routes.OtherIncomeController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAll = 1)
        }

        s"redirect to '${
          incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        }' on the both journey" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(incomeSource = TestModels.testIncomeSourceBoth))

          val goodRequest = callSubmit()

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAll = 1)
        }
      }

      "in edit mode" should {
        s"redirect to '${
          incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
        }'" in {
          val goodRequest = callSubmit(isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAll = 0)
        }
      }
    }

    "NewIncomeSourceFlowFeature is enabled" should {
      "not in edit mode" should {
        s"redirect to '${
          incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        }' on the business journey" in {
          enable(NewIncomeSourceFlowFeature)
          setupMockKeystore(fetchAll = testCacheMapCustom(
            rentUkProperty = testRentUkProperty_no_property,
            workForYourself = testWorkForYourself_yes
          ))

          val goodRequest = callSubmit()

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAll = 1)
        }

        s"redirect to '${
          incometax.subscription.controllers.routes.TermsController.show().url
        }' on the property journey" in {
          enable(NewIncomeSourceFlowFeature)
          setupMockKeystore(fetchAll = testCacheMapCustom(
            rentUkProperty = testRentUkProperty_property_and_other,
            workForYourself = testWorkForYourself_no
          ))

          val goodRequest = callSubmit()

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.incomesource.controllers.routes.OtherIncomeController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAll = 1)
        }

        s"redirect to '${
          incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        }' on the both journey" in {
          enable(NewIncomeSourceFlowFeature)
          setupMockKeystore(fetchAll = testCacheMapCustom(
            rentUkProperty = testRentUkProperty_property_and_other,
            workForYourself = testWorkForYourself_yes
          ))

          val goodRequest = callSubmit()

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAll = 1)
        }
      }

      "in edit mode" should {
        s"redirect to '${
          incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
        }'" in {
          enable(NewIncomeSourceFlowFeature)
          val goodRequest = callSubmit(isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAll = 0)
        }
      }
    }
  }


  "backUrl" when {
    def evalBackUrl(incomeSourceType: IncomeSourceType, matchTaxYear: Option[Boolean], isEditMode: Boolean) =
      TestCannotReportYetController.getBackUrl(incomeSourceType, matchTaxYear, isEditMode)

    "income source is property" when {
      "when new income source is disabled, return income source" in {
        val result = evalBackUrl(Property, None, isEditMode = false)
        result mustBe incometax.incomesource.controllers.routes.IncomeSourceController.show().url
      }

      "when new income source is enabled, return work for yourself" in {
        enable(NewIncomeSourceFlowFeature)
        val result = evalBackUrl(Property, None, isEditMode = false)
        result mustBe incometax.incomesource.controllers.routes.WorkForYourselfController.show().url
      }
    }

    "income source is business" when {
      "not in edit mode and " when {
        "match tax year is answered no, return other income" in {
          val result = evalBackUrl(Business, Some(false), isEditMode = false)
          result mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url
        }
        "match tax year is answered yes, return other income error" in {
          val result = evalBackUrl(Business, Some(true), isEditMode = false)
          result mustBe incometax.business.controllers.routes.MatchTaxYearController.show().url
        }
      }
      "in edit mode and " when {
        "match tax year is answered no, return other income" in {
          val result = evalBackUrl(Business, Some(false), isEditMode = true)
          result mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true).url
        }
        "match tax year is answered yes, return other income error" in {
          val result = evalBackUrl(Business, Some(true), isEditMode = true)
          result mustBe incometax.business.controllers.routes.MatchTaxYearController.show(editMode = true).url
        }
      }
    }

    "income source is both" when {
      "not in edit mode and " when {
        "match tax year is answered no, return other income" in {
          val result = evalBackUrl(Both, Some(false), isEditMode = false)
          result mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show().url
        }
        "match tax year is answered yes, return other income error" in {
          val result = evalBackUrl(Both, Some(true), isEditMode = false)
          result mustBe incometax.business.controllers.routes.MatchTaxYearController.show().url
        }
      }
      "in edit mode and " when {
        "match tax year is answered no, return other income" in {
          val result = evalBackUrl(Both, Some(false), isEditMode = true)
          result mustBe incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true).url
        }
        "match tax year is answered yes, return other income error" in {
          val result = evalBackUrl(Both, Some(true), isEditMode = true)
          result mustBe incometax.business.controllers.routes.MatchTaxYearController.show(editMode = true).url
        }
      }
    }
  }

  authorisationTests()
}
