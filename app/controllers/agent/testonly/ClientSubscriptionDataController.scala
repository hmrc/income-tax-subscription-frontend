/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.agent.testonly

import controllers.agent.testonly.routes._
import agent.testonly.forms.NinoForm._
import agent.testonly.views.html.delete_client_subscription_data
import core.config.{AppConfig, BaseControllerConfig}
import core.services.AuthService
import incometax.unauthorisedagent.connectors.SubscriptionStoreConnector
import incometax.unauthorisedagent.models.{DeleteSubscriptionFailure, DeleteSubscriptionSuccess}
import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Action
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

//$COVERAGE-OFF$Disabling scoverage on this class as it is only intended to be used by the test only controller

@Singleton
class ClientSubscriptionDataController @Inject()(val baseConfig: BaseControllerConfig,
                                                 val messagesApi: MessagesApi,
                                                 val authService: AuthService,
                                                 val subscriptionStoreConnector: SubscriptionStoreConnector
                                                ) extends FrontendController with I18nSupport {


  implicit lazy val appConfig: AppConfig = baseConfig.applicationConfig

  lazy val show = Action.async { implicit request =>
    Future.successful(Ok(delete_client_subscription_data(ninoForm,
      postAction = ClientSubscriptionDataController.submit())))
  }

  lazy val submit = Action.async { implicit request =>
    authService.authorised() {
      ninoForm.bindFromRequest.fold(
        _ => Future.successful(InternalServerError),
        ninoModel => {
          subscriptionStoreConnector.deleteSubscriptionData(ninoModel.nino) map {
            case Right(DeleteSubscriptionSuccess) => Ok("Success")
            case Left(DeleteSubscriptionFailure(reason)) => InternalServerError("Failure: " + reason)
          }
        })
    }
  }

}

// $COVERAGE-ON$
