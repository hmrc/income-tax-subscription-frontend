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

package controllers.agent.tasklist.ukproperty

import auth.agent.AuthenticatedController
import config.AppConfig
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.ReferenceRetrieval
import forms.agent.ClientRemoveUkPropertyForm
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SessionDataService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys
import views.html.agent.tasklist.ukproperty.RemoveUkPropertyBusiness

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveUkPropertyController @Inject()(incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                           removeUkProperty: RemoveUkPropertyBusiness)
                                          (val auditingService: AuditingService,
                                           val authService: AuthService,
                                           val subscriptionDetailsService: SubscriptionDetailsService,
                                           val appConfig: AppConfig,
                                           val sessionDataService: SessionDataService)
                                          (implicit val ec: ExecutionContext,
                                           mcc: MessagesControllerComponents) extends AuthenticatedController with ReferenceRetrieval {

  private val form: Form[YesNo] = ClientRemoveUkPropertyForm.removeUkPropertyForm

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => withAgentReference { reference =>

      subscriptionDetailsService.fetchProperty(reference) map{
        case Some(_) =>
          Ok(view(form))
        case None =>
          Redirect(controllers.agent.tasklist.addbusiness.routes.BusinessAlreadyRemovedController.show())
      }
    }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      form.bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(view(form = hasErrors))), {
          case Yes => withAgentReference { reference =>
            incomeTaxSubscriptionConnector.deleteSubscriptionDetails(reference, SubscriptionDataKeys.Property) flatMap {
              case Right(_) => incomeTaxSubscriptionConnector.deleteSubscriptionDetails(reference, SubscriptionDataKeys.IncomeSourceConfirmation).map{
                case Right(_) =>  Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
                case Left(_) => throw new InternalServerException("[RemoveUkPropertyController][submit] - Failure to delete income source confirmation")
              }
              case Left(_) => throw new InternalServerException("[RemoveUkPropertyController][submit] - Could not remove UK property")
            }
          }
          case No => Future.successful(Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show))
        }
      )
  }

  private def view(form: Form[YesNo])(implicit request: Request[_]): Html = removeUkProperty(
    yesNoForm = form,
    postAction = controllers.agent.tasklist.ukproperty.routes.RemoveUkPropertyController.submit,
    backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
  )
}
