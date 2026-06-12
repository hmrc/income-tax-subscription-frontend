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

package services.individual

import common.Constants
import common.Constants.GovernmentGateway.*
import config.AppConfig
import config.featureswitch.FeatureSwitch.DistributedKnownFactsPattern
import config.featureswitch.FeatureSwitching
import connectors.individual.httpparsers.UpsertEnrolmentResponseHttpParser.{KnownFactsFailure, KnownFactsSuccess}
import connectors.individual.subscription.mocks.MockTaxEnrolmentsConnector
import models.common.subscription.{EnrolmentKey, EnrolmentVerifiers}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import utilities.individual.TestConstants.*

import scala.concurrent.Future

class KnownFactsServiceSpec extends PlaySpec
  with ScalaFutures
  with MockTaxEnrolmentsConnector
  with FeatureSwitching {

  override val appConfig: AppConfig = mock[AppConfig]

  object TestKnownFactsService extends KnownFactsService(
    appConfig,
    mockTaxEnrolmentsConnector
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(DistributedKnownFactsPattern)
  }

  "addKnownFacts" should {

    def result: Future[Either[KnownFactsFailure, KnownFactsSuccess.type]] =
      TestKnownFactsService.addKnownFacts(testMTDID, testNino)

    val testEnrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> testMTDID)
    val testEnrolmentVerifiers = EnrolmentVerifiers(NINO -> testNino)

    "return a success from the EnrolmentStoreConnector" in {
      Seq(false, true).foreach { skipES6 =>
        reset(mockTaxEnrolmentsConnector)
        mockUpsertEnrolmentSuccess(testEnrolmentKey, testEnrolmentVerifiers)

        if (skipES6) {
          enable(DistributedKnownFactsPattern)
          info("[DistributedKnownFactsPattern] is enabled")
        } else {
          disable(DistributedKnownFactsPattern)
          info("[DistributedKnownFactsPattern] is disabled")
        }

        whenReady(result)(_ mustBe Right(KnownFactsSuccess))

        verify(mockTaxEnrolmentsConnector, times(if (skipES6) 0 else 1)).upsertEnrolment(
          ArgumentMatchers.eq(testEnrolmentKey),
          ArgumentMatchers.eq(testEnrolmentVerifiers)
        )(any())
      }
    }

    "return a failure from the EnrolmentStoreConnector" in {
      mockUpsertEnrolmentFailure(testEnrolmentKey, testEnrolmentVerifiers)

      whenReady(result)(_ mustBe Left(KnownFactsFailure(testErrorMessage)))
    }

    "pass through the exception if the EnrolmentStoreConnector fails" in {
      mockUpsertEnrolmentException(testEnrolmentKey, testEnrolmentVerifiers)

      whenReady(result.failed)(_ mustBe testException)
    }
  }

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

}
