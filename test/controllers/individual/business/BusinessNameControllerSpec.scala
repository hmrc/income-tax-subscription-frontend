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

import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import connectors.httpparser.PostSubscriptionDetailsHttpParser._
import controllers.ControllerBaseSpec
import forms.individual.business.BusinessNameForm
import models.DateModel
import models.common.BusinessNameModel
import models.individual.business.{BusinessStartDate, BusinessTradeNameModel, SelfEmploymentData}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockMultipleSelfEmploymentsService, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys.BusinessName
import utilities.TestModels._

import scala.concurrent.Future

class BusinessNameControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService with MockMultipleSelfEmploymentsService with FeatureSwitching {

  val id: String = "testId"

  override val controllerName: String = "BusinessNameController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessNameController.show(id, isEditMode = false),
    "submit" -> TestBusinessNameController.submit(id, isEditMode = false)
  )

  object TestBusinessNameController extends BusinessNameController(
    mockAuthService,
    MockSubscriptionDetailsService,
    mockMultipleSelfEmploymentsService
  )

  val testBusinessNameModel: BusinessNameModel = BusinessNameModel("ITSA me, Mario")

  val selfEmploymentData: SelfEmploymentData = SelfEmploymentData(
    id = id,
    businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1"))),
    businessName = Some(BusinessNameModel("testName")),
    businessTradeName = Some(BusinessTradeNameModel("testTrade"))
  )

  def modelToFormData(businessNameModel: BusinessNameModel): Seq[(String, String)] = {
    BusinessNameForm.businessNameValidationForm(Nil).fill(businessNameModel).data.toSeq
  }

  "Release four is disabled" when {
    "Calling the show action of the BusinessNameController with an authorised user" should {
      lazy val result = TestBusinessNameController.show(id, isEditMode = false)(subscriptionRequest)

      "return ok (200)" in {
        disable(ReleaseFour)
        mockFetchBusinessNameFromSubscriptionDetails(None)
        status(result) must be(Status.OK)

        await(result)
        verifySubscriptionDetailsFetch(BusinessName, 1)
        verifySubscriptionDetailsSave(BusinessName, 0)

      }
    }
  }

  "Release four is disabled" when {
    "Calling the submit action of the BusinessNameController with an authorised user on the sign up journey and valid submission" when {

      def callShow(isEditMode: Boolean): Future[Result] =
        TestBusinessNameController.submit(id, isEditMode = isEditMode)(
          subscriptionRequest
            .post(BusinessNameForm.businessNameForm(Nil).form, BusinessNameModel("Test business"))
        )

      "it is in edit mode" should {
        s"return a redirect status (SEE_OTHER - 303) to '${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchBusinessNameFromSubscriptionDetails(None)

          val goodRequest = callShow(isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

          await(goodRequest)
          verifySubscriptionDetailsFetch(BusinessName, 1)
          verifySubscriptionDetailsSave(BusinessName, 1)
        }
      }

    "it is not in edit mode" when {
      "the user is business only" should {
        s"redirect to ${controllers.individual.business.routes.WhatYearToSignUpController.show().url}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testCacheMap(incomeSource = testIncomeSourceBusiness))

            val goodRequest = callShow(isEditMode = false)

            status(goodRequest) mustBe Status.SEE_OTHER
            redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.WhatYearToSignUpController.show().url)

            await(goodRequest)
            verifySubscriptionDetailsFetchAll(2)
            verifySubscriptionDetailsSave(BusinessName, 1)
          }
        }
      }

    }

    "Release four is enabled" when {
      "show" should {
        "return ok (200)" when {
          "the connector returns data for the current business" in {
            enable(ReleaseFour)
            mockFetchAllBusinesses(
              Seq(selfEmploymentData)
            )
            val result = TestBusinessNameController.show(id, isEditMode = false)(subscriptionRequest)
            status(result) mustBe OK
            contentType(result) mustBe Some("text/html")
          }
          "the connector returns data for the current business but with no business name" in {
            enable(ReleaseFour)
            mockFetchAllBusinesses(
              Seq(selfEmploymentData.copy(businessName = None))
            )
            val result = TestBusinessNameController.show(id, isEditMode = false)(subscriptionRequest)
            status(result) mustBe OK
            contentType(result) mustBe Some("text/html")
          }
        }
        "Throw an internal exception error" when {
          "the connector returns an error" in {
            enable(ReleaseFour)
            mockFetchAllBusinessesException()

            intercept[InternalServerException](await(TestBusinessNameController.show(id, isEditMode = false)(subscriptionRequest)))
          }

        }
      }

      "submit" when {
        "not in edit mode" should {
          s"return $SEE_OTHER" when {
            "the users input is valid and is saved" in {
              enable(ReleaseFour)
              mockFetchAllBusinesses(
                Seq(selfEmploymentData.copy(businessName = None, businessTradeName = None))
              )
              mockSaveBusinessName(id, testBusinessNameModel)(Right(PostSubscriptionDetailsSuccessResponse))
              val result = TestBusinessNameController.submit(id, isEditMode = false)(subscriptionRequest
                .withFormUrlEncodedBody(modelToFormData(testBusinessNameModel): _*)
              )
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(routes.BusinessTradeNameController.show(id).url)
            }
          }
          s"return $BAD_REQUEST" when {
            "the user submits invalid data" in {
              enable(ReleaseFour)
              mockFetchAllBusinesses(
                Seq(selfEmploymentData.copy(businessName = None, businessTradeName = None))
              )
              val result = TestBusinessNameController.submit(id, isEditMode = false)(subscriptionRequest)
              status(result) mustBe BAD_REQUEST
              contentType(result) mustBe Some("text/html")
            }
            "the user enters a business name which would cause a duplicate business name / trade combination" in {
              enable(ReleaseFour)
              mockFetchAllBusinesses(
                Seq(
                  selfEmploymentData.copy(
                    id = "idOne",
                    businessName = Some(BusinessNameModel("nameOne")),
                    businessTradeName = Some(BusinessTradeNameModel("tradeOne"))
                  ),
                  selfEmploymentData.copy(
                    id = "idTwo",
                    businessName = None,
                    businessTradeName = Some(BusinessTradeNameModel("tradeOne"))
                  )
                )
              )
              val result = TestBusinessNameController.submit("idTwo", isEditMode = false)(
                subscriptionRequest.withFormUrlEncodedBody(modelToFormData(BusinessNameModel("nameOne")): _*)
              )
              status(result) mustBe BAD_REQUEST
              contentType(result) mustBe Some("text/html")
            }
          }
          "throw an exception" when {
            "an error is returned when retrieving all businesses" in {
              enable(ReleaseFour)
              mockFetchAllBusinessesException()

              intercept[InternalServerException](await(TestBusinessNameController.submit(id, isEditMode = false)(subscriptionRequest)))
            }
          }
        }
        "in edit mode" should {
          s"return $SEE_OTHER" when {
            "the users answer is updated correctly" in {
              enable(ReleaseFour)
              mockFetchAllBusinesses(
                Seq(selfEmploymentData.copy(businessName = Some(BusinessNameModel("nameOne"))))
              )
              mockSaveBusinessName(id, testBusinessNameModel)(Right(PostSubscriptionDetailsSuccessResponse))
              val result = TestBusinessNameController.submit(id, isEditMode = true)(
                subscriptionRequest.withFormUrlEncodedBody(modelToFormData(testBusinessNameModel): _*)
              )
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.SelfEmploymentsCYAController.show().url)
            }
            "the user does not update their answer" in {
              enable(ReleaseFour)
              mockFetchAllBusinesses(
                Seq(selfEmploymentData)
              )
              mockSaveBusinessName(id, testBusinessNameModel)(Right(PostSubscriptionDetailsSuccessResponse))
              val result = TestBusinessNameController.submit(id, isEditMode = true)(
                subscriptionRequest.withFormUrlEncodedBody(modelToFormData(testBusinessNameModel): _*)
              )
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.SelfEmploymentsCYAController.show().url)
            }
          }
        }

      }
    }

    "Calling the submit action of the BusinessNameController with an authorised user and invalid submission" should {
      lazy val badRequest = TestBusinessNameController.submit(id, isEditMode = false)(subscriptionRequest)

      "return a bad request status (400)" in {
        disable(ReleaseFour)
        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsFetch(BusinessName, 0)
        verifySubscriptionDetailsSave(BusinessName, 0)
      }
    }

    "The back url" when {
      "in edit mode and release four disabled" should {
        s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          disable(ReleaseFour)
          TestBusinessNameController.backUrl(isEditMode = true) mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }
      }
      "in edit mode and release four enabled" should {
        s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          enable(ReleaseFour)
          TestBusinessNameController.backUrl(isEditMode = true) mustBe controllers.individual.subscription.routes.SelfEmploymentsCYAController.show().url
        }
      }
      "not in edit mode" should {
        s"redirect to ${controllers.individual.incomesource.routes.IncomeSourceController.show().url}" in {
          TestBusinessNameController.backUrl(isEditMode = false) mustBe controllers.individual.incomesource.routes.IncomeSourceController.show().url
        }
      }
    }
  }

  authorisationTests()

}
