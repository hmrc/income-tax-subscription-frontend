/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.unauthorisedagent.controllers

import javax.inject.{Inject, Singleton}

import core.ITSASessionKeys
import core.ITSASessionKeys._
import core.auth.AuthenticatedController
import core.config.BaseControllerConfig
import core.services.CacheUtil._
import core.services.{AuthService, KeystoreService}
import incometax.subscription.models.SubscriptionSuccess
import incometax.subscription.services.SubscriptionOrchestrationService
import incometax.unauthorisedagent.forms.ConfirmAgentForm
import incometax.unauthorisedagent.models.ConfirmAgentModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import usermatching.userjourneys.ConfirmAgentSubscription

import scala.concurrent.Future

@Singleton
class AuthoriseAgentController @Inject()(val baseConfig: BaseControllerConfig,
                                         val messagesApi: MessagesApi,
                                         val authService: AuthService,
                                         keystoreService: KeystoreService,
                                         subscriptionOrchestrationService: SubscriptionOrchestrationService
                                        ) extends AuthenticatedController[ConfirmAgentSubscription.type] {

  private[controllers] def getAgentName(implicit request: Request[AnyContent]): String =
    request.session(AgencyName)

  def view(form: Form[ConfirmAgentModel], agentName: String)(implicit request: Request[_]): Html =
    incometax.unauthorisedagent.views.html.authorise_agent(
      authoriseAgentForm = form,
      agentName = agentName,
      postAction = routes.AuthoriseAgentController.submit()
    )

  def show(): Action[AnyContent] = Authenticated { implicit req =>
    implicit user =>
      Ok(view(ConfirmAgentForm.confirmAgentForm, getAgentName))
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit req =>
    implicit user =>
      ConfirmAgentForm.confirmAgentForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, agentName = getAgentName))),
        authoriseAgent => authoriseAgent.choice match {
          case ConfirmAgentForm.option_yes =>
            keystoreService.fetchAll flatMap {
              case Some(cache) =>
                val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> req.uri)

                subscriptionOrchestrationService.createSubscription(user.nino.get, cache.getSummary())(headerCarrier) flatMap {
                  case Right(SubscriptionSuccess(id)) =>
                    keystoreService.saveSubscriptionId(id) map {
                      _ => Redirect(incometax.subscription.controllers.routes.ConfirmationController.show())
                    }
                  case Left(failure) =>
                    Future.failed(new InternalServerException(failure.toString))
                }
            }
          case ConfirmAgentForm.option_no =>
            Future.successful(Redirect(routes.AgentNotAuthorisedController.show()))
        }
      )


  }
}
