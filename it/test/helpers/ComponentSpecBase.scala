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

import common.Constants.ITSASessionKeys.*
import config.AppConfig
import config.featureswitch.{FeatureSwitch, FeatureSwitching}
import connectors.stubs.SessionDataConnectorStub.stubGetAllSessionData
import forms.individual.*
import forms.individual.business.*
import helpers.IntegrationTestConstants.*
import helpers.servicemocks.{AuditStub, WireMockMethods}
import models.*
import models.individual.JourneyStep.SignUp
import org.jsoup.nodes.Element
import org.scalatest.*
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.*
import play.api.data.Form
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.CookieSigner
import play.api.libs.json.*
import play.api.libs.ws.WSBodyWritables.writeableOf_urlEncodedForm
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
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
  override val cookieSigner: CookieSigner = cookieSignerCache(app)

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val reference: String = "test-reference"

  def configuration: Map[String, String] = Map(
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "microservice.services.income-tax-subscription.host" -> mockHost,
    "microservice.services.income-tax-subscription.port" -> mockPort,
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
    "microservice.services.authenticator.host" -> mockHost,
    "microservice.services.authenticator.port" -> mockPort,
    "microservice.services.citizen-details.host" -> mockHost,
    "microservice.services.citizen-details.port" -> mockPort,
    "microservice.services.tax-enrolments.host" -> mockHost,
    "microservice.services.tax-enrolments.port" -> mockPort,
    "microservice.services.income-tax-subscription-eligibility.host" -> mockHost,
    "microservice.services.income-tax-subscription-eligibility.port" -> mockPort,
    "microservice.services.enrolment-store-proxy.host" -> mockHost,
    "microservice.services.enrolment-store-proxy.port" -> mockPort,
    "microservice.services.users-groups-search.host" -> mockHost,
    "microservice.services.users-groups-search.port" -> mockPort,
    "microservice.services.channel-preferences.host" -> mockHost,
    "microservice.services.channel-preferences.port" -> mockPort,
    "retries.intervals.0" -> "1.millisecond",
    "retries.intervals.1" -> "1.millisecond",
    "retries.intervals.2" -> "1.millisecond"
  ) ++ overriddenConfig()

  def overriddenConfig(): Map[String, String] = Map.empty

  lazy val fakeUUIDProvider: UUIDProvider = new UUIDProvider {
    override def getUUID: String = "test-uuid"
  }

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(configuration)
    .overrides(inject.bind[UUIDProvider].to(fakeUUIDProvider))
    .build()

  implicit lazy val crypto: Encrypter with Decrypter = app.injector.instanceOf[ApplicationCrypto].JsonCrypto

  implicit lazy val sensitiveFormat: Format[SensitiveString] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

  override implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWiremock()
    AuditStub.stubAuditing()

    stubGetAllSessionData(Map(
      REFERENCE -> JsString(reference),
      JourneyStateKey -> JsString(SignUp.key)
    ), addReference = false)
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
      if (!includeState) {
        stubGetAllSessionData(Map(
          REFERENCE -> JsString(reference)
        ), addReference = false)
      }
      val additionalSPSCookie: Map[String, String] = if (includeSPSEntityId) Map(SPSEntityId -> "test-id") else Map.empty
      val sanitizedCookies = additionalCookies - JourneyStateKey
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map(REFERENCE -> "test-reference") ++ additionalSPSCookie ++ sanitizedCookies))
        .get()
        .futureValue
    }

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty, includeSPSEntityId: Boolean = true, includeJourneyState: Boolean = true)(body: Map[String, Seq[String]]): WSResponse = {
      if (!includeJourneyState) {
        stubGetAllSessionData(Map(
          REFERENCE -> JsString(reference)
        ), addReference = false)
      }
      val additionalSPSCookie: Map[String, String] = if (includeSPSEntityId) Map(SPSEntityId -> "test-id") else Map.empty
      val sanitizedCookies = additionalCookies - JourneyStateKey
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map(REFERENCE -> "test-reference") ++ additionalSPSCookie ++ sanitizedCookies), "Csrf-Token" -> "nocheck")
        .post(body)
        .futureValue
    }

    def callback(): WSResponse =
      get("/callback")

    def indexPage(): WSResponse = get(
      uri = "/",
      includeSPSEntityId = false
    )

    def spsHandoff(): WSResponse =
      get("/sps-handoff")

    def claimEnrolSpsHandoff(sessionKeys: Map[String, String] = Map.empty): WSResponse =
      get("/claim-enrolment/sps-handoff", sessionKeys)

    def ivFailure(sessionKeys: Map[String, String] = Map.empty): WSResponse =
      get("/iv-failure", sessionKeys)

    def ivSuccess(sessionKeys: Map[String, String] = Map.empty): WSResponse =
      get("/iv-success", sessionKeys)

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

    def income(): WSResponse =
      get("/income")

    def incomeSource(): WSResponse =
      get("/details/income-receive")

    def yourIncomeSources(): WSResponse =
      get("/details/your-income-source")

    def submitYourIncomeSources(): WSResponse =
      post("/details/your-income-source")(Map.empty[String, Seq[String]])

    def businessYourIncomeSource(): WSResponse =
      get("/details/your-income-source")

    def showBusinessAlreadyRemoved(): WSResponse =
      get("/error/business-already-removed")

    def thankYou(): WSResponse =
      get("/thank-you")

    def cannotSignUp(): WSResponse =
      get("/error/cannot-sign-up")

    def showUsingSoftware(): WSResponse =
      get("/using-software")

    def submitUsingSoftware(request: Option[YesNo]): WSResponse =
      post("/using-software")(
        request.fold(Map.empty[String, Seq[String]])(
          model => UsingSoftwareForm.usingSoftwareForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )

    def showNoSoftware(): WSResponse =
      get("/no-compatible-software")

    def cannotUseService(): WSResponse =
      get("/error/cannot-use-service")

    def notEligibleForIncomeTax(): WSResponse =
      get("/cannot-use-service-yet")

    def cannotReportYet(): WSResponse =
      get("/error/cannot-report-yet")

    def submitCannotReportYet(editMode: Boolean): WSResponse =
      post(s"/error/cannot-report-yet${if (editMode) "?editMode=true" else ""}")(Map.empty)

    def sessionTimeout(): WSResponse =
      get("/session-timeout")

    def timeout(sessionKeys: Map[String, String] = Map.empty): WSResponse =
      get("/timeout", sessionKeys)

    def keepAlive(): WSResponse = get(
      uri = "/keep-alive"
    )

    def notAuthorised(): WSResponse =
      get("/not-authorised")

    def signIn(): WSResponse =
      get("/sign-in")

    def signOut: WSResponse =
      get("/logout")

    def alreadySignedUp(): WSResponse =
      get("/already-signed-up")

    def submitAlreadySignedUp(): WSResponse =
      post("/already-signed-up")(Map.empty)

    def whatYouNeedToDo(session: Map[String, String] = Map.empty): WSResponse =
      get("/what-you-need-to-do", session)

    def submitWhatYouNeedToDo(): WSResponse =
      post("/what-you-need-to-do")(Map.empty)

    def checkYourAnswers(): WSResponse =
      get("/check-your-answers")

    def submitCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse =
      post("/check-your-answers", sessionData)(Map.empty)

    def getRemoveOverseasProperty(sessionData: Map[String, String] = Map.empty): WSResponse =
      get("/business/remove-overseas-property-business", sessionData)

    def submitRemoveOverseasProperty(sessionData: Map[String, String] = Map.empty)(request: Option[YesNo] = None): WSResponse =
      post("/business/remove-overseas-property-business", sessionData)(
        request.fold(Map.empty[String, Seq[String]])(
          model => RemoveOverseasPropertyForm.removeOverseasPropertyForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )

    def getGlobalCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse =
      get("/final-check-your-answers", sessionData)

    def submitGlobalCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse =
      post("/final-check-your-answers", sessionData)(
        Map.empty
      )

    def getPropertyCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse =
      get("/business/uk-property-check-your-answers", sessionData)

    def submitPropertyCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse =
      post("/business/uk-property-check-your-answers", sessionData)(Map.empty)

    def getOverseasPropertyCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse =
      get("/business/overseas-property-check-your-answers", sessionData)

    def submitOverseasPropertyCheckYourAnswers(sessionData: Map[String, String] = Map.empty): WSResponse =
      post("/business/overseas-property-check-your-answers", sessionData)(Map.empty)

    def getRemoveBusiness(sessionData: Map[String, String] = Map.empty, id: String = testId): WSResponse =
      get(s"/business/remove-sole-trader-business?id=$id", sessionData)

    def submitRemoveBusiness(request: Option[YesNo]): WSResponse =
      post(s"/business/remove-sole-trader-business?id=$testId")(
        request.fold(Map.empty[String, Seq[String]])(
          model => RemoveBusinessForm.removeBusinessForm().fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )

    def getProgressSaved(saveAndRetrieveLocation: Option[String] = None, sessionData: Map[String, String] = Map.empty): WSResponse =
      get(
        saveAndRetrieveLocation.fold(
          "/business/progress-saved"
        )(
          location => s"/business/progress-saved?location=$location"
        ), sessionData
      )

    def getCheckIncomeSources: WSResponse =
      get("/hand-offs/check-income-sources")

    def postCheckIncomeSources: WSResponse =
      post("/hand-offs/check-income-sources")(Map.empty)

    def submitMainIncomeError(): WSResponse =
      post("/error/main-income")(Map.empty)

    def getRemoveUkProperty: WSResponse =
      get("/business/remove-uk-property-business")

    def submitRemoveUkProperty(body: Map[String, Seq[String]]): WSResponse =
      post("/business/remove-uk-property-business")(body)

    def maintenance(): WSResponse =
      get("/error/maintenance")

    def noSA(): WSResponse =
      get("/register-for-SA")

    def getRouting: WSResponse =
      get("/business/routing")
    
    def whenDoYouWantToStart(): WSResponse =
      get("/tax-year/select-tax-year")

    def submitWhenDoYouWantToStart(inEditMode: Boolean, request: Option[AccountingYear]): WSResponse = {
      val uri = s"/tax-year/select-tax-year?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model => AccountingYearForm.accountingYearForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        ))
    }

    def submitMaintenance(): WSResponse =
      post("/error/maintenance")(Map.empty)

    def submitAddMTDITOverview(): WSResponse =
      post("/claim-enrolment/overview")(Map.empty)

    def confirmation(additionalCookies: Map[String, String] = Map.empty[String, String], includeConfirmationState: Boolean = true): WSResponse =
      get("/confirmation")

    def submitConfirmation(): WSResponse =
      post("/confirmation")(Map.empty)

    def claimEnrolmentConfirmation(): WSResponse =
      get("/claim-enrolment/confirmation")

    def continueClaimEnrolmentJourneyConfirmation(): WSResponse =
      post("/claim-enrolment/confirmation")(Map.empty)

    def loadingConfirmationStatus(): WSResponse =
      get("/confirming-please-wait")

    def loadingConfirmationStatusQuery(): WSResponse =
      get("/confirming-please-wait/query")

    def youCanSignUp(): WSResponse =
      get("/you-can-sign-up-now")

    def ukPropertyStartDateBeforeLimit(isEditMode: Boolean = false, isGlobalEdit: Boolean = false, includeState: Boolean = true): WSResponse =
      get(s"/business/property-start-date-before-limit?editMode=$isEditMode&isGlobalEdit=$isGlobalEdit", includeState = includeState)

    def submitUKPropertyStartDateBeforeLimit(isEditMode: Boolean = false, isGlobalEdit: Boolean = false, includeJourneyState: Boolean = true)(request: Option[YesNo]): WSResponse = {
      post(s"/business/property-start-date-before-limit?editMode=$isEditMode&isGlobalEdit=$isGlobalEdit", includeJourneyState = includeJourneyState)(
        request.fold(Map.empty[String, Seq[String]]) { model =>
          PropertyStartDateBeforeLimitForm.startDateBeforeLimitForm.fill(model).data.map {
            case (k, v) => (k, Seq(v))
          }
        }
      )
    }

    def foreignPropertyStartDateBeforeLimit(isEditMode: Boolean = false, isGlobalEdit: Boolean = false, includeState: Boolean = true): WSResponse =
      get(s"/business/foreign-property-start-date-before-limit?editMode=$isEditMode&isGlobalEdit=$isGlobalEdit", includeState = includeState)

    def submitForeignPropertyStartDateBeforeLimit(isEditMode: Boolean = false, isGlobalEdit: Boolean = false, includeJourneyState: Boolean = true)(request: Option[YesNo]): WSResponse = {
      post(s"/business/foreign-property-start-date-before-limit?editMode=$isEditMode&isGlobalEdit=$isGlobalEdit", includeJourneyState = includeJourneyState)(
        request.fold(Map.empty[String, Seq[String]]) { model =>
          ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimitForm.fill(model).data.map {
            case (k, v) => (k, Seq(v))
          }
        }
      )
    }

    def propertyStartDate(): WSResponse =
      get("/business/property-commencement-date")

    def getOverseasPropertyStartDate: WSResponse =
      get("/business/overseas-property-start-date")

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
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            ForeignPropertyStartDateForm.startDateForm(_.toString)
              .fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def showIncomeSourcesIncomplete(includeState: Boolean = true): WSResponse =
      get("/details/income-sources-incomplete", includeState = includeState)

    def submitIncomeSourcesIncomplete(sessionData: Map[String, String] = Map.empty): WSResponse =
      post("/details/income-sources-incomplete", sessionData)(Map.empty)

    def iv(): WSResponse =
      get("/iv")

    def showAffinityGroupError(): WSResponse =
      get("/error/affinity-group")

    def addMTDITOverview(maybeOrigin: Option[String] = None): WSResponse = get(
      uri = s"/claim-enrolment/overview" + maybeOrigin.map(origin => s"?origin=$origin").getOrElse("")
    )

    def claimEnrolmentResolver(): WSResponse =
      get("/claim-enrolment/resolve")

    def claimEnrolmentUseSelfAssessment(): WSResponse =
      get("/claim-enrolment/use-self-assessment-details")

    def submitClaimEnrolmentUseSelfAssessment(request: Option[YesNo]): WSResponse =
      post("/claim-enrolment/use-self-assessment-details")(
        request.fold(Map.empty[String, Seq[String]]) { model =>
          IRSACredentialForm.irsaCredentialForm.fill(model).data.map {
            case (k, v) => (k, Seq(v))
          }
        }
      )

    def agentSigningUp(): WSResponse =
      get("/eligibility/client/signing-up")

    def individualSigningUp(): WSResponse =
      get("/eligibility/signing-up")

    def agentSigningUpPost(): WSResponse =
      post("/eligibility/client/signing-up")(Map.empty)

    def individualSigningUpPost(): WSResponse =
      post("/eligibility/signing-up")(Map.empty)

    def agentIndex(): WSResponse =
      get("/eligibility/client")

    def individualIndex(): WSResponse =
      get("/eligibility")

    def matchingUseSelfAssessment(): WSResponse =
      get("/use-self-assessment-details")

    def submitMatchingUseSelfAssessment(request: Option[YesNo]): WSResponse =
      post("/use-self-assessment-details")(
        request.fold(Map.empty[String, Seq[String]]) { model =>
          IRSACredentialForm.irsaCredentialForm.fill(model).data.map {
            case (k, v) => (k, Seq(v))
          }
        }
      )
    
    def showNonEligibleVoluntary(): WSResponse =
      get("/tax-year/sign-up-next-year-voluntary")

    def submitNonEligibleVoluntary(): WSResponse =
      post("/tax-year/sign-up-next-year-voluntary")(Map.empty)
  }

  def toFormData[T](form: Form[T], data: T): Map[String, Seq[String]] =
    form.fill(data).data map { case (k, v) => k -> Seq(v) }

  implicit val nilWrites: Writes[Nil.type] = new Writes[Nil.type] {
    override def writes(o: Nil.type): JsValue = JsArray()
  }

  def removeHtmlMarkup(stringWithMarkup: String): String =
    stringWithMarkup.replaceAll("<.+?>", " ").replaceAll("[\\s]{2,}", " ").trim
}
