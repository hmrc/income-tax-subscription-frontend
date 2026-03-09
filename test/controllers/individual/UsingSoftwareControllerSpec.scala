/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.individual

import connectors.httpparser.SaveSessionDataHttpParser.{SaveSessionDataSuccessResponse, UnexpectedStatusFailure}
import forms.individual.UsingSoftwareForm
import models.status.MandationStatus.{Mandated, Voluntary}
import models.{EligibilityStatus, No, SessionData, Yes}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.*
import uk.gov.hmrc.http.InternalServerException
import utilities.agent.TestModels.testSelectedTaxYearCurrent
import views.ViewSpecTrait.testBackUrl
import views.html.individual.UsingSoftware

import scala.concurrent.Future

class UsingSoftwareControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockClientDetailsRetrieval
  with MockGetEligibilityStatusService
  with MockMandationStatusService
  with MockSessionDataService
  with MockReferenceRetrieval
  with MockSubscriptionDetailsService {

  val mockUsingSoftware: UsingSoftware = mock[UsingSoftware]

  when(mockUsingSoftware(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)

  object TestUsingSoftwareController extends UsingSoftwareController(
    mockUsingSoftware,
    mockSessionDataService,
    mockGetEligibilityStatusService,
    mockMandationStatusService,
    mockReferenceRetrieval,
    mockSubscriptionDetailsService
  )(
    mockAuditingService,
    mockAuthService,
    appConfig
  )

  trait Setup {
    val usingSoftware: UsingSoftware = mock[UsingSoftware]
    val controller: UsingSoftwareController = new UsingSoftwareController(
      usingSoftware,
      mockSessionDataService,
      mockGetEligibilityStatusService,
      mockMandationStatusService,
      mockReferenceRetrieval,
      mockSubscriptionDetailsService
    )(
      mockAuditingService,
      mockAuthService,
      appConfig
    )
  }

  override val controllerName: String = "UsingSoftwareController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUsingSoftwareController.show(false),
    "submit" -> TestUsingSoftwareController.submit(false)
  )

  "show" must {
    "return OK with the page content" in new Setup {
      mockGetAllSessionData(SessionData())
      mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
      mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
      when(usingSoftware(
        ArgumentMatchers.eq(UsingSoftwareForm.usingSoftwareForm),
        ArgumentMatchers.eq(routes.UsingSoftwareController.submit(false)),
        ArgumentMatchers.eq(controllers.individual.accountingperiod.routes.AccountingPeriodController.show.url)
      )(any(), any())).thenReturn(HtmlFormat.empty)

      val result: Future[Result] = TestUsingSoftwareController.show(false)(
        subscriptionRequest
      )
      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
    }
  }

  "submit" when {
    "the users submission is invalid" should {
      "return a bad request with the page content" in new Setup {
        mockGetAllSessionData(SessionData())
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
        mockGetMandationService(Voluntary, Mandated)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
        when(usingSoftware(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.submit(false)(subscriptionRequest)

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
    }
    "the user submits 'Yes'" should {
      "redirect to the your income sources page" when {
        "the user is mandated for the current tax year" in new Setup {
          mockGetMandationService(Mandated, Voluntary)
          mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
          mockSaveSoftwareStatus(Yes)(Right(SaveSessionDataSuccessResponse))

          val result: Future[Result] = controller.submit(false)(subscriptionRequest.post(UsingSoftwareForm.usingSoftwareForm, Yes))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        }
      }
      "redirect to the what year to sign up page" when {
        "the user is able to sign up for both tax years" in new Setup {
          Seq(Yes, No).foreach { answer =>
            mockGetMandationService(Voluntary, Voluntary)
            mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))
            mockSaveSoftwareStatus(answer)(Right(SaveSessionDataSuccessResponse))

            Seq(false, true).foreach { editMode =>
              val result: Future[Result] = controller.submit(editMode)(subscriptionRequest.post(UsingSoftwareForm.usingSoftwareForm, Yes))

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(editMode match {
                case false => controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
                case true => controllers.individual.routes.GlobalCheckYourAnswersController.show.url
              })
            }
          }
        }
      }
    }
    "an error occurs when saving the software status" should {
      "throw an internal server exception" in new Setup {
        mockSaveSoftwareStatus(Yes)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))

        val result: Future[Result] = controller.submit(false)(subscriptionRequest.post(UsingSoftwareForm.usingSoftwareForm, Yes))

        intercept[InternalServerException](await(result))
          .message mustBe "[UsingSoftwareController][submit] - Could not save using software answer"
      }
    }
  }

}
