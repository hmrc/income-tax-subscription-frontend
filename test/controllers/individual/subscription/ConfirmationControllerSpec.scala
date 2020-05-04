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

package controllers.individual.subscription

import java.time.LocalDateTime

import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import org.scalatest.Matchers._
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.individual.mocks.MockKeystoreService
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import utilities.{ITSASessionKeys, TestModels}

import scala.concurrent.Future

class ConfirmationControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching {

  object TestConfirmationController extends ConfirmationController(
    mockAuthService,
    MockKeystoreService
  )

  implicit val request: Request[_] = FakeRequest()

  override val controllerName: String = "ConfirmationControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestConfirmationController.show
  )

  implicit class SessionUtil[T](fakeRequest: FakeRequest[T]) {
    def addStartTime(time: LocalDateTime): FakeRequest[T] = fakeRequest.withSession(
      (fakeRequest.session.data + (ITSASessionKeys.StartTime -> time.toString)).toSeq: _*
    )
  }

  "ConfirmationController" when {
    val startTime: LocalDateTime = LocalDateTime.now()
    "the user is in confirmation journey state" should {
      "get the ID from keystore if the user is enrolled" in {
        mockAuthEnrolled()
        mockFetchAllFromKeyStore(TestModels.testCacheMap)
        val result: Future[Result] = TestConfirmationController.show(
          subscriptionRequest.addStartTime(startTime)
        )

        status(result) shouldBe OK

        Jsoup.parse(contentAsString(result)).title shouldBe Messages("sign-up-complete.title")

      }

      "fail if no income source is stored" in {
        mockAuthEnrolled()
        mockFetchAllFromKeyStore(TestModels.emptyCacheMap)
        val result: Future[Result] = TestConfirmationController.show(
          subscriptionRequest.addStartTime(startTime)
        )

        intercept[InternalServerException](await(result))
      }

      "return not found if the user is not enrolled" in {
        mockFetchSubscriptionIdFromKeyStore("testId")
        val result = TestConfirmationController.show(subscriptionRequest)

        intercept[NotFoundException](await(result)).message shouldBe "AuthPredicates.enrolledPredicate"
      }
    }

  }

  authorisationTests()

}
