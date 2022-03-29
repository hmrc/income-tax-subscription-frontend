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

import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.testMTDITEnrolmentKey
import helpers.servicemocks.AuditStub.verifyAudit
import helpers.servicemocks.{AuthStub, EnrolmentStoreProxyStub, SubscriptionStub, TaxEnrolmentsStub}
import helpers.{ComponentSpecBase, SessionCookieCrumbler}
import play.api.http.Status._

class ClaimEnrolmentResolverControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {



  "GET /claim-enrolment/resolve" when {
      "all calls are successful and an auditing has been sent" in {

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
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(claimEnrolSpsHandoffRouteURI)
            )
          }
        }

      "redirect the user to the claim enrolment not subscribed" when {
        "the user is not signed up to mtd income tax" in {

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetNoSubscription()

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return a SEE_OTHER with a redirect location of the claim enrolment not subscribed page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(notSubscribedURI)
          )
        }
      }
      "redirect the user to the claim enrolment already signed up" when {
        "the enrolment is already allocated" in {

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFound()
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testMTDITEnrolmentKey)(OK)

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return a SEE_OTHER with a redirect location of the claim enrolment already signed up page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(claimEnrolmentAlreadySignedUpURI)
          )
        }
      }
      "return an InternalServerError" when {
        "nino could not be retrieved from the user's cred" in {

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthNoNino()

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "an unexpected status is returned when making a call to check the user has a subscription" in {

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFail()

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "the response payload could not be parsed when checking the allocation status of the enrolment" in {

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFound()
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentJsError(testMTDITEnrolmentKey)

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "an unexpected status is returned when making a call to check the allocation status of the enrolment" in {

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFound()
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testMTDITEnrolmentKey)(INTERNAL_SERVER_ERROR)

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "an unexpected status is returned when making a call to upsert the known facts" in {

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFound()
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testMTDITEnrolmentKey)(NO_CONTENT)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, INTERNAL_SERVER_ERROR)

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "an unexpected status is returned when making a call to allocate the enrolment" in {

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          SubscriptionStub.stubGetSubscriptionFound()
          EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus(testMTDITEnrolmentKey)(NO_CONTENT)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testMTDITEnrolmentKey.asString, NO_CONTENT)
          TaxEnrolmentsStub.stubAllocateEnrolmentResult(testGroupId, testMTDITEnrolmentKey.asString, INTERNAL_SERVER_ERROR)

          When("GET /claim-enrolment/resolve is called")
          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentResolver()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }


}
