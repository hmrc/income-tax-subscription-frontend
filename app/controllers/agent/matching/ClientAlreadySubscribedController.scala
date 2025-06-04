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

package controllers.agent.matching

import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import views.html.agent.matching.ClientAlreadySubscribed

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ClientAlreadySubscribedController @Inject()(identify: IdentifierAction,
                                                  clientAlreadySubscribed: ClientAlreadySubscribed)
                                                 (val config: Configuration, val env: Environment)
                                                 (implicit mcc: MessagesControllerComponents) extends SignUpBaseController with AuthRedirects {

  val show: Action[AnyContent] = identify.async { implicit request =>
    Future.successful(Ok(clientAlreadySubscribed(
      postAction = controllers.agent.matching.routes.ClientAlreadySubscribedController.submit
    )))
  }

  val submit: Action[AnyContent] = identify.async { _ =>
    Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsController.show()))
  }

}
