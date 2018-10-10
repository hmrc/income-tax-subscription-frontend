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

package agent.controllers

import agent.audit.Logging
import agent.services.mocks.MockKeystoreService
import org.jsoup.Jsoup
import org.scalatest.Matchers._
import play.api.Play
import play.api.i18n.{Lang, Messages}
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Cookie}
import play.api.test.Helpers._
import play.i18n.MessagesApi
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.language.LanguageUtils._
import agent.utils.TestModels._

class ConfirmationControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService {

  object TestConfirmationController extends ConfirmationController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    app.injector.instanceOf[Logging]
  )

  override val controllerName: String = "ConfirmationControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showConfirmation" -> TestConfirmationController.show
  )

  "ConfirmationController" when {

    "submitted is not in session" should {
      "return a NotFoundException" in {
        val result = TestConfirmationController.show(subscriptionRequest)

        intercept[NotFoundException](await(result))
      }
    }

    "submitted is not in session but user is also marked as unauthorised" should {
      "return SEE OTHER" in {
        setupMockKeystore(fetchAll = testCacheMap)

        val result = TestConfirmationController.show(
          subscriptionRequest
            .addingToSession(ITSASessionKeys.MTDITID -> "any",
              ITSASessionKeys.UnauthorisedAgentKey -> true.toString)
        )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(agent.controllers.routes.HomeController.index().url)
      }
    }

    "submitted is in session" should {
      "return OK" in {
        setupMockKeystore(fetchAll = testCacheMap)

        val result = TestConfirmationController.show(subscriptionRequest.addingToSession(ITSASessionKeys.MTDITID -> "any"))
        status(result) shouldBe OK
      }
    }

    "submitted is in session and welsh content applies" should {
      "return OK" in {
        setupMockKeystore(fetchAll = testCacheMap)

        val result = TestConfirmationController.show(
          subscriptionRequest
            .addingToSession(ITSASessionKeys.MTDITID -> "any")
            .withCookies(Cookie(Play.langCookieName(applicationMessagesApi), WelshLangCode))
        )
        status(result) shouldBe OK

        Jsoup.parse(contentAsString(result)).title shouldBe Messages("agent.sign-up-complete.title")(applicationMessages(Welsh, app))
      }
    }

    "submitted is in session and new Confirmation content applies" should {
      "return OK" in {
        setupMockKeystore(fetchAll = testCacheMap)

        val result = TestConfirmationController.show(
          subscriptionRequest
            .addingToSession(ITSASessionKeys.MTDITID -> "any")
        )
        status(result) shouldBe OK

        Jsoup.parse(contentAsString(result)).title shouldBe Messages("agent.sign-up-complete.title")
      }
    }

  }

  authorisationTests()

}
