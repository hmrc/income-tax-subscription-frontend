/*
 * Copyright 2025 HM Revenue & Customs
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

import auth.MockAuth
import common.Constants
import config.AppConfig
import config.featureswitch.FeatureSwitch.{CompositeEnrolmentKey, DistributedKnownFactsPattern}
import config.featureswitch.FeatureSwitching
import connectors.individual.subscription.mocks.MockTaxEnrolmentsConnector
import models.common.subscription.{EmacEnrolmentRequest, EnrolmentKey, EnrolmentVerifiers}
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.HeaderCarrier
import org.mockito.Mockito.reset

import scala.concurrent.ExecutionContext.Implicits.global

class UpsertAndAllocateEnrolmentServiceSpec extends PlaySpec with MockTaxEnrolmentsConnector with MockAuth with FeatureSwitching {

  override val appConfig: AppConfig = mock[AppConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(CompositeEnrolmentKey)
    disable(DistributedKnownFactsPattern)
  }

  "upsertAndAllocate" must {
    "return an UpsertAndAllocateEnrolmentSuccess" when {
      "the enrolment was successfully upserted and allocated" in {
        Seq(false, true).foreach { useCompositeKey =>

          if (useCompositeKey) {
            enable(CompositeEnrolmentKey)
            info("[CompositeEnrolmentKey] is enabled")
          } else {
            disable(CompositeEnrolmentKey)
            info("[CompositeEnrolmentKey] is disabled")
          }

          val enrolmentKey = testEnrolmentKey(useCompositeKey)
          mockUpsertEnrolmentSuccess(enrolmentKey, testEnrolmentVerifiers)
          mockAuthorise(EmptyPredicate, credentials and groupIdentifier)(new~(Some(testCredentials), Some(testGroupId)))
          mockAllocateEnrolmentSuccess(testGroupId, enrolmentKey, testEnrolmentRequest)

          val result = TestUpsertAndAllocateEnrolmentService.upsertAndAllocate(testMTDITID, testNino, testUtr)

          await(result) mustBe Right(UpsertAndAllocateEnrolmentService.UpsertAndAllocateEnrolmentSuccess)
          enrolmentKey.asString.contains(testUtr) mustBe useCompositeKey
          
          verifyUpsertEnrolment(enrolmentKey, testEnrolmentVerifiers)
          verifyAllocateEnrolment(testGroupId, enrolmentKey, testEnrolmentRequest)
        }
      }
    }
    "return an UpsertAndAllocateEnrolmentFailure" which {
      "is an UpsertKnownFactsFailure" when {
        "there is a failure upserting known facts" in {
          mockUpsertEnrolmentFailure(testEnrolmentKey(false), testEnrolmentVerifiers)

          val result = TestUpsertAndAllocateEnrolmentService.upsertAndAllocate(testMTDITID, testNino, testUtr)

          await(result) mustBe Left(UpsertAndAllocateEnrolmentService.UpsertKnownFactsFailure)

          verifyUpsertEnrolment(testEnrolmentKey(false), testEnrolmentVerifiers)
          verifyAllocateEnrolment(testGroupId, testEnrolmentKey(false), testEnrolmentRequest, count = 0)
        }
      }
      "is a NoGroupIdFailure" when {
        "there is no group id returned from auth" in {
          mockUpsertEnrolmentSuccess(testEnrolmentKey(false), testEnrolmentVerifiers)
          mockAuthorise(EmptyPredicate, credentials and groupIdentifier)(new~(Some(testCredentials), None))

          val result = TestUpsertAndAllocateEnrolmentService.upsertAndAllocate(testMTDITID, testNino, testUtr)

          await(result) mustBe Left(UpsertAndAllocateEnrolmentService.NoGroupIdFailure)

          verifyUpsertEnrolment(testEnrolmentKey(false), testEnrolmentVerifiers)
          verifyAllocateEnrolment(testGroupId, testEnrolmentKey(false), testEnrolmentRequest, count = 0)
        }
      }
      "is a NoCredentialsFailure" when {
        "there is no credential returned from auth" in {
          mockUpsertEnrolmentSuccess(testEnrolmentKey(false), testEnrolmentVerifiers)
          mockAuthorise(EmptyPredicate, credentials and groupIdentifier)(new~(None, Some(testGroupId)))

          val result = TestUpsertAndAllocateEnrolmentService.upsertAndAllocate(testMTDITID, testNino, testUtr)

          await(result) mustBe Left(UpsertAndAllocateEnrolmentService.NoCredentialsFailure)

          verifyUpsertEnrolment(testEnrolmentKey(false), testEnrolmentVerifiers)
          verifyAllocateEnrolment(testGroupId, testEnrolmentKey(false), testEnrolmentRequest, count = 0)
        }
      }
      "is an AllocateEnrolmentFailure" when {
        "there is a failure allocating the enrolment" in {
          mockUpsertEnrolmentSuccess(testEnrolmentKey(false), testEnrolmentVerifiers)
          mockAuthorise(EmptyPredicate, credentials and groupIdentifier)(new~(Some(testCredentials), Some(testGroupId)))
          mockAllocateEnrolmentFailure(testGroupId, testEnrolmentKey(false), testEnrolmentRequest)

          val result = TestUpsertAndAllocateEnrolmentService.upsertAndAllocate(testMTDITID, testNino, testUtr)

          await(result) mustBe Left(UpsertAndAllocateEnrolmentService.AllocateEnrolmentFailure)

          verifyUpsertEnrolment(testEnrolmentKey(false), testEnrolmentVerifiers)
          verifyAllocateEnrolment(testGroupId, testEnrolmentKey(false), testEnrolmentRequest)
        }
      }
    }

    "upsertEnrolment" must {
      "Skip connector call if FS is on" in {
        Seq(false, true).foreach { skipES6 =>
          val key = testEnrolmentKey(false)

          reset(mockTaxEnrolmentsConnector)
          mockUpsertEnrolmentSuccess(key, testEnrolmentVerifiers)

          if (skipES6) {
            enable(DistributedKnownFactsPattern)
            info("[DistributedKnownFactsPattern] is enabled")
          } else {
            disable(DistributedKnownFactsPattern)
            info("[DistributedKnownFactsPattern] is disabled")
          }

          await(TestUpsertAndAllocateEnrolmentService.upsertEnrolment(key, testNino))

          verifyUpsertEnrolment(key, testEnrolmentVerifiers, if (skipES6) 0 else 1)
        }
      }
    }
  }

  object TestUpsertAndAllocateEnrolmentService extends UpsertAndAllocateEnrolmentService(
    mockTaxEnrolmentsConnector, appConfig, mockAuth
  )

  lazy val testMTDITID: String = "test-mtditid"
  lazy val testNino: String = "test-nino"
  lazy val testUtr = "test_utr"
  
  def testEnrolmentKey(useCompositeKey: Boolean): EnrolmentKey = {
    val utrId = if (useCompositeKey) Seq("UTR" -> testUtr) else Seq.empty
    EnrolmentKey(Constants.mtdItsaEnrolmentName, Seq(Constants.mtdItsaEnrolmentIdentifierKey -> testMTDITID) ++ utrId:_*)
  }

  lazy val testEnrolmentVerifiers: EnrolmentVerifiers = EnrolmentVerifiers(Constants.ninoEnrolmentIdentifierKey -> testNino)
  lazy val testCredentialId: String = "test-cred-id"
  lazy val testEnrolmentRequest: EmacEnrolmentRequest = EmacEnrolmentRequest(testCredentialId, testNino)
  lazy val testCredentials: Credentials = Credentials(testCredentialId, "")
  lazy val testGroupId: String = "test-group-id"

  lazy implicit val hc: HeaderCarrier = HeaderCarrier()

}
