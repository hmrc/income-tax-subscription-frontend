/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.agent.eligibility

import auth.agent.StatelessController
import config.AppConfig
import forms.agent.SoleTraderForm.soleTraderForm
import models._
import models.audits.EligibilityAnswerAuditing.EligibilityAnswerAuditModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.eligibility.AreYouASoleTrader

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

@Singleton
class SoleTraderController @Inject()(val auditingService: AuditingService,
                                     val authService: AuthService,
                                     areYouASoleTrader: AreYouASoleTrader)
                                    (implicit val appConfig: AppConfig,
                                     mcc: MessagesControllerComponents,
                                     override val languageUtils: LanguageUtils,
                                     val ec: ExecutionContext) extends StatelessController with I18nSupport with ImplicitDateFormatter {

  private def startDateLimit: LocalDate = LocalDate.now.minusYears(2)

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  private def formatNino(clientNino: String): String = {
    clientNino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }
  }

  def backUrl: String = routes.OtherSourcesOfIncomeController.show.url

  def view(form: Form[YesNo], startDateLimit: LocalDate, clientName: String, clientNino: String)(implicit request: Request[_]): Html = {
    areYouASoleTrader(
      soleTraderForm = form,
      postAction = routes.SoleTraderController.submit(),
      startDateLimit = startDateLimit.toLongDate,
      clientName,
      clientNino,
      backUrl = backUrl
    )
  }

  def show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      Ok(view(
        form = soleTraderForm(startDateLimit.toLongDate),
        startDateLimit = startDateLimit,
        clientName = request.fetchClientName.getOrElse(
          throw new InternalServerException("[AccountingPeriodCheckController][show] - could not retrieve client name from session")
        ),
        clientNino = formatNino(user.clientNino.getOrElse(
          throw new InternalServerException("[AccountingPeriodCheckController][show] - could not retrieve client nino from session")
        )),
      ))
  }

  def submit(): Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      val clientName = request.fetchClientName.get
      val clientNino = user.clientNino.get
      soleTraderForm(startDateLimit.toLongDate).bindFromRequest().fold(
        formWithErrors => BadRequest(view(
          form = formWithErrors,
          startDateLimit = startDateLimit,
          clientName,
          clientNino
        )), {
          case Yes =>
            auditingService.audit(EligibilityAnswerAuditModel(eligible = false, "yes", "soleTraderBusinessStartDate", user.arn))
            Redirect(routes.CannotTakePartController.show)
          case No =>
            auditingService.audit(EligibilityAnswerAuditModel(eligible = true, "no", "soleTraderBusinessStartDate", user.arn))
            Redirect(routes.PropertyTradingStartAfterController.show())
        }
      )
  }
}
