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

package controllers.individual.business

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import models.common.TimestampModel
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.Configuration
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, status}
import play.twirl.api.HtmlFormat
import services.mocks.MockSubscriptionDetailsService
import utilities.CacheExpiryDateProvider
import views.html.individual.incometax.business.ProgressSaved
import java.time.LocalDateTime

import scala.concurrent.Future

class ProgressSavedControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with FeatureSwitching {
  override val controllerName: String = "ProgressSavedController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()
  implicit lazy val config:Configuration = app.injector.instanceOf[Configuration]

  private val testTimestamp = TimestampModel(
    LocalDateTime.of(1970, 1, 1, 1, 0, 0, 0)
  )

  "signInUrl" should {
    "return the sign in url" in withController { (controller, _) =>
      controller.signInUrl mustBe "/bas-gateway/sign-in"
    }
  }

  "Show" should {
    "return status OK with the progress saved page" in withController { (controller, _) =>
      enable(SaveAndRetrieve)

      mockFetchLastUpdatedTimestamp(Some(testTimestamp))

      val result: Future[Result] = await(controller.show()(subscriptionRequest))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }

    "return render the correct expiry date" in withController { (controller, mockedView) =>
      enable(SaveAndRetrieve)
      mockFetchLastUpdatedTimestamp(Some(testTimestamp))

      val result: Future[Result] = await(controller.show()(subscriptionRequest))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)


      verify(mockedView).apply(meq("Monday, 20 October 2021"), any())(any(), any(), any())
    }

    "throw an exception if the last updated timestamp cannot be retrieve" in withController { (controller, _) =>
      enable(SaveAndRetrieve)
      mockFetchLastUpdatedTimestamp(None)

      val result: Future[Result] = await(controller.show()(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
    }

    "throw an exception if feature not enabled" in withController { (controller, _) =>
      disable(SaveAndRetrieve)

      val result: Future[Result] = await(controller.show()(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.NotFoundException]
    }
  }

  private def withController(testCode: (ProgressSavedController, ProgressSaved)=> Any): Unit = {
    val progressSavedView = mock[ProgressSaved]

    when(progressSavedView(meq("Monday, 20 October 2021"), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val cacheExpiryDateProvider = mock[CacheExpiryDateProvider]

    when(cacheExpiryDateProvider.expiryDateOf(any())(any()))
      .thenReturn("Monday, 20 October 2021")

    val controller = new ProgressSavedController(
      progressSavedView,
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      cacheExpiryDateProvider
    )

    testCode(controller, progressSavedView)
  }
}
