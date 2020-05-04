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

package controllers.individual

import auth.individual.StatelessController
import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.AuthService

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class IdentityVerificationController @Inject()(val authService: AuthService)(implicit val ec: ExecutionContext, appConfig: AppConfig,
                                               mcc: MessagesControllerComponents) extends StatelessController {

  def identityVerificationUrl(implicit request: Request[AnyContent]): String =
    appConfig.identityVerificationURL +
      IdentityVerificationController.identityVerificationUrl(
        appConfig.contactFormServiceIdentifier,
        appConfig.baseUrl
      )

  def gotoIV: Action[AnyContent] = Authenticated.asyncUnrestricted {
    implicit user =>
      implicit request =>
        Future.successful(Redirect(identityVerificationUrl))
  }

  def ivFailed: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.individual.iv_failed(controllers.individual.routes.IdentityVerificationController.gotoIV().url))
  }
}

object IdentityVerificationController {

  def completionUri(baseUrl: String): String = baseUrl + controllers.usermatching.routes.HomeController.index().url

  def failureUri(baseUrl: String): String = baseUrl + controllers.individual.routes.IdentityVerificationController.ivFailed().url

  def identityVerificationUrl(origin: String, baseUrl: String)(implicit request: Request[AnyContent]): String =
    s"/mdtp/uplift?origin=$origin&confidenceLevel=200&completionURL=${completionUri(baseUrl)}&failureURL=${failureUri(baseUrl)}"

}
