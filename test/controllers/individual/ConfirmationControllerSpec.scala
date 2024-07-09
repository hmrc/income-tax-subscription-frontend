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

package controllers.individual

import connectors.individual.PreferencesFrontendConnector
import models.EligibilityStatus
import models.status.MandationStatus.{Mandated, Voluntary}
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.{reset, when}
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks._
import uk.gov.hmrc.http.NotFoundException
import utilities.TestModels.{testSelectedTaxYearCurrent, testSelectedTaxYearNext}
import utilities.individual.TestConstants.{testNino, testUtr}
import views.html.individual.confirmation.SignUpConfirmation

import scala.concurrent.Future

class ConfirmationControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAccountingPeriodService
  with MockUserMatchingService
  with MockNinoService
  with MockReferenceRetrieval
  with MockAuditingService {

  val mockSignUpConfirmation: SignUpConfirmation = mock[SignUpConfirmation]
  val mockPreferencesFrontendConnector: PreferencesFrontendConnector = mock[PreferencesFrontendConnector]

  override def beforeEach(): Unit = {
    reset(mockSignUpConfirmation)
    reset(mockPreferencesFrontendConnector)
    super.beforeEach()
  }

  object TestConfirmationController extends ConfirmationController(
    mockSignUpConfirmation,
    mockMandationStatusService,
    mockNinoService,
    mockReferenceRetrieval,
    mockPreferencesFrontendConnector,
    MockSubscriptionDetailsService
  )(
    mockAuditingService,
    mockAuthService
  )

  val taxQuarter1: (String, String) = ("agent.sign-up.complete.julyUpdate", "2020")
  val taxQuarter2: (String, String) = ("agent.sign-up.complete.octoberUpdate", "2020")
  val taxQuarter3: (String, String) = ("agent.sign-up.complete.januaryUpdate", "2021")
  val taxQuarter4: (String, String) = ("agent.sign-up.complete.aprilUpdate", "2021")


  implicit val request: Request[_] = FakeRequest()

  override val controllerName: String = "ConfirmationControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmationController.show,
    "submit" -> TestConfirmationController.submit
  )

  "show" must {
    "return a not found exception" when {
      "the user is not enrolled" in {
        val result = TestConfirmationController.show(subscriptionRequest)

        intercept[NotFoundException](await(result)).message mustBe "AuthPredicates.enrolledPredicate"
      }
    }
  }

  "show" when {
    "the confirmation page feature switch is enabled" must {
      "return the sign up confirmation page" when {
        "the user signed up for the current tax year" when {
          "the user is mandated for current year" when {
            "the user has no digital preference available" in {
              mockGetMandationService(testUtr)(Mandated, Voluntary)
              mockGetEligibilityStatus(testUtr)(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
              mockGetNino(testNino)
              mockAuthEnrolled()
              mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))

              when(mockPreferencesFrontendConnector.getOptedInStatus(any())) thenReturn Future.successful(None)
              when(mockSignUpConfirmation(matches(true), matches(false), any(), matches(testNino), matches(None))(any(), any(), any()))
                .thenReturn(HtmlFormat.empty)

              val result: Future[Result] = TestConfirmationController.show(subscriptionRequest)

              status(result) mustBe OK
              contentType(result) mustBe Some(HTML)
            }
            "the user has a paper preference" in {
              mockGetMandationService(testUtr)(Mandated, Voluntary)
              mockGetEligibilityStatus(testUtr)(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
              mockGetNino(testNino)
              mockAuthEnrolled()
              mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
              when(mockPreferencesFrontendConnector.getOptedInStatus(any())) thenReturn Future.successful(Some(false))
              when(mockSignUpConfirmation(matches(true), matches(false), any(), matches(testNino), matches(Some(false)))(any(), any(), any()))
                .thenReturn(HtmlFormat.empty)

              val result: Future[Result] = TestConfirmationController.show(subscriptionRequest)

              status(result) mustBe OK
              contentType(result) mustBe Some(HTML)
            }
            "the user has a digital preference" in {
              mockGetMandationService(testUtr)(Mandated, Voluntary)
              mockGetEligibilityStatus(testUtr)(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
              mockGetNino(testNino)
              mockAuthEnrolled()
              mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
              when(mockPreferencesFrontendConnector.getOptedInStatus(any())) thenReturn Future.successful(Some(true))
              when(mockSignUpConfirmation(matches(true), matches(false), any(), matches(testNino), matches(Some(true)))(any(), any(), any()))
                .thenReturn(HtmlFormat.empty)

              val result: Future[Result] = TestConfirmationController.show(subscriptionRequest)

              status(result) mustBe OK
              contentType(result) mustBe Some(HTML)
            }
          }
          "the user is not mandated for current year" when {
            "the user has no digital preference available" in {
              mockGetMandationService(testUtr)(Voluntary, Voluntary)
              mockGetEligibilityStatus(testUtr)(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
              mockGetNino(testNino)
              mockAuthEnrolled()
              mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
              when(mockPreferencesFrontendConnector.getOptedInStatus(any())) thenReturn Future.successful(None)
              when(mockSignUpConfirmation(matches(false), matches(false), any(), matches(testNino), matches(None))(any(), any(), any()))
                .thenReturn(HtmlFormat.empty)

              val result: Future[Result] = TestConfirmationController.show(subscriptionRequest)

              status(result) mustBe OK
              contentType(result) mustBe Some(HTML)
            }
            "the user has a paper preference" in {
              mockGetMandationService(testUtr)(Voluntary, Voluntary)
              mockGetEligibilityStatus(testUtr)(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
              mockGetNino(testNino)
              mockAuthEnrolled()
              mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
              when(mockPreferencesFrontendConnector.getOptedInStatus(any())) thenReturn Future.successful(Some(false))
              when(mockSignUpConfirmation(matches(false), matches(false), any(), matches(testNino), matches(Some(false)))(any(), any(), any()))
                .thenReturn(HtmlFormat.empty)

              val result: Future[Result] = TestConfirmationController.show(subscriptionRequest)

              status(result) mustBe OK
              contentType(result) mustBe Some(HTML)
            }
            "the user has a digital preference" in {
              mockGetMandationService(testUtr)(Voluntary, Voluntary)
              mockGetEligibilityStatus(testUtr)(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
              mockGetNino(testNino)
              mockAuthEnrolled()
              mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))
              when(mockPreferencesFrontendConnector.getOptedInStatus(any())) thenReturn Future.successful(Some(true))
              when(mockSignUpConfirmation(matches(false), matches(false), any(), matches(testNino), matches(Some(true)))(any(), any(), any()))
                .thenReturn(HtmlFormat.empty)

              val result: Future[Result] = TestConfirmationController.show(subscriptionRequest)

              status(result) mustBe OK
              contentType(result) mustBe Some(HTML)
            }
          }
        }
        "the user signed up for the next tax year" when {
          "the user has no digital preference available" in {
            mockGetMandationService(testUtr)(Voluntary, Voluntary)
            mockGetEligibilityStatus(testUtr)(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
            mockGetNino(testNino)
            mockAuthEnrolled()
            mockFetchSelectedTaxYear(Some(testSelectedTaxYearNext))
            when(mockPreferencesFrontendConnector.getOptedInStatus(any())) thenReturn Future.successful(None)
            when(mockSignUpConfirmation(matches(false), matches(true), any(), matches(testNino), matches(None))(any(), any(), any()))
              .thenReturn(HtmlFormat.empty)

            val result: Future[Result] = TestConfirmationController.show(subscriptionRequest)

            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
          "the user has a paper preference" in {
            mockGetMandationService(testUtr)(Voluntary, Voluntary)
            mockGetEligibilityStatus(testUtr)(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
            mockGetNino(testNino)
            mockAuthEnrolled()
            mockFetchSelectedTaxYear(Some(testSelectedTaxYearNext))
            when(mockPreferencesFrontendConnector.getOptedInStatus(any())) thenReturn Future.successful(Some(false))
            when(mockSignUpConfirmation(matches(false), matches(true), any(), matches(testNino), matches(Some(false)))(any(), any(), any()))
              .thenReturn(HtmlFormat.empty)

            val result: Future[Result] = TestConfirmationController.show(subscriptionRequest)

            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
          "the user has a digital preference" in {
            mockGetMandationService(testUtr)(Voluntary, Voluntary)
            mockGetEligibilityStatus(testUtr)(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
            mockGetNino(testNino)
            mockAuthEnrolled()
            mockFetchSelectedTaxYear(Some(testSelectedTaxYearNext))
            when(mockPreferencesFrontendConnector.getOptedInStatus(any())) thenReturn Future.successful(Some(true))
            when(mockSignUpConfirmation(matches(false), matches(true), any(), matches(testNino), matches(Some(true)))(any(), any(), any()))
              .thenReturn(HtmlFormat.empty)

            val result: Future[Result] = TestConfirmationController.show(subscriptionRequest)

            status(result) mustBe OK
            contentType(result) mustBe Some(HTML)
          }
        }
      }
    }
  }

  "submit" should {
    "redirect the user to the sign out controller" in {
      mockAuthEnrolled()

      val result: Future[Result] = TestConfirmationController.submit(subscriptionRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SignOutController.signOut.url)
    }
  }

  authorisationTests()

}
