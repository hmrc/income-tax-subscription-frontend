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

package controllers.usermatching

import agent.audit.mocks.MockAuditingService
import config.MockConfig
import config.featureswitch.FeatureSwitch.PrePopulate
import controllers.ControllerBaseSpec
import models._
import org.mockito.Mockito.reset
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{await, _}
import services.mocks._
import uk.gov.hmrc.http.InternalServerException
import utilities.ITSASessionKeys
import utilities.SubscriptionDataKeys._
import utilities.individual.TestConstants

import scala.concurrent.Future

class HomeControllerSpec extends ControllerBaseSpec
  with MockSubscriptionService
  with MockSubscriptionDetailsService
  with MockCitizenDetailsService
  with MockGetEligibilityStatusService
  with MockPrePopulationService
  with MockAuditingService {

  private val eligibleWithoutPrepopData = EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, None)
  private val eligibleWithPrepopData = EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, Some(mock[PrePopData]))
  private val ineligible = EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = false, None)

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> testHomeController(showStartPage = false).index()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)
    disable(PrePopulate)
    mockNinoRetrieval()
  }

  def testHomeController(showStartPage: Boolean = true): HomeController = new HomeController(
    mockAuditingService,
    mockAuthService,
    mockCitizenDetailsService,
    mockGetEligibilityStatusService,
    MockSubscriptionDetailsService,
    mockPrePopulationService,
    mockSubscriptionService
  )(implicitly, MockConfig, mockMessagesControllerComponents)

  import TestConstants.{testNino, testReference, testUtr}

  "Calling the home action of the Home controller with an authorised user" when {
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
          mockLookupUserWithUtr(testNino)(testUtr)
          setupMockGetSubscriptionFound(testNino)
          setupMockSubscriptionDetailsSaveFunctions()
          mockRetrieveReferenceSuccess(testUtr)(testReference)

          val result = testHomeController().index(fakeRequest)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe controllers.individual.subscription.routes.ClaimSubscriptionController.claim.url

          verifySubscriptionDetailsSave(MtditId, 1)
        }
      }
      "the user does not already have an MTDIT subscription on ETMP" when {
        "the user is eligible " when {
          "the user does not have a current unauthorised subscription journey" when {
            "the user has a UTR" should {
              "redirect to the sign up journey" when {
                "feature switch PrePopulate is enabled but there is no PrePop" when {
                  "redirect to SPSHandoff controller" in {
                    mockNinoAndUtrRetrieval()
                    mockLookupUserWithUtr(testNino)(testUtr)
                    setupMockGetSubscriptionNotFound(testNino)
                    mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibleWithoutPrepopData)))

                    enable(PrePopulate)

                    val result = await(testHomeController().index(fakeRequest))
                    status(result) must be(Status.SEE_OTHER)
                    redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url

                    verifyPrePopulationSave(0, testReference)
                  }
                }
                "feature switch PrePopulate is enabled and there is PrePop" when {
                  "redirect to SPSHandoff controller after saving prepop informtion" in {
                    mockNinoAndUtrRetrieval()
                    mockLookupUserWithUtr(testNino)(testUtr)
                    setupMockGetSubscriptionNotFound(testNino)
                    mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibleWithPrepopData)))
                    mockRetrieveReferenceSuccess(testUtr)(testReference)
                    setupMockSubscriptionDetailsSaveFunctions()
                    setupMockPrePopulateSave(testReference)

                    enable(PrePopulate)

                    val result = await(testHomeController().index(fakeRequest))
                    status(result) must be(Status.SEE_OTHER)
                    redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url

                    verifyPrePopulationSave(1, testReference)
                  }
                }
                "feature switch PrePopulate is disabled" when {
                  "redirect to SPSHandoff controller" in {
                    mockNinoAndUtrRetrieval()
                    mockLookupUserWithUtr(testNino)(testUtr)
                    setupMockGetSubscriptionNotFound(testNino)
                    mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibleWithPrepopData)))

                    val result = await(testHomeController().index(fakeRequest))
                    status(result) must be(Status.SEE_OTHER)
                    redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url

                    verifySubscriptionDetailsSaveWithField(0, OverseasProperty)
                    verifySubscriptionDetailsSaveWithField(0, Property)
                    verifySubscriptionDetailsSaveWithField(0, BusinessesKey)
                    verifySubscriptionDetailsSaveWithField(0, BusinessAccountingMethod)
                    verifySubscriptionDetailsSaveWithField(0, subscriptionId)
                  }
                }
              }
            }

            "the user does not have a utr" when {
              "the user has a matching utr in CID against their NINO" when {
                "redirect to SPSHandoff controller" in {
                  mockNinoRetrieval()
                  mockLookupUserWithUtr(testNino)(testUtr)
                  setupMockGetSubscriptionNotFound(testNino)
                  mockGetEligibilityStatus(testUtr)(Future.successful(Right(eligibleWithoutPrepopData)))

                  val result = await(testHomeController().index(fakeRequest))

                  status(result) mustBe SEE_OTHER
                  redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url

                  session(result).get(ITSASessionKeys.UTR) mustBe Some(testUtr)
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
                }
              }
            }
          }
        }
        "the user is not eligible" should {
          "redirect to the Not eligible page" in {
            mockNinoAndUtrRetrieval()
            mockLookupUserWithUtr(testNino)(testUtr)
            setupMockGetSubscriptionNotFound(testNino)
            mockGetEligibilityStatus(testUtr)(Future.successful(Right(ineligible)))

            val result = await(testHomeController().index(fakeRequest))
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.eligibility.routes.NotEligibleForIncomeTaxController.show().url)
          }
        }
      }
      "the call to check the user's subscription status fails" should {
        "return an error page" in {
          mockNinoAndUtrRetrieval()
          mockLookupUserWithUtr(testNino)(testUtr)
          setupMockGetSubscriptionFailure(testNino)

          intercept[InternalServerException](await(testHomeController().index(fakeRequest)))
        }
      }
    }
    "the user does not have a nino" should {
      "throw an InternalServerException describing the issue" in {
        mockIndividualWithNoEnrolments()

        intercept[InternalServerException](await(testHomeController().index(fakeRequest)))
          .message mustBe "[HomeController][index] - Could not retrieve nino from user"
      }
    }
  }

  authorisationTests()

}
