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

import _root_.common.Constants.ITSASessionKeys
import _root_.common.Constants.ITSASessionKeys._
import auth.individual.{JourneyState, SignUp, ClaimEnrolment => ClaimEnrolmentJourney}
import config.AppConfig
import config.featureswitch.{FeatureSwitch, FeatureSwitching}
import connectors.stubs.SessionDataConnectorStub.stubGetSessionData
import forms.individual._
import forms.individual.accountingperiod.{AccountingPeriodForm, AccountingPeriodNonStandardForm}
import forms.individual.business._
import forms.individual.email.{CaptureConsentForm, EmailCaptureForm}
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuditStub, WireMockMethods}
import models._
import models.common.BusinessAccountingPeriod
import models.individual.JourneyStep.{Confirmation, PreSignUp}
import org.jsoup.nodes.Element
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api._
import play.api.data.Form
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.CookieSigner
import play.api.libs.json.{JsArray, JsString, JsValue, Writes}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter}
import utilities.UUIDProvider

import java.time.LocalDate
import java.util.UUID

trait ComponentSpecBase extends AnyWordSpecLike with Matchers with OptionValues with GivenWhenThen with TestSuite
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with CustomMatchers with WireMockMethods with FeatureSwitching with SessionCookieBaker {

  lazy val mockHost: String = WiremockHelper.wiremockHost
  lazy val mockPort: String = WiremockHelper.wiremockPort.toString
  lazy val mockUrl = s"http://$mockHost:$mockPort"

  implicit class CustomSelectors(element: Element) {
    def selectOptionally(selector: String): Option[Element] = {
      element.select(selector).headOption
    }
  }

  val cookieSignerCache: Application => CookieSigner = Application.instanceCache[CookieSigner]
  override lazy val cookieSigner: CookieSigner = cookieSignerCache(app)

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val reference: String = "test-reference"

  def configuration: Map[String, String] = Map(
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
    "auditing.enabled" -> "true",
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
    "microservice.services.users-groups-search.port" -> mockPort,
    "microservice.services.channel-preferences.host" -> mockHost,
    "microservice.services.channel-preferences.port" -> mockPort
  )

  lazy val fakeUUIDProvider: UUIDProvider = new UUIDProvider {
    override def getUUID: String = "test-uuid"
  }

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(configuration)
    .overrides(inject.bind[UUIDProvider].to(fakeUUIDProvider))
    .build()

  implicit lazy val crypto: Encrypter with Decrypter = app.injector.instanceOf[ApplicationCrypto].JsonCrypto

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWiremock()
    AuditStub.stubAuditing()

    stubGetSessionData(REFERENCE)(OK, JsString(reference))
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

  def getWithHeaders(uri: String, headers: (String, String)*): WSResponse = {
    buildClient(uri)
      .withHttpHeaders(headers: _*)
      .get()
      .futureValue
  }

  object IncomeTaxSubscriptionFrontend extends UserMatchingIntegrationRequestSupport {
    val csrfToken: String = UUID.randomUUID().toString

    def get(uri: String, additionalCookies: Map[String, String] = Map.empty, includeSPSEntityId: Boolean = true, includeState: Boolean = true): WSResponse = {
      val additionalSPSCookie: Map[String, String] = if (includeSPSEntityId) Map(SPSEntityId -> "test-id") else Map.empty
      val stateCookie: Map[String, String] = if (includeState) Map(JourneyStateKey -> SignUp.name) else Map.empty
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map(REFERENCE -> "test-reference") ++ stateCookie ++ additionalSPSCookie ++ additionalCookies))
        .get()
        .futureValue
    }

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty, includeSPSEntityId: Boolean = true, includeJourneyState: Boolean = true)(body: Map[String, Seq[String]]): WSResponse = {
      val additionalSPSCookie: Map[String, String] = if (includeSPSEntityId) Map(SPSEntityId -> "test-id") else Map.empty
      val journeyState: Map[String, String] = if (includeJourneyState) Map(JourneyStateKey -> SignUp.name) else Map.empty
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map(REFERENCE -> "test-reference") ++ journeyState ++ additionalSPSCookie ++ additionalCookies), "Csrf-Token" -> "nocheck")
        .post(body)
        .futureValue
    }

    def callback(): WSResponse = get("/callback")

    def indexPage(includeState: Boolean = true): WSResponse = get(
      uri = "/",
      includeSPSEntityId = false,
      additionalCookies = if (includeState) Map(JourneyStateKey -> PreSignUp.key) else Map.empty[String, String],
      includeState = false
    )

    def spsHandoff(): WSResponse = get("/sps-handoff")

    def claimEnrolSpsHandoff(sessionKeys: Map[String, String] = Map.empty): WSResponse = {

      get("/claim-enrolment/sps-handoff", sessionKeys)

    }

    def ivFailure(): WSResponse = get("/iv-failure")

    def ivSuccess(sessionKeys: Map[String, String] = Map.empty): WSResponse = get("/iv-success", sessionKeys)

    def spsCallback(hasEntityId: Boolean): WSResponse = {
      if (hasEntityId) {
        get("/sps-callback?entityId=testId")
      } else {
        get("/sps-callback")
      }
    }

    def claimEnrolSpsCallback(hasEntityId: Boolean, sessionKeys: Map[String, String] = Map.empty): WSResponse = {
      if (hasEntityId) {
        get("/claim-enrolment/sps-callback?entityId=testId", sessionKeys)
      } else {
        get("/claim-enrolment/sps-callback", sessionKeys)
      }
    }

    def income(): WSResponse = get("/income")

    def incomeSource(): WSResponse = get("/details/income-receive")

    def yourIncomeSources(): WSResponse = get("/details/your-income-source")

    def submitYourIncomeSources(): WSResponse = post("/details/your-income-source")(Map.empty[String, Seq[String]])

    def businessYourIncomeSource(): WSResponse = get("/details/your-income-source")

    def showBusinessAlreadyRemoved(): WSResponse = get("/error/business-already-removed")

    def thankYou(): WSResponse = get("/thank-you")

    def cannotSignUp(): WSResponse = get("/error/cannot-sign-up")

    def showCannotSignUpThisYear: WSResponse = get("/error/cannot-sign-up-for-current-year")

    def submitCannotSignUpThisYear(request: Option[YesNo]): WSResponse = post("/error/cannot-sign-up-for-current-year")(
      request.fold(Map.empty[String, Seq[String]])(
        model => CannotSignUpThisYearForm.cannotSignUpThisYearForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def showUsingSoftware(): WSResponse = get("/using-software")

    def submitUsingSoftware(request: Option[YesNo]): WSResponse = {
      post("/using-software")(
        request.fold(Map.empty[String, Seq[String]])(
          model => UsingSoftwareForm.usingSoftwareForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def showCaptureConsent(includeState: Boolean = true): WSResponse = get("/capture-consent", includeState = includeState)

    def submitCaptureConsent(request: Option[YesNo])(includeState: Boolean = true): WSResponse = {
      post("/capture-consent", includeJourneyState = includeState)(
        request.fold(Map.empty[String, Seq[String]])(
          model => CaptureConsentForm.captureConsentForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def showNoSoftware(): WSResponse = get("/no-compatible-software")

    def showEmailCapture(includeState: Boolean = true): WSResponse = get("/email-capture", includeState = includeState)

    def submitEmailCapture(request: Option[String])(includeState: Boolean = true): WSResponse = {
      post("/email-capture", includeJourneyState = includeState)(
        request.fold(Map.empty[String, Seq[String]])(
          model => EmailCaptureForm.form.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def cannotUseService(): WSResponse = get("/error/cannot-use-service")

    def notEligibleForIncomeTax(): WSResponse = get("/cannot-use-service-yet")

    def cannotReportYet(): WSResponse = get("/error/cannot-report-yet")

    def submitCannotReportYet(editMode: Boolean): WSResponse =
      post(s"/error/cannot-report-yet${if (editMode) "?editMode=true" else ""}")(Map.empty)

    def sessionTimeout(): WSResponse = get("/session-timeout")

    def timeout(sessionKeys: Map[String, String] = Map.empty): WSResponse = get("/timeout", sessionKeys)

    def keepAlive(sessionKeys: Map[String, String] = Map.empty): WSResponse = get(
      uri = "/keep-alive",
      additionalCookies = sessionKeys
    )

    def notAuthorised(): WSResponse = get("/not-authorised")

    def signIn(): WSResponse = get("/sign-in")

    def signOut: WSResponse = get("/logout")

    def alreadyEnrolled(): WSResponse = get("/already-enrolled")

    def whatYouNeedToDo(session: Map[String, String] = Map.empty): WSResponse = get("/what-you-need-to-do", session)

    def submitWhatYouNeedToDo(): WSResponse = post("/what-you-need-to-do")(Map.empty)

    def declinedSignUpNextYear(): WSResponse = get("/declined-sign-up-next-year")

    def checkYourAnswers(): WSResponse = get("/check-your-answers")

    def submitCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse = {
      post("/check-your-answers", sessionData)(Map.empty)
    }

    def getRemoveOverseasProperty(sessionData: Map[String, String] = Map.empty): WSResponse = {
      get("/business/remove-overseas-property-business", sessionData)
    }

    def submitRemoveOverseasProperty(sessionData: Map[String, String] = Map.empty)(request: Option[YesNo] = None): WSResponse = {
      post("/business/remove-overseas-property-business", sessionData)(
        request.fold(Map.empty[String, Seq[String]])(
          model => RemoveOverseasPropertyForm.removeOverseasPropertyForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def getGlobalCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse = {
      get("/final-check-your-answers", sessionData)
    }

    def submitGlobalCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse = {
      post("/final-check-your-answers", sessionData)(
        Map.empty
      )
    }

    def getPropertyCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse = {
      get("/business/uk-property-check-your-answers", sessionData)
    }

    def submitPropertyCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse = {
      post("/business/uk-property-check-your-answers", sessionData)(Map.empty)
    }

    def getOverseasPropertyCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse = {
      get("/business/overseas-property-check-your-answers", sessionData)
    }

    def submitOverseasPropertyCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse = {
      post("/business/overseas-property-check-your-answers", sessionData)(Map.empty)
    }

    def getRemoveBusiness(sessionData: Map[String, String] = Map.empty, id: String = testId): WSResponse = {
      get(s"/business/remove-sole-trader-business?id=$id", sessionData)
    }

    def submitRemoveBusiness(request: Option[YesNo]): WSResponse = post(s"/business/remove-sole-trader-business?id=$testId")(
      request.fold(Map.empty[String, Seq[String]])(
        model => RemoveBusinessForm.removeBusinessForm().fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def getProgressSaved(saveAndRetrieveLocation: Option[String] = None, sessionData: Map[String, String] = Map.empty): WSResponse = {
      get(
        saveAndRetrieveLocation.fold(
          "/business/progress-saved"
        )(
          location => s"/business/progress-saved?location=$location"
        ), sessionData
      )
    }

    def submitMainIncomeError(): WSResponse = post("/error/main-income")(Map.empty)

    def businessAccountingMethod(): WSResponse = get("/business/accounting-method")

    def propertyAccountingMethod(): WSResponse = get("/business/accounting-method-property")

    def getRemoveUkProperty: WSResponse = get("/business/remove-uk-property-business")

    def submitRemoveUkProperty(body: Map[String, Seq[String]]): WSResponse = post("/business/remove-uk-property-business")(body)

    def overseasPropertyAccountingMethod: WSResponse = get("/business/overseas-property-accounting-method")

    def businessAddress(state: JourneyState): WSResponse = get("/business/address", Map(JourneyStateKey -> state.name))

    def submitBusinessAddress(editMode: Boolean, state: JourneyState): WSResponse =
      post(s"/business/address${if (editMode) "?editMode=true" else ""}", Map(JourneyStateKey -> state.name))(Map.empty)

    def businessAddressInit(state: JourneyState): WSResponse = get("/business/address/init", Map(JourneyStateKey -> state.name))

    def businessAddressCallback(editMode: Boolean, state: JourneyState): WSResponse = {
      val url = s"/business/address/callback${if (editMode) "/edit" else ""}?id=$testId"
      get(url, Map(JourneyStateKey -> state.name))
    }

    def maintenance(): WSResponse = get("/error/maintenance")

    def noSA(): WSResponse = get("/register-for-SA")

    def getRouting: WSResponse = get("/business/routing")

    def accountingYear(): WSResponse = get("/business/what-year-to-sign-up")

    def submitAccountingYear(inEditMode: Boolean, request: Option[AccountingYear]): WSResponse = {
      val uri = s"/business/what-year-to-sign-up?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model => AccountingYearForm.accountingYearForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        ))
    }

    def accountingPeriod: WSResponse = get("/accounting-period-check")

    def submitAccountingPeriod(request: Option[BusinessAccountingPeriod]): WSResponse = {
      post("/accounting-period-check")(
        request.fold(Map.empty[String, Seq[String]])(
          model => AccountingPeriodForm.accountingPeriodForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def showNonStandardAccountingPeriod(): WSResponse = get("/accounting-period-non-standard")
    def submitNonStandardAccountingPeriod(request: Option[YesNo]): WSResponse = {
      post("/accounting-period-non-standard")(
        request.fold(Map.empty[String, Seq[String]])(
          model => AccountingPeriodNonStandardForm.nonStandardAccountingPeriodForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def accountingPeriodNotSupported: WSResponse = get("/accounting-period-not-supported")

    def submitMaintenance(): WSResponse = post("/error/maintenance")(Map.empty)

    def submitAddMTDITOverview(): WSResponse = post("/claim-enrolment/overview", Map(JourneyStateKey -> ClaimEnrolmentJourney.name))(Map.empty)

    def confirmation(additionalCookies: Map[String, String] = Map.empty[String, String], includeConfirmationState: Boolean = true): WSResponse = {
      val confirmationStateSession: Map[String, String] = if (includeConfirmationState) Map(JourneyStateKey -> Confirmation.key) else Map.empty
      get("/confirmation", additionalCookies ++ confirmationStateSession, includeState = false)
    }

    def submitConfirmation(includeConfirmationState: Boolean = true): WSResponse = {
      val confirmationStateSession: Map[String, String] = if (includeConfirmationState) Map(JourneyStateKey -> Confirmation.key) else Map.empty
      post("/confirmation", additionalCookies = confirmationStateSession)(Map.empty)
    }

    def claimEnrolmentConfirmation(): WSResponse = get("/claim-enrolment/confirmation", Map(JourneyStateKey -> ClaimEnrolmentJourney.name))

    def continueClaimEnrolmentJourneyConfirmation(): WSResponse = post("/claim-enrolment/confirmation", Map(JourneyStateKey -> ClaimEnrolmentJourney.name))(Map.empty)

    def notSubscribed(): WSResponse = get("/claim-enrolment/not-subscribed", Map(JourneyStateKey -> ClaimEnrolmentJourney.name))

    def alreadySignedUp(): WSResponse = get("/claim-enrolment/already-signed-up", Map(JourneyStateKey -> ClaimEnrolmentJourney.name))

    def youCanSignUp(): WSResponse = get("/you-can-sign-up-now")

    def submitPropertyAccountingMethod(inEditMode: Boolean, request: Option[AccountingMethod]): WSResponse = {
      val uri = s"/business/accounting-method-property?editMode=$inEditMode"
      post(uri, Map(ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true"))(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def submitForeignPropertyAccountingMethod(inEditMode: Boolean, request: Option[AccountingMethod]): WSResponse = {
      val uri = s"/business/overseas-property-accounting-method?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def ukPropertyStartDateBeforeLimit(isEditMode: Boolean = false, isGlobalEdit: Boolean = false): WSResponse = {
      get(s"/business/property-start-date-before-limit?editMode=$isEditMode&isGlobalEdit=$isGlobalEdit")
    }

    def submitUKPropertyStartDateBeforeLimit(isEditMode: Boolean = false, isGlobalEdit: Boolean = false)(request: Option[YesNo]): WSResponse = {
      post(s"/business/property-start-date-before-limit?editMode=$isEditMode&isGlobalEdit=$isGlobalEdit")(
        request.fold(Map.empty[String, Seq[String]]) { model =>
          PropertyStartDateBeforeLimitForm.startDateBeforeLimitForm.fill(model).data.map {
            case (k, v) => (k, Seq(v))
          }
        }
      )
    }

    def foreignPropertyStartDateBeforeLimit(isEditMode: Boolean = false, isGlobalEdit: Boolean = false): WSResponse = {
      get(s"/business/foreign-property-start-date-before-limit?editMode=$isEditMode&isGlobalEdit=$isGlobalEdit")
    }

    def submitForeignPropertyStartDateBeforeLimit(isEditMode: Boolean = false, isGlobalEdit: Boolean = false)(request: Option[YesNo]): WSResponse = {
      post(s"/business/foreign-property-start-date-before-limit?editMode=$isEditMode&isGlobalEdit=$isGlobalEdit")(
        request.fold(Map.empty[String, Seq[String]]) { model =>
          ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimitForm.fill(model).data.map {
            case (k, v) => (k, Seq(v))
          }
        }
      )
    }

    def propertyStartDate(): WSResponse = get("/business/property-commencement-date")

    def getOverseasPropertyStartDate: WSResponse = get("/business/overseas-property-start-date")

    def submitPropertyStartDate(inEditMode: Boolean, request: Option[DateModel]): WSResponse = {

      val testValidMaxStartDate = LocalDate.now.minusYears(1)
      val testValidMinStartDate = LocalDate.of(1900, 1, 1)
      val uri = s"/business/property-commencement-date?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            PropertyStartDateForm.propertyStartDateForm(testValidMinStartDate, testValidMaxStartDate, d => d.toString)
              .fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def submitOverseasPropertyStartDate(inEditMode: Boolean, request: Option[DateModel]): WSResponse = {
      val uri = s"/business/overseas-property-start-date?editMode=$inEditMode"
      post(uri, Map(ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true"))(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            ForeignPropertyStartDateForm.startDateForm(_.toString)
              .fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def showIncomeSourcesIncomplete(includeState: Boolean = true): WSResponse = get("/details/income-sources-incomplete", includeState = includeState)

    def submitIncomeSourcesIncomplete(sessionData: Map[String, String] = Map.empty): WSResponse = {
      post("/details/income-sources-incomplete", sessionData)(Map.empty)
    }

    def iv(): WSResponse = get("/iv")

    def showAffinityGroupError(): WSResponse = get("/error/affinity-group")

    def addMTDITOverview(): WSResponse = get("/claim-enrolment/overview")

    def claimEnrolmentResolver(): WSResponse = get("/claim-enrolment/resolve", Map(JourneyStateKey -> ClaimEnrolmentJourney.name))

  }

  def toFormData[T](form: Form[T], data: T): Map[String, Seq[String]] =
    form.fill(data).data map { case (k, v) => k -> Seq(v) }

  implicit val nilWrites: Writes[Nil.type] = new Writes[Nil.type] {
    override def writes(o: Nil.type): JsValue = JsArray()
  }

  def removeHtmlMarkup(stringWithMarkup: String): String =
    stringWithMarkup.replaceAll("<.+?>", " ").replaceAll("[\\s]{2,}", " ").trim
}
