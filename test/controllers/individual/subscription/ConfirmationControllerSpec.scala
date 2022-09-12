/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.individual.subscription

import common.Constants.ITSASessionKeys
import controllers.ControllerBaseSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks.{MockAccountingPeriodService, MockAuditingService, MockSubscriptionDetailsService, MockUserMatchingService}
import uk.gov.hmrc.http.NotFoundException
import utilities.TestModels.testSelectedTaxYearNext
import views.html.individual.incometax.subscription.SignUpComplete

import java.time.LocalDateTime
import scala.concurrent.Future

class ConfirmationControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAccountingPeriodService
  with MockUserMatchingService
  with MockAuditingService {

  val mockSignUpComplete: SignUpComplete = mock[SignUpComplete]

  override def beforeEach(): Unit = {
    reset(mockSignUpComplete)
    super.beforeEach()
  }

  object TestConfirmationController extends ConfirmationController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockSignUpComplete
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

  implicit class SessionUtil[T](fakeRequest: FakeRequest[T]) {
    def addStartTime(time: LocalDateTime): FakeRequest[T] = fakeRequest.withSession(
      (fakeRequest.session.data + (ITSASessionKeys.StartTime -> time.toString)).toSeq: _*
    )
  }

  "show" when {
    val startTime: LocalDateTime = LocalDateTime.now()
    "the user is in confirmation journey state" should {
      "get the ID from Subscription Details  if the user is enrolled" in {
        mockAuthEnrolled()
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearNext))

        when(mockSignUpComplete(ArgumentMatchers.eq(Some(testSelectedTaxYearNext.accountingYear)), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = TestConfirmationController.show(subscriptionRequest.addStartTime(startTime))

        status(result) mustBe OK
      }

      "submitted is not in session" should {
        "return a NotFoundException" in {
          val result = TestConfirmationController.show(subscriptionRequest)

          intercept[NotFoundException](await(result))
        }
      }

      "return not found if the user is not enrolled" in {
        mockFetchSubscriptionIdFromSubscriptionDetails(Some("testId"))
        val result = TestConfirmationController.show(subscriptionRequest)

        intercept[NotFoundException](await(result)).message mustBe "AuthPredicates.enrolledPredicate"
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
