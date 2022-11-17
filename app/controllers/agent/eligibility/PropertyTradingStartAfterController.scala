/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.agent.PropertyTradingStartDateForm.propertyTradingStartDateForm
import models.audits.EligibilityAnswerAuditing
import models.audits.EligibilityAnswerAuditing.EligibilityAnswerAuditModel
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService}
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import views.html.agent.eligibility.PropertyTradingAfter

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PropertyTradingStartAfterController @Inject()(val auditingService: AuditingService,
                                                    val authService: AuthService,
                                                    val languageUtils: LanguageUtils,
                                                    propertyTradingAfter: PropertyTradingAfter)
                                                   (implicit val appConfig: AppConfig,
                                                    mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext) extends StatelessController with ImplicitDateFormatter {

  private def startDateLimit: LocalDate = LocalDate.now.minusYears(1)

  def backUrl: String = routes.SoleTraderController.show().url

  def view(form: Form[YesNo], startDateLimit: LocalDate)(implicit request: Request[_]): Html = {
    propertyTradingAfter(
      propertyTradingBeforeDateForm = form,
      postAction = routes.PropertyTradingStartAfterController.submit(),
      startDateLimit = startDateLimit.toLongDate,
      backUrl = backUrl
    )
  }

  def show: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      Ok(view(
        form = propertyTradingStartDateForm(startDateLimit.toLongDate),
        startDateLimit = startDateLimit
      ))
  }

  def submit(): Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      val arn: Option[String] = user.arn
      propertyTradingStartDateForm(startDateLimit.toLongDate).bindFromRequest().fold(
        formWithErrors => BadRequest(view(
          form = formWithErrors,
          startDateLimit = startDateLimit
        )), {
          case Yes =>
            auditingService.audit(EligibilityAnswerAuditModel(EligibilityAnswerAuditing.eligibilityAnswerAgent, eligible = false, "yes",
              "propertyBusinessStartDate", arn))
            Redirect(routes.CannotTakePartController.show)
          case No =>
            auditingService.audit(EligibilityAnswerAuditModel(EligibilityAnswerAuditing.eligibilityAnswerAgent, eligible = true, "no",
              "propertyBusinessStartDate", arn))
            Redirect(routes.AccountingPeriodCheckController.show)
        }
      )
  }

}
