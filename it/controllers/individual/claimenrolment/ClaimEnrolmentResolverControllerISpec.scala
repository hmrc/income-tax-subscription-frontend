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

package controllers.individual.claimenrolment

import config.featureswitch.FeatureSwitch.{ClaimEnrolment, SPSEnabled}
import config.featureswitch.FeatureSwitching
import controllers.Assets.{CREATED, INTERNAL_SERVER_ERROR, NO_CONTENT, SEE_OTHER}
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.testMTDITEnrolmentKey
import helpers.servicemocks.AuditStub.verifyAudit
import helpers.servicemocks.{AuthStub, EnrolmentStoreProxyStub, SubscriptionStub, TaxEnrolmentsStub}
import helpers.{ComponentSpecBase, SessionCookieCrumbler}
import play.api.http.Status.{NOT_FOUND, OK}

class ClaimEnrolmentResolverControllerISpec extends ComponentSpecBase with FeatureSwitching with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    disable(ClaimEnrolment)
    disable(SPSEnabled)
    super.beforeEach()
  }

  "GET /claim-enrolment/resolve" when {
    "the claim enrolment feature switch is disabled" should {
      "return a NotFound status" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /claim-enrolment/resolve is called")
        val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()
        Then("Should return a OK with the AddMTDITOverview page")
        res should have(
          httpStatus(NOT_FOUND),
          pageTitle("Page not found - 404")
        )
      }
    }
    "the claim enrolment feature switch is enabled" when {
      "all calls are successful and an auditing has been sent" when {
        "the SPS feature switch is enabled" should {
          "redirect the user to SPS" in {
            enable(ClaimEnrolment)
            enable(SPSEnabled)

            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            SubscriptionStub.stubGetSubscriptionFound()
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testMTDITEnrolmentKey)(NO_CONTENT)
            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, CREATED)

            When("GET /claim-enrolment/resolve is called")
            val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

            verifyAudit()
            Then("Should return a SEE_OTHER with a redirect location of the SPS beginning page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(claimEnrolSpsHandoffRouteURI)
            )
          }
        }

        "the SPS feature switch is disabled" should {
          "redirect the user to the claim enrolment confirmation" in {
            enable(ClaimEnrolment)

            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            SubscriptionStub.stubGetSubscriptionFound()
            EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testMTDITEnrolmentKey)(NO_CONTENT)
            TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
            TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, CREATED)

            When("GET /claim-enrolment/resolve is called")
            val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

            verifyAudit()
            Then("Should return a SEE_OTHER with a redirect location of the claim enrolment confirmation page")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(claimEnrolmentConfirmationURI)
            )
          }
        }
      }
      "redirect the user to the claim enrolment not subscribed" when {
        "the user is not signed up to mtd income tax" in {
          enable(ClaimEnrolment)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetNoSubscription()

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return a SEE_OTHER with a redirect location of the claim enrolment not subscribed page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(notSubscribedURI)
          )
        }
      }
      "redirect the user to the claim enrolment already signed up" when {
        "the enrolment is already allocated" in {
          enable(ClaimEnrolment)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFound()
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testMTDITEnrolmentKey)(OK)

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return a SEE_OTHER with a redirect location of the claim enrolment already signed up page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(claimEnrolmentAlreadySignedUpURI)
          )
        }
      }
      "return an InternalServerError" when {
        "nino could not be retrieved from the user's cred" in {
          enable(ClaimEnrolment)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthNoNino()

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "an unexpected status is returned when making a call to check the user has a subscription" in {
          enable(ClaimEnrolment)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFail()

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "the response payload could not be parsed when checking the allocation status of the enrolment" in {
          enable(ClaimEnrolment)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFound()
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentJsError(testMTDITEnrolmentKey)

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "an unexpected status is returned when making a call to check the allocation status of the enrolment" in {
          enable(ClaimEnrolment)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFound()
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testMTDITEnrolmentKey)(INTERNAL_SERVER_ERROR)

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "an unexpected status is returned when making a call to upsert the known facts" in {
          enable(ClaimEnrolment)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFound()
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testMTDITEnrolmentKey)(NO_CONTENT)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, INTERNAL_SERVER_ERROR)

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "an unexpected status is returned when making a call to allocate the enrolment" in {
          enable(ClaimEnrolment)

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFound()
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testMTDITEnrolmentKey)(NO_CONTENT)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
          TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, INTERNAL_SERVER_ERROR)

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }

}
