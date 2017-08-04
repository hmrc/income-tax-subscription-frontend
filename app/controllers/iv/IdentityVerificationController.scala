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
import config.BaseControllerConfig
import controllers.AuthenticatedController
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

  def gotoIV: Action[AnyContent] = IV.async { implicit user =>
    implicit request =>
      Future.successful(Redirect(identityVerificationUrl))
  }

  def callback(journeyId: String): Action[AnyContent] = Authenticated.async { implicit user =>
    implicit request =>
      Future.successful(Redirect(controllers.routes.HomeController.index()))
  }

  def failureCallBack(journeyId: String): Action[AnyContent] = IV.async { implicit user =>
    implicit request =>
      // TODO call back IV and find out what type of error it was, display timeout page if it was a time out, redirect to no nino page otherwise
//      Future.successful(Ok(s"journeyId=$journeyId"))
      Future.successful(Redirect(controllers.routes.NoNinoController.showNoNino()))
  }
}

object IdentityVerificationController {

  private[iv] def removeQueryString(baseUrl: String, call: Call)(implicit request: Request[AnyContent]) = {
    val url = baseUrl + call.url
    url.substring(0, url.indexOf("?"))
  }

  def completionUri(baseUrl: String)(implicit request: Request[AnyContent]): String =
    removeQueryString(baseUrl, controllers.iv.routes.IdentityVerificationController.callback(""))

  //TODO confirm
  lazy val origin = "mtd-itsa"

  def failureUri(baseUrl: String)(implicit request: Request[AnyContent]): String =
    removeQueryString(baseUrl, controllers.iv.routes.IdentityVerificationController.failureCallBack(""))

  def identityVerificationUrl(baseUrl: String)(implicit request: Request[AnyContent]): String =
    s"/mdtp/uplift?origin=$origin&confidenceLevel=200&completionURL=${completionUri(baseUrl)}&failureURL=${failureUri(baseUrl)}"

}