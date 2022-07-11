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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.ReferenceRetrieval
import forms.individual.business.RemoveOverseasPropertyForm
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import utilities.SubscriptionDataKeys
import views.html.individual.incometax.business.RemoveOverseasProperty

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveOverseasPropertyController @Inject()(val auditingService: AuditingService,
                                                 val authService: AuthService,
                                                 val subscriptionDetailsService: SubscriptionDetailsService,
                                                 incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                                 removeOverseasProperty: RemoveOverseasProperty)
                                                (implicit val ec: ExecutionContext,
                                                 val appConfig: AppConfig,
                                                 mcc: MessagesControllerComponents) extends SignUpController
   with ReferenceRetrieval {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      Future.successful(Ok(view(form)))
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      form.bindFromRequest.fold(
        hasErrors => Future.successful(BadRequest(view(form = hasErrors))), {
          case Yes => withReference { reference =>
            incomeTaxSubscriptionConnector.deleteSubscriptionDetails(reference, SubscriptionDataKeys.OverseasProperty) map { _ =>
              Redirect(controllers.individual.business.routes.TaskListController.show())
            }
          }
          case No => Future.successful(Redirect(controllers.individual.business.routes.TaskListController.show()))
        }
      )
  }

  private def view(form: Form[YesNo])(implicit request: Request[_]): Html = removeOverseasProperty(
    yesNoForm = form,
    postAction = routes.RemoveOverseasPropertyController.submit,
    backUrl = routes.TaskListController.show().url
  )

  private val form: Form[YesNo] = RemoveOverseasPropertyForm.removeOverseasPropertyForm
}
