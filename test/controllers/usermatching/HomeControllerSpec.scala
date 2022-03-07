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
import config.featureswitch.FeatureSwitch.{PrePopulate, SPSEnabled}
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import models._
import models.common.business.{AccountingMethodModel, BusinessNameModel, BusinessTradeNameModel, SelfEmploymentData}
import models.common.{OverseasPropertyModel, PropertyModel}
import org.mockito.Mockito.reset
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{await, _}
import services.mocks.{MockCitizenDetailsService, MockGetEligibilityStatusService, MockSubscriptionDetailsService, MockSubscriptionService}
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
  with MockAuditingService
  with FeatureSwitching {

  private val selfEmploymentsWithAccountingMethod = Some(List(
    PrePopSelfEmployment(
      Some("testBusinessName1"), "testBusinessTradeName1", None, None, None, Some(Accruals)),
    PrePopSelfEmployment(
      Some("testBusinessName2"), "testBusinessTradeName2", None, None, None, Some(Cash))
  ))
  private val testSelfEmployments = List(
    SelfEmploymentData(
      "",
      None,
      Some(BusinessNameModel("testBusinessName1")),
      Some(BusinessTradeNameModel("testBusinessTradeName1"))),
    SelfEmploymentData(
      "",
      None,
      Some(BusinessNameModel("testBusinessName2")),
      Some(BusinessTradeNameModel("testBusinessTradeName2")))
  )
  private val testBusinessAccountingMethod = AccountingMethodModel(Accruals) // first found, see selfEmploymentsWithAccountingMethod

  private val ukProperty = Some(PrePopUkProperty(Some(DateModel("1", "1", "2001")), Some(Cash)))
  private val testUkProperty = PropertyModel(Some(Cash), Some(DateModel("1", "1", "2001")), false)

  private val overseasProperty = Some(PrePopOverseasProperty(Some(DateModel("2", "2", "2002")), Some(Accruals)))
  private val testOverseasProperty = OverseasPropertyModel(Some(Accruals), Some(DateModel("2", "2", "2002")), false)

  private val prePop = PrePopData(selfEmploymentsWithAccountingMethod, ukProperty, overseasProperty)

  private val eligibleWithoutPrepopData = EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, None)
  private val eligibleWithPrepopData = EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, Some(prePop))
  private val ineligible = EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = false, None)

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> testHomeController(showStartPage = false).index()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)
    disable(SPSEnabled)
    disable(PrePopulate)
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
          mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
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
                  "feature switch SPSEnabled is enabled" should {
                    "redirect to SPSHandoff controller" in {
                      mockNinoAndUtrRetrieval()
                      mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
                      setupMockGetSubscriptionNotFound(testNino)
                      mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithoutPrepopData))
                      mockRetrievePrePopFlag(None)

                      enable(SPSEnabled)
                      enable(PrePopulate)

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
                  "feature switch SPSEnabled is disabled" should {
                    "redirect to preference controller" in {
                      mockNinoAndUtrRetrieval()
                      mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
                      setupMockGetSubscriptionNotFound(testNino)
                      mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithoutPrepopData))

                      enable(PrePopulate)

                      val result = await(testHomeController().index(fakeRequest))
                      status(result) must be(Status.SEE_OTHER)
                      redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences.url

                      verifySubscriptionDetailsSaveWithField(0, OverseasProperty)
                      verifySubscriptionDetailsSaveWithField(0, Property)
                      verifySubscriptionDetailsSaveWithField(0, BusinessesKey)
                      verifySubscriptionDetailsSaveWithField(0, BusinessAccountingMethod)
                      verifySubscriptionDetailsSaveWithField(0, subscriptionId)
                    }
                  }
                }
                "feature switch PrePopulate is enabled and there is PrePop" when {
                  "feature switch SPSEnabled is enabled" should {
                    "redirect to SPSHandoff controller after saving prepop informtion" in {
                      mockNinoAndUtrRetrieval()
                      mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
                      setupMockGetSubscriptionNotFound(testNino)
                      mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithPrepopData))
                      mockRetrievePrePopFlag(None)
                      mockRetrieveReferenceSuccess(testUtr)(testReference)
                      setupMockSubscriptionDetailsSaveFunctions()

                      enable(SPSEnabled)
                      enable(PrePopulate)

                      val result = await(testHomeController().index(fakeRequest))
                      status(result) must be(Status.SEE_OTHER)
                      redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url

                      verifySubscriptionDetailsSaveWithField(1, OverseasProperty, testOverseasProperty)
                      verifySubscriptionDetailsSaveWithField(1, Property, testUkProperty)
                      verifySubscriptionDetailsSaveWithField(1, BusinessesKey, SelfEmploymentListMatcher(testSelfEmployments))
                      verifySubscriptionDetailsSaveWithField(1, BusinessAccountingMethod, testBusinessAccountingMethod)
                      verifySubscriptionDetailsSaveWithField(1, subscriptionId)
                    }
                  }
                  "feature switch SPSEnabled is disabled" should {
                    "redirect to preference controller" in {
                      mockNinoAndUtrRetrieval()
                      mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
                      setupMockGetSubscriptionNotFound(testNino)
                      mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithPrepopData))
                      mockRetrievePrePopFlag(None)
                      mockRetrieveReferenceSuccess(testUtr)(testReference)
                      setupMockSubscriptionDetailsSaveFunctions()

                      enable(PrePopulate)

                      val result = await(testHomeController().index(fakeRequest))
                      status(result) must be(Status.SEE_OTHER)
                      redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences.url

                      verifySubscriptionDetailsSaveWithField(1, OverseasProperty, testOverseasProperty)
                      verifySubscriptionDetailsSaveWithField(1, Property, testUkProperty)
                      verifySubscriptionDetailsSaveWithField(1, BusinessesKey, SelfEmploymentListMatcher(testSelfEmployments))
                      verifySubscriptionDetailsSaveWithField(1, BusinessAccountingMethod, testBusinessAccountingMethod)
                      verifySubscriptionDetailsSaveWithField(1, subscriptionId)
                    }
                  }
                }
                "feature switch PrePopulate is enabled and there is PrePop but the user has already been pre populated" when {
                  "feature switch SPSEnabled is enabled" should {
                    "redirect to SPSHandoff controller after saving prepop informtion" in {
                      mockNinoAndUtrRetrieval()
                      mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
                      setupMockGetSubscriptionNotFound(testNino)
                      mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithPrepopData))
                      mockRetrievePrePopFlag(Some(true))
                      mockRetrieveReferenceSuccess(testUtr)(testReference)
                      setupMockSubscriptionDetailsSaveFunctions()

                      enable(SPSEnabled)
                      enable(PrePopulate)

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
                  "feature switch SPSEnabled is disabled" should {
                    "redirect to preference controller" in {
                      mockNinoAndUtrRetrieval()
                      mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
                      setupMockGetSubscriptionNotFound(testNino)
                      mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithPrepopData))
                      mockRetrievePrePopFlag(Some(true))
                      mockRetrieveReferenceSuccess(testUtr)(testReference)

                      enable(PrePopulate)

                      val result = await(testHomeController().index(fakeRequest))
                      status(result) must be(Status.SEE_OTHER)
                      redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences.url

                      verifySubscriptionDetailsSaveWithField(0, OverseasProperty)
                      verifySubscriptionDetailsSaveWithField(0, Property)
                      verifySubscriptionDetailsSaveWithField(0, BusinessesKey)
                      verifySubscriptionDetailsSaveWithField(0, BusinessAccountingMethod)
                      verifySubscriptionDetailsSaveWithField(0, subscriptionId)
                    }
                  }
                }
                "feature switch PrePopulate is disabled" when {
                  "feature switch SPSEnabled is enabled" should {
                    "redirect to SPSHandoff controller" in {
                      mockNinoAndUtrRetrieval()
                      mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
                      setupMockGetSubscriptionNotFound(testNino)
                      mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithPrepopData))

                      enable(SPSEnabled)

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
                  "feature switch SPSEnabled is disabled" should {
                    "redirect to preference controller" in {
                      mockNinoAndUtrRetrieval()
                      mockResolveIdentifiers(Some(testNino), Some(testUtr))(Some(testNino), Some(testUtr))
                      setupMockGetSubscriptionNotFound(testNino)
                      mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithoutPrepopData))

                      val result = await(testHomeController().index(fakeRequest))
                      status(result) must be(Status.SEE_OTHER)
                      redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences.url

                      verifySubscriptionDetailsSaveWithField(0, OverseasProperty)
                      verifySubscriptionDetailsSaveWithField(0, Property)
                      verifySubscriptionDetailsSaveWithField(0, BusinessesKey)
                      verifySubscriptionDetailsSaveWithField(0, BusinessAccountingMethod)
                      verifySubscriptionDetailsSaveWithField(0, subscriptionId)
                    }
                  }
                }
              }
            }

            "the user does not have a utr" when {
              "the user has a matching utr in CID against their NINO" when {
                "feature switch SPSEnabled is enabled" should {
                  "redirect to SPSHandoff controller" in {
                    mockNinoRetrieval()
                    mockResolveIdentifiers(Some(testNino), None)(Some(testNino), Some(testUtr))
                    setupMockGetSubscriptionNotFound(testNino)
                    mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithoutPrepopData))

                    enable(SPSEnabled)

                    val result = await(testHomeController().index(fakeRequest))

                    status(result) mustBe SEE_OTHER
                    redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url

                    session(result).get(ITSASessionKeys.UTR) mustBe Some(testUtr)
                  }
                }

                "feature switch SPSEnabled is disabled" should {
                  "redirect to preference controller" in {
                    mockNinoRetrieval()
                    mockResolveIdentifiers(Some(testNino), None)(Some(testNino), Some(testUtr))
                    setupMockGetSubscriptionNotFound(testNino)
                    mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithoutPrepopData))

                    val result = await(testHomeController().index(fakeRequest))

                    status(result) mustBe SEE_OTHER
                    redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences.url

                    session(result).get(ITSASessionKeys.UTR) mustBe Some(testUtr)
                  }
                }
              }

              "the user does not have a matching utr in CID" should {

                "redirect to the no SA page" in {
                  mockNinoRetrieval()
                  mockResolveIdentifiers(Some(testNino), None)(Some(testNino), None)
                  setupMockGetSubscriptionNotFound(testNino)
                  mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithoutPrepopData))

                  val result = testHomeController().index()(userMatchingRequest)

                  status(result) mustBe SEE_OTHER
                  redirectLocation(result).get mustBe controllers.usermatching.routes.NoSAController.show.url

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
            mockGetEligibilityStatus(testUtr)(Future.successful(ineligible))

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
          mockRetrieveReferenceSuccess(testUtr)(testReference)

          val result = testHomeController().index(fakeRequest)

          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe controllers.individual.subscription.routes.ClaimSubscriptionController.claim.url

          verifySubscriptionDetailsSave(MtditId, 1)
        }
      }
      "the user does not already have an MTDIT subscription on ETMP" when {
        "the user is eligible" when {
          "the user does not have a current unauthorised subscription journey" when {
            "redirect to the sign up journey" when {
              "feature switch SPSEnabled is enabled" should {
                "redirect to SPSHandoff controller" in {


                  mockUtrRetrieval()
                  mockResolveIdentifiers(None, Some(testUtr))(Some(testNino), Some(testUtr))
                  setupMockGetSubscriptionNotFound(testNino)
                  mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithoutPrepopData))
                  enable(SPSEnabled)

                  val result = await(testHomeController().index(fakeRequest))
                  status(result) must be(Status.SEE_OTHER)
                  redirectLocation(result).get mustBe controllers.individual.sps.routes.SPSHandoffController.redirectToSPS.url
                  session(result).get(ITSASessionKeys.NINO) must contain(testNino)
                }
              }

              "feature switch SPSEnabled is disabled" should {
                "redirect to Preference controller" in {


                  mockUtrRetrieval()
                  mockResolveIdentifiers(None, Some(testUtr))(Some(testNino), Some(testUtr))
                  setupMockGetSubscriptionNotFound(testNino)
                  mockGetEligibilityStatus(testUtr)(Future.successful(eligibleWithoutPrepopData))

                  val result = await(testHomeController().index(fakeRequest))
                  status(result) must be(Status.SEE_OTHER)
                  redirectLocation(result).get mustBe controllers.individual.routes.PreferencesController.checkPreferences.url
                  session(result).get(ITSASessionKeys.NINO) must contain(testNino)
                }
              }
            }
          }
        }
        "the user is not eligible" should {
          "redirect to the Not eligible page" in {
            mockUtrRetrieval()
            mockResolveIdentifiers(None, Some(testUtr))(Some(testNino), Some(testUtr))
            setupMockGetSubscriptionNotFound(testNino)
            mockGetEligibilityStatus(testUtr)(Future.successful(ineligible))

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
