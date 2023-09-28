
package helpers.agent

import _root_.common.Constants.ITSASessionKeys
import auth.agent.{AgentJourneyState, AgentSignUp, AgentUserMatching}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import config.AppConfig
import config.featureswitch.FeatureSwitching
import forms.agent._
import forms.individual.business.RemoveBusinessForm
import helpers.IntegrationTestConstants.{testFirstName, testId, testLastName}
import helpers.UserMatchingIntegrationRequestSupport
import helpers.agent.IntegrationTestConstants._
import helpers.agent.WiremockHelper._
import helpers.agent.servicemocks.WireMockMethods
import helpers.servicemocks.AuditStub
import models._
import models.common._
import models.usermatching.UserDetailsModel
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
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
import play.api.libs.json.{JsArray, Writes}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.{Headers, Session}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utilities.UserMatchingSessionUtil

import java.time.LocalDate
import java.util.UUID
import scala.jdk.CollectionConverters._

trait ComponentSpecBase extends AnyWordSpecLike with Matchers with OptionValues
  with GivenWhenThen with TestSuite
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience
  with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with CustomMatchers with WireMockMethods with SessionCookieBaker with FeatureSwitching {

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())


  val cookieSignerCache: Application => CookieSigner = Application.instanceCache[CookieSigner]
  override lazy val cookieSigner: CookieSigner = cookieSignerCache(app)

  lazy val wmConfig: WireMockConfiguration = wireMockConfig().port(wiremockPort)
  lazy val wireMockServer = new WireMockServer(wmConfig)

  def startWiremock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = WireMock.reset()

  def buildClient(path: String): WSRequest = ws.url(s"http://localhost:$port$baseURI$path").withFollowRedirects(false)

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build()

  lazy val mockHost: String = WiremockHelper.wiremockHost
  lazy val mockPort: String = WiremockHelper.wiremockPort.toString
  lazy val mockUrl = s"http://$mockHost:$mockPort"

  def config: Map[String, String] = Map(
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "microservice.services.session-cache.host" -> mockHost,
    "microservice.services.session-cache.port" -> mockPort,
    "microservice.services.subscription-service.host" -> mockHost,
    "microservice.services.subscription-service.port" -> mockPort,
    "microservice.services.authenticator.host" -> mockHost,
    "microservice.services.authenticator.port" -> mockPort,
    "microservice.services.agent-microservice.host" -> mockHost,
    "microservice.services.agent-microservice.port" -> mockPort,
    "microservice.services.tax-enrolments.host" -> mockHost,
    "microservice.services.tax-enrolments.port" -> mockPort,
    "microservice.services.income-tax-subscription-eligibility.host" -> mockHost,
    "microservice.services.income-tax-subscription-eligibility.port" -> mockPort,
    "income-tax-subscription-eligibility-frontend.url" -> mockUrl,
    "auditing.enabled" -> "true",
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "microservice.services.enrolment-store-proxy.host" -> mockHost,
    "microservice.services.enrolment-store-proxy.port" -> mockPort,
    "microservice.services.users-groups-search.host" -> mockHost,
    "microservice.services.users-groups-search.port" -> mockPort,
    "microservice.services.channel-preferences.host" -> mockHost,
    "microservice.services.channel-preferences.port" -> mockPort
  )

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
  }


  object IncomeTaxSubscriptionFrontend extends UserMatchingIntegrationRequestSupport {
    val csrfToken: String = UUID.randomUUID().toString

    def defaultCookies(withUTR: Boolean = true, withJourneyStateSignUp: Boolean = true): Map[String, String] = {
      val utrKvp = if (withUTR)
        Map(ITSASessionKeys.UTR -> testUtr)
      else
        Map()
      val stateKvp = if (withJourneyStateSignUp)
        Map(ITSASessionKeys.JourneyStateKey -> AgentSignUp.name)
      else
        Map()
      Map(
        ITSASessionKeys.ArnKey -> IntegrationTestConstants.testARN,
        ITSASessionKeys.REFERENCE -> "test-reference"
      ) ++ utrKvp ++ stateKvp
    }

    val headers: Seq[(String, String)] = Seq(
      HeaderNames.COOKIE -> bakeSessionCookie(defaultCookies()),
      "Csrf-Token" -> "nocheck"
    )


    implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(
      FakeRequest().withHeaders(Headers(headers: _*)),
      Session()
    )

    def get(uri: String, additionalCookies: Map[String, String] = Map.empty, withUTR: Boolean = true, withJourneyStateSignUp: Boolean = true): WSResponse =
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(defaultCookies(withUTR, withJourneyStateSignUp) ++ additionalCookies))
        .get()
        .futureValue

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty, withUTR: Boolean = true)(body: Map[String, Seq[String]]): WSResponse =
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(defaultCookies(withUTR) ++ additionalCookies), "Csrf-Token" -> "nocheck")
        .post(body)
        .futureValue

    def startPage(): WSResponse = get("/")

    def indexPage(maybeJourneyState: Option[AgentJourneyState] = Some(AgentSignUp), sessionMap: Map[String, String] = Map.empty[String, String]): WSResponse = {
      get("/index",
        sessionMap ++ maybeJourneyState.map(state => ITSASessionKeys.JourneyStateKey -> state.name),
        withUTR = false,
        withJourneyStateSignUp = false
      )
    }

    def showOtherSourcesOfIncome(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = get("/eligibility/income-sources", sessionData)

    def submitOtherSourcesOfIncome(request: Option[YesNo], sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = post("/eligibility/income-sources", sessionData)(
      request.fold(Map.empty[String, Seq[String]])(
        model => OtherSourcesOfIncomeForm.otherSourcesOfIncomeForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def showPropertyTradingStartAfter(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = get("/eligibility/property-start-date", sessionData)

    def submitPropertyTradingStartAfter(request: Option[YesNo], sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = post("/eligibility/property-start-date", sessionData)(
      request.fold(Map.empty[String, Seq[String]])(
        model => PropertyTradingStartDateForm.propertyTradingStartDateForm("").fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def getAgentGlobalCheckYourAnswers(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino)): WSResponse = {
      get("/final-check-your-answers", sessionData)
    }

    def submitAgentGlobalCheckYourAnswers(request: Option[YesNo])(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino)): WSResponse = {
      post("/final-check-your-answers", sessionData)(Map.empty
      )
    }

    def showCannotTakePart: WSResponse = get("/error/cannot-sign-up")

    def showCannotSignUpThisYear: WSResponse = get("/error/cannot-sign-up-for-current-year")

    def submitCannotSignUpThisYear(request: Option[YesNo]): WSResponse = post("/error/cannot-sign-up-for-current-year")(
      request.fold(Map.empty[String, Seq[String]])(
        model => CannotSignUpThisYearForm.cannotSignUpThisYearForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def showSoleTrader(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = get("/eligibility/sole-trader-start-date", sessionData)

    def submitSoleTraderForm(request: Option[YesNo], sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = post("/eligibility/sole-trader-start-date", sessionData)(
      request.fold(Map.empty[String, Seq[String]])(
        model => SoleTraderForm.soleTraderForm("").fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def showAccountingPeriodCheck(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = get("/eligibility/accounting-period-check", sessionData)

    def submitAccountingPeriodCheck(request: Option[YesNo], sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = post("/eligibility/accounting-period-check", sessionData)(
      request.fold(Map.empty[String, Seq[String]])(
        model => AccountingPeriodCheckForm.accountingPeriodCheckForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def whatYouNeedToDo(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = get("/what-you-need-to-do", sessionData)

    def submitWhatYouNeedToDo(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = post("/what-you-need-to-do", sessionData)(Map.empty)

    def declinedSignUpNextYear(): WSResponse = get("/declined-sign-up-next-year")

    def submitDeclinedSignUpNextYear(): WSResponse = post("/declined-sign-up-next-year")(Map.empty)

    def income(): WSResponse = get("/income")

    def mainIncomeError(): WSResponse = get("/error/main-income")

    def sessionTimeout(): WSResponse = get("/session-timeout")

    def keepAlive(sessionKeys: Map[String, String] = Map.empty): WSResponse = get("/keep-alive", sessionKeys)

    def timeout(sessionKeys: Map[String, String] = Map.empty): WSResponse = get("/timeout", sessionKeys)

    def showClientDetails(): WSResponse = get("/client-details", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

    def submitClientDetails(newSubmission: Option[UserDetailsModel], storedSubmission: Option[UserDetailsModel]): WSResponse =
      post("/client-details", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name).addUserDetails(storedSubmission))(
        newSubmission.fold(Map.empty: Map[String, Seq[String]])(
          cd => toFormData(ClientDetailsForm.clientDetailsForm, cd)
        )
      )

    def showClientDetailsError(): WSResponse = get("/error/client-details", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

    def showClientDetailsLockout(): WSResponse = get("/error/lockout", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

    def showConfirmation(hasSubmitted: Boolean, firstName: String, lastName: String, nino: String): WSResponse =
      if (hasSubmitted)
        get("/confirmation", Map(ITSASessionKeys.MTDITID -> testMTDID, UserMatchingSessionUtil.firstName -> firstName,
          UserMatchingSessionUtil.lastName -> lastName, ITSASessionKeys.NINO -> nino))
      else
        get("/confirmation", Map[String, String](UserMatchingSessionUtil.firstName -> firstName,
          UserMatchingSessionUtil.lastName -> lastName, ITSASessionKeys.NINO -> nino))

    def feedback(): WSResponse = get("/feedback-submitted")

    def signIn(): WSResponse = get("/sign-in")

    def signOut(): WSResponse = get("/logout")

    def notEnrolledAgentServices(): WSResponse = get("/not-enrolled-agent-services")

    def getNoClientRelationship: WSResponse = get("/error/no-client-relationship", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

    def postNoClientRelationship(): WSResponse = post("/error/no-client-relationship", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))(Map.empty)

    def clientAlreadySubscribed(): WSResponse = get("/error/client-already-subscribed", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

    def submitClientAlreadySubscribed(): WSResponse = post("/error/client-already-subscribed", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))(Map.empty)

    def checkYourAnswers(): WSResponse = get("/check-your-answers", Map(
      ITSASessionKeys.ArnKey -> testARN,
      ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
      ITSASessionKeys.NINO -> testNino,
      ITSASessionKeys.UTR -> testUtr
    ))

    def submitCheckYourAnswers(): WSResponse = post("/check-your-answers",
      Map(
        ITSASessionKeys.ArnKey -> testARN,
        ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
        ITSASessionKeys.NINO -> testNino,
        ITSASessionKeys.UTR -> testUtr
      )
    )(Map.empty)

    def submitConfirmClient(previouslyFailedAttempts: Int = 0,
                            storedUserDetails: Option[UserDetailsModel] = Some(IntegrationTestModels.testClientDetails)): WSResponse = {
      val failedAttemptCounter: Map[String, String] = previouslyFailedAttempts match {
        case 0 => Map.empty
        case _ => Map(ITSASessionKeys.FailedClientMatching -> previouslyFailedAttempts.toString)
      }
      post("/confirm-client",
        additionalCookies = failedAttemptCounter ++ Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name)
          .addUserDetails(storedUserDetails), withUTR = false)(Map.empty)
    }

    def businessIncomeSource(sessionData: Map[String, String] = Map.empty): WSResponse = {
      get("/income-source", sessionData)
    }

    def agentBusinessIncomeSource(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = get("/your-income-source", sessionData)

    def submitBusinessIncomeSource(request: Option[BusinessIncomeSource],
                                   incomeSourcesStatus: IncomeSourcesStatus = IncomeSourcesStatus(
                                     selfEmploymentAvailable = true,
                                     ukPropertyAvailable = true,
                                     overseasPropertyAvailable = true
                                   )): WSResponse = {
      post("/income-source")(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            BusinessIncomeSourceForm.businessIncomeSourceForm(incomeSourcesStatus)
              .fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def accountingYear(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = get("/business/what-year-to-sign-up", sessionData)

    def businessAccountingPeriodPrior(): WSResponse = get("/business/accounting-period-prior")

    def businessAccountingMethod(): WSResponse = get("/business/accounting-method")

    def propertyAccountingMethod(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = get("/business/accounting-method-property", sessionData)

    def overseasPropertyAccountingMethod(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = get("/business/overseas-property-accounting-method", sessionData)

    def ukPropertyStartDate(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = get("/business/property-commencement-date", sessionData)

    def submitUkPropertyStartDate(isEditMode: Boolean = false, request: Option[DateModel], sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = {
      val testValidMaxStartDate = LocalDate.now.minusYears(1)
      val testValidMinStartDate = LocalDate.of(1900, 1, 1)
      val uri = s"/business/property-commencement-date?editMode=$isEditMode"
      post(uri, sessionData)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            PropertyStartDateForm.propertyStartDateForm(testValidMinStartDate, testValidMaxStartDate, d => d.toString)
              .fill(model).data.map { case (k, v) => (k, Seq(v))
            }
        )
      )
    }

    def getPropertyCheckYourAnswers(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = {
      get("/business/uk-property-check-your-answers", sessionData)
    }

    def submitPropertyCheckYourAnswers(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = {
      post("/business/uk-property-check-your-answers", sessionData)(Map.empty)
    }

    def overseasPropertyStartDate(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = get("/business/overseas-commencement-date", sessionData)

    def submitOverseasPropertyStartDate(inEditMode: Boolean, request: Option[DateModel], sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = {
      val testValidMaxStartDate = LocalDate.now.minusYears(1)
      val testValidMinStartDate = LocalDate.of(1900, 1, 1)
      val uri = s"/business/overseas-commencement-date?editMode=$inEditMode"
      post(uri, sessionData)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            OverseasPropertyStartDateForm.overseasPropertyStartDateForm(testValidMinStartDate, testValidMaxStartDate, d => d.toString)
              .fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def getClientRemoveUkProperty: WSResponse = get("/business/remove-uk-property-business")


    def submitClientRemoveUkProperty(body: Map[String, Seq[String]]): WSResponse = post("/business/remove-uk-property-business")(body)


    def getRemoveClientOverseasProperty: WSResponse = get("/business/remove-overseas-property-business")


    def submitRemoveClientOverseasProperty(body: Map[String, Seq[String]]): WSResponse = post("/business/remove-overseas-property-business")(body)

    def getOverseasPropertyCheckYourAnswers(sessionData: Map[String, String] = Map()): WSResponse = {
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

    def getTaskList(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = {
      get("/business/task-list", sessionData)
    }

    def submitTaskList(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino,
      ITSASessionKeys.UTR -> testUtr
    )): WSResponse = {
      post("/business/task-list", sessionData)(Map.empty)
    }

    def getAddAnotherClient(hasSubmitted: Boolean): WSResponse =
      if (hasSubmitted)
        get("/add-another", Map(ITSASessionKeys.MTDITID -> testMTDID))
      else
        get("/add-another")

    def confirmation(): WSResponse = get("/confirmation")

    def submitAccountingYear(inEditMode: Boolean, sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    ), request: Option[AccountingYear]): WSResponse = {
      val uri = s"/business/what-year-to-sign-up?editMode=$inEditMode"
      post(uri, sessionData)(
        request.fold(Map.empty[String, Seq[String]])(
          model => AccountingYearForm.accountingYearForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def submitPropertyAccountingMethod(inEditMode: Boolean, request: Option[AccountingMethod], sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = {
      val uri = s"/business/accounting-method-property?editMode=$inEditMode"
      post(uri, sessionData)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def submitOverseasPropertyAccountingMethod(inEditMode: Boolean, request: Option[AccountingMethod], sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = {
      val uri = s"/business/overseas-property-accounting-method?editMode=$inEditMode"
      post(uri, sessionData)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def getTaxYearCheckYourAnswers(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = {
      get("/business/tax-year-check-your-answers", sessionData)
    }

    def submitTaxYearCheckYourAnswers(sessionData: Map[String, String] = Map(
      UserMatchingSessionUtil.firstName -> testFirstName,
      UserMatchingSessionUtil.lastName -> testLastName,
      ITSASessionKeys.NINO -> testNino
    )): WSResponse = {
      post("/business/tax-year-check-your-answers", sessionData)(Map.empty)
    }

    def getProgressSaved(saveAndRetrieveLocation: Option[String] = None, sessionData: Map[String, String] = Map(
      ITSASessionKeys.ArnKey -> testARN,
      ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
      ITSASessionKeys.NINO -> testNino,
      ITSASessionKeys.UTR -> testUtr
    )): WSResponse = {
      get(
        saveAndRetrieveLocation.fold(
          "/business/progress-saved"
        )(
          location => s"/business/progress-saved?location=$location"
        ), sessionData
      )
    }

    def noSA(): WSResponse = get("/register-for-SA")

    def getRouting(editMode: Boolean = false): WSResponse = get(s"/business/routing?editMode=$editMode")

    def cannotReportYet(): WSResponse = get("/error/cannot-report-yet")

    def submitCannotReportYet(editMode: Boolean): WSResponse =
      post(s"/error/cannot-report-yet${if (editMode) "?editMode=true" else ""}")(Map.empty)

  }

  def toFormData[T](form: Form[T], data: T): Map[String, Seq[String]] =
    form.fill(data).data map { case (k, v) => k -> Seq(v) }

  implicit val nilWrites: Writes[Nil.type] = (o: Nil.type) => JsArray()

  def removeHtmlMarkup(stringWithMarkup: String): String =
    stringWithMarkup.replaceAll("<.+?>", " ").replaceAll("[\\s]{2,}", " ").trim

  implicit class CustomSelectors(element: Element) {

    def selectHead(selector: String): Element = {
      element.select(selector).asScala.headOption match {
        case Some(element) => element
        case None => fail(s"No elements returned for selector: $selector")
      }
    }

    def selectSeq(selector: String): Seq[Element] = {
      element.select(selector).asScala.toSeq
    }

    def selectNth(selector: String, nth: Int): Element = {
      selectSeq(s"$selector").lift(nth - 1) match {
        case Some(element) => element
        case None => fail(s"Could not retrieve $selector number $nth")
      }
    }

    def selectOptionally(selector: String): Option[Element] = {
      element.select(selector).asScala.headOption
    }

    def content: Element = element.selectHead("article")

    def mainContent: Element = element.selectHead("main")

    def getParagraphs: Elements = element.getElementsByTag("p")

    def getNthParagraph(nth: Int): Element = element.selectHead(s"p:nth-of-type($nth)")

    def getNthUnorderedList(nth: Int): Element = element.selectHead(s"ul:nth-of-type($nth)")

    def getNthListItem(nth: Int): Element = element.selectHead(s"li:nth-of-type($nth)")

    def getBulletPoints: Elements = element.getElementsByTag("li")

    def getH1Element: Element = element.selectHead("h1")

    def getH2Elements: Elements = element.getElementsByTag("h2")

    def getFormElements: Elements = element.getElementsByClass("form-field-group")

    def getErrorSummaryMessage: Elements = element.select("#error-summary-display ul")

    def getErrorSummary: Elements = element.select("#error-summary-display")

    def getSubmitButton: Element = element.selectHead("button[type=submit]")

    def getGovUkSubmitButton: Element = element.getElementsByClass("govuk-button").asScala.head

    def getHintText: String = element.select(s"""[class=form-hint]""").text()

    def getForm: Element = element.selectHead("form")

    def getFieldset: Element = element.selectHead("fieldset")

    def getBackLink: Element = element.selectHead(s"a[class=link-back]")

    def getGovukBackLink: Element = element.selectHead("a[class=govuk-back-link]")

    def getParagraphNth(index: Int = 0): String = {
      element.select("p").get(index).text()
    }

    def getRadioButtonByIndex(index: Int = 0): Element = element.select("div .multiple-choice").get(index)

    def getSpan(id: String): Elements = element.select(s"""span[id=$id]""")

    def getLink(id: String): Element = element.selectHead(s"""a[id=$id]""")

    def getTextFieldInput(id: String): Elements = element.select(s"""input[id=$id]""")

    def getFieldErrorMessage(id: String): Elements = element.select(s"""a[id=$id-error-summary]""")

    //Check your answers selectors
    def getSummaryList: Element = element.selectHead("dl.govuk-summary-list")

    def getSummaryListRow(nth: Int): Element = {
      element.selectHead(s"div.govuk-summary-list__row:nth-of-type($nth)")
    }

    def getSummaryListKey: Element = element.selectHead("dt.govuk-summary-list__key")

    def getSummaryListValue: Element = element.selectHead("dd.govuk-summary-list__value")

    def getSummaryListActions: Element = element.selectHead("dd.govuk-summary-list__actions")

  }

  implicit class ElementTests(element: Element) {

    def mustHaveTextField(name: String, label: String): Assertion = {
      val eles = element.select(s"input[name=$name]")
      if (eles.isEmpty) fail(s"$name does not have an input field with name=$name\ncurrent list of inputs:\n[${element.select("input")}]")
      if (eles.size() > 1) fail(s"$name have multiple input fields with name=$name")
      val ele = eles.asScala.head
      ele.attr("type") mustBe "text"
      element.select(s"label[for=$name]").text() mustBe label
    }

    def listErrorMessages(errors: List[String]): Assertion = {
      errors.zipWithIndex.map {
        case (error, index) => element.select(s"span.error-notification:nth-child(${index + 1})").text mustBe error
      } forall (_ == succeed) mustBe true
    }

    def mustHaveDateField(id: String, legend: String, exampleDate: String, error: Option[String] = None): Assertion = {
      val ele = element.getElementById(id)
      ele.select("span.form-label-bold").text() mustBe legend
      ele.select("span.form-hint").text() mustBe exampleDate
      ele.tag().toString mustBe "fieldset"
      mustHaveTextField(s"$id.dateDay", "Day")
      mustHaveTextField(s"$id.dateMonth", "Month")
      mustHaveTextField(s"$id.dateYear", "Year")
      error.map { message =>
        ele.select("legend").select(".error-notification").attr("role") mustBe "tooltip"
        ele.select("legend").select(".error-notification").text mustBe message
      }.getOrElse(succeed)
    }

    def mustHavePara(paragraph: String): Assertion = {
      element.getElementsByTag("p").text() must include(paragraph)
    }

    def mustHaveErrorSummary(errors: List[String]): Assertion = {
      element.getErrorSummary.attr("class") mustBe "flash error-summary error-summary--show"
      element.getErrorSummary.attr("role") mustBe "alert"
      element.getErrorSummary.attr("aria-labelledby") mustBe "error-summary-heading"
      element.getErrorSummary.attr("tabindex") mustBe "-1"
      element.getErrorSummary.select("h2").attr("id") mustBe "error-summary-heading"
      element.getErrorSummary.select("h2").text mustBe "There is a problem"
      element.getErrorSummary.select("ul > li").text mustBe errors.mkString(" ")
    }


  }

}
