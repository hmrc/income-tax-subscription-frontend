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

package controllers.agent.resolvers

import common.Constants.hmrcAsAgent
import config.MockConfig.mustBe
import config.featureswitch.FeatureSwitch.OptBackIn
import config.featureswitch.FeatureSwitching
import config.{AppConfig, MockConfig}
import controllers.ControllerSpec
import models.requests.agent.IdentifierRequest
import models.status.GetITSAStatus.*
import models.status.GetITSAStatusModel
import models.{Channel, CustomerLed, HmrcLedConfirmed, HmrcLedUnconfirmed, SessionData}
import org.apache.pekko.util.Timeout
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.SEE_OTHER
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation, session, status}
import services.GetITSAStatusService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.util.concurrent.TimeUnit
import scala.concurrent.Future

class AlreadySignedUpResolverSpec extends ControllerSpec
  with FeatureSwitching
  with BeforeAndAfterEach {

  override val appConfig: AppConfig = MockConfig

  private val mockGetITSAStatusService = mock[GetITSAStatusService]

  private val resolver = new AlreadySignedUpResolver(
    mockGetITSAStatusService,
    appConfig
  )(ec)

  private val sessionData = SessionData()
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val timeout: Timeout = Timeout(5, TimeUnit.SECONDS)

  private val baseRequest: FakeRequest[AnyContent] = FakeRequest()
  private implicit val request: IdentifierRequest[AnyContent] =
    IdentifierRequest(baseRequest, arn = "test-arn", sessionData = sessionData)

  private val notOptedOut = Seq(
    NoStatus,
    MTDMandated,
    MTDVoluntary,
    DigitallyExempt,
    Dormant,
    MTDExempt
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockGetITSAStatusService
    )
    enable(OptBackIn)
  }

  "resolve" should {
    "redirect to Client already signed up (HOA06C) when client is not opted out and OptBackIn feature switch is enabled" in {
      val channels = Seq[Option[Channel]](Some(CustomerLed), Some(HmrcLedConfirmed), None)
      channels.foreach { channel =>
        notOptedOut.foreach { itsaStatus =>
          reset(mockGetITSAStatusService)

          when(mockGetITSAStatusService.getITSAStatus(
            ArgumentMatchers.eq(sessionData)
          )(ArgumentMatchers.any())).thenReturn(
            Future.successful(GetITSAStatusModel(itsaStatus))
          )

          val result = resolver.resolve(sessionData, channel)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientAlreadySubscribedController.show.url)
          session(result).get(hmrcAsAgent) mustBe Some("true")

          verify(mockGetITSAStatusService, times(1)).getITSAStatus(
            ArgumentMatchers.eq(sessionData)
          )(ArgumentMatchers.any())
        }
      }
    }

    "redirect to Client already signed up (HOA06C) when OptBackIn feature switch is disabled" in {
      disable(OptBackIn)
      val channels = Seq[Option[Channel]](Some(CustomerLed), Some(HmrcLedConfirmed), None)
      channels.foreach { channel =>
        reset(mockGetITSAStatusService)

        val result = resolver.resolve(sessionData, channel)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientAlreadySubscribedController.show.url)
        session(result).get(hmrcAsAgent) mustBe Some("true")

        verify(mockGetITSAStatusService, times(0)).getITSAStatus(
          ArgumentMatchers.eq(sessionData)
        )(ArgumentMatchers.any())
      }
    }

    "throw an exception for Client migrated by HMRC (HOA06A) when channel is unconfirmed" in {
      val result = resolver.resolve(sessionData, Some(HmrcLedUnconfirmed))

      intercept[InternalServerException](await(result)).message mustBe "AlreadySignedUpResolver - Agent - HOA06A - Client migrated by HMRC"

      verify(mockGetITSAStatusService, times(0)).getITSAStatus(
        ArgumentMatchers.eq(sessionData)
      )(ArgumentMatchers.any())
    }


    "Go to Client opted out (HOA06B) when not triggered migration and ITSA status is Annual" in {
      val channels = Seq[Option[Channel]](Some(CustomerLed), Some(HmrcLedConfirmed), None)
      channels.foreach { channel =>
        reset(mockGetITSAStatusService)

        when(mockGetITSAStatusService.getITSAStatus(
          ArgumentMatchers.eq(sessionData)
        )(ArgumentMatchers.any())).thenReturn(
          Future.successful(GetITSAStatusModel(Annual))
        )

        val result = resolver.resolve(sessionData, channel)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.handoffs.routes.OptedOutController.show.url)
      }
    }
  }
}