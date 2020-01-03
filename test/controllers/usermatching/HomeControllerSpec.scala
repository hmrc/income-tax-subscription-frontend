/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.usermatching

import controllers.ControllerBaseSpec
import core.ITSASessionKeys
import core.audit.Logging
import core.auth.Registration
import core.config.MockConfig
import core.services.mocks.MockKeystoreService
import core.utils.TestConstants
import core.utils.TestConstants._
import incometax.eligibility.httpparsers.{Eligible, Ineligible}
import incometax.eligibility.services.mocks.MockGetEligibilityStatusService
import incometax.subscription.services.mocks.MockSubscriptionService
import incometax.unauthorisedagent.services.mocks.MockSubscriptionStoreRetrievalService
import org.mockito.Mockito.reset
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.http.InternalServerException
import usermatching.services.mocks.MockCitizenDetailsService

import scala.concurrent.Future


class HomeControllerSpec extends ControllerBaseSpec
  with MockSubscriptionService
  with MockKeystoreService
  with MockCitizenDetailsService
  with MockSubscriptionStoreRetrievalService
  with MockGetEligibilityStatusService {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> TestHomeController(showStartPage = false).index()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)
    mockNinoRetrieval()
  }

  def TestHomeController(showStartPage: Boolean = true, registrationFeature: Boolean = false): HomeController = new HomeController(
    mockBaseControllerConfig(new MockConfig {
      override val showGuidance: Boolean = showStartPage
      override val enableRegistration: Boolean = registrationFeature
    }),
    messagesApi,
    mockSubscriptionService,
    MockKeystoreService,
    mockAuthService,
    mockCitizenDetailsService,
    mockSubscriptionStoreRetrievalService,
    mockGetEligibilityStatusService,
    app.injector.instanceOf[Logging]
  )

  val testNino = TestConstants.testNino
  val testUtr = TestConstants.testUtr

  "Calling the home action of the Home controller with an authorised user" when {
    "there is no start page" should {
      lazy val result = TestHomeController(showStartPage = false).home()(subscriptionRequest)

      "Return status SEE_OTHER (303) redirect" in {
        status(result) must be(Status.SEE_OTHER)
      }
      "Redirect to the 'Index' page" in {
        redirectLocation(result).get mustBe controllers.usermatching.routes.HomeController.index().url
      }
    }
  }

  "index" when {
    "the user has a nino" when {
      "the user already has an MTDIT subscription on ETMP" should {
        "redirect to the claim subscription page" in {
          mockNinoAndUtrRetrieval()
          mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
          setupMockGetSubscriptionFound(testNino)
          setupMockKeystoreSaveFunctions()

          val result = TestHomeController().index(fakeRequest)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe controllers.individual.subscription.routes.ClaimSubscriptionController.claim().url

          verifyKeystore(saveSubscriptionId = 1)
        }
      }
      "the user does not already have an MTDIT subscription on ETMP" when {
        "the user is eligible " when {
          "the user does not have a current unauthorised subscription journey" when {
            "the user has a UTR" should {
              "redirect to the sign up journey" in {
                mockNinoAndUtrRetrieval()
                mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
                setupMockGetSubscriptionNotFound(testNino)
                mockRetrieveSubscriptionData(testNino)(successfulSubscriptionNotFound)
                mockGetEligibilityStatus(testUtr)(Future.successful(Eligible))

                val result = await(TestHomeController().index(fakeRequest))
                status(result) must be(Status.SEE_OTHER)
                redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences().url
              }
            }

            "the user does not have a utr" when {
              "the user has a matching utr in CID against their NINO" in {
                mockNinoRetrieval()
                mockResolveIdentifiers(Some(testNino), None)(Some(testNino), Some(testUtr))
                setupMockGetSubscriptionNotFound(testNino)
                mockRetrieveSubscriptionData(testNino)(successfulSubscriptionNotFound)
                mockGetEligibilityStatus(testUtr)(Future.successful(Eligible))

                val result = await(TestHomeController().index(fakeRequest))

                status(result) mustBe SEE_OTHER
                redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences().url

                session(result).get(ITSASessionKeys.UTR) mustBe Some(testUtr)
              }

              "the user does not have a matching utr in CID" when {
                "the registration feature flag is on" should {
                  "redirect to the registration journey" in {
                    mockNinoRetrieval()
                    mockResolveIdentifiers(Some(testNino), None)(Some(testNino), None)
                    setupMockGetSubscriptionNotFound(testNino)
                    mockGetEligibilityStatus(testUtr)(Future.successful(Eligible))
                    mockRetrieveSubscriptionData(testNino)(successfulSubscriptionNotFound)

                    val result = TestHomeController(registrationFeature = true).index()(registrationRequest)

                    status(result) mustBe SEE_OTHER
                    redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences().url
                    await(result).session(registrationRequest).get(ITSASessionKeys.JourneyStateKey) must contain(Registration.name)
                  }
                }
                "the registration feature flag is off" should {
                  "redirect to the no SA page" in {
                    mockNinoRetrieval()
                    mockResolveIdentifiers(Some(testNino), None)(Some(testNino), None)
                    setupMockGetSubscriptionNotFound(testNino)
                    mockGetEligibilityStatus(testUtr)(Future.successful(Eligible))
                    mockRetrieveSubscriptionData(testNino)(successfulSubscriptionNotFound)

                    val result = TestHomeController(registrationFeature = false).index()(registrationRequest)

                    status(result) mustBe SEE_OTHER
                    redirectLocation(result).get mustBe controllers.usermatching.routes.NoSAController.show().url

                    await(result).session(registrationRequest).get(ITSASessionKeys.UTR) mustBe None
                  }
                }
              }
            }
          }
          "the user's unauthorised agent has already started the subscription journey" should {
            "redirect to the confirm agent subscription page" in {
              mockNinoAndUtrRetrieval()
              mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
              setupMockGetSubscriptionNotFound(testNino)
              mockGetEligibilityStatus(testUtr)(Future.successful(Eligible))
              mockRetrieveSubscriptionData(testNino)(successfulStoredSubscriptionFound)

              val result = TestHomeController().index(fakeRequest)

              status(result) must be(Status.SEE_OTHER)
              redirectLocation(result).get mustBe controllers.unauthorisedagent.routes.AuthoriseAgentController.show().url
              session(result).get(ITSASessionKeys.AgentReferenceNumber) must contain(testArn)
            }
          }
        }
        "the user is not eligible" should {
          "redirect to the Not eligible page" in {
            mockNinoAndUtrRetrieval()
            mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
            setupMockGetSubscriptionNotFound(testNino)
            //mockRetrieveSubscriptionData(testNino)(successfulSubscriptionNotFound)
            mockGetEligibilityStatus(testUtr)(Future.successful(Ineligible))

            val result = await(TestHomeController().index(fakeRequest))
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.eligibility.routes.NotEligibleForIncomeTaxController.show().url)
          }
        }
      }
      "the call to check the user's subscription status fails" should {
        "return an error page" in {
          mockNinoAndUtrRetrieval()
          mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
          setupMockGetSubscriptionFailure(testNino)

          intercept[InternalServerException](await(TestHomeController().index(fakeRequest)))
        }
      }
    }
    "the user does not have a nino but has an IR-SA enrolment" when {
      "the user already has an MTDIT subscription on ETMP" should {
        "redirect to the claim subscription page" in {
          mockUtrRetrieval()
          mockResolveIdentifiers(None, Some(testUtr))(Some(testNino), Some(testUtr))
          setupMockGetSubscriptionFound(testNino)
          setupMockKeystoreSaveFunctions()

          val result = TestHomeController().index(fakeRequest)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe controllers.individual.subscription.routes.ClaimSubscriptionController.claim().url

          verifyKeystore(saveSubscriptionId = 1)
        }
      }
      "the user does not already have an MTDIT subscription on ETMP" when {
        "the user is eligible" when {
          "the user's unauthorised agent has already started the subscription journey" should {
            "redirect to the confirm agent subscription page" in {
              mockUtrRetrieval()
              mockResolveIdentifiers(None, Some(testUtr))(Some(testNino), Some(testUtr))
              setupMockGetSubscriptionNotFound(testNino)
              mockGetEligibilityStatus(testUtr)(Future.successful(Eligible))
              mockRetrieveSubscriptionData(testNino)(successfulStoredSubscriptionFound)

              val result = TestHomeController().index(fakeRequest)

              status(result) must be(Status.SEE_OTHER)
              redirectLocation(result).get mustBe controllers.unauthorisedagent.routes.AuthoriseAgentController.show().url
              session(result).get(ITSASessionKeys.AgentReferenceNumber) must contain(testArn)
            }
          }
          "the user does not have a current unauthorised subscription journey" when {
            "redirect to the sign up journey" in {
              mockUtrRetrieval()
              mockResolveIdentifiers(None, Some(testUtr))(Some(testNino), Some(testUtr))
              setupMockGetSubscriptionNotFound(testNino)
              mockGetEligibilityStatus(testUtr)(Future.successful(Eligible))
              mockRetrieveSubscriptionData(testNino)(successfulSubscriptionNotFound)

              val result = await(TestHomeController().index(fakeRequest))
              status(result) must be(Status.SEE_OTHER)
              redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences().url
              session(result).get(ITSASessionKeys.NINO) must contain(testNino)
            }
          }
        }
        "the user is not eligible" should {
          "redirect to the Not eligible page" in {
            mockUtrRetrieval()
            mockResolveIdentifiers(None, Some(testUtr))(Some(testNino), Some(testUtr))
            setupMockGetSubscriptionNotFound(testNino)
            mockGetEligibilityStatus(testUtr)(Future.successful(Ineligible))

            val result = await(TestHomeController().index(fakeRequest))
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.eligibility.routes.NotEligibleForIncomeTaxController.show().url)
          }
        }
      }
      "the call to check the user's subscription status fails" should {
        "return an error page" in {
          mockNinoAndUtrRetrieval()
          mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
          setupMockGetSubscriptionFailure(testNino)

          intercept[InternalServerException](await(TestHomeController().index(fakeRequest)))
        }
      }
    }
    "the user does not have a nino or an IR-SA enrolment" should {
      "redirect to the NINO resolver if the user doesn't have an IR-SA enrolment" in {
        mockResolveIdentifiers(None, None)(None, None)
        mockIndividualWithNoEnrolments()

        val result = TestHomeController().index(fakeRequest)

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result).get mustBe controllers.usermatching.routes.UserDetailsController.show().url
      }
    }
  }

  authorisationTests()

}
