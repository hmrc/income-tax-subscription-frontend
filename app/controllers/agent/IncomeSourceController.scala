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

import agent.auth.AuthenticatedController
import core.config.BaseControllerConfig
import core.config.featureswitch.FeatureSwitching
import forms.agent.IncomeSourceForm
import javax.inject.{Inject, Singleton}
import models.individual.subscription.{Both, Business, IncomeSourceType, Property}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.AuthService
import services.agent.KeystoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService
                                      )(implicit val ec: ExecutionContext) extends AuthenticatedController with FeatureSwitching {

  def view(incomeSourceForm: Form[IncomeSourceType], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.agent.income_source(
      incomeSourceForm = incomeSourceForm,
      postAction = controllers.agent.routes.IncomeSourceController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchIncomeSource() map {
        incomeSource => Ok(view(incomeSourceForm = IncomeSourceForm.incomeSourceForm.fill(incomeSource), isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      IncomeSourceForm.incomeSourceForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(incomeSourceForm = formWithErrors, isEditMode = isEditMode))),
        incomeSource => {
          if (!isEditMode) {
            saveIncomeSourceAndContinue(incomeSource)
          } else {
            keystoreService.fetchIncomeSource().flatMap { oldIncomeSource =>
              if (oldIncomeSource.contains(incomeSource)) {
                Future.successful(Redirect(routes.CheckYourAnswersController.show()))
              } else {
                saveIncomeSourceAndContinue(incomeSource)
              }
            }
          }
        }
      )
  }

  private def saveIncomeSourceAndContinue(incomeSource: IncomeSourceType)(implicit hc: HeaderCarrier): Future[Result] = {
    keystoreService.saveIncomeSource(incomeSource) map { _ =>
      incomeSource match {
        case Business | Both =>
          Redirect(business.routes.BusinessNameController.show())
        case Property =>
          Redirect(business.routes.PropertyAccountingMethodController.show())
      }
    }
  }

  lazy val backUrl: String =
    controllers.agent.routes.CheckYourAnswersController.show().url
}
