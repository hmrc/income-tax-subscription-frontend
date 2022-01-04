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

package controllers.individual.claimenrolment.spsClaimEnrol

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.{ClaimEnrolment, SPSEnabled}
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.individual.mocks.MockApplicationCrypto
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.Future

class SPSHandoffForClaimEnrolControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with FeatureSwitching
  with MockApplicationCrypto {

  override def beforeEach(): Unit = {
    disable(SPSEnabled)
    disable(ClaimEnrolment)
    super.beforeEach()
  }

  object TestSPSHandoffForClaimEnrolController extends SPSHandoffForClaimEnrolController(
    mockAuditingService,
    mockAuthService,
    mockApplicationCrypto
  )(executionContext, appConfig, mockMessagesControllerComponents)

  override val controllerName: String = "SPSHandoffForClaimEnrolController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "spsHandoff" -> TestSPSHandoffForClaimEnrolController.redirectToSPS
  )

  "Calling the redirectToSPS action of the SPSController with an authorised user" when {
    implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = claimEnrolmentRequest

    def result: Future[Result] = TestSPSHandoffForClaimEnrolController.redirectToSPS(request)

    "feature switch SPSEnabled and claim enrolment both set to true" should {

      "Redirect to SPS" in {

        enable(SPSEnabled)
        enable(ClaimEnrolment)
        mockEncrypt()
        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get must be(s"${appConfig.preferencesFrontendRedirect}/paperless/choose/capture?returnUrl=encryptedValue&returnLinkText=encryptedValue&regime=encryptedValue")
      }
    }

    "feature switch SPSEnabled and claim enrolment both set to false" should {

      "throw a NotFoundException" in {

        mockEncrypt()
        val ex = intercept[NotFoundException](await(result))
        ex.getMessage mustBe ("[SPSHandoffForClaimEnrolController][redirectToSPS] - both SPS and claim enrolment feature switch are not enabled")

      }
    }

    "feature switch SPSEnabled set to false and claim enrolment set to true" should {

      "throw a NotFoundException" in {
        enable(ClaimEnrolment)
        mockEncrypt()
        val ex = intercept[NotFoundException](await(result))
        ex.getMessage mustBe ("[SPSHandoffForClaimEnrolController][redirectToSPS] - SPS feature switch is not enabled")

      }
    }

    "feature switch SPSEnabled set to true and claim enrolment set to false" should {

      "throw a NotFoundException" in {
        enable(SPSEnabled)
        mockEncrypt()
        val ex = intercept[NotFoundException](await(result))
        ex.getMessage mustBe ("[SPSHandoffForClaimEnrolController][redirectToSPS] - claim enrolment feature switch is not enabled")

      }
    }

  }

  authorisationTests()

}
