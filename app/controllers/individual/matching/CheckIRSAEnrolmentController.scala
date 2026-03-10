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

package controllers.individual.matching

import config.AppConfig
import connectors.UsersGroupsSearchConnector
import connectors.agent.EnrolmentStoreProxyConnector
import controllers.SignUpBaseController
import controllers.individual.CheckIRSAEnrolmentBaseController
import controllers.individual.actions.IdentifierAction
import forms.individual.IRSACredentialForm
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UTRService
import views.html.individual.IRSACredential

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckIRSAEnrolmentController @Inject()(identify: IdentifierAction,
                                             utrService: UTRService,
                                             usersGroupsSearchConnector: UsersGroupsSearchConnector,
                                             enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
                                             irsaCredential: IRSACredential,
                                             appConfig: AppConfig)
                                            (implicit mcc: MessagesControllerComponents,
                                             ec: ExecutionContext) extends CheckIRSAEnrolmentBaseController(utrService, usersGroupsSearchConnector, enrolmentStoreProxyConnector) {

  def show: Action[AnyContent] = identify.async { implicit request =>
    getIdentifierDetails map {
      case Right(identifierDetails) =>
        Ok(irsaCredential(
          irsaCredentialForm = IRSACredentialForm.irsaCredentialForm,
          postAction = routes.CheckIRSAEnrolmentController.submit,
          currentCredential = identifierDetails.currentCredential,
          saCredential = identifierDetails.saCredential
        ))
      case Left(_) =>
        Redirect(controllers.individual.matching.routes.HomeController.index)
    }
  }

  def submit: Action[AnyContent] = identify.async { implicit request =>
    IRSACredentialForm.irsaCredentialForm.bindFromRequest().fold(
      hasErrors => {
        getIdentifierDetails map {
          case Right(identifierDetails) =>
            BadRequest(irsaCredential(
              irsaCredentialForm = hasErrors,
              postAction = routes.CheckIRSAEnrolmentController.submit,
              currentCredential = identifierDetails.currentCredential,
              saCredential = identifierDetails.saCredential
            ))
          case Left(_) =>
            Redirect(controllers.individual.matching.routes.HomeController.index)
        }
      },
      {
        case Yes => Future.successful(Redirect(appConfig.ggSignOutUrl()))
        case No => Future.successful(Redirect(controllers.individual.matching.routes.HomeController.index))
      }
    )
  }
}
