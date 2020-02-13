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

package core.config

import javax.inject.Inject

import views.html.templates.error_template
import play.api.i18n.MessagesApi
import play.api.mvc.Results._
import play.api.mvc.{Request, RequestHeader, Result}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.{AuthorisationException, BearerTokenExpired, InsufficientEnrolments}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

import scala.concurrent.Future

class ErrorHandler @Inject()(val appConfig: AppConfig,
                             val messagesApi: MessagesApi,
                             val config: Configuration,
                             val env: Environment
                            ) extends FrontendErrorHandler with AuthRedirects {

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(resolveError(request, exception))
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]):
  play.twirl.api.HtmlFormat.Appendable =
    error_template(pageTitle, heading, message)(implicitly, implicitly, appConfig)

  override def resolveError(rh: RequestHeader, ex: Throwable): Result = {
    ex match {
      case _: InsufficientEnrolments =>
        Logger.debug("[AuthenticationPredicate][async] No HMRC-MTD-IT Enrolment and/or No NINO.")
        super.resolveError(rh, ex)
      case _: BearerTokenExpired =>
        Logger.debug("[AuthenticationPredicate][async] Bearer Token Timed Out.")
        Redirect(controllers.routes.SessionTimeoutController.show())
      case _: AuthorisationException =>
        Logger.debug("[AuthenticationPredicate][async] Unauthorised request. Redirect to Sign In.")
        toGGLogin(rh.path)
      case _: NotFoundException =>
        NotFound(notFoundTemplate(Request(rh, "")))
      case _ =>
        Logger.error(s"[ErrorHandler][resolveError] Internal Server Error, (${rh.method})(${rh.uri})", ex)
        super.resolveError(rh, ex)
    }
  }

}
