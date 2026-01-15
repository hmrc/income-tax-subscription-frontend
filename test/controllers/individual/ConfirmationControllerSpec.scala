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

import common.Constants.ITSASessionKeys.FULLNAME
import connectors.individual.PreferencesFrontendConnector
import controllers.ControllerSpec
import controllers.individual.actions.mocks.MockConfirmationJourneyRefiner
import models.common.AccountingYearModel
import models.{Current, Next, SessionData}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import play.api.mvc.Result
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import services.mocks.*
import views.html.individual.confirmation.SignUpConfirmation
import services.SignedUpDateService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import utilities.ImplicitDateFormatterImpl

import scala.concurrent.Future

class ConfirmationControllerSpec extends ControllerSpec with MockConfirmationJourneyRefiner with MockSubscriptionDetailsService {

  "show" must {
    "return OK with the page content" when {
      "the user signed up for the current tax year" when {
        "the user has a paperless preference" in {
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
          when(mockPreferencesFrontendConnector.getOptedInStatus(ArgumentMatchers.any())).thenReturn(Future.successful(Some(true)))
          when(mockSignedUpDateService.getSignedUpDate(ArgumentMatchers.any[SessionData]())(ArgumentMatchers.any[HeaderCarrier]()))
            .thenReturn(Future.successful(LocalDate.of(2026, 1, 13)))
          when(mockSignUpConfirmation(
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(Some("FirstName LastName")),
            ArgumentMatchers.eq(nino),
            ArgumentMatchers.eq(Some(true)),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.any[LocalDate]()
          )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = TestConfirmationController.show()(request.withSession(FULLNAME -> "FirstName LastName"))

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "the user has a paper preference" in {
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
          when(mockPreferencesFrontendConnector.getOptedInStatus(ArgumentMatchers.any())).thenReturn(Future.successful(Some(false)))
          when(mockSignedUpDateService.getSignedUpDate(ArgumentMatchers.any[SessionData]())(ArgumentMatchers.any[HeaderCarrier]()))
            .thenReturn(Future.successful(LocalDate.of(2026, 1, 13)))
          when(mockSignUpConfirmation(
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(Some("FirstName LastName")),
            ArgumentMatchers.eq(nino),
            ArgumentMatchers.eq(Some(false)),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.any[LocalDate]()
          )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = TestConfirmationController.show()(request.withSession(FULLNAME -> "FirstName LastName"))

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "no preference could be retrieved" in {
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
          when(mockPreferencesFrontendConnector.getOptedInStatus(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          when(mockSignedUpDateService.getSignedUpDate(ArgumentMatchers.any[SessionData]())(ArgumentMatchers.any[HeaderCarrier]()))
            .thenReturn(Future.successful(LocalDate.of(2026, 1, 13)))

          when(mockSignUpConfirmation(
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(Some("FirstName LastName")),
            ArgumentMatchers.eq(nino),
            ArgumentMatchers.eq(None),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.any[LocalDate]()
          )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = TestConfirmationController.show()(request.withSession(FULLNAME -> "FirstName LastName"))

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
      }
      "the user signed up for the next tax year" when {
        "the user has a paperless preference" in {
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
          when(mockPreferencesFrontendConnector.getOptedInStatus(ArgumentMatchers.any())).thenReturn(Future.successful(Some(true)))
          when(mockSignedUpDateService.getSignedUpDate(ArgumentMatchers.any[SessionData]())(ArgumentMatchers.any[HeaderCarrier]()))
            .thenReturn(Future.successful(LocalDate.of(2026, 1, 13)))
          when(mockSignUpConfirmation(
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(Some("FirstName LastName")),
            ArgumentMatchers.eq(nino),
            ArgumentMatchers.eq(Some(true)),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.any[LocalDate]()
          )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = TestConfirmationController.show()(request.withSession(FULLNAME -> "FirstName LastName"))

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "the user has a paper preference" in {
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
          when(mockPreferencesFrontendConnector.getOptedInStatus(ArgumentMatchers.any())).thenReturn(Future.successful(Some(false)))
          when(mockSignedUpDateService.getSignedUpDate(ArgumentMatchers.any[SessionData]())(ArgumentMatchers.any[HeaderCarrier]()))
            .thenReturn(Future.successful(LocalDate.of(2026, 1, 13)))
          when(mockSignUpConfirmation(
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(Some("FirstName LastName")),
            ArgumentMatchers.eq(nino),
            ArgumentMatchers.eq(Some(false)),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.any[LocalDate]()
          )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = TestConfirmationController.show()(request.withSession(FULLNAME -> "FirstName LastName"))

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
        "no preference could be retrieved" in {
          mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
          when(mockPreferencesFrontendConnector.getOptedInStatus(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          when(mockSignedUpDateService.getSignedUpDate(ArgumentMatchers.any[SessionData]())(ArgumentMatchers.any[HeaderCarrier]()))
            .thenReturn(Future.successful(LocalDate.of(2026, 1, 13)))

          when(mockSignUpConfirmation(
            ArgumentMatchers.eq(false),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.eq(Some("FirstName LastName")),
            ArgumentMatchers.eq(nino),
            ArgumentMatchers.eq(None),
            ArgumentMatchers.eq(true),
            ArgumentMatchers.any[LocalDate]()
          )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

          val result: Future[Result] = TestConfirmationController.show()(request.withSession(FULLNAME -> "FirstName LastName"))

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
      }
    }
  }

  "submit" must {
    "redirect to the sign out route" in {
      val result: Future[Result] = TestConfirmationController.submit(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SignOutController.signOut.url)
    }
  }

  lazy val mockPreferencesFrontendConnector: PreferencesFrontendConnector = mock[PreferencesFrontendConnector]
  lazy val mockSignUpConfirmation: SignUpConfirmation = mock[SignUpConfirmation]
  lazy val mockSignedUpDateService: SignedUpDateService = mock[SignedUpDateService]
  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = mock[ImplicitDateFormatterImpl]

  override def beforeEach(): Unit = {
    reset(mockSignUpConfirmation)
    reset(mockSignedUpDateService)
    reset(mockPreferencesFrontendConnector)
    super.beforeEach()
  }

  object TestConfirmationController extends ConfirmationController(
    fakeIdentifierAction,
    fakeConfirmationJourneyRefiner,
    mockPreferencesFrontendConnector,
    mockSubscriptionDetailsService,
    mockSignedUpDateService,
    mockSignUpConfirmation
  )

}
