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

package controllers.individual.actions

import auth.MockAuth
import common.Constants
import common.Constants.ITSASessionKeys
import config.MockConfig
import models.SessionData
import models.audits.IVHandoffAuditing.IVHandoffAuditModel
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsString
import play.api.mvc.{BodyParsers, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout, redirectLocation, status}
import services.mocks.{MockAuditingService, MockSessionDataService}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.{EmptyRetrieval, ~}
import uk.gov.hmrc.http.InternalServerException

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BasicIdentifierActionSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockAuth {

  val basicIdentifierAction: BasicIdentifierAction = new BasicIdentifierAction(
    authConnector = mockAuth,
    parser = app.injector.instanceOf[BodyParsers.Default],
  )(MockConfig)

  "BasicIdentifierAction" when {
    "authorising" must {
      "execute the request" in {
        mockAuthorise[Unit](EmptyPredicate, EmptyRetrieval)(())

        val result: Future[Result] = basicIdentifierAction { request =>
          Results.Ok
        }(FakeRequest())

        status(result) mustBe OK
      }
    }
    "authorisation throws an AuthorisationException" must {
      "redirect the user to login" in {
        mockAuthoriseFailure[Unit](EmptyPredicate, EmptyRetrieval) {
          BearerTokenExpired()
        }

        val result: Future[Result] = basicIdentifierAction { _ =>
          Results.Ok
        }(FakeRequest(method = "GET", path = "/test-url"))

        status(result) mustBe SEE_OTHER
        val url = controllers.individual.matching.routes.HomeController.index.url.replace("/", "%2F")
        redirectLocation(result) mustBe Some(s"/gg/sign-in?continue=$url&origin=${MockConfig.appName}")
      }
    }
  }

  private def random() =
    UUID.randomUUID().toString
}
