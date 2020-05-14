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

package helpers

import java.util.UUID

import auth.individual.{JourneyState, Registration, SignUp, UserMatching}
import config.AppConfig
import config.featureswitch.{FeatureSwitch, FeatureSwitching}
import forms.individual.business._
import forms.individual.incomesource.{AreYouSelfEmployedForm, RentUkPropertyForm}
import forms.usermatching.UserDetailsForm
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuditStub, WireMockMethods}
import models.common.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingYearModel, BusinessNameModel}
import models.individual.business._
import models.individual.incomesource.{AreYouSelfEmployedModel, RentUkPropertyModel}
import models.usermatching.UserDetailsModel
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api._
import play.api.data.Form
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.json.{JsArray, JsValue, Writes}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import utilities.ITSASessionKeys._

trait ComponentSpecBase extends UnitSpec with GivenWhenThen with TestSuite
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with CustomMatchers with WireMockMethods with FeatureSwitching with SessionCookieBaker {

  lazy val mockHost: String = WiremockHelper.wiremockHost
  lazy val mockPort: String = WiremockHelper.wiremockPort.toString
  lazy val mockUrl = s"http://$mockHost:$mockPort"

  override lazy val cookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  def config: Map[String, String] = Map(
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "microservice.services.subscription-service.host" -> mockHost,
    "microservice.services.subscription-service.port" -> mockPort,
    "microservice.services.session-cache.host" -> mockHost,
    "microservice.services.session-cache.port" -> mockPort,
    "microservice.services.preferences.host" -> mockHost,
    "microservice.services.preferences.port" -> mockPort,
    "microservice.services.preferences-frontend.host" -> mockHost,
    "microservice.services.preferences-frontend.port" -> mockPort,
    "preferences-frontend.url" -> mockUrl,
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "microservice.services.government-gateway.host" -> mockHost,
    "microservice.services.government-gateway.port" -> mockPort,
    "microservice.services.gg-authentication.host" -> mockHost,
    "microservice.services.gg-authentication.port" -> mockPort,
    "microservice.services.authenticator.host" -> mockHost,
    "microservice.services.authenticator.port" -> mockPort,
    "microservice.services.citizen-details.host" -> mockHost,
    "microservice.services.citizen-details.port" -> mockPort,
    "microservice.services.address-lookup-frontend.host" -> mockHost,
    "microservice.services.address-lookup-frontend.port" -> mockPort,
    "microservice.services.tax-enrolments.host" -> mockHost,
    "microservice.services.tax-enrolments.port" -> mockPort,
    "microservice.services.income-tax-subscription-eligibility.host" -> mockHost,
    "microservice.services.income-tax-subscription-eligibility.port" -> mockPort,
    "microservice.services.enrolment-store-proxy.host" -> mockHost,
    "microservice.services.enrolment-store-proxy.port" -> mockPort,
    "microservice.services.users-groups-search.host" -> mockHost,
    "microservice.services.users-groups-search.port" -> mockPort
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWiremock()
    AuditStub.stubAuditing()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
    FeatureSwitch.switches foreach disable
  }

  object IncomeTaxSubscriptionFrontend extends UserMatchingIntegrationRequestSupport {
    val csrfToken: String = UUID.randomUUID().toString

    def get(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = await(
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map(JourneyStateKey -> SignUp.name) ++ additionalCookies))
        .get()
    )

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty)(body: Map[String, Seq[String]]): WSResponse = await(
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map(JourneyStateKey -> SignUp.name) ++ additionalCookies), "Csrf-Token" -> "nocheck")
        .post(body)
    )

    def startPage(): WSResponse = get("/")

    def preferences(): WSResponse = get("/preferences")

    def paperlessError(): WSResponse = get("/paperless-error")

    def callback(): WSResponse = get("/callback")

    def indexPage(): WSResponse = get("/index")

    def income(): WSResponse = get("/income")

    def rentUkProperty(): WSResponse = get("/rent-uk-property")

    def areYouSelfEmployed(): WSResponse = get("/are-you-self-employed")

    def submitAreYouSelfEmployed(inEditMode: Boolean, request: Option[AreYouSelfEmployedModel]): WSResponse = {
      val uri = s"/are-you-self-employed?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AreYouSelfEmployedForm.areYouSelfEmployedForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def thankYou(): WSResponse = get("/thank-you")

    def cannotSignUp(): WSResponse = get("/error/cannot-sign-up")

    def cannotUseService(): WSResponse = get("/error/cannot-use-service")

    def notEligibleForIncomeTax(): WSResponse = get("/cannot-use-service-yet")

    def agentNotEligibleForIncomeTax(): WSResponse = get("/cannot-use-service-yet")

    def cannotReportYet(): WSResponse = get("/error/cannot-report-yet")

    def submitCannotReportYet(editMode: Boolean): WSResponse =
      post(s"/error/cannot-report-yet${if (editMode) "?editMode=true" else ""}")(Map.empty)

    def sessionTimeout(): WSResponse = get("/session-timeout")

    def notAuthorised(): WSResponse = get("/not-authorised")

    def signIn(): WSResponse = get("/sign-in")

    def signOut(origin: String): WSResponse = get(s"/logout?origin=$origin")

    def alreadyEnrolled(): WSResponse = get("/already-enrolled")

    def checkYourAnswers(): WSResponse = get("/check-your-answers")

    def submitCheckYourAnswers(): WSResponse = post("/check-your-answers")(Map.empty)

    def submitMainIncomeError(): WSResponse = post("/error/main-income")(Map.empty)

    def submitPaperlessError(sessionKeys: Map[String, String] = Map.empty): WSResponse = post(
      uri = "/paperless-error",
      additionalCookies = sessionKeys
    )(Map.empty)

    def matchTaxYear(): WSResponse = get("/business/match-to-tax-year")

    def businessAccountingPeriodDates(): WSResponse = get("/business/accounting-period-dates")

    def businessStartDate(): WSResponse = get("/business/start-date", Map(JourneyStateKey -> Registration.name))

    def businessAccountingMethod(): WSResponse = get("/business/accounting-method")

    def propertyAccountingMethod(): WSResponse = get("/business/accounting-method-property")

    def businessName(): WSResponse = get("/business/name")

    def businessAddress(state: JourneyState): WSResponse = get("/business/address", Map(JourneyStateKey -> state.name))

    def submitBusinessAddress(editMode: Boolean, state: JourneyState): WSResponse =
      post(s"/business/address${if (editMode) "?editMode=true" else ""}", Map(JourneyStateKey -> state.name))(Map.empty)

    def businessAddressInit(state: JourneyState): WSResponse = get("/business/address/init", Map(JourneyStateKey -> state.name))

    def businessAddressCallback(editMode: Boolean, state: JourneyState): WSResponse = {
      val url = s"/business/address/callback${if (editMode) "/edit" else ""}?id=$testId"
      get(url, Map(JourneyStateKey -> state.name))
    }

    def businessPhoneNumber(): WSResponse = get("/business/phone-number", Map(JourneyStateKey -> Registration.name))

    def maintenance(): WSResponse = get("/error/maintenance")

    def noSA(): WSResponse = get("/register-for-SA")

    def submitMatchTaxYear(inEditMode: Boolean, request: Option[MatchTaxYearModel]): WSResponse = {
      val uri = s"/business/match-to-tax-year?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model => MatchTaxYearForm.matchTaxYearForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        ))
    }

    def accountingYear(): WSResponse = get("/business/what-year-to-sign-up")

    def submitAccountingYear(inEditMode: Boolean, request: Option[AccountingYearModel]): WSResponse = {
      val uri = s"/business/what-year-to-sign-up?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model => AccountingYearForm.accountingYearForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        ))
    }

    def submitMaintenance(): WSResponse = post("/error/maintenance")(Map.empty)

    def claimSubscription(): WSResponse = {
      val uri = s"/claim-subscription"
      get(uri)
    }

    def submitRentUkProperty(inEditMode: Boolean, request: Option[RentUkPropertyModel]): WSResponse = {
      val uri = s"/rent-uk-property?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            RentUkPropertyForm.rentUkPropertyForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def confirmation(): WSResponse = get("/confirmation")

    def submitAccountingPeriodDates(inEditMode: Boolean, request: Option[AccountingPeriodModel]): WSResponse = {
      val uri = s"/business/accounting-period-dates?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingPeriodDateForm.accountingPeriodDateForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def submitBusinessName(inEditMode: Boolean, request: Option[BusinessNameModel]): WSResponse = {
      val uri = s"/business/name?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            BusinessNameForm.businessNameValidationForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def submitBusinessPhoneNumber(inEditMode: Boolean, request: Option[BusinessPhoneNumberModel]): WSResponse = {
      val uri = s"/business/phone-number?editMode=$inEditMode"
      post(uri, Map(JourneyStateKey -> Registration.name))(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            BusinessPhoneNumberForm.businessPhoneNumberValidationForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def submitBusinessStartDate(inEditMode: Boolean, request: Option[BusinessStartDateModel]): WSResponse = {
      val uri = s"/business/start-date?editMode=$inEditMode"
      post(uri, Map(JourneyStateKey -> Registration.name))(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            BusinessStartDateForm.businessStartDateForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def submitAccountingMethod(inEditMode: Boolean, request: Option[AccountingMethodModel]): WSResponse = {
      val uri = s"/business/accounting-method?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingMethodForm.accountingMethodForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def submitPropertyAccountingMethod(inEditMode: Boolean, request: Option[AccountingMethodPropertyModel]): WSResponse = {
      val uri = s"/business/accounting-method-property?editMode-=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def iv(): WSResponse = get("/iv")

    def showUserDetails(): WSResponse = get("/user-details", Map(JourneyStateKey -> UserMatching.name))

    def submitUserDetails(newSubmission: Option[UserDetailsModel], storedSubmission: Option[UserDetailsModel]): WSResponse =
      post("/user-details", Map(JourneyStateKey -> UserMatching.name).addUserDetails(storedSubmission))(
        newSubmission.fold(Map.empty: Map[String, Seq[String]])(
          cd => toFormData(UserDetailsForm.userDetailsValidationForm, cd)
        )
      )

    def showUserDetailsError(): WSResponse = get("/error/user-details", Map(JourneyStateKey -> UserMatching.name))

    def showUserDetailsLockout(): WSResponse = get("/error/lockout", Map(JourneyStateKey -> UserMatching.name))

    def submitConfirmUser(previouslyFailedAttempts: Int = 0,
                          storedUserDetails: Option[UserDetailsModel] = Some(IntegrationTestModels.testUserDetails)): WSResponse = {
      val failedAttemptCounter: Map[String, String] = previouslyFailedAttempts match {
        case 0 => Map.empty
        case count => Map(FailedUserMatching -> previouslyFailedAttempts.toString)
      }
      post("/confirm-user",
        additionalCookies = Map(JourneyStateKey -> UserMatching.name) ++ failedAttemptCounter.addUserDetails(storedUserDetails)
      )(Map.empty)
    }

    def showAffinityGroupError(): WSResponse = get("/error/affinity-group")

  }

  def toFormData[T](form: Form[T], data: T): Map[String, Seq[String]] =
    form.fill(data).data map { case (k, v) => k -> Seq(v) }

  implicit val nilWrites: Writes[Nil.type] = new Writes[Nil.type] {
    override def writes(o: Nil.type): JsValue = JsArray()
  }

  def removeHtmlMarkup(stringWithMarkup: String): String =
    stringWithMarkup.replaceAll("<.+?>", " ").replaceAll("[\\s]{2,}", " ").trim

}
