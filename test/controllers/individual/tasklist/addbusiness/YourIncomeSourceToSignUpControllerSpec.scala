/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.individual.tasklist.addbusiness

import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import controllers.individual.ControllerBaseSpec
import controllers.individual.actions.mocks.{MockIdentifierAction, MockSignUpJourneyRefiner}
import models.common.business._
import models.common.{IncomeSources, OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAccountingPeriodService, MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.addbusiness.YourIncomeSourceToSignUp

import scala.concurrent.Future

class YourIncomeSourceToSignUpControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockReferenceRetrieval
  with MockAuditingService
  with MockAccountingPeriodService
  with MockIdentifierAction
  with MockSignUpJourneyRefiner {

  override val controllerName: String = "YourIncomeSourceToSignUpController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "show" should {
    "return OK status" when {
      "there are no income sources added" in new Setup {

        mockFetchAllIncomeSources(IncomeSources(Seq.empty, None, None, None))
        mockFetchPrePopFlag(Some(true))

        mockYourIncomeSourceToSignUpView(IncomeSources(Seq.empty[SelfEmploymentData], None, None, None), isPrePopped = true)

        val result: Result = await(controller.show()(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there are multiple different income sources added" in new Setup {

        mockFetchAllIncomeSources(IncomeSources(
          Seq(testSelfEmployment("id"), testSelfEmployment("id2")),
          Some(Cash),
          Some(testUkProperty),
          Some(testForeignProperty)
        ))
        mockFetchPrePopFlag(Some(true))

        mockYourIncomeSourceToSignUpView(
          IncomeSources(
            selfEmployments = Seq(
              testSelfEmployment("id"),
              testSelfEmployment("id2")
            ),
            selfEmploymentAccountingMethod = Some(Cash),
            ukProperty = Some(testUkProperty),
            foreignProperty = Some(testForeignProperty)
          ),
          isPrePopped = true
        )

        val result: Result = await(controller.show()(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is a PrePop flag" in new Setup {
        mockFetchAllIncomeSources(IncomeSources(Seq.empty, None, None, None))
        mockFetchPrePopFlag(Some(true))

        mockYourIncomeSourceToSignUpView(IncomeSources(Seq.empty[SelfEmploymentData], None, None, None), isPrePopped = true)

        val result: Result = await(controller.show()(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there is no PrePop flag" when {
        "fetching the flag returns false" in new Setup {
          mockFetchAllIncomeSources(IncomeSources(Seq.empty, None, None, None))
          mockFetchPrePopFlag(Some(false))

          mockYourIncomeSourceToSignUpView(IncomeSources(Seq.empty[SelfEmploymentData], None, None, None), isPrePopped = false)

          val result: Result = await(controller.show()(subscriptionRequest))

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "fetching the flag returns none" in new Setup {
          mockFetchAllIncomeSources(IncomeSources(Seq.empty, None, None, None))
          mockFetchPrePopFlag(None)

          mockYourIncomeSourceToSignUpView(IncomeSources(Seq.empty[SelfEmploymentData], None, None, None), isPrePopped = false)

          val result: Result = await(controller.show()(subscriptionRequest))

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
      }
    }
  }

  "submit" should {
    "redirect to the declaration page and save the income sources as complete" when {
      "all income sources are complete" in new Setup {
        mockFetchAllIncomeSources(IncomeSources(
          Seq(testSelfEmployment("id"), testSelfEmployment("id2")),
          Some(Cash),
          Some(testUkProperty),
          Some(testForeignProperty)
        ))
        mockSaveIncomeSourceConfirmation(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)

        verifySaveIncomeSourceConfirmation()
      }
      "only self employment income sources are added and complete" in new Setup {
        mockFetchAllIncomeSources(IncomeSources(
          Seq(testSelfEmployment("id"), testSelfEmployment("id2")),
          Some(Cash),
          None,
          None,
        ))
        mockSaveIncomeSourceConfirmation(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)

        verifySaveIncomeSourceConfirmation()
      }
      "only uk property income sources are added and complete" in new Setup {
        mockFetchAllIncomeSources(IncomeSources(
          Seq.empty,
          None,
          Some(testUkProperty),
          None
        ))
        mockSaveIncomeSourceConfirmation(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)

        verifySaveIncomeSourceConfirmation()
      }
      "only foreign property income sources are added and complete" in new Setup {
        mockFetchAllIncomeSources(IncomeSources(
          Seq.empty,
          None,
          None,
          Some(testForeignProperty)
        ))
        mockSaveIncomeSourceConfirmation(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.GlobalCheckYourAnswersController.show.url)

        verifySaveIncomeSourceConfirmation()
      }
    }
    "throw an exception" when {
      "failed to save income sources" in new Setup{
        mockFetchAllIncomeSources(IncomeSources(
          Seq(testSelfEmployment("id"), testSelfEmployment("id2")),
          Some(Cash),
          Some(testUkProperty),
          Some(testForeignProperty)
        ))
        mockSaveIncomeSourceConfirmation(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        intercept[InternalServerException](await(result))
          .message mustBe "[YourIncomeSourceToSignUpController][submit] - failed to save income sources confirmation"
      }
    }
  }

  "backUrl" when {
      "go to the ORM page" in new Setup {
        controller.backUrl mustBe controllers.individual.routes.WhatYouNeedToDoController.show.url
      }
  }

  trait Setup {
    val yourIncomeSourceToSignUpView: YourIncomeSourceToSignUp = mock[YourIncomeSourceToSignUp]

    val controller = new YourIncomeSourceToSignUpController(
      fakeIdentifierAction,
      fakeSignUpJourneyRefiner,
      yourIncomeSourceToSignUpView,
      mockSubscriptionDetailsService)

    def mockYourIncomeSourceToSignUpView(incomeSources: IncomeSources, isPrePopped: Boolean): Unit = {
      when(yourIncomeSourceToSignUpView(
        ArgumentMatchers.eq(routes.YourIncomeSourceToSignUpController.submit),
        ArgumentMatchers.eq(controllers.individual.routes.WhatYouNeedToDoController.show.url),
        ArgumentMatchers.eq(incomeSources),
        ArgumentMatchers.eq(isPrePopped),
      )(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(HtmlFormat.empty)
    }
  }

  def testSelfEmployment(id: String): SelfEmploymentData = SelfEmploymentData(
    id = id,
    businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
    businessName = Some(BusinessNameModel("business name")),
    businessTradeName = Some(BusinessTradeNameModel("business trade")),
    businessAddress = Some(BusinessAddressModel(Address(
      lines = Seq(
        "1 long road",
        "lonely street"
      ),
      postcode = Some("ZZ1 1ZZ")
    ))),
    confirmed = true
  )

  def testUkProperty: PropertyModel = PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("2", "2", "1981")),
    confirmed = true
  )

  def testForeignProperty: OverseasPropertyModel = OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("3", "3", "1982")),
    confirmed = true
  )

}
