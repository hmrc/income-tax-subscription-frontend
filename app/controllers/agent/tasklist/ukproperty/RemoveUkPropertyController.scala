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

import connectors.IncomeTaxSubscriptionConnector
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.ClientRemoveUkPropertyForm.removeUkPropertyForm
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys
import views.html.agent.tasklist.ukproperty.RemoveUkPropertyBusiness

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveUkPropertyController @Inject()(removeUkProperty: RemoveUkPropertyBusiness,
                                           identify: IdentifierAction,
                                           journeyRefiner: ConfirmedClientJourneyRefiner,
                                           incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                           subscriptionDetailsService: SubscriptionDetailsService)
                                          (implicit val ec: ExecutionContext,
                                           mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    subscriptionDetailsService.fetchProperty(request.reference) map {
      case Some(_) =>
        Ok(removeUkProperty(
          yesNoForm = removeUkPropertyForm,
          postAction = controllers.agent.tasklist.ukproperty.routes.RemoveUkPropertyController.submit,
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
        ))
      case None =>
        Redirect(controllers.agent.tasklist.addbusiness.routes.BusinessAlreadyRemovedController.show())
    }
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    removeUkPropertyForm.bindFromRequest().fold(
      hasErrors => Future.successful(
        BadRequest(removeUkProperty(
          yesNoForm = hasErrors,
          postAction = controllers.agent.tasklist.ukproperty.routes.RemoveUkPropertyController.submit,
          backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
        ))
      ), {
        case Yes => incomeTaxSubscriptionConnector.deleteSubscriptionDetails(request.reference, SubscriptionDataKeys.Property) flatMap {
          case Right(_) => incomeTaxSubscriptionConnector.deleteSubscriptionDetails(request.reference, SubscriptionDataKeys.IncomeSourceConfirmation).map {
            case Right(_) => Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
            case Left(_) => throw new InternalServerException("[RemoveUkPropertyController][submit] - Failure to delete income source confirmation")
          }
          case Left(_) => throw new InternalServerException("[RemoveUkPropertyController][submit] - Could not remove UK property")
        }
        case No => Future.successful(Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show))
      }
    )
  }

}
