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

package controllers.agent.matching

import common.Constants.ITSASessionKeys
import com.github.tomakehurst.wiremock.client.WireMock
import config.MockConfig.appConfig.ggLoginUrl
import config.featureswitch.FeatureSwitch.ThrottlingFeature
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import helpers.WiremockHelper
import helpers.servicemocks.PrePopStub
import helpers.IntegrationTestModels.testBusiness
import models.EligibilityStatus
import models.agent.JourneyStep.UserMatching
import models.common.SoleTraderBusinesses
import models.common.business.*
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.{MandationStatus, MandationStatusModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.{Format, JsBoolean, JsString, Json}
import services.AgentStartOfJourneyThrottle
import utilities.SubscriptionDataKeys
import utilities.agent.TestConstants.{testNino, testUtr}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.crypto.SymmetricCryptoFactory.aesGcmCryptoFromConfig
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption

class ConfirmedClientResolverControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(ThrottlingFeature)
    SessionDataConnectorStub.stubGetAllSessionData(Map(
      ITSASessionKeys.JourneyStateKey -> JsString(UserMatching.key)
    ))
    SessionDataConnectorStub.stubSaveJourneyState()(OK)
  }

  s"GET ${routes.ConfirmedClientResolver.resolve.url}" when {
    "the agent is unauthenticated" should {
      "redirect to login" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(ggLoginUrl)
        )
      }
    }

    "the agent has no nino in " should {
      "return an internal server error" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle) -> JsBoolean(true)
        ))

        val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  s"GET ${routes.ConfirmedClientResolver.resolve.url}" when {
    "the client is ineligible" should {
      "redirect to the cannot take part page" in {
        AuthStub.stubAuthSuccess()
        stubFullSession(eligibleCurrent = false, eligibleNext = false)

        val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.eligibility.routes.CannotTakePartController.show.url)
        )
      }
    }
    "the different tax year selection scenarios" when {
      "CY is eligible and voluntary, CY+1 is eligible and voluntary" should {
        "redirect to the WhenDoYouWantToStartController page" in {

          AuthStub.stubAuthSuccess()
          stubFullSession()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          stubFullPrePop()

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.taxyear.routes.WhenDoYouWantToStartController.show().url)
          )
          IncomeTaxSubscriptionConnectorStub.verifySaveSoleTraderBusinesses(prePopBusinesses)
        }
      }
      // TEMPORARY TESTS: Remove these encryption-migration scenarios after the migration period is complete.
      "the client has legacy-encrypted businesses and no new encryption flag" should {
        "read the legacy data, save it with the new encryption, then save the flag before continuing" in {
          AuthStub.stubAuthSuccess()
          stubFullSession()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetAllSubscriptionDetails(
            OK,
            Json.obj(SubscriptionDataKeys.SoleTraderBusinessesKey -> legacyEncryptedBusinesses)
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, OK, JsBoolean(true))
          IncomeTaxSubscriptionConnectorStub.stubSaveSoleTraderBusinesses()
          IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean]("newEncryption", true)

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.taxyear.routes.WhenDoYouWantToStartController.show().url)
          )
          WiremockHelper.verifyGet(IncomeTaxSubscriptionConnectorStub.allSubscriptionDataUri(), Some(1))
          WiremockHelper.verifyPost(
            IncomeTaxSubscriptionConnectorStub.subscriptionUri("newEncryption"),
            Some("true"),
            Some(1)
          )
          verifyBusinessesSavedWithAesGcm(legacyBusinesses)
        }
      }

      "the client already has the new encryption flag" should {
        "not rewrite the businesses or save the flag again" in {
          AuthStub.stubAuthSuccess()
          stubFullSession()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetAllSubscriptionDetails(
            OK,
            Json.obj(
              SubscriptionDataKeys.SoleTraderBusinessesKey -> gcmEncryptedBusinesses,
              "newEncryption" -> JsBoolean(true)
            )
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, OK, JsBoolean(true))

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.taxyear.routes.WhenDoYouWantToStartController.show().url)
          )
          WiremockHelper.verifyGet(IncomeTaxSubscriptionConnectorStub.allSubscriptionDataUri(), Some(1))
          WiremockHelper.verifyPost(IncomeTaxSubscriptionConnectorStub.subscriptionUri(SubscriptionDataKeys.SoleTraderBusinessesKey), count = Some(0))
          WiremockHelper.verifyPost(IncomeTaxSubscriptionConnectorStub.subscriptionUri("newEncryption"), count = Some(0))
        }
      }

      "the client has other subscription data but no encrypted businesses" should {
        "save the flag without creating an empty businesses record" in {
          AuthStub.stubAuthSuccess()
          stubFullSession()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetAllSubscriptionDetails(
            OK,
            Json.obj(SubscriptionDataKeys.PrePopFlag -> JsBoolean(true))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, OK, JsBoolean(true))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean]("newEncryption", true)

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.taxyear.routes.WhenDoYouWantToStartController.show().url)
          )
          WiremockHelper.verifyPost(IncomeTaxSubscriptionConnectorStub.subscriptionUri("newEncryption"), Some("true"), Some(1))
          WiremockHelper.verifyPost(IncomeTaxSubscriptionConnectorStub.subscriptionUri(SubscriptionDataKeys.SoleTraderBusinessesKey), count = Some(0))
        }
      }

      "the client has no subscription data" should {
        "save the new encryption flag without writing an empty businesses record" in {
          AuthStub.stubAuthSuccess()
          stubFullSession()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetAllSubscriptionDetails(NO_CONTENT)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, OK, JsBoolean(true))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean]("newEncryption", true)

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.taxyear.routes.WhenDoYouWantToStartController.show().url)
          )
          WiremockHelper.verifyGet(IncomeTaxSubscriptionConnectorStub.allSubscriptionDataUri(), Some(1))
          WiremockHelper.verifyPost(IncomeTaxSubscriptionConnectorStub.subscriptionUri("newEncryption"), Some("true"), Some(1))
          WiremockHelper.verifyPost(IncomeTaxSubscriptionConnectorStub.subscriptionUri(SubscriptionDataKeys.SoleTraderBusinessesKey), count = Some(0))
        }
      }
      "CY is eligible and voluntary, CY+1 is eligible and mandated" should {
        "redirect to the NextYearMandatorySignUpController page" in {

          AuthStub.stubAuthSuccess()
          stubFullSession(statusNext = Mandated)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          stubFullPrePop()

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.taxyear.routes.NextYearMandatorySignUpController.show().url)
          )
          IncomeTaxSubscriptionConnectorStub.verifySaveSoleTraderBusinesses(prePopBusinesses)
        }
      }

      "CY is eligible and mandated" should {
        "redirect to the MandatoryBothSignUpController page" in {

          AuthStub.stubAuthSuccess()
          stubFullSession(statusCurrent = Mandated)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          stubFullPrePop()

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.taxyear.routes.MandatoryBothSignUpController.show.url)
          )
          IncomeTaxSubscriptionConnectorStub.verifySaveSoleTraderBusinesses(prePopBusinesses)
        }
      }

      "CY is ineligible, CY+1 is eligible and voluntary" should {
        "redirect to the NonEligibleVoluntaryController page" in {

          AuthStub.stubAuthSuccess()
          stubFullSession(eligibleCurrent = false)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          stubFullPrePop()

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.taxyear.routes.NonEligibleVoluntaryController.show.url)
          )
          IncomeTaxSubscriptionConnectorStub.verifySaveSoleTraderBusinesses(prePopBusinesses)
        }
      }

      "CY is ineligible, CY+1 is eligible and mandated" should {
        "redirect to the NonEligibleMandatedController page" in {

          AuthStub.stubAuthSuccess()
          stubFullSession(eligibleCurrent = false, statusNext = Mandated)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          stubFullPrePop()

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.taxyear.routes.NonEligibleMandatedController.show.url)
          )
          IncomeTaxSubscriptionConnectorStub.verifySaveSoleTraderBusinesses(prePopBusinesses)
        }
      }
    }
    
    "result in technical difficulties" when {
      "there was an error returned from the pre-pop connection" in {
        AuthStub.stubAuthSuccess()
        stubFullSession()
        IncomeTaxSubscriptionConnectorStub.stubGetAllSubscriptionDetails(
          OK,
          Json.obj("newEncryption" -> JsBoolean(true))
        )
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, NO_CONTENT)
        PrePopStub.stubGetPrePop(testNino)(
          status = INTERNAL_SERVER_ERROR,
          body = Json.obj()
        )

        val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver()

        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  def stubFullSession(eligibleCurrent: Boolean = true,
                      eligibleNext: Boolean = true,
                      statusCurrent: MandationStatus = Voluntary,
                      statusNext: MandationStatus = Voluntary): Unit = {
    SessionDataConnectorStub.stubGetAllSessionData(Map(
      ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle) -> JsBoolean(true),
      ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(statusCurrent, statusNext)),
      ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(
        eligibleCurrentYear = eligibleCurrent, eligibleNextYear = eligibleNext, exemptionReason = None
      )),
      ITSASessionKeys.NINO -> JsString(testNino),
      ITSASessionKeys.UTR -> JsString(testUtr)
    ))
  }

  def stubFullPrePop(): Unit = {
    IncomeTaxSubscriptionConnectorStub.stubGetAllSubscriptionDetails(
      OK,
      Json.obj("newEncryption" -> JsBoolean(true))
    )
    IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, NO_CONTENT)
    PrePopStub.stubGetPrePop(testNino)(
      status = OK,
      body = Json.obj(
        "selfEmployment" -> Json.arr(
          Json.obj(
            "name" -> "ABC",
            "trade" -> "Plumbing",
            "address" -> Json.obj(
              "lines" -> Json.arr(
                "1 long road"
              ),
              "postcode" -> "ZZ1 1ZZ"
            ),
            "startDate" -> Json.obj(
              "day" -> "01",
              "month" -> "02",
              "year" -> "2000"
            )
          )
        )
      )
    )
    IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)
    IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean](SubscriptionDataKeys.PrePopFlag, true)
    IncomeTaxSubscriptionConnectorStub.stubSaveSoleTraderBusinesses()
  }

  private lazy val legacySensitiveFormat: Format[SensitiveString] = {
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)(
      implicitly[Format[String]],
      app.injector.instanceOf[ApplicationCrypto].JsonCrypto
    )
  }

  private val legacyBusinessesModel = SoleTraderBusinesses(Seq(
    testBusiness("legacy-business").copy(
      businessName = Some(BusinessNameModel("Legacy business")),
      businessDisplayName = Some("Legacy display name")
    ).toSoleTraderBusiness
  ))

  private def legacyEncryptedBusinesses =
    Json.toJson(legacyBusinessesModel)(SoleTraderBusinesses.encryptedFormat(legacySensitiveFormat))

  private def gcmEncryptedBusinesses =
    Json.toJson(legacyBusinessesModel)(SoleTraderBusinesses.encryptedFormat(sensitiveFormat))

  private val legacyBusinesses = legacyBusinessesModel

  private def verifyBusinessesSavedWithAesGcm(expected: SoleTraderBusinesses): Unit = {
    val requests = WireMock.findAll(WireMock.postRequestedFor(
      WireMock.urlEqualTo(IncomeTaxSubscriptionConnectorStub.subscriptionUri(SubscriptionDataKeys.SoleTraderBusinessesKey))
    ))
    requests.size() mustBe 1

    val gcmSensitiveFormat = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)(
      implicitly[Format[String]],
      aesGcmCryptoFromConfig("json.encryption.gcm", app.configuration.underlying)
    )
    Json.parse(requests.get(0).getBodyAsString)
      .as[SoleTraderBusinesses](SoleTraderBusinesses.encryptedFormat(gcmSensitiveFormat)) mustBe expected
  }

  private val prePopBusinesses = SoleTraderBusinesses(
    Seq(SelfEmploymentData(
      id = "test-uuid",
      startDateBeforeLimit = Some(true),
      businessName = Some(BusinessNameModel("ABC")),
      businessTradeName = Some(BusinessTradeNameModel("Plumbing")),
      businessAddress = Some(BusinessAddressModel(Address(
        lines = Seq("1 long road"),
        postcode = Some("ZZ1 1ZZ"),
        country = None
      )))
    ).toSoleTraderBusiness)
  )

}
