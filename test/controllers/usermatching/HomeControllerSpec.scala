/*
 * Copyright 2021 HM Revenue & Customs
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

import agent.audit.mocks.MockAuditingService
import config.MockConfig
import connectors.individual.eligibility.httpparsers.{Eligible, Ineligible}
import controllers.ControllerBaseSpec
import org.mockito.Mockito.reset
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{await, _}
import services.mocks.{MockCitizenDetailsService, MockGetEligibilityStatusService, MockSubscriptionDetailsService, MockSubscriptionService}
import uk.gov.hmrc.http.InternalServerException
import utilities.ITSASessionKeys
import utilities.SubscriptionDataKeys.MtditId
import utilities.individual.TestConstants

import scala.concurrent.Future

class HomeControllerSpec extends ControllerBaseSpec
  with MockSubscriptionService
  with MockSubscriptionDetailsService
  with MockCitizenDetailsService
  with MockGetEligibilityStatusService
  with MockAuditingService {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> testHomeController(showStartPage = false).index()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)
    mockNinoRetrieval()
  }

  def testHomeController(showStartPage: Boolean = true): HomeController = new HomeController(
    mockAuditingService,
    mockAuthService,
    mockCitizenDetailsService,
    mockGetEligibilityStatusService,
    MockSubscriptionDetailsService,
    mockSubscriptionService
  )(implicitly, MockConfig, mockMessagesControllerComponents)

  val testNino: String = TestConstants.testNino
  val testUtr: String = TestConstants.testUtr

  "Calling the home action of the Home controller with an authorised user" when {
    "there is no start page" should {
      lazy val result = testHomeController(showStartPage = false).home()(subscriptionRequest)

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
          setupMockSubscriptionDetailsSaveFunctions()

          val result = testHomeController().index(fakeRequest)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe controllers.individual.subscription.routes.ClaimSubscriptionController.claim().url

          verifySubscriptionDetailsSave(MtditId, 1)
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
                mockGetEligibilityStatus(testUtr)(Future.successful(Eligible))

                val result = await(testHomeController().index(fakeRequest))
                status(result) must be(Status.SEE_OTHER)
                redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences().url
              }
            }

            "the user does not have a utr" when {
              "the user has a matching utr in CID against their NINO" in {
                mockNinoRetrieval()
                mockResolveIdentifiers(Some(testNino), None)(Some(testNino), Some(testUtr))
                setupMockGetSubscriptionNotFound(testNino)
                mockGetEligibilityStatus(testUtr)(Future.successful(Eligible))

                val result = await(testHomeController().index(fakeRequest))

                status(result) mustBe SEE_OTHER
                redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences().url

                session(result).get(ITSASessionKeys.UTR) mustBe Some(testUtr)
              }

              "the user does not have a matching utr in CID" should {

                "redirect to the no SA page" in {
                  mockNinoRetrieval()
                  mockResolveIdentifiers(Some(testNino), None)(Some(testNino), None)
                  setupMockGetSubscriptionNotFound(testNino)
                  mockGetEligibilityStatus(testUtr)(Future.successful(Eligible))

                  val result = testHomeController().index()(userMatchingRequest)

                  status(result) mustBe SEE_OTHER
                  redirectLocation(result).get mustBe controllers.usermatching.routes.NoSAController.show().url

                  await(result).session(userMatchingRequest).get(ITSASessionKeys.UTR) mustBe None
                }
              }
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

            val result = await(testHomeController().index(fakeRequest))
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

          intercept[InternalServerException](await(testHomeController().index(fakeRequest)))
        }
      }
    }
    "the user does not have a nino but has an IR-SA enrolment" when {
      "the user already has an MTDIT subscription on ETMP" should {
        "redirect to the claim subscription page" in {
          mockUtrRetrieval()
          mockResolveIdentifiers(None, Some(testUtr))(Some(testNino), Some(testUtr))
          setupMockGetSubscriptionFound(testNino)
          setupMockSubscriptionDetailsSaveFunctions()

          val result = testHomeController().index(fakeRequest)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe controllers.individual.subscription.routes.ClaimSubscriptionController.claim().url

          verifySubscriptionDetailsSave(MtditId, 1)
        }
      }
      "the user does not already have an MTDIT subscription on ETMP" when {
        "the user is eligible" when {
          "the user does not have a current unauthorised subscription journey" when {
            "redirect to the sign up journey" in {
              mockUtrRetrieval()
              mockResolveIdentifiers(None, Some(testUtr))(Some(testNino), Some(testUtr))
              setupMockGetSubscriptionNotFound(testNino)
              mockGetEligibilityStatus(testUtr)(Future.successful(Eligible))

              val result = await(testHomeController().index(fakeRequest))
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

            val result = await(testHomeController().index(fakeRequest))
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

          intercept[InternalServerException](await(testHomeController().index(fakeRequest)))
        }
      }
    }
    "the user does not have a nino or an IR-SA enrolment" should {
      "redirect to the NINO resolver if the user doesn't have an IR-SA enrolment" in {
        mockResolveIdentifiers(None, None)(None, None)
        mockIndividualWithNoEnrolments()

        val result = testHomeController().index(fakeRequest)

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result).get mustBe controllers.usermatching.routes.UserDetailsController.show().url
      }
    }
  }

  authorisationTests()

}
