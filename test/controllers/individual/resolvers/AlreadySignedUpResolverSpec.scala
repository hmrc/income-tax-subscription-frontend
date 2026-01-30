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

package controllers.individual.resolvers

import config.MockConfig.mustBe
import controllers.ControllerSpec
import models.status.GetITSAStatus.{Annual, NoStatus}
import models.status.GetITSAStatusModel
import models.{CustomerLed, HmrcLedConfirmed, HmrcLedUnconfirmed, SessionData}
import org.apache.pekko.util.Timeout
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{redirectLocation, status}
import services.GetITSAStatusService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.concurrent.TimeUnit
import scala.concurrent.Future

class AlreadySignedUpResolverSpec extends ControllerSpec {

  private val mockService = mock[GetITSAStatusService]

  private val resolver = new AlreadySignedUpResolver(mockService)

  private val sessionData = SessionData()
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val timeout: Timeout = Timeout(5, TimeUnit.SECONDS)

  "resolve" should {
    "Go to the already signed up page when there is no channel" in {
      Seq(false, true).foreach { hasEnrolment =>
        val result = resolver.resolve(sessionData, hasEnrolment, None)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show.url)
      }
    }

    "Go to the already signed up page when user " +
      "has signed-up manually or has been signed-up by HMRC and confirmed income sources " +
      "and has enrolled" +
      "amd has not opted-out" in {
      Seq(CustomerLed, HmrcLedConfirmed).foreach { channel =>
        when(mockService.getITSAStatus(
          ArgumentMatchers.eq(sessionData)
        )(ArgumentMatchers.any())).thenReturn(
          Future.successful(GetITSAStatusModel(NoStatus))
        )
        val result = resolver.resolve(sessionData, true, Some(channel))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show.url)
      }
    }

    "Go to the claim enrollment when user " +
      "has signed-up manually or has been signed-up by HMRC and confirmed income sources " +
      "and has not enrolled" in {
      Seq(CustomerLed, HmrcLedConfirmed).foreach { channel =>
        val result = resolver.resolve(sessionData, false, Some(channel))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.claimenrolment.routes.ClaimEnrolmentAlreadySignedUpController.show.url)
      }
    }

    "Go to the check income sources page when user " +
      "has signed-up by HMRC and not confirmed income sources" in {
      val result = resolver.resolve(sessionData, true, Some(HmrcLedUnconfirmed))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.handoffs.routes.CheckIncomeSourcesController.show.url)
    }

    "Go to the opted-out page when user " +
      "has signed-up manually or has been signed-up by HMRC and confirmed income sources " +
      "and has enrolled" +
      "amd has opted-out" in {
      Seq(CustomerLed, HmrcLedConfirmed).foreach { channel =>
        when(mockService.getITSAStatus(
          ArgumentMatchers.eq(sessionData)
        )(ArgumentMatchers.any())).thenReturn(
          Future.successful(GetITSAStatusModel(Annual))
        )
        val result = resolver.resolve(sessionData, true, Some(channel))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.handoffs.routes.OptedOutController.show.url)
      }
    }
  }
}
