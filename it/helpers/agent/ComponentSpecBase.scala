
package helpers.agent

import auth.agent.{AgentJourneyState, AgentSignUp, AgentUserMatching}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import config.AppConfig
import config.featureswitch.FeatureSwitching
import controllers.agent.ITSASessionKeys
import forms.agent._
import helpers.UserMatchingIntegrationRequestSupport
import helpers.agent.IntegrationTestConstants._
import helpers.agent.WiremockHelper._
import helpers.agent.servicemocks.WireMockMethods
import helpers.servicemocks.AuditStub
import models.common._
import models.common.business.{AccountingMethodModel, BusinessNameModel}
import models.usermatching.UserDetailsModel
import models.{AccountingYear, DateModel, YesNo}
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
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
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.Headers
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec
import utilities.UserMatchingSessionUtil

import java.time.LocalDate
import java.util.UUID
import scala.collection.JavaConversions._

trait ComponentSpecBase extends UnitSpec
  with GivenWhenThen with TestSuite
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with CustomMatchers with WireMockMethods with SessionCookieBaker with FeatureSwitching {

  lazy val ws = app.injector.instanceOf[WSClient]

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  override lazy val cookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

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
          .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(defaultCookies ++ additionalCookies))
          .get()
      )

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty)(body: Map[String, Seq[String]]): WSResponse = await(
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(defaultCookies ++ additionalCookies), "Csrf-Token" -> "nocheck")
        .post(body)
    )

    def startPage(): WSResponse = get("/")

    def indexPage(journeySate: Option[AgentJourneyState] = None, sessionMap: Map[String, String] = Map.empty[String, String]): WSResponse = {
      get("/index", journeySate.fold(sessionMap)(state => sessionMap.+(ITSASessionKeys.JourneyStateKey -> state.name)))
    }

    def showOtherSourcesOfIncome: WSResponse = get("/eligibility/income-sources")

    def submitOtherSourcesOfIncome(request: Option[YesNo]): WSResponse = post("/eligibility/income-sources")(
      request.fold(Map.empty[String, Seq[String]])(
        model => OtherSourcesOfIncomeForm.otherSourcesOfIncomeForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def showPropertyTradingStartAfter: WSResponse = get("/eligibility/property-start-date")

    def submitPropertyTradingStartAfter(request: Option[YesNo]): WSResponse = post("/eligibility/property-start-date")(
      request.fold(Map.empty[String, Seq[String]])(
        model => PropertyTradingStartDateForm.propertyTradingStartDateForm("").fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def showCannotTakePart: WSResponse = get("/error/cannot-sign-up")

    def showCovid19ClaimCheck(): WSResponse = get("/eligibility/covid-19")

    def submitCovid19ClaimCheck(request: Option[YesNo]): WSResponse = post("/eligibility/covid-19")(
      request.fold(Map.empty[String, Seq[String]])(
        model => Covid19ClaimCheckForm.covid19ClaimCheckForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def showSoleTrader(): WSResponse = get("/eligibility/sole-trader-start-date")

    def submitSoleTraderForm(request: Option[YesNo]): WSResponse = post("/eligibility/sole-trader-start-date")(
      request.fold(Map.empty[String, Seq[String]])(
        model => SoleTraderForm.soleTraderForm("").fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )


    def showCovidCannotSignUp(): WSResponse = get("/error/covid-cannot-sign-up")

    def showAccountingPeriodCheck: WSResponse = get("/eligibility/accounting-period-check")

    def submitAccountingPeriodCheck(request: Option[YesNo]): WSResponse = post("/eligibility/accounting-period-check")(
      request.fold(Map.empty[String, Seq[String]])(
        model => AccountingPeriodCheckForm.accountingPeriodCheckForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def income(): WSResponse = get("/income")

    def submitIncomeSource(inEditMode: Boolean, request: Option[IncomeSourceModel]): WSResponse = {
      val uri = s"/income?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            IncomeSourceForm.incomeSourceForm(true).fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def mainIncomeError(): WSResponse = get("/error/main-income")

    def sessionTimeout(): WSResponse = get("/session-timeout")

    def keepAlive(sessionKeys: Map[String, String] = Map.empty): WSResponse = get("/keep-alive", sessionKeys)

    def timeout(sessionKeys: Map[String, String] = Map.empty): WSResponse = get("/timeout", sessionKeys)

    def showClientDetails(): WSResponse = get("/client-details", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

    def submitClientDetails(newSubmission: Option[UserDetailsModel], storedSubmission: Option[UserDetailsModel]): WSResponse =
      post("/client-details", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name).addUserDetails(storedSubmission))(
        newSubmission.fold(Map.empty: Map[String, Seq[String]])(
          cd => toFormData(ClientDetailsForm.clientDetailsValidationForm, cd)
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

    def getNoClientRelationship(): WSResponse = get("/error/no-client-relationship", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

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
        case count => Map(ITSASessionKeys.FailedClientMatching -> previouslyFailedAttempts.toString)
      }
      post("/confirm-client",
        additionalCookies = failedAttemptCounter ++ Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name)
          .addUserDetails(storedUserDetails)
      )(Map.empty)
    }

    def accountingYear(): WSResponse = get("/business/what-year-to-sign-up")

    def businessAccountingPeriodPrior(): WSResponse = get("/business/accounting-period-prior")

    def businessAccountingMethod(): WSResponse = get("/business/accounting-method")

    def propertyAccountingMethod(): WSResponse = get("/business/accounting-method-property")

    def overseasPropertyAccountingMethod(): WSResponse = get("/business/overseas-property-accounting-method")

    def businessName(): WSResponse = get("/business/name")

    def ukPropertyStartDate(): WSResponse = get("/business/property-commencement-date")

    def submitUkPropertyStartDate(isEditMode: Boolean = false, request: Option[PropertyStartDateModel]): WSResponse = {
      val testValidMaxStartDate: String = DateModel.dateConvert(LocalDate.now.minusYears(1)).toString
      val testValidMinStartDate: String = DateModel.dateConvert(LocalDate.of(1900, 1, 1)).toString
      val uri = s"/business/property-commencement-date?editMode=$isEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            PropertyStartDateForm.propertyStartDateForm(testValidMinStartDate, testValidMaxStartDate).fill(model).data.map { case (k, v) =>
              (k, Seq(v))
            }
        )
      )
    }

    def overseasPropertyStartDate(): WSResponse = get("/business/overseas-commencement-date")

    def submitOverseasPropertyStartDate(inEditMode: Boolean, request: Option[OverseasPropertyStartDateModel]): WSResponse = {
      val testValidMaxStartDate: String = DateModel.dateConvert(LocalDate.now.minusYears(1)).toString
      val testValidMinStartDate: String = DateModel.dateConvert(LocalDate.of(1900, 1, 1)).toString
      val uri = s"/business/overseas-commencement-date?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            OverseasPropertyStartDateForm.overseasPropertyStartDateForm(testValidMinStartDate, testValidMaxStartDate).fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def getAddAnotherClient(hasSubmitted: Boolean): WSResponse =
      if (hasSubmitted)
        get("/add-another", Map(ITSASessionKeys.MTDITID -> testMTDID))
      else
        get("/add-another")

    def submitIncome(inEditMode: Boolean, request: Option[IncomeSourceModel]): WSResponse = {
      val uri = s"/income?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            IncomeSourceForm.incomeSourceForm(true).fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def confirmation(): WSResponse = get("/confirmation")

    def submitAccountingYear(inEditMode: Boolean, request: Option[AccountingYear]): WSResponse = {
      val uri = s"/business/what-year-to-sign-up?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model => AccountingYearForm.accountingYearForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
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

    def submitOverseasPropertyAccountingMethod(inEditMode: Boolean, request: Option[OverseasAccountingMethodPropertyModel]): WSResponse = {
      val uri = s"/business/overseas-property-accounting-method?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
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

  implicit val nilWrites: Writes[Nil.type] = new Writes[Nil.type] {
    override def writes(o: Nil.type): JsValue = JsArray()
  }

  def removeHtmlMarkup(stringWithMarkup: String): String =
    stringWithMarkup.replaceAll("<.+?>", " ").replaceAll("[\\s]{2,}", " ").trim

  implicit class CustomSelectors(element: Element) {

    def firstOf(selector: String): Element = {
      element.select(selector).headOption match {
        case Some(element) => element
        case None => fail(s"No elements returned for selector: $selector")
      }
    }

    def selectOptionally(selector: String): Option[Element] = {
      element.select(selector).headOption
    }

    def content: Element = element.firstOf("article")

    def mainContent: Element = element.firstOf("main")

    def getParagraphs: Elements = element.getElementsByTag("p")

    def getNthParagraph(nth: Int): Element = element.firstOf(s"p:nth-of-type($nth)")

    def getNthUnorderedList(nth: Int): Element = element.firstOf(s"ul:nth-of-type($nth)")

    def getNthListItem(nth: Int): Element = element.firstOf(s"li:nth-of-type($nth)")

    def getBulletPoints: Elements = element.getElementsByTag("li")

    def getH1Element: Element = element.firstOf("h1")

    def getH2Elements: Elements = element.getElementsByTag("h2")

    def getFormElements: Elements = element.getElementsByClass("form-field-group")

    def getErrorSummaryMessage: Elements = element.select("#error-summary-display ul")

    def getErrorSummary: Elements = element.select("#error-summary-display")

    def getSubmitButton: Element = element.firstOf("button[type=submit]")

    def getGovUkSubmitButton: Element = element.getElementsByClass("govuk-button").head

    def getHintText: String = element.select(s"""[class=form-hint]""").text()

    def getForm: Element = element.firstOf("form")

    def getFieldset: Element = element.firstOf("fieldset")

    def getBackLink: Element = element.firstOf(s"a[class=link-back]")

    def getGovukBackLink: Element = element.firstOf("a[class=govuk-back-link]")

    def getParagraphNth(index: Int = 0): String = {
      element.select("p").get(index).text()
    }

    def getRadioButtonByIndex(index: Int = 0): Element = element.select("div .multiple-choice").get(index)

    def getSpan(id: String): Elements = element.select(s"""span[id=$id]""")

    def getLink(id: String): Element = element.firstOf(s"""a[id=$id]""")

    def getTextFieldInput(id: String): Elements = element.select(s"""input[id=$id]""")

    def getFieldErrorMessage(id: String): Elements = element.select(s"""a[id=$id-error-summary]""")

    //Check your answers selectors
    def getSummaryList: Element = element.firstOf("dl.govuk-summary-list")

    def getSummaryListRow(nth: Int): Element = {
      element.firstOf(s"div.govuk-summary-list__row:nth-of-type($nth)")
    }

    def getSummaryListKey: Element = element.firstOf("dt.govuk-summary-list__key")

    def getSummaryListValue: Element = element.firstOf("dd.govuk-summary-list__value")

    def getSummaryListActions: Element = element.firstOf("dd.govuk-summary-list__actions")

  }

  implicit class ElementTests(element: Element) {

    def mustHaveTextField(name: String, label: String): Assertion = {
      val eles = element.select(s"input[name=$name]")
      if (eles.isEmpty) fail(s"$name does not have an input field with name=$name\ncurrent list of inputs:\n[${element.select("input")}]")
      if (eles.size() > 1) fail(s"$name have multiple input fields with name=$name")
      val ele = eles.head
      ele.attr("type") shouldBe "text"
      element.select(s"label[for=$name]").text() shouldBe label
    }

    def listErrorMessages(errors: List[String]): Assertion = {
      errors.zipWithIndex.map {
        case (error, index) => element.select(s"span.error-notification:nth-child(${index + 1})").text shouldBe error
      } forall (_ == succeed) shouldBe true
    }

    def mustHaveDateField(id: String, legend: String, exampleDate: String, error: Option[String] = None): Assertion = {
      val ele = element.getElementById(id)
      ele.select("span.form-label-bold").text() shouldBe legend
      ele.select("span.form-hint").text() shouldBe exampleDate
      ele.tag().toString shouldBe "fieldset"
      mustHaveTextField(s"$id.dateDay", "Day")
      mustHaveTextField(s"$id.dateMonth", "Month")
      mustHaveTextField(s"$id.dateYear", "Year")
      error.map { message =>
        ele.select("legend").select(".error-notification").attr("role") shouldBe "tooltip"
        ele.select("legend").select(".error-notification").text shouldBe message
      }.getOrElse(succeed)
    }

    def mustHavePara(paragraph: String): Assertion = {
      element.getElementsByTag("p").text() should include(paragraph)
    }

    def mustHaveErrorSummary(errors: List[String]): Assertion = {
      element.getErrorSummary.attr("class") shouldBe "flash error-summary error-summary--show"
      element.getErrorSummary.attr("role") shouldBe "alert"
      element.getErrorSummary.attr("aria-labelledby") shouldBe "error-summary-heading"
      element.getErrorSummary.attr("tabindex") shouldBe "-1"
      element.getErrorSummary.select("h2").attr("id") shouldBe "error-summary-heading"
      element.getErrorSummary.select("h2").text shouldBe "There is a problem"
      element.getErrorSummary.select("ul > li").text shouldBe errors.mkString(" ")
    }


  }

}
