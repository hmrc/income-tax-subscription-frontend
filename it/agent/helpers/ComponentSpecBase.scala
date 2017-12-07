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

package agent.helpers

import java.util.UUID

import _root_.agent.auth.{AgentJourneyState, AgentSignUp, AgentUserMatching}
import _root_.agent.controllers.ITSASessionKeys
import _root_.agent.forms._
import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.SessionCookieBaker._
import _root_.agent.helpers.servicemocks.WireMockMethods
import _root_.agent.models._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import helpers.servicemocks.AuditStub
import incometax.business.models.AccountingPeriodModel
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api._
import play.api.data.Form
import play.api.http.HeaderNames
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsValue, Writes}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.Headers
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec
import usermatching.models.UserDetailsModel

trait ComponentSpecBase extends UnitSpec
  with GivenWhenThen with TestSuite
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with I18nSupport with CustomMatchers with WireMockMethods {

  import WiremockHelper._

  lazy val ws = app.injector.instanceOf[WSClient]

  lazy val wmConfig = wireMockConfig().port(wiremockPort)
  lazy val wireMockServer = new WireMockServer(wmConfig)

  def startWiremock() = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock() = wireMockServer.stop()

  def resetWiremock() = WireMock.reset()

  def buildClient(path: String) = ws.url(s"http://localhost:$port$baseURI$path").withFollowRedirects(false)

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build
  override lazy val messagesApi = app.injector.instanceOf[MessagesApi]
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort.toString
  val mockUrl = s"http://$mockHost:$mockPort"

  def config: Map[String, String] = Map(
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "microservice.services.gg-admin.host" -> mockHost,
    "microservice.services.gg-admin.port" -> mockPort,
    "microservice.services.session-cache.host" -> mockHost,
    "microservice.services.session-cache.port" -> mockPort,
    "microservice.services.subscription-service.host" -> mockHost,
    "microservice.services.subscription-service.port" -> mockPort,
    "microservice.services.authenticator.host" -> mockHost,
    "microservice.services.authenticator.port" -> mockPort,
    "microservice.services.agent-microservice.host" -> mockHost,
    "microservice.services.agent-microservice.port" -> mockPort,
    "microservice.services.feature-switch.show-guidance" -> "true",
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort
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


  object IncomeTaxSubscriptionFrontend {
    val csrfToken = UUID.randomUUID().toString

    val defaultCookies = Map(
      ITSASessionKeys.ArnKey -> IntegrationTestConstants.testARN,
      ITSASessionKeys.JourneyStateKey -> AgentSignUp.name
    )

    val headers = Seq(
      HeaderNames.COOKIE -> bakeSessionCookie(defaultCookies),
      "Csrf-Token" -> "nocheck"
    )

    implicit val headerCarrier = HeaderCarrierConverter.fromHeadersAndSession(Headers(headers: _*))

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

    def otherIncome(): WSResponse = get("/income-other")

    def mainIncomeError(): WSResponse = get("/error/main-income")

    def submitOtherIncomeError(): WSResponse = post("/error/other-income")(Map.empty)

    def otherIncomeError(): WSResponse = get("/error/other-income")

    def terms(): WSResponse = get("/terms")

    def sessionTimeout(): WSResponse = get("/session-timeout")

    def showClientDetails(): WSResponse = get("/client-details", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))

    def submitClientDetails(clientDetails: Option[UserDetailsModel]): WSResponse =
      post("/client-details", Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))(
        clientDetails.fold(Map.empty: Map[String, Seq[String]])(
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

    def thankYou(): WSResponse = get("/thankyou")

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

    def submitCheckYourAnswers(): WSResponse = post("/check-your-answers", Map(
      ITSASessionKeys.ArnKey -> testARN,
      ITSASessionKeys.JourneyStateKey -> AgentSignUp.name,
      ITSASessionKeys.NINO -> testNino,
      ITSASessionKeys.UTR -> testUtr
    ))(Map.empty)

    def submitConfirmClient(previouslyFailedAttempts: Int = 0): WSResponse = {
      val failedAttemptCounter: Map[String, String] = previouslyFailedAttempts match {
        case 0 => Map.empty
        case count => Map(ITSASessionKeys.FailedClientMatching -> previouslyFailedAttempts.toString)
      }
      post("/confirm-client", additionalCookies = failedAttemptCounter ++ Map(ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name))(Map.empty)
    }

    def submitTerms(): WSResponse = post("/terms")(Map.empty)

    def submitExitSurvey(): WSResponse = post("/exit-survey")(Map.empty)

    def exitSurvey(origin: String): WSResponse = get(s"/exit-survey?origin=$origin")

    def businessAccountingPeriodPrior(): WSResponse = get("/business/accounting-period-prior")

    def businessAccountingPeriodDates(): WSResponse = get("/business/accounting-period-dates")

    def registerNextAccountingPeriod(): WSResponse = get("/business/register-next-accounting-period")

    def businessAccountingMethod(): WSResponse = get("/business/accounting-method")

    def businessName(): WSResponse = get("/business/name")

    def getAddAnotherClient(hasSubmitted: Boolean): WSResponse =
      if (hasSubmitted)
        get("/add-another", Map(ITSASessionKeys.MTDITID -> testMTDID))
      else
        get("/add-another")

    def submitRegisterNextAccountingPeriod(): WSResponse = post("/business/register-next-accounting-period")(Map.empty)

    def submitBusinessAccountingPeriodPrior(inEditMode: Boolean, request: Option[AccountingPeriodPriorModel]): WSResponse = {
      val uri = s"/business/accounting-period-prior?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingPeriodPriorForm.accountingPeriodPriorForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
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

    def submitAccountingMethod(inEditMode: Boolean, request: Option[AccountingMethodModel]): WSResponse = {
      val uri = s"/business/accounting-method?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingMethodForm.accountingMethodForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def noSA(): WSResponse = get("/register-for-SA")

  }

  def toFormData[T](form: Form[T], data: T): Map[String, Seq[String]] =
    form.fill(data).data map { case (k, v) => k -> Seq(v) }

  implicit val nilWrites: Writes[Nil.type] = new Writes[Nil.type] {
    override def writes(o: Nil.type): JsValue = JsArray()
  }

  def removeHtmlMarkup(stringWithMarkup: String): String =
    stringWithMarkup.replaceAll("<.+?>", " ").replaceAll("[\\s]{2,}", " ").trim

}
