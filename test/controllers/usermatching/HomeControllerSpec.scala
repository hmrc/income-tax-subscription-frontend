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

package controllers.usermatching

import _root_.common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.{ELIGIBLE_NEXT_YEAR_ONLY, MANDATED_CURRENT_YEAR, MANDATED_NEXT_YEAR}
import config.MockConfig
import config.featureswitch.FeatureSwitch.{PrePopulate, ThrottlingFeature}
import controllers.ControllerBaseSpec
import models.status.MandationStatus.{Mandated, Voluntary}
import models.{EligibilityStatus, PrePopData}
import org.mockito.Mockito.{never, reset, times}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.ThrottlingService
import services.mocks._
import uk.gov.hmrc.http.InternalServerException
import utilities.individual.TestConstants
import utilities.individual.TestConstants.testFullName

import scala.concurrent.Future

class HomeControllerSpec extends ControllerBaseSpec
  with MockSubscriptionService
  with MockSubscriptionDetailsService
  with MockCitizenDetailsService
  with MockGetEligibilityStatusService
  with MockPrePopulationService
  with MockAuditingService
  with MockThrottlingConnector
  with MockMandationStatusConnector {

  private val eligibleWithoutPrepopData = EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, None)
  private val eligibleWithPrepopData = EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, Some(mock[PrePopData]))
  private val ineligible = EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = false, None)
  private val eligibleNextYearOnly = EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, None)
  private val eligibleNextYearOnlyWithPrepopData = EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, Some(mock[PrePopData]))

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
    mockAuditingService,
    mockAuthService,
    mockCitizenDetailsService,
    mockGetEligibilityStatusService,
    MockSubscriptionDetailsService,
    mockPrePopulationService,
    mockSubscriptionService,
    new ThrottlingService(mockThrottlingConnector, appConfig),
    mockMandationStatusConnector
  )(implicitly, MockConfig, mockMessagesControllerComponents)

  import TestConstants.{testNino, testReference, testUtr}

  "home" when {
    "there is no start page" should {
      lazy val result = testHomeController(showStartPage = false).home()(subscriptionRequest)

      "Return status SEE_OTHER (303) redirect" in {
        status(result) must be(Status.SEE_OTHER)
      }

      "Redirect to the 'Index' page" in {
        redirectLocation(result).get mustBe controllers.usermatching.routes.HomeController.index.url
      }
    }
  }

  "index" when {
    "the user has a nino" when {
      "the user already has an MTDIT subscription on ETMP" should {
        "redirect to the claim subscription page" in {
          mockNinoAndUtrRetrieval()
          mockLookupUserWithUtr(testNino)(testUtr, testFullName)
          setupMockGetSubscriptionFound(testNino)
          setupMockSubscriptionDetailsSaveFunctions()
          mockRetrieveReferenceSuccess(testUtr)(testReference)

          val result = testHomeController().index(fakeRequest)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show.url

          verifyGetThrottleStatusCalls(times(1))
        }
      }

      "the user already has an MTDIT subscription on ETMP and the session is in SignUp state" should {
        "redirect to the claim subscription page" in {
          mockNinoAndUtrRetrieval()
          mockLookupUserWithUtr(testNino)(testUtr, testFullName)
          setupMockGetSubscriptionFound(testNino)
          setupMockSubscriptionDetailsSaveFunctions()
          mockRetrieveReferenceSuccess(testUtr)(testReference)

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
                  mockLookupUserWithUtr(testNino)(testUtr, testFullName)
                  setupMockGetSubscriptionNotFound(testNino)
                  mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibleWithoutPrepopData)))
                  mockRetrieveReferenceSuccess(testUtr)(testReference)
                  mockGetMandationStatus(Voluntary, Mandated)

                  enable(PrePopulate)

                  val result = await(testHomeController().index(fakeRequest))
                  status(result) must be(Status.SEE_OTHER)
                  redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url

                  verifyPrePopulationSave(0, testReference)
                  verifyGetThrottleStatusCalls(times(1))
                  result.session(fakeRequest).data must contain(ELIGIBLE_NEXT_YEAR_ONLY -> "false")
                  result.session(fakeRequest).data must contain(MANDATED_CURRENT_YEAR -> "false")
                  result.session(fakeRequest).data must contain(MANDATED_NEXT_YEAR -> "true")
                }
              }

              "PrePopulate and ITSA mandation status are on and there is PrePop data" when {
                "redirect to SPSHandoff controller after saving prepop informtion" in {
                  mockNinoAndUtrRetrieval()
                  mockLookupUserWithUtr(testNino)(testUtr, testFullName)
                  setupMockGetSubscriptionNotFound(testNino)
                  mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibleWithPrepopData)))
                  mockRetrieveReferenceSuccess(testUtr)(testReference)
                  setupMockSubscriptionDetailsSaveFunctions()
                  setupMockPrePopulateSave(testReference)
                  mockGetMandationStatus(Mandated, Voluntary)

                  enable(PrePopulate)

                  val result = await(testHomeController().index(fakeRequest))
                  status(result) must be(Status.SEE_OTHER)
                  redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url

                  verifyPrePopulationSave(1, testReference)
                  verifyGetThrottleStatusCalls(times(1))
                  result.session(fakeRequest).data must contain(ELIGIBLE_NEXT_YEAR_ONLY -> "false")
                  result.session(fakeRequest).data must contain(MANDATED_CURRENT_YEAR -> "true")
                  result.session(fakeRequest).data must contain(MANDATED_NEXT_YEAR -> "false")
                }
              }

            }
          }

          "the user does not have a utr" when {
            "the user has a matching utr in CID against their NINO" when {
              "redirect to SPSHandoff controller" in {
                mockNinoRetrieval()
                mockLookupUserWithUtr(testNino)(testUtr, testFullName)
                setupMockGetSubscriptionNotFound(testNino)
                mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibleWithoutPrepopData)))
                mockRetrieveReferenceSuccess(testUtr)(testReference)
                mockGetMandationStatus(Voluntary, Voluntary)

                val result = await(testHomeController().index(fakeRequest))

                status(result) mustBe SEE_OTHER
                redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url

                session(result).get(ITSASessionKeys.UTR) mustBe Some(testUtr)
                session(result).get(ITSASessionKeys.FULLNAME) mustBe Some(testFullName)

                verifyGetThrottleStatusCalls(times(1))
                result.session(fakeRequest).data must contain(ELIGIBLE_NEXT_YEAR_ONLY -> "false")
                result.session(fakeRequest).data must contain(MANDATED_CURRENT_YEAR -> "false")
                result.session(fakeRequest).data must contain(MANDATED_NEXT_YEAR -> "false")
              }
            }

            "the user does not have a matching utr in CID" should {
              "redirect to the no SA page" in {
                mockNinoRetrieval()
                mockLookupUserWithoutUtr(testNino)
                setupMockGetSubscriptionNotFound(testNino)
                mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibleWithoutPrepopData)))

                val result = testHomeController().index()(fakeRequest)

                status(result) mustBe SEE_OTHER
                redirectLocation(result).get mustBe controllers.usermatching.routes.NoSAController.show.url

                session(result).get(ITSASessionKeys.UTR) mustBe None
                session(result).get(ITSASessionKeys.FULLNAME) mustBe None
                verifyGetThrottleStatusCalls(never())
              }
            }
          }

        }

        "the user is not eligible this year, but is eligible next year" when {
          "user has prepop and prepop is enabled" should {
            "redirect to the Cannot Sign Up This Year page" in {
              mockNinoAndUtrRetrieval()
              mockLookupUserWithUtr(testNino)(testUtr, testFullName)
              setupMockGetSubscriptionNotFound(testNino)
              mockRetrieveReferenceSuccess(testUtr)(testReference)
              mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibleNextYearOnlyWithPrepopData)))
              setupMockPrePopulateSave(testReference)
              enable(PrePopulate)
              mockGetMandationStatus(Voluntary, Voluntary)

              val result = await(testHomeController().index(fakeRequest))
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(controllers.individual.eligibility.routes.CannotSignUpThisYearController.show.url)
              verifyGetThrottleStatusCalls(times(1))
              verifyPrePopulationSave(1, testReference)
              result.session(fakeRequest).data must contain(ELIGIBLE_NEXT_YEAR_ONLY -> "true")
              result.session(fakeRequest).data must contain(MANDATED_CURRENT_YEAR -> "false")
              result.session(fakeRequest).data must contain(MANDATED_NEXT_YEAR -> "false")
            }
          }
          "user has no prepop and prepop is enabled" should {
            "redirect to the Cannot Sign Up This Year page" in {
              mockNinoAndUtrRetrieval()
              mockLookupUserWithUtr(testNino)(testUtr, testFullName)
              setupMockGetSubscriptionNotFound(testNino)
              mockRetrieveReferenceSuccess(testUtr)(testReference)
              mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibleNextYearOnly)))
              mockGetMandationStatus(Voluntary, Voluntary)

              val result = await(testHomeController().index(fakeRequest))
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(controllers.individual.eligibility.routes.CannotSignUpThisYearController.show.url)
              verifyGetThrottleStatusCalls(times(1))
              verifyPrePopulationSave(0, testReference)
              result.session(fakeRequest).data must contain(ELIGIBLE_NEXT_YEAR_ONLY -> "true")
              result.session(fakeRequest).data must contain(MANDATED_CURRENT_YEAR -> "false")
              result.session(fakeRequest).data must contain(MANDATED_NEXT_YEAR -> "false")
            }
          }
        }

        "the user is not eligible" should {
          "redirect to the Not eligible page" in {
            mockNinoAndUtrRetrieval()
            mockLookupUserWithUtr(testNino)(testUtr, testFullName)
            setupMockGetSubscriptionNotFound(testNino)
            mockRetrieveReferenceSuccess(testUtr)(testReference)
            mockGetEligibilityStatus(testUtr)(Future.successful(Right(ineligible)))

            val result = await(testHomeController().index(fakeRequest))
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.eligibility.routes.NotEligibleForIncomeTaxController.show().url)
            verifyGetThrottleStatusCalls(times(1))
          }
        }
      }

      "the call to check the user's subscription status fails" should {
        "return an error page" in {
          mockNinoAndUtrRetrieval()
          mockLookupUserWithUtr(testNino)(testUtr, testFullName)
          setupMockGetSubscriptionFailure(testNino)

          intercept[InternalServerException](await(testHomeController().index(fakeRequest)))
          verifyGetThrottleStatusCalls(times(1))
        }
      }
    }

    "the user does not have a nino" should {
      "throw an InternalServerException describing the issue" in {
        mockIndividualWithNoEnrolments()

        intercept[InternalServerException](await(testHomeController().index(fakeRequest)))
          .message mustBe "[HomeController][index] - Could not retrieve nino from user"
        verifyGetThrottleStatusCalls(never())
      }
    }
  }

  authorisationTests()

}
