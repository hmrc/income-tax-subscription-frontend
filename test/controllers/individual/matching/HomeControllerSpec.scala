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

package controllers.individual.matching

import _root_.common.Constants.ITSASessionKeys
import config.MockConfig
import config.featureswitch.FeatureSwitch.{PrePopulate, ThrottlingFeature}
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataSuccessResponse
import controllers.individual.ControllerBaseSpec
import models.EligibilityStatus
import models.status.MandationStatus.{Mandated, Voluntary}
import org.mockito.Mockito.{never, reset, times}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks._
import services.{IndividualStartOfJourneyThrottle, ThrottlingService}
import uk.gov.hmrc.http.InternalServerException
import utilities.individual.TestConstants
import utilities.individual.TestConstants.testFullName

class HomeControllerSpec extends ControllerBaseSpec
  with MockSubscriptionService
  with MockSubscriptionDetailsService
  with MockCitizenDetailsService
  with MockGetEligibilityStatusService
  with MockPrePopulationService
  with MockAuditingService
  with MockThrottlingConnector
  with MockSessionDataService
  with MockReferenceRetrieval
  with MockNinoService
  with MockMandationStatusConnector {

  private val eligible = EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)
  private val ineligible = EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = false)
  private val eligibleNextYearOnly = EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true)

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> testHomeController(showStartPage = false).index()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)
    disable(PrePopulate)
    enable(ThrottlingFeature)
    notThrottled()
    mockNinoRetrieval()
  }

  def testHomeController(showStartPage: Boolean = true): HomeController = new HomeController(
    mockCitizenDetailsService,
    mockGetEligibilityStatusService,
    mockSubscriptionService,
    new ThrottlingService(mockThrottlingConnector, mockSessionDataService, appConfig),
    mockPrePopulationService,
    mockNinoService,
    mockSessionDataService,
    mockReferenceRetrieval
  )(
    mockAuditingService,
    mockAuthService,
    MockConfig
  )(implicitly, mockMessagesControllerComponents)

  import TestConstants.{testNino, testUtr}

  "home" when {
    "there is no start page" should {
      lazy val result = testHomeController(showStartPage = false).home()(subscriptionRequest)

      "Return status SEE_OTHER (303) redirect" in {
        status(result) must be(Status.SEE_OTHER)
      }

      "Redirect to the 'Index' page" in {
        redirectLocation(result).get mustBe controllers.individual.matching.routes.HomeController.index.url
      }
    }
  }

  "index" when {
    "the user has a nino" when {
      "the user already has an MTDIT subscription on ETMP" should {
        "redirect to the claim subscription page" in {
          mockGetNino(testNino)
          mockFetchUTR(Right(None))
          mockNinoAndUtrRetrieval()
          mockLookupUserWithUtr(testNino)(testUtr, testFullName)
          setupMockGetSubscriptionFound(testNino)
          mockFetchThrottlePassed(IndividualStartOfJourneyThrottle)(Right(None))
          mockSaveThrottlePassed(IndividualStartOfJourneyThrottle)(Right(SaveSessionDataSuccessResponse))

          val result = testHomeController().index(fakeRequest)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show.url

          verifyGetThrottleStatusCalls(times(1))
        }
      }

      "the user already has an MTDIT subscription on ETMP and the session is in SignUp state" should {
        "redirect to the sps callback route with the already stored entity id" in {
          mockNinoAndUtrRetrieval()
          mockGetNino(testNino)
          mockFetchUTR(Right(None))
          mockLookupUserWithUtr(testNino)(testUtr, testFullName)
          setupMockGetSubscriptionFound(testNino)

          val result = testHomeController().index(subscriptionRequest)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSCallbackController.callback(Some(TestConstants.testSpsEntityId)).url

          verifyGetThrottleStatusCalls(never())
        }
      }

      "the user does not already have an MTDIT subscription on ETMP" when {
        "the user is eligible " when {
          "the user has a UTR" should {
            "redirect to the sign up journey" when {
              "PrePopulate and ITSA mandation status are on but there is no PrePop data" when {
                "redirect to SPSHandoff controller" in {
                  mockNinoAndUtrRetrieval()
                  mockGetNino(testNino)
                  mockFetchUTR(Right(None))
                  mockSaveUTR(testUtr)(Right(SaveSessionDataSuccessResponse))
                  mockLookupUserWithUtr(testNino)(testUtr, testFullName)
                  setupMockGetSubscriptionNotFound(testNino)
                  mockGetEligibilityStatus(eligible)
                  mockGetMandationStatus(testNino, testUtr)(Voluntary, Mandated)
                  mockFetchThrottlePassed(IndividualStartOfJourneyThrottle)(Right(None))
                  mockSaveThrottlePassed(IndividualStartOfJourneyThrottle)(Right(SaveSessionDataSuccessResponse))

                  enable(PrePopulate)

                  val result = await(testHomeController().index(fakeRequest))
                  status(result) must be(Status.SEE_OTHER)
                  redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url

                  verifyPrePopulationSave(0, testReference)
                  verifyGetThrottleStatusCalls(times(1))
                }
              }
            }
          }

          "the user does not have a utr" when {
            "the user has a matching utr in CID against their NINO" when {
              "redirect to SPSHandoff controller" in {
                mockNinoRetrieval()
                mockGetNino(testNino)
                mockFetchUTR(Right(None))
                mockSaveUTR(testUtr)(Right(SaveSessionDataSuccessResponse))
                mockLookupUserWithUtr(testNino)(testUtr, testFullName)
                setupMockGetSubscriptionNotFound(testNino)
                mockGetEligibilityStatus(eligible)
                mockGetMandationStatus(testNino, testUtr)(Voluntary, Voluntary)
                mockFetchThrottlePassed(IndividualStartOfJourneyThrottle)(Right(None))
                mockSaveThrottlePassed(IndividualStartOfJourneyThrottle)(Right(SaveSessionDataSuccessResponse))

                val result = await(testHomeController().index(fakeRequest))

                status(result) mustBe SEE_OTHER
                redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url

                session(result).get(ITSASessionKeys.FULLNAME) mustBe Some(testFullName)

                verifyGetThrottleStatusCalls(times(1))
              }
            }

            "the user does not have a matching utr in CID" should {
              "redirect to the no SA page" in {
                mockNinoRetrieval()
                mockGetNino(testNino)
                mockFetchUTR(Right(None))
                mockSaveUTR(testUtr)(Right(SaveSessionDataSuccessResponse))
                mockLookupUserWithoutUtr(testNino)

                val result = testHomeController().index()(fakeRequest)

                status(result) mustBe SEE_OTHER
                redirectLocation(result).get mustBe controllers.individual.matching.routes.NoSAController.show.url

                session(result).get(ITSASessionKeys.FULLNAME) mustBe None
                verifyGetThrottleStatusCalls(never())
              }
            }
          }

        }

        "the user is not eligible this year, but is eligible next year" should {
          "redirect to the Cannot Sign Up This Year page" in {
            mockNinoAndUtrRetrieval()
            mockGetNino(testNino)
            mockFetchUTR(Right(None))
            mockSaveUTR(testUtr)(Right(SaveSessionDataSuccessResponse))
            mockLookupUserWithUtr(testNino)(testUtr, testFullName)
            setupMockGetSubscriptionNotFound(testNino)
            mockGetEligibilityStatus(eligibleNextYearOnly)
            mockGetMandationStatus(testNino, testUtr)(Voluntary, Voluntary)
            mockFetchThrottlePassed(IndividualStartOfJourneyThrottle)(Right(None))
            mockSaveThrottlePassed(IndividualStartOfJourneyThrottle)(Right(SaveSessionDataSuccessResponse))

            val result = await(testHomeController().index(fakeRequest))
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.controllist.routes.CannotSignUpThisYearController.show.url)
            verifyGetThrottleStatusCalls(times(1))
            verifyPrePopulationSave(0, testReference)
          }
        }

        "the user is not eligible" should {
          "redirect to the Not eligible page" in {
            mockNinoAndUtrRetrieval()
            mockGetNino(testNino)
            mockFetchUTR(Right(None))
            mockSaveUTR(testUtr)(Right(SaveSessionDataSuccessResponse))
            mockLookupUserWithUtr(testNino)(testUtr, testFullName)
            setupMockGetSubscriptionNotFound(testNino)
            mockGetEligibilityStatus(ineligible)
            mockFetchThrottlePassed(IndividualStartOfJourneyThrottle)(Right(None))
            mockSaveThrottlePassed(IndividualStartOfJourneyThrottle)(Right(SaveSessionDataSuccessResponse))

            val result = await(testHomeController().index(fakeRequest))
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.controllist.routes.NotEligibleForIncomeTaxController.show().url)
            verifyGetThrottleStatusCalls(times(1))
          }
        }
      }

      "the call to check the user's subscription status fails" should {
        "return an error page" in {
          mockNinoAndUtrRetrieval()
          mockGetNino(testNino)
          mockFetchUTR(Right(None))
          mockSaveUTR(testUtr)(Right(SaveSessionDataSuccessResponse))
          mockLookupUserWithUtr(testNino)(testUtr, testFullName)
          setupMockGetSubscriptionFailure(testNino)
          mockFetchThrottlePassed(IndividualStartOfJourneyThrottle)(Right(None))
          mockSaveThrottlePassed(IndividualStartOfJourneyThrottle)(Right(SaveSessionDataSuccessResponse))

          intercept[InternalServerException](await(testHomeController().index(fakeRequest)))
          verifyGetThrottleStatusCalls(times(1))
        }
      }
    }
  }

  authorisationTests()

}
