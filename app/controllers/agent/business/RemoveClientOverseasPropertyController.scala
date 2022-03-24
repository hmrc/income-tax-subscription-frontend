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

package controllers.agent.business

import auth.agent.AuthenticatedController
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.ReferenceRetrieval
import forms.agent.RemoveClientOverseasPropertyForm
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.NotFoundException
import utilities.SubscriptionDataKeys
import views.html.agent.business.RemoveClientOverseasProperty

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveClientOverseasPropertyController @Inject()(val auditingService: AuditingService,
                                                 val authService: AuthService,
                                                 val subscriptionDetailsService: SubscriptionDetailsService,
                                                 incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                                 removeOverseasProperty: RemoveClientOverseasProperty)
                                                (implicit val ec: ExecutionContext,
                                                 val appConfig: AppConfig,
                                                 mcc: MessagesControllerComponents) extends AuthenticatedController
   with ReferenceRetrieval {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      if (isEnabled(SaveAndRetrieve)) {
        Future.successful(Ok(view(form)))
      } else {
        throw new NotFoundException("[RemoveClientOverseasPropertyController][show] - S&R feature switch is disabled")
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>

      if (isEnabled(SaveAndRetrieve)) {
        form.bindFromRequest.fold(
          hasErrors => Future.successful(BadRequest(view(form = hasErrors))), {
            case Yes => withAgentReference { reference =>
              incomeTaxSubscriptionConnector.deleteSubscriptionDetails(reference, SubscriptionDataKeys.OverseasProperty) map { _ =>
                Redirect(controllers.agent.routes.TaskListController.show())
              }
            }
            case No => Future.successful(Redirect(controllers.agent.routes.TaskListController.show()))
          }
        )
      } else {
        throw new NotFoundException("[RemoveClientOverseasPropertyController][submit] - S&R feature switch is disabled")
      }
  }

  private def view(form: Form[YesNo])(implicit request: Request[_]): Html = removeOverseasProperty(
    yesNoForm = form,
    postAction = controllers.agent.business.routes.RemoveClientOverseasPropertyController.submit,
    backUrl = controllers.agent.routes.TaskListController.show().url
  )

  private val form: Form[YesNo] = RemoveClientOverseasPropertyForm.removeClientOverseasPropertyForm

}

