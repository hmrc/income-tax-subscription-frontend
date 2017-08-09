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

package controllers.iv

import javax.inject.{Inject, Singleton}

import audit.Logging
import auth.AuthenticatedController
import config.BaseControllerConfig
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Call, Request}
import services.AuthService

import scala.concurrent.Future


@Singleton
class IdentityVerificationController @Inject()(override val baseConfig: BaseControllerConfig,
                                               override val messagesApi: MessagesApi,
                                               val authService: AuthService,
                                               logging: Logging
                                              ) extends AuthenticatedController {

  def identityVerificationUrl(implicit request: Request[AnyContent]): String =
    applicationConfig.identityVerificationURL + IdentityVerificationController.identityVerificationUrl(applicationConfig.baseUrl)

  def gotoIV: Action[AnyContent] = Authenticated.asyncForIV {
    implicit user =>
      implicit request =>
        Future.successful(Redirect(identityVerificationUrl))
  }

  def ivFailed: Action[AnyContent] = Action {implicit request =>
    Ok(views.html.iv.iv_failed(controllers.iv.routes.IdentityVerificationController.gotoIV()))
  }
}

object IdentityVerificationController {
  //TODO confirm
  lazy val origin = "mtd-itsa"

  def completionUri(baseUrl: String): String = baseUrl + controllers.routes.HomeController.index().url

  def failureUri(baseUrl: String): String = baseUrl + controllers.iv.routes.IdentityVerificationController.ivFailed().url

  def identityVerificationUrl(baseUrl: String)(implicit request: Request[AnyContent]): String =
    s"/mdtp/uplift?origin=$origin&confidenceLevel=200&completionURL=${completionUri(baseUrl)}&failureURL=${failureUri(baseUrl)}"

}