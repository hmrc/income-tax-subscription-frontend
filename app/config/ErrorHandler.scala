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

package config

import play.api.i18n.MessagesApi
import play.api.mvc.Results._
import play.api.mvc.{Request, RequestHeader, Result}
import play.api.{Configuration, Environment, Logging}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{AuthorisationException, BearerTokenExpired, InsufficientEnrolments}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.templates.ErrorTemplate

import javax.inject.Inject
import scala.concurrent.Future

class ErrorHandler @Inject()(val errorTemplate: ErrorTemplate,
                             val appConfig: AppConfig,
                             val messagesApi: MessagesApi,
                             val config: Configuration,
                             val env: Environment
                            ) extends FrontendErrorHandler with AuthRedirects with Logging {

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(resolveError(request, exception))
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    errorTemplate(pageTitle, heading, message)(implicitly, implicitly)

  override def resolveError(rh: RequestHeader, ex: Throwable): Result = {
    ex match {
      case _: InsufficientEnrolments =>
        logger.debug("[AuthenticationPredicate][async] No HMRC-MTD-IT Enrolment and/or No NINO.")
        super.resolveError(rh, ex)
      case _: BearerTokenExpired =>
        logger.debug("[AuthenticationPredicate][async] Bearer Token Timed Out.")
        Redirect(controllers.routes.SessionTimeoutController.show)
      case _: AuthorisationException =>
        logger.debug("[AuthenticationPredicate][async] Unauthorised request. Redirect to Sign In.")
        toGGLogin(rh.path)
      case _: NotFoundException =>
        NotFound(notFoundTemplate(Request(rh, "")))
      case _ =>
        logger.error(s"[ErrorHandler][resolveError] Internal Server Error, (${rh.method})(${rh.uri})", ex)
        super.resolveError(rh, ex)
    }
  }

}
