/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.controllers.business

import agent.auth.AuthenticatedController
import agent.forms.MatchTaxYearForm
import agent.models.MatchTaxYearModel
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.models.{No, Yes}
import core.services.AuthService
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}

class MatchTaxYearController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService)
                                      (implicit val ec: ExecutionContext) extends AuthenticatedController {

  def backUrl(isEditMode: Boolean): String = if(isEditMode) {
    agent.controllers.routes.CheckYourAnswersController.show().url
  } else {
    agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url
  }

  private def view(matchTaxYearForm: Form[MatchTaxYearModel], isEditMode: Boolean)(implicit request: Request[AnyContent]): Html =
    agent.views.html.business.match_to_tax_year(
      matchTaxYearForm,
      agent.controllers.business.routes.MatchTaxYearController.submit(isEditMode),
      backUrl(isEditMode),
      isEditMode
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchMatchTaxYear() map { matchTaxYear =>
        Ok(view(MatchTaxYearForm.matchTaxYearForm.fill(matchTaxYear), isEditMode))
      }
  }

  private def redirectLocation(currentAnswer: MatchTaxYearModel, isEditMode: Boolean)(implicit request: Request[AnyContent]): Future[Result] = {
    keystoreService.fetchMatchTaxYear map {
      case Some(`currentAnswer`) if isEditMode =>
        Redirect(agent.controllers.routes.CheckYourAnswersController.show())
      case _ =>
        currentAnswer.matchTaxYear match {
          case Yes => Redirect(agent.controllers.business.routes.BusinessAccountingMethodController.show())
          case No => Redirect(agent.controllers.business.routes.BusinessAccountingPeriodDateController.show())
        }
    }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      MatchTaxYearForm.matchTaxYearForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(matchTaxYearForm = formWithErrors, isEditMode = isEditMode))),
        matchTaxYear => for {
          redirect <- redirectLocation(matchTaxYear, isEditMode)
          _ <- keystoreService.saveMatchTaxYear(matchTaxYear)
        } yield redirect
      )
  }
}
