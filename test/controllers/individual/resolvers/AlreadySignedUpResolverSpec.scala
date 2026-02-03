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

import common.Constants.{mtdItsaEnrolmentIdentifierKey, mtdItsaEnrolmentName}
import config.MockConfig.mustBe
import config.featureswitch.FeatureSwitch.OptBackIn
import config.featureswitch.FeatureSwitching
import config.{AppConfig, MockConfig}
import controllers.ControllerSpec
import models.common.subscription.EnrolmentKey
import models.status.GetITSAStatus.*
import models.status.GetITSAStatusModel
import models.{CustomerLed, HmrcLedConfirmed, HmrcLedUnconfirmed, SessionData}
import org.apache.pekko.util.Timeout
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{redirectLocation, status}
import services.GetITSAStatusService
import services.agent.CheckEnrolmentAllocationService.{EnrolmentAlreadyAllocated, EnrolmentNotAllocated}
import services.mocks.MockCheckEnrolmentAllocationService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.concurrent.TimeUnit
import scala.concurrent.Future

class AlreadySignedUpResolverSpec extends ControllerSpec
  with MockCheckEnrolmentAllocationService
  with FeatureSwitching {

  override val appConfig: AppConfig = MockConfig

  private val mockGetITSAStatusService = mock[GetITSAStatusService]

  private val resolver = new AlreadySignedUpResolver(
    mockCheckEnrolmentAllocationService,
    mockGetITSAStatusService,
    appConfig
  )

  private val sessionData = SessionData()
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val timeout: Timeout = Timeout(5, TimeUnit.SECONDS)

  private val notOptedOut = Seq(
    NoStatus,
    MTDMandated,
    MTDVoluntary,
    DigitallyExempt,
    Dormant,
    MTDExempt
  )

  private val testMTDITID: String = "XAIT0000000001"
  private val enrolmentKey = EnrolmentKey(mtdItsaEnrolmentName, mtdItsaEnrolmentIdentifierKey -> testMTDITID)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGetITSAStatusService)
    enable(OptBackIn)
  }

  "resolve" should {
    "Go to the already signed up page when user " +
      "has signed-up manually or has been signed-up by HMRC and confirmed income sources " +
      "and has enrolled" +
      "amd has not opted-out" +
      "and OptBackIn FS in enabled" in {
      enable(OptBackIn)
      Seq(CustomerLed, HmrcLedConfirmed).foreach { channel =>
        notOptedOut.foreach { ITSAStatus =>
          reset(mockGetITSAStatusService)
          mockGetGroupIdForEnrolment(enrolmentKey)(Left(EnrolmentAlreadyAllocated(testMTDITID)))
          when(mockGetITSAStatusService.getITSAStatus(
            ArgumentMatchers.eq(sessionData)
          )(ArgumentMatchers.any())).thenReturn(
            Future.successful(GetITSAStatusModel(ITSAStatus))
          )
          val result = resolver.resolve(sessionData, testMTDITID, Some(channel))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.matching.routes.AlreadyEnrolledController.show.url)
          verify(mockGetITSAStatusService, times(1)).getITSAStatus(
            ArgumentMatchers.eq(sessionData)
          )(ArgumentMatchers.any())
        }
      }
    }

    "Go to the already signed up page when user " +
      "has signed-up manually or has been signed-up by HMRC and confirmed income sources " +
      "and has enrolled" +
      "and OptBackIn FS in disabled" in {
      disable(OptBackIn)
      Seq(CustomerLed, HmrcLedConfirmed).foreach { channel =>
        notOptedOut.foreach { ITSAStatus =>
          reset(mockGetITSAStatusService)
          mockGetGroupIdForEnrolment(enrolmentKey)(Left(EnrolmentAlreadyAllocated(testMTDITID)))
          val result = resolver.resolve(sessionData, testMTDITID, Some(channel))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.matching.routes.AlreadyEnrolledController.show.url)
          verify(mockGetITSAStatusService, times(0)).getITSAStatus(
            ArgumentMatchers.eq(sessionData)
          )(ArgumentMatchers.any())
        }
      }
    }

    "Go to the claim enrollment when user " +
      "has signed-up manually or has been signed-up by HMRC and confirmed income sources " +
      "and has not enrolled" in {
      Seq(CustomerLed, HmrcLedConfirmed).foreach { channel =>
        mockGetGroupIdForEnrolment(enrolmentKey)(Right(EnrolmentNotAllocated))
        val result = resolver.resolve(sessionData, testMTDITID, Some(channel))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show.url)
      }
    }

    "Go to the check income sources page when user " +
      "has signed-up by HMRC and not confirmed income sources" in {
      mockGetGroupIdForEnrolment(enrolmentKey)(Left(EnrolmentAlreadyAllocated(testMTDITID)))
      val result = resolver.resolve(sessionData, testMTDITID, Some(HmrcLedUnconfirmed))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.handoffs.routes.CheckIncomeSourcesController.show.url)
    }

    "Go to the opted-out page when user " +
      "has signed-up manually or has been signed-up by HMRC and confirmed income sources " +
      "and has enrolled" +
      "amd has opted-out" in {
      Seq(CustomerLed, HmrcLedConfirmed).foreach { channel =>
        mockGetGroupIdForEnrolment(enrolmentKey)(Left(EnrolmentAlreadyAllocated(testMTDITID)))
        when(mockGetITSAStatusService.getITSAStatus(
          ArgumentMatchers.eq(sessionData)
        )(ArgumentMatchers.any())).thenReturn(
          Future.successful(GetITSAStatusModel(Annual))
        )
        val result = resolver.resolve(sessionData, testMTDITID, Some(channel))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.handoffs.routes.OptedOutController.show.url)
      }
    }
  }
}
