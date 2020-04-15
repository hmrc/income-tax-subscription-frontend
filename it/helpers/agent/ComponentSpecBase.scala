
package helpers.agent

import java.util.UUID

import auth.agent.{AgentJourneyState, AgentSignUp, AgentUserMatching}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import controllers.agent.ITSASessionKeys
import forms.agent._
import helpers.UserMatchingIntegrationRequestSupport
import helpers.agent.IntegrationTestConstants._
import helpers.agent.SessionCookieBaker._
import helpers.agent.WiremockHelper._
import helpers.agent.servicemocks.WireMockMethods
import helpers.servicemocks.AuditStub
import models.agent._
import models.individual.business.{AccountingPeriodModel, MatchTaxYearModel}
import models.individual.subscription.IncomeSourceType
import models.usermatching.UserDetailsModel
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api._
import play.api.data.Form
import play.api.http.HeaderNames
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsValue, Writes}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.Headers
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec

trait ComponentSpecBase extends UnitSpec
  with GivenWhenThen with TestSuite
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with I18nSupport with CustomMatchers with WireMockMethods {

  lazy val ws = app.injector.instanceOf[WSClient]

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
    .build
  override lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString
  val mockUrl = s"http://$mockHost:$mockPort"

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
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "microservice.services.enrolment-store-proxy.host" -> mockHost,
    "microservice.services.enrolment-store-proxy.port" -> mockPort,
    "microservice.services.users-groups-search.host" -> mockHost,
    "microservice.services.users-groups-search.port" -> mockPort
  )

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

    val defaultCookies: Map[String, String] = Map(
      ITSASessionKeys.ArnKey -> IntegrationTestConstants.testARN,
      ITSASessionKeys.JourneyStateKey -> AgentSignUp.name
    )

    val headers: Seq[(String, String)] = Seq(
      HeaderNames.COOKIE -> bakeSessionCookie(defaultCookies),
      "Csrf-Token" -> "nocheck"
    )

    implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(Headers(headers: _*))

    def get(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse =
      await(
        buildClient(uri)
          .withHeaders(HeaderNames.COOKIE -> bakeSessionCookie(defaultCookies ++ additionalCookies))
          .get()
      )

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty)(body: Map[String, Seq[String]]): WSResponse = await(
      buildClient(uri)
        .withHeaders(HeaderNames.COOKIE -> bakeSessionCookie(defaultCookies ++ additionalCookies), "Csrf-Token" -> "nocheck")
        .post(body)
    )

    def startPage(): WSResponse = get("/")

    def indexPage(journeySate: Option[AgentJourneyState] = None, sessionMap: Map[String, String] = Map.empty[String, String]): WSResponse = {
      get("/index", journeySate.fold(sessionMap)(state => sessionMap.+(ITSASessionKeys.JourneyStateKey -> state.name)))
    }

    def income(): WSResponse = get("/income")

    def mainIncomeError(): WSResponse = get("/error/main-income")

    def sessionTimeout(): WSResponse = get("/session-timeout")

    def showClientDetails(): WSResponse = get("/client-details", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

    def submitClientDetails(newSubmission: Option[UserDetailsModel], storedSubmission: Option[UserDetailsModel]): WSResponse =
      post("/client-details", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name).addUserDetails(storedSubmission))(
        newSubmission.fold(Map.empty: Map[String, Seq[String]])(
          cd => toFormData(ClientDetailsForm.clientDetailsValidationForm, cd)
        )
      )

    def showClientDetailsError(): WSResponse = get("/error/client-details", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

    def showClientDetailsLockout(): WSResponse = get("/error/lockout", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

    def showConfirmation(hasSubmitted: Boolean): WSResponse =
      if (hasSubmitted)
        get("/confirmation", Map(ITSASessionKeys.MTDITID -> testMTDID))
      else
        get("/confirmation")

    def feedback(): WSResponse = get("/feedback-submitted")

    def signIn(): WSResponse = get("/sign-in")

    def signOut(): WSResponse = get("/logout")

    def notEnrolledAgentServices(): WSResponse = get("/not-enrolled-agent-services")

    def noClientRelationship(): WSResponse = get("/error/no-client-relationship", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

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
        case count => Map(ITSASessionKeys.FailedClientMatching -> previouslyFailedAttempts.toString)
      }
      post("/confirm-client",
        additionalCookies = failedAttemptCounter ++ Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name)
          .addUserDetails(storedUserDetails)
      )(Map.empty)
    }

    def matchTaxYear(): WSResponse = get("/business/match-to-tax-year")

    def accountingYear(): WSResponse = get("/business/what-year-to-sign-up")

    def businessAccountingPeriodPrior(): WSResponse = get("/business/accounting-period-prior")

    def businessAccountingPeriodDates(): WSResponse = get("/business/accounting-period-dates")

    def businessAccountingMethod(): WSResponse = get("/business/accounting-method")

    def propertyAccountingMethod(): WSResponse = get("/business/accounting-method-property")

    def businessName(): WSResponse = get("/business/name")

    def getAddAnotherClient(hasSubmitted: Boolean): WSResponse =
      if (hasSubmitted)
        get("/add-another", Map(ITSASessionKeys.MTDITID -> testMTDID))
      else
        get("/add-another")

    def submitIncome(inEditMode: Boolean, request: Option[IncomeSourceType]): WSResponse = {
      val uri = s"/income?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            IncomeSourceForm.incomeSourceForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def confirmation(): WSResponse = get("/confirmation")

    def submitMatchTaxYear(inEditMode: Boolean, request: Option[MatchTaxYearModel]): WSResponse = {
      val uri = s"/business/match-to-tax-year?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model => MatchTaxYearForm.matchTaxYearForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def submitAccountingYear(inEditMode: Boolean, request: Option[AccountingYearModel]): WSResponse = {
      val uri = s"/business/what-year-to-sign-up?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model => AccountingYearForm.accountingYearForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
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
      val uri = s"/business/accounting-method-property?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def noSA(): WSResponse = get("/register-for-SA")

    def cannotReportYet(): WSResponse = get("/error/cannot-report-yet")

    def submitCannotReportYet(editMode: Boolean): WSResponse =
      post(s"/error/cannot-report-yet${if (editMode) "?editMode=true" else ""}")(Map.empty)

  }

  def toFormData[T](form: Form[T], data: T): Map[String, Seq[String]] =
    form.fill(data).data map { case (k, v) => k -> Seq(v) }

  implicit val nilWrites: Writes[Nil.type] = new Writes[Nil.type] {
    override def writes(o: Nil.type): JsValue = JsArray()
  }

  def removeHtmlMarkup(stringWithMarkup: String): String =
    stringWithMarkup.replaceAll("<.+?>", " ").replaceAll("[\\s]{2,}", " ").trim

}
