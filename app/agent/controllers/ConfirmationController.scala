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

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import agent.audit.Logging
import agent.auth.PostSubmissionController
import agent.config.BaseControllerConfig
import agent.models.DateModel.dateConvert
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import agent.services.{AuthService, KeystoreService}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class ConfirmationController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService,
                                       val logging: Logging
                                      ) extends PostSubmissionController {

  val showConfirmation: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchSubscriptionId.map {
        case Some(id) =>
          Ok(agent.views.html.confirmation(
            subscriptionId = id,
            submissionDate = dateConvert(LocalDate.now()),
            agent.controllers.routes.AddAnotherClientController.addAnother(),
            agent.controllers.routes.ExitSurveyController.show()
          ))
        case _ =>
          logging.info("User attempted to view confirmation with no subscriptionId stored in Keystore")
          throw new InternalServerException("confirmation controller, tried to view with no subscription ID")
      }
  }

  val signOut: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => Future.successful(Redirect(routes.ExitSurveyController.show()).withNewSession)
  }

}
