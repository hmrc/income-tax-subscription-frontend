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

import config.AppConfig
import controllers.ITSASessionKeys.GoHome
import forms._
import helpers.SessionCookieBaker._
import helpers.servicemocks.{AuditStub, WireMockMethods}
import models._
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

trait ComponentSpecBase extends UnitSpec
  with GivenWhenThen with TestSuite
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with I18nSupport with CustomMatchers with WireMockMethods {

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
    "preferences.url" -> mockUrl,
    "microservice.services.preferences-frontend.host" -> mockHost,
    "microservice.services.preferences-frontend.port" -> mockPort,
    "microservice.services.feature-switch.show-guidance" -> "true",
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "microservice.services.gg-admin.host" -> mockHost,
    "microservice.services.gg-admin.port" -> mockPort,
    "microservice.services.government-gateway.host" -> mockHost,
    "microservice.services.government-gateway.port" -> mockPort,
    "microservice.services.gg-authentication.host" -> mockHost,
    "microservice.services.gg-authentication.port" -> mockPort
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build

  val appConfig = app.injector.instanceOf[AppConfig]

  override lazy val messagesApi = app.injector.instanceOf[MessagesApi]

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
    AuditStub.stubAuditing()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  object IncomeTaxSubscriptionFrontend {
    val csrfToken = UUID.randomUUID().toString

    def get(uri: String): WSResponse = await(
      buildClient(uri)
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie(Map(GoHome -> "et")))
        .get()
    )

    def post(uri: String)(body: Map[String, Seq[String]]): WSResponse = await(
      buildClient(uri)
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie(Map(GoHome -> "et")), "Csrf-Token" -> "nocheck")
        .post(body)
    )

    def startPage(): WSResponse = get("/")

    def preferences(): WSResponse = get("/preferences")

    def indexPage(): WSResponse = get("/index")

    def income(): WSResponse = get("/income")

    def otherIncome(): WSResponse = get("/income-other")

    def mainIncomeError(): WSResponse = get("/error/main-income")

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

    def submitTerms(): WSResponse = post("/terms")(Map.empty)

    def submitExitSurvey(): WSResponse = post("/exit-survey")(Map.empty)

    def businessAccountingPeriodPrior(): WSResponse = get("/business/accounting-period-prior")

    def businessAccountingPeriodDates(): WSResponse = get("/business/accounting-period-dates")

    def registerNextAccountingPeriod(): WSResponse = get("/business/register-next-accounting-period")

    def businessAccountingMethod(): WSResponse = get("/business/accounting-method")

    def businessName(): WSResponse = get("/business/name")

    def maintenance(): WSResponse = get("/error/maintenance")

    def noNino(): WSResponse = get("/error/no-nino")

    def exitSurvey(): WSResponse = get("/exit-survey")

    def submitRegisterNextAccountingPeriod(): WSResponse = post("/business/register-next-accounting-period")(Map.empty)

    def submitMaintenance(): WSResponse = post("/error/maintenance")(Map.empty)

    def submitNoNino(): WSResponse = post("/error/no-nino")(Map.empty)

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

    def submitAccountingMethod(inEditMode: Boolean, request: Option[AccountingMethodModel]): WSResponse = {
      val uri = s"/business/accounting-method?editMode=$inEditMode"
      post(uri)(
        request.fold(Map.empty[String, Seq[String]])(
          model =>
            AccountingMethodForm.accountingMethodForm.fill(model).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def iv(): WSResponse = get("iv")
  }

  def toFormData[T](form: Form[T], data: T): Map[String, Seq[String]] =
    form.fill(data).data map { case (k, v) => k -> Seq(v) }

  implicit val nilWrites: Writes[Nil.type] = new Writes[Nil.type] {
    override def writes(o: Nil.type): JsValue = JsArray()
  }

  def removeHtmlMarkup(stringWithMarkup: String): String =
    stringWithMarkup.replaceAll("<.+?>", " ").replaceAll("[\\s]{2,}", " ").trim

}
