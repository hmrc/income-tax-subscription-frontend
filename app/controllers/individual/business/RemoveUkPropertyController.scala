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
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.ReferenceRetrieval
import forms.individual.business.RemoveUkPropertyForm
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.NotFoundException
import utilities.SubscriptionDataKeys
import views.html.individual.incometax.business.RemoveUkProperty

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveUkPropertyController @Inject()(val auditingService: AuditingService,
                                           val authService: AuthService,
                                           val subscriptionDetailsService: SubscriptionDetailsService,
                                           incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                           removeUkProperty: RemoveUkProperty)
                                          (implicit val ec: ExecutionContext,
                                           val appConfig: AppConfig,
                                           mcc: MessagesControllerComponents) extends SignUpController
  with FeatureSwitching with ReferenceRetrieval {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      if (isEnabled(SaveAndRetrieve)) {
        Future.successful(Ok(view(form)))
      } else {
        throw new NotFoundException("[RemoveUkPropertyController][show] - S&R feature switch is disabled")
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(SaveAndRetrieve)) {
        form.bindFromRequest.fold(
          hasErrors => Future.successful(BadRequest(view(form = hasErrors))), {
            case Yes => withReference { reference =>
              incomeTaxSubscriptionConnector.deleteSubscriptionDetails(reference, SubscriptionDataKeys.Property) map { _ =>
                Redirect(controllers.individual.business.routes.TaskListController.show())
              }
            }
            case No => Future.successful(Redirect(controllers.individual.business.routes.TaskListController.show()))
          }
        )
      } else {
        throw new NotFoundException("[RemoveUkPropertyController][submit] - S&R feature switch is disabled")
      }
  }

  private def view(form: Form[YesNo])(implicit request: Request[_]): Html = removeUkProperty(
    yesNoForm = form,
    postAction = routes.RemoveUkPropertyController.submit,
    backUrl = routes.TaskListController.show().url
  )

  private val form: Form[YesNo] = RemoveUkPropertyForm.removeUkPropertyForm

}
