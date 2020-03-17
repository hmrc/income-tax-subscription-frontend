/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.agent

import core.config.AppConfig
import core.utils.Implicits._
import forms.agent.ExitSurveyForm
import javax.inject.{Inject, Singleton}
import models.agent.ExitSurveyModel
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.{AuditModel, AuditingService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class ExitSurveyController @Inject()(val messagesApi: MessagesApi,
                                     auditingService: AuditingService)
                                    (implicit appConfig: AppConfig) extends FrontendController with I18nSupport {

  def view(exitSurveyForm: Form[ExitSurveyModel])(implicit request: Request[_]): Html =
    views.html.agent.exit_survey(
      exitSurveyForm = exitSurveyForm,
      routes.ExitSurveyController.submit()
    )

  def show(origin: String): Action[AnyContent] = Action { implicit request =>
    Ok(view(ExitSurveyForm.exitSurveyForm.form))
  }

  val submit: Action[AnyContent] = Action.async { implicit request =>
    ExitSurveyForm.exitSurveyForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(exitSurveyForm = formWithErrors))),
      survey => {
        submitSurvey(survey)
        Redirect(routes.ThankYouController.show())
      }
    )
  }

  private def submitSurvey(survey: ExitSurveyModel)(implicit hc: HeaderCarrier, request: Request[_]): Unit = {
    auditingService.audit(SurveyAuditModel(survey))
  }

  private[controllers] final def surveyFormDataToMap(survey: ExitSurveyModel): Map[String, String] = {
    survey.getClass.getDeclaredFields.map {
      field =>
        field.setAccessible(true)
        field.getName -> (field.get(survey) match {
          case Some(x) => x.toString
          case xs: Seq[Any] => xs.mkString(",")
          case x => x.toString
        })
    }.toMap.filter { case (x, y) =>
      // don't keep any fields in the map if they were not answered
      y match {
        case "None" => false
        case _ => true
      }
    }
  }

  case class SurveyAuditModel(survey: ExitSurveyModel) extends AuditModel {
    override val transactionName: String = "ITSA Survey"
    override val detail: Map[String, String] = surveyFormDataToMap(survey)
    override val auditType: String = "mtdItsaExitSurveyFeedback"
  }

}
