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

package controllers.individual.tasklist.overseasproperty

import connectors.IncomeTaxSubscriptionConnector
import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import forms.individual.business.RemoveOverseasPropertyForm
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys
import views.html.individual.tasklist.overseasproperty.RemoveOverseasPropertyBusiness

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveOverseasPropertyController @Inject()(removeOverseasProperty: RemoveOverseasPropertyBusiness,
                                                 incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                                 subscriptionDetailsService: SubscriptionDetailsService)
                                                (identify: IdentifierAction,
                                                 journeyRefiner: SignUpJourneyRefiner)
                                                (implicit val ec: ExecutionContext,
                                                 mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    subscriptionDetailsService.fetchOverseasProperty(request.reference) map {
      case Some(_) =>
        Ok(view(form))
      case None =>
        Redirect(controllers.individual.tasklist.addbusiness.routes.BusinessAlreadyRemovedController.show())
    }
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    form.bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(view(form = hasErrors))), {
        case Yes =>
          incomeTaxSubscriptionConnector.deleteSubscriptionDetails(request.reference, SubscriptionDataKeys.OverseasProperty).flatMap {
            case Right(_) =>
              incomeTaxSubscriptionConnector.deleteSubscriptionDetails(request.reference, SubscriptionDataKeys.IncomeSourceConfirmation).map {
                case Right(_) => Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
                case Left(_) => throw new InternalServerException("[RemoveOverseasPropertyController][submit] - Failure to delete income source confirmation")
              }
            case Left(_) =>
              throw new InternalServerException("[RemoveOverseasPropertyController][submit] - Could not remove overseas property")
          }
        case No =>
          Future.successful(Redirect(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show))
      }
    )
  }

  private def view(form: Form[YesNo])(implicit request: Request[_]): Html = removeOverseasProperty(
    yesNoForm = form,
    postAction = controllers.individual.tasklist.overseasproperty.routes.RemoveOverseasPropertyController.submit
  )

  private val form: Form[YesNo] = RemoveOverseasPropertyForm.removeOverseasPropertyForm
}
