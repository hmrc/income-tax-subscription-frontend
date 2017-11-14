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

package helpers

import java.util.UUID

import core.ITSASessionKeys
import core.auth.{JourneyState, Registration, SignUp, UserMatching}
import core.config.AppConfig
import core.config.featureswitch.{FeatureSwitch, FeatureSwitching}
import helpers.IntegrationTestConstants._
import helpers.SessionCookieBaker._
import helpers.servicemocks.{AuditStub, WireMockMethods}
import incometax.business.forms._
import incometax.business.models._
import incometax.incomesource.forms.{IncomeSourceForm, OtherIncomeForm}
import incometax.incomesource.models.{IncomeSourceModel, OtherIncomeModel}
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api._
import play.api.data.Form
import play.api.http.HeaderNames
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsValue, Writes}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.play.test.UnitSpec
import usermatching.forms.UserDetailsForm
import usermatching.models.UserDetailsModel

trait ComponentSpecBase extends UnitSpec
  with GivenWhenThen with TestSuite
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with I18nSupport with CustomMatchers with WireMockMethods with FeatureSwitching {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort.toString
  val mockUrl = s"http://$mockHost:$mockPort"

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
    "microservice.services.feature-switch.show-guidance" -> "true",
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "microservice.services.gg-admin.host" -> mockHost,
    "microservice.services.gg-admin.port" -> mockPort,
    "microservice.services.government-gateway.host" -> mockHost,
    "microservice.services.government-gateway.port" -> mockPort,
    "microservice.services.gg-authentication.host" -> mockHost,
    "microservice.services.gg-authentication.port" -> mockPort,
    "microservice.services.authenticator.host" -> mockHost,
    "microservice.services.authenticator.port" -> mockPort,
    "microservice.services.citizen-details.host" -> mockHost,
    "microservice.services.citizen-details.port" -> mockPort,
    "microservice.services.address-lookup-frontend.host" -> mockHost,
    "microservice.services.address-lookup-frontend.port" -> mockPort
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build

  implicit lazy val appConfig = app.injector.instanceOf[AppConfig]

  override lazy val messagesApi = app.injector.instanceOf[MessagesApi]

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

  object IncomeTaxSubscriptionFrontend {
    val csrfToken = UUID.randomUUID().toString

    def get(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = await(
      buildClient(uri)
        .withHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map(ITSASessionKeys.JourneyStateKey -> SignUp.name) ++ additionalCookies))
        .get()
    )

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty)(body: Map[String, Seq[String]]): WSResponse = await(
      buildClient(uri)
        .withHeaders(HeaderNames.COOKIE -> SessionCookieBaker.bakeSessionCookie(Map(ITSASessionKeys.JourneyStateKey -> SignUp.name) ++ additionalCookies), "Csrf-Token" -> "nocheck")
        .post(body)
    )

    def startPage(): WSResponse = get("/")

    def preferences(): WSResponse = get("/preferences")

    def paperlessError(): WSResponse = get("/paperless-error")

    def callback(): WSResponse = get("/callback")

    def indexPage(): WSResponse = get("/index")

    def income(): WSResponse = get("/income")

    def otherIncome(): WSResponse = get("/income-other")

    def otherIncomeError(): WSResponse = get("/error/other-income")

    def terms(): WSResponse = get("/terms")

    def sessionTimeout(): WSResponse = get("/session-timeout")

    def notAuthorised(): WSResponse = get("/not-authorised")

    def thankYou(): WSResponse = get("/thankyou")

    def feedback(): WSResponse = get("/feedback-submitted")

    def signIn(): WSResponse = get("/sign-in")

    def signOut(): WSResponse = get("/logout")

    def alreadyEnrolled(): WSResponse = get("/already-enrolled")

    def checkYourAnswers(): WSResponse = get("/check-your-answers")

    def submitCheckYourAnswers(): WSResponse = post("/check-your-answers")(Map.empty)

    def submitMainIncomeError(): WSResponse = post("/error/main-income")(Map.empty)

    def submitOtherIncomeError(): WSResponse = post("/error/other-income")(Map.empty)

    def submitPaperlessError(sessionKeys: Map[String, String] = Map.empty): WSResponse = post(
      uri = "/paperless-error",
      additionalCookies = sessionKeys
    )(Map.empty)

    def submitTerms(): WSResponse = post("/terms")(Map.empty)

    def submitExitSurvey(): WSResponse = post("/exit-survey")(Map.empty)

    def businessAccountingPeriodPrior(): WSResponse = get("/business/accounting-period-prior")

    def businessAccountingPeriodDates(): WSResponse = get("/business/accounting-period-dates")

    def businessStartDate(): WSResponse = get("/business/start-date", Map(ITSASessionKeys.JourneyStateKey -> Registration.name))

    def registerNextAccountingPeriod(): WSResponse = get("/business/register-next-accounting-period")

    def businessAccountingMethod(): WSResponse = get("/business/accounting-method")

    def businessName(): WSResponse = get("/business/name")

    def businessAddress(state: JourneyState): WSResponse = get("/business/address", Map(ITSASessionKeys.JourneyStateKey -> state.name))

    def submitBusinessAddress(editMode: Boolean, state: JourneyState): WSResponse =
      post(s"/business/address${if (editMode) "?editMode=true" else ""}", Map(ITSASessionKeys.JourneyStateKey -> state.name))(Map.empty)

    def businessAddressInit(state: JourneyState): WSResponse = get("/business/address/init", Map(ITSASessionKeys.JourneyStateKey -> state.name))

    def businessAddressCallback(editMode: Boolean, state: JourneyState): WSResponse = {
      val url = s"/business/address/callback${if (editMode) "/edit" else ""}?id=$testId"
      get(url, Map(ITSASessionKeys.JourneyStateKey -> state.name))
    }

    def businessPhoneNumber(): WSResponse = get("/business/phone-number", Map(ITSASessionKeys.JourneyStateKey -> Registration.name))

    def maintenance(): WSResponse = get("/error/maintenance")

    def noSA(): WSResponse = get("/register-for-SA")

    def submitNoSA(): WSResponse = post("/register-for-SA")(Map.empty)

    def exitSurvey(): WSResponse = get("/exit-survey")

    def submitRegisterNextAccountingPeriod(): WSResponse = post("/business/register-next-accounting-period")(Map.empty)

    def submitMaintenance(): WSResponse = post("/error/maintenance")(Map.empty)

    def submitBusinessAccountingPeriodPrior(inEditMode: Boolean, request: Option[AccountingPeriodPriorModel]): WSResponse = {
      val uri = s"/business/accounting-period-prior?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingPeriodPriorForm.accountingPeriodPriorForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def claimSubscription(): WSResponse = {
      val uri = s"/claim-subscription"
      get(uri)
    }

    def submitIncome(inEditMode: Boolean, request: Option[IncomeSourceModel]): WSResponse = {
      val uri = s"/income?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            IncomeSourceForm.incomeSourceForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def confirmation(): WSResponse = get("/confirmation")

    def submitOtherIncome(inEditMode: Boolean, request: Option[OtherIncomeModel]): WSResponse = {
      val uri = s"/income-other?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            OtherIncomeForm.otherIncomeForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

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
      post(uri, Map(ITSASessionKeys.JourneyStateKey -> Registration.name))(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            BusinessPhoneNumberForm.businessPhoneNumberValidationForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def submitBusinessStartDate(inEditMode: Boolean, request: Option[BusinessStartDateModel]): WSResponse = {
      val uri = s"/business/start-date?editMode=$inEditMode"
      post(uri, Map(ITSASessionKeys.JourneyStateKey -> Registration.name))(
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

    def iv(): WSResponse = get("/iv")

    def showUserDetails(): WSResponse = get("/user-details", Map(ITSASessionKeys.JourneyStateKey -> UserMatching.name))

    def submitUserDetails(clientDetails: Option[UserDetailsModel]): WSResponse =
      post("/user-details", Map(ITSASessionKeys.JourneyStateKey -> UserMatching.name))(
        clientDetails.fold(Map.empty: Map[String, Seq[String]])(
          cd => toFormData(UserDetailsForm.userDetailsValidationForm, cd)
        )
      )

    def showUserDetailsError(): WSResponse = get("/error/user-details", Map(ITSASessionKeys.JourneyStateKey -> UserMatching.name))

    def showUserDetailsLockout(): WSResponse = get("/error/lockout", Map(ITSASessionKeys.JourneyStateKey -> UserMatching.name))

    def submitUserDetailsLockout(): WSResponse = post("/error/lockout", Map(ITSASessionKeys.JourneyStateKey -> UserMatching.name))(Map.empty)

    def submitConfirmUser(previouslyFailedAttempts: Int = 0): WSResponse = {
      val failedAttemptCounter: Map[String, String] = previouslyFailedAttempts match {
        case 0 => Map.empty
        case count => Map(ITSASessionKeys.FailedUserMatching -> previouslyFailedAttempts.toString)
      }
      post("/confirm-user", additionalCookies = Map(ITSASessionKeys.JourneyStateKey -> UserMatching.name) ++ failedAttemptCounter)(Map.empty)
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
