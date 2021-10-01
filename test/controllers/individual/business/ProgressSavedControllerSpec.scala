/*
 * Copyright 2021 HM Revenue & Customs
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

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import controllers.Assets.OK
import controllers.ControllerBaseSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.Configuration
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, status}
import play.twirl.api.HtmlFormat
import views.html.individual.incometax.business.ProgressSaved

import scala.concurrent.Future

class ProgressSavedControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with FeatureSwitching {
  override val controllerName: String = "ProgressSavedController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()
  implicit lazy val config:Configuration = app.injector.instanceOf[Configuration]

  "signInUrl" should {
    "return the sign in url" in withController { controller =>
      controller.signInUrl mustBe "/bas-gateway/sign-in"
    }
  }

  "Show" should {
    "return status OK with the progress saved page" in withController { controller =>
      enable(SaveAndRetrieve)

      val result: Future[Result] = await(controller.show()(subscriptionRequest))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)

    }

    "throw an exception if feature not enabled" in withController { controller =>
      disable(SaveAndRetrieve)

      val result: Future[Result] = await(controller.show()(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.NotFoundException]
    }
  }

  private def withController(testCode: ProgressSavedController => Any): Unit = {
    val progressSavedView = mock[ProgressSaved]

    when(progressSavedView(any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new ProgressSavedController(
      progressSavedView,
      mockAuditingService,
      mockAuthService,
    )

    testCode(controller)
  }
}
