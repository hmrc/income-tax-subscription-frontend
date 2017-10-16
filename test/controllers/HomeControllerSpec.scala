/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import assets.MessageLookup.FrontPage
import audit.Logging
import auth.{MockConfig, Registration, SignUp}
import config.BaseControllerConfig
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers._
import services.mocks.{MockKeystoreService, MockSubscriptionService}
import uk.gov.hmrc.http.InternalServerException
import usermatching.services.mocks.MockCitizenDetailsService
import utils.TestConstants

import scala.concurrent.Future


class HomeControllerSpec extends ControllerBaseSpec
  with MockSubscriptionService
  with MockKeystoreService
  with MockCitizenDetailsService {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> TestHomeController(showGuidance = false).index()
  )

  def mockBaseControllerConfig(showStartPage: Boolean, userMatching: Boolean, registrationFeature: Boolean): BaseControllerConfig = {
    val mockConfig = new MockConfig {
      override val showGuidance: Boolean = showStartPage
      override val userMatchingFeature: Boolean = userMatching
      override val enableRegistration: Boolean = registrationFeature
    }
    mockBaseControllerConfig(mockConfig)
  }

  def TestHomeController(showGuidance: Boolean = true, userMatchingFeature: Boolean = false, registrationFeature: Boolean = false) = new HomeController(
    mockBaseControllerConfig(showGuidance, userMatchingFeature, registrationFeature),
    messagesApi,
    mockSubscriptionService,
    MockKeystoreService,
    mockAuthService,
    mockCitizenDetailsService,
    app.injector.instanceOf[Logging]
  )

  val testNino = TestConstants.testNino
  val testUtr = TestConstants.testUtr

  "Calling the home action of the Home controller with an authorised user" when {

    "the start page (showGuidance) is enabled" should {

      lazy val result = TestHomeController(showGuidance = true).home()(subscriptionRequest)

      "Return status OK (200)" in {
        status(result) must be(Status.OK)
      }

      "Should have the page title" in {
        Jsoup.parse(contentAsString(result)).title mustBe FrontPage.title
      }
    }

    "the start page (showGuidance) is disabled" should {
      lazy val result = TestHomeController(showGuidance = false).home()(subscriptionRequest)

      "Return status SEE_OTHER (303) redirect" in {
        status(result) must be(Status.SEE_OTHER)
      }

      "Redirect to the 'Index' page" in {
        redirectLocation(result).get mustBe controllers.routes.HomeController.index().url
      }
    }
  }

  "Calling the index action of the HomeController with an authorised user with both utr and nino enrolments" should {
    def call() = TestHomeController(showGuidance = false).index()(subscriptionRequest)

    "redirect them to already subscribed page if they already has a subscription" in {
      setupMockGetSubscriptionFound(testNino)
      setupMockKeystoreSaveFunctions()

      val result = call()
      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result).get mustBe controllers.routes.ClaimSubscriptionController.claim().url

      verifyKeystore(saveSubscriptionId = 1)
    }

    "display the error page if there was an error checking the subscription" in {
      setupMockGetSubscriptionFailure(testNino)

      intercept[InternalServerException](await(call()))
    }

    // N.B. the subscribeNone case is covered below
  }

  "Calling the index action of the HomeController with an authorised user with only a nino enrolments" when {
    def call(request: Request[AnyContent] = subscriptionRequest) = TestHomeController(showGuidance = false).index()(request)

    def userSetup(): Unit = {
      import org.mockito.Mockito._
      reset(mockAuthService)
      mockNinoRetrieval()
    }

    "user is found but their utr is not in CID" when {
      "the registration feature flag is off" should {
        "redirect the user to no SA page" in {
          userSetup()
          mockLookupUserWithoutUtr(testNino)

          val result = call()

          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.routes.NoSAController.show().url

          await(result).session(subscriptionRequest).get(ITSASessionKeys.UTR) mustBe None
        }
      }
      "the registration feature flag is on" should {
        "redirect the user to the income source page with the Registration journey state added to session" in {
          userSetup()

          val result = TestHomeController(registrationFeature = true).index()(subscriptionRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe digitalcontact.controllers.routes.PreferencesController.checkPreferences().url
          await(result).session(subscriptionRequest).get(ITSASessionKeys.JourneyStateKey) must contain(Registration.name)
        }
      }
    }

    "user had successfully been through user matching" when {
      "a user with a UTR should be marked as sign up" in {
        userSetup()

        setupMockGetSubscriptionNotFound(testNino)

        val request = userMatchedRequest
        val result = call(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe digitalcontact.controllers.routes.PreferencesController.checkPreferences().url

        await(result).session(subscriptionRequest).get(ITSASessionKeys.JourneyStateKey) must contain(SignUp.name)
      }

      "the registration feature flag is off" should {
        "a user with no UTR should see the no sa page" in {
          userSetup()

          setupMockGetSubscriptionNotFound(testNino)

          val request = userMatchedRequestNoUtr
          val result = call(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.routes.NoSAController.show().url

          await(result).session(subscriptionRequest).get(ITSASessionKeys.JourneyStateKey) mustBe None
        }
      }

      "the registration feature flag is on" should {
        "a user with no UTR should be marked as registration" in {
          userSetup()

          setupMockGetSubscriptionNotFound(testNino)

          val request = userMatchedRequestNoUtr
          val result = TestHomeController(registrationFeature = true).index()(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe digitalcontact.controllers.routes.PreferencesController.checkPreferences().url

          await(result).session(subscriptionRequest).get(ITSASessionKeys.JourneyStateKey) must contain(Registration.name)
        }
      }
    }

    "user is found and their utr is in CID" should {
      "the user should be allowed to continue normally" in {
        userSetup()
        mockLookupUserWithUtr(testNino)(testUtr)

        setupMockGetSubscriptionNotFound(testNino)

        val result = call()

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe digitalcontact.controllers.routes.PreferencesController.checkPreferences().url

        await(result).session(subscriptionRequest).get(ITSASessionKeys.UTR) mustBe Some(testUtr)
      }
    }

    "user is not found in CID" should {
      "show error page" in {
        userSetup()
        mockLookupUserNotFound(testNino)

        val result = call()

        val e = intercept[InternalServerException] {
          await(result)
        }

        e.message mustBe "HomeController.checkCID: unexpected error calling the citizen details service"
      }
    }

    "calls to CID failed" should {
      "show error page" in {
        userSetup()
        mockLookupFailure(testNino)

        val result = call()

        val e = intercept[InternalServerException] {
          await(result)
        }

        e.message mustBe "HomeController.checkCID: unexpected error calling the citizen details service"
      }
    }
  }

  "Calling the index action of the HomeController with an authorised user with only a utr enrolments" should {
    def call() = TestHomeController(showGuidance = false).index()(subscriptionRequest)

    def userSetup(): Unit = {
      import org.mockito.Mockito._
      reset(mockAuthService)
      mockUtrRetrieval()
    }

    // n.b. since gateway should have used the utr to look up the nino from CID during user login
    "return an internal server error" in {
      userSetup()

      val result = call()

      val ex = intercept[InternalServerException](status(result))
      ex.message mustBe "AuthPredicates.ninoPredicate: unexpected user state, the user has a utr but no nino"
    }
  }

  "Calling the index action of the HomeController with an authorised user who does not already have a subscription" should {

    def getResult: Future[Result] = TestHomeController(showGuidance = false).index()(subscriptionRequest)

    "redirect to check preferences if the user qualifies" in {
      setupMockGetSubscriptionNotFound(testNino)

      val result = getResult

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result).get mustBe digitalcontact.controllers.routes.PreferencesController.checkPreferences().url
    }

    "redirect when auth returns an org affinity" in {
      mockNinoRetrievalWithOrg()

      val result = getResult

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.AffinityGroupErrorController.show().url
    }

    "redirect when auth returns no affinity" in {
      mockNinoRetrievalWithNoAffinity()

      val result = getResult

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.AffinityGroupErrorController.show().url
    }
  }

  "If a user doesn't have a NINO" when {

    def getResult(userMatchingFeature: Boolean): Future[Result] =
      TestHomeController(showGuidance = false, userMatchingFeature = userMatchingFeature).index()(subscriptionRequest)

    "userMatchingFeature in config is set to true" should {
      "redirect them to user details" in {
        mockIndividualWithNoEnrolments()

        val result = getResult(userMatchingFeature = true)

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result).get mustBe usermatching.controllers.routes.NinoResolverController.resolveNino().url
      }
    }

    "userMatchingFeature in config is set to false" should {
      "redirect them to iv" in {
        mockIndividualWithNoEnrolments()

        val result = getResult(userMatchingFeature = false)

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result).get mustBe usermatching.controllers.routes.NinoResolverController.resolveNino().url
      }
    }
  }

  authorisationTests()

}
