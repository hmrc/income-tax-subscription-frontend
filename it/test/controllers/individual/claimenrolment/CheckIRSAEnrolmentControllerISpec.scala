/*
 * Copyright 2026 HM Revenue & Customs
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

import common.Constants.ITSASessionKeys
import connectors.stubs.{SessionDataConnectorStub, UsersGroupsSearchStub}
import helpers.IntegrationTestConstants.*
import helpers.servicemocks.{AuthStub, EnrolmentStoreProxyStub}
import helpers.{ComponentSpecBase, SessionCookieCrumbler}
import models.{No, Yes}
import play.api.http.Status.*
import play.api.libs.json.{JsString, Json}

class CheckIRSAEnrolmentControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"

  "GET /claim-enrolment/use-self-assessment-details" should {
    "redirect the user to log in" when {
      "the user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.claimEnrolmentUseSelfAssessment()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn())
        )
      }
    }

    "redirect to the claim enrolment overview page" when {
      "it is identified that there are no other credentials with the IR-SA enrolment" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(NO_CONTENT)

        val res = IncomeTaxSubscriptionFrontend.claimEnrolmentUseSelfAssessment()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.AddMTDITOverviewController.show().url)
        )
      }
      "it is identified that the current credential has the IR-SA enrolment" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, Json.arr(testCredId))

        val res = IncomeTaxSubscriptionFrontend.claimEnrolmentUseSelfAssessment()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.AddMTDITOverviewController.show().url)
        )
      }
      "there was a problem fetching the credentials assigned to an IR-SA enrolment" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(INTERNAL_SERVER_ERROR)

        val res = IncomeTaxSubscriptionFrontend.claimEnrolmentUseSelfAssessment()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.AddMTDITOverviewController.show().url)
        )
      }
      "there was a problem fetching the user details for the SA credential" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, Json.obj("principalUserIds" -> Json.arr(testCredentialId2)))
        UsersGroupsSearchStub.stubGetUserDetailsByCredId(testCredentialId2)(INTERNAL_SERVER_ERROR)

        val res = IncomeTaxSubscriptionFrontend.claimEnrolmentUseSelfAssessment()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.AddMTDITOverviewController.show().url)
        )
      }
      "there was a problem fetching the user details for the current credential" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, Json.obj("principalUserIds" -> Json.arr(testCredentialId2)))
        UsersGroupsSearchStub.stubGetUserDetailsByCredId(testCredentialId2)(NON_AUTHORITATIVE_INFORMATION, Json.obj(
          "identityProviderType" -> "ONE_LOGIN",
          "email" -> "test@email.com"
        ))
        UsersGroupsSearchStub.stubGetUserDetailsByCredId(testCredId)(INTERNAL_SERVER_ERROR)

        val res = IncomeTaxSubscriptionFrontend.claimEnrolmentUseSelfAssessment()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.AddMTDITOverviewController.show().url)
        )
      }
    }

    "display the page" when {
      "a different credential was found to have the IR-SA enrolment and user details were retrieved" when {
        "the current credential is GG and the SA credential is One Login" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))
          EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, Json.obj("principalUserIds" -> Json.arr(testCredentialId2)))
          UsersGroupsSearchStub.stubGetUserDetailsByCredId(testCredentialId2)(NON_AUTHORITATIVE_INFORMATION, Json.obj(
            "identityProviderType" -> "SCP",
            "obfuscatedUserId" -> "*****678"
          ))
          UsersGroupsSearchStub.stubGetUserDetailsByCredId(testCredId)(NON_AUTHORITATIVE_INFORMATION, Json.obj(
            "identityProviderType" -> "ONE_LOGIN",
            "email" -> "test@email.com"
          ))

          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentUseSelfAssessment()

          res must have(
            httpStatus(OK),
            pageTitle(s"${messages("irsa-cred.title")}$serviceNameGovUk")
          )
        }
        "the current credential is One Login and the SA credential is GG" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.UTR -> JsString(testUtr)
          ))
          EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, Json.obj("principalUserIds" -> Json.arr(testCredentialId2)))
          UsersGroupsSearchStub.stubGetUserDetailsByCredId(testCredentialId2)(NON_AUTHORITATIVE_INFORMATION, Json.obj(
            "identityProviderType" -> "ONE_LOGIN",
            "email" -> "test@email.com"
          ))
          UsersGroupsSearchStub.stubGetUserDetailsByCredId(testCredId)(NON_AUTHORITATIVE_INFORMATION, Json.obj(
            "identityProviderType" -> "SCP",
            "obfuscatedUserId" -> "*****678"
          ))

          val res = IncomeTaxSubscriptionFrontend.claimEnrolmentUseSelfAssessment()

          res must have(
            httpStatus(OK),
            pageTitle(s"${messages("irsa-cred.title")}$serviceNameGovUk")
          )
        }
      }
    }
  }

  "POST /claim-enrolment/use-self-assessment-details" should {
    "redirect the user to log in" when {
      "the user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.submitClaimEnrolmentUseSelfAssessment(request = Some(No))

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn())
        )
      }
    }

    "display the page" when {
      "no input was submitted" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(OK, Json.obj("principalUserIds" -> Json.arr(testCredentialId2)))
        UsersGroupsSearchStub.stubGetUserDetailsByCredId(testCredentialId2)(NON_AUTHORITATIVE_INFORMATION, Json.obj(
          "identityProviderType" -> "ONE_LOGIN",
          "email" -> "test@email.com"
        ))
        UsersGroupsSearchStub.stubGetUserDetailsByCredId(testCredId)(NON_AUTHORITATIVE_INFORMATION, Json.obj(
          "identityProviderType" -> "SCP",
          "obfuscatedUserId" -> "*****678"
        ))

        val res = IncomeTaxSubscriptionFrontend.submitClaimEnrolmentUseSelfAssessment(request = None)

        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle(s"Error: ${messages("irsa-cred.title")}$serviceNameGovUk")
        )
      }
    }
    "redirect to the claim enrolment overview page" when {
      "there was a problem fetching information for the page when an error occurs" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        EnrolmentStoreProxyStub.stubGetUserIds(testUtr)(INTERNAL_SERVER_ERROR)

        val res = IncomeTaxSubscriptionFrontend.submitClaimEnrolmentUseSelfAssessment(request = None)

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.AddMTDITOverviewController.show().url)
        )
      }
      "the user selects to not change accounts" in {
        AuthStub.stubAuthSuccess()

        val res = IncomeTaxSubscriptionFrontend.submitClaimEnrolmentUseSelfAssessment(request = Some(No))

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.AddMTDITOverviewController.show().url)
        )
      }
    }
    "redirect to the sign out route" when {
      "the user selects to change accounts" in {
        AuthStub.stubAuthSuccess()

        val res = IncomeTaxSubscriptionFrontend.submitClaimEnrolmentUseSelfAssessment(request = Some(Yes))

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(appConfig.ggSignOutUrl())
        )
      }
    }
  }

}
