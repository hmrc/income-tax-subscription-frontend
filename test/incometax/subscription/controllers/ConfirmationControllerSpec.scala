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

package incometax.subscription.controllers

import java.time.LocalDateTime

import core.ITSASessionKeys
import core.audit.Logging
import core.config.featureswitch.FeatureSwitching
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import core.utils.TestModels
import org.jsoup.Jsoup
import org.scalatest.Matchers._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.Future

class ConfirmationControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching {

  object TestConfirmationController extends ConfirmationController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    app.injector.instanceOf[Logging],
    mockAuthService
  )

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
    "the user is not in the unauthorised agent journey state" should {
      "get the ID from keystore if the user is enrolled" in {
        mockAuthEnrolled()
        setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(incomeSource = None))
        val result: Future[Result] = TestConfirmationController.show(
          subscriptionRequest.addStartTime(startTime)
        )

        status(result) shouldBe OK

        await(result)
      }

      "return not found if the user is not enrolled" in {
        setupMockKeystore(fetchSubscriptionId = "testId")
        val result = TestConfirmationController.show(subscriptionRequest)

        intercept[NotFoundException](await(result)).message shouldBe "AuthPredicates.enrolledPredicate"
      }
    }
    "the user is in the unauthorised agent journey state" should {
      "return OK with the confirm subscription request view" in {
        mockAuthEnrolled()
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)
        val result: Future[Result] = TestConfirmationController.show(
          confirmAgentSubscriptionRequest
            .addStartTime(startTime)
        )

        status(result) shouldBe OK

        Jsoup.parse(contentAsString(result)).title shouldBe Messages("confirmation.unauthorised.title")

        await(result)
      }
    }
  }

  authorisationTests()

}
