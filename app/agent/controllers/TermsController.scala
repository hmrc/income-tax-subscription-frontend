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

package agent.controllers

import javax.inject.{Inject, Singleton}

import agent.auth.AuthenticatedController
import agent.config.BaseControllerConfig
import agent.forms.IncomeSourceForm
import agent.models.OtherIncomeModel
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import agent.services.{AuthService, KeystoreService}
import uk.gov.hmrc.http.InternalServerException
import core.utils.Implicits._

import scala.concurrent.Future

@Singleton
class TermsController @Inject()(val baseConfig: BaseControllerConfig,
                                val messagesApi: MessagesApi,
                                val keystoreService: KeystoreService,
                                val authService: AuthService
                               ) extends AuthenticatedController {

  def view(backUrl: String)(implicit request: Request[_]): Html =
    agent.views.html.terms(
      postAction = agent.controllers.routes.TermsController.submitTerms(),
      backUrl
    )

  def showTerms(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        backUrl <- backUrl
      } yield Ok(view(backUrl = backUrl))
  }

  def submitTerms(isEditMode: Boolean = false): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.saveTerms(terms = true) map (
        _ => Redirect(agent.controllers.routes.CheckYourAnswersController.show()))


  }

  def backUrl(implicit request: Request[_]): Future[String] =
    keystoreService.fetchIncomeSource() flatMap {
      case Some(source) => source.source match {
        case IncomeSourceForm.option_business =>
          agent.controllers.business.routes.BusinessAccountingMethodController.show().url
        case IncomeSourceForm.option_both =>
          agent.controllers.business.routes.BusinessAccountingMethodController.show().url
        case IncomeSourceForm.option_property =>
          import agent.forms.OtherIncomeForm._
          keystoreService.fetchOtherIncome() flatMap {
            case Some(OtherIncomeModel(`option_yes`)) =>
              agent.controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url
            case Some(OtherIncomeModel(`option_no`)) =>
              agent.controllers.routes.OtherIncomeController.showOtherIncome().url
            case _ => new InternalServerException(s"Internal Server Error - TermsController.backUrl, no other income answer")
          }
        case x => new InternalServerException(s"Internal Server Error - TermsController.backUrl, unexpected income source: '$x'")
      }
      case _ => new InternalServerException(s"Internal Server Error - TermsController.backUrl, no income source retrieve from Keystore")
    }

}