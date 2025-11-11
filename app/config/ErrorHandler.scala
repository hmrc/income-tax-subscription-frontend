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

import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc.{RequestHeader, Result, Results}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{AuthorisationException, InsufficientEnrolments}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.templates.ErrorTemplate

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ErrorHandler @Inject()(val errorTemplate: ErrorTemplate,
                             val appConfig: AppConfig,
                             val messagesApi: MessagesApi)
                            (implicit val ec: ExecutionContext) extends FrontendErrorHandler with Logging {

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    resolveError(request, exception)
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: RequestHeader): Future[Html] = {
    Future.successful(errorTemplate(pageTitle, heading, message))
  }

  override def resolveError(rh: RequestHeader, ex: Throwable): Future[Result] = {
    ex match {
      case _: InsufficientEnrolments =>
        logger.debug("[AuthenticationPredicate][async] No HMRC-MTD-IT Enrolment and/or No NINO.")
        super.resolveError(rh, ex)
      case _: AuthorisationException =>
        logger.debug("[AuthenticationPredicate][async] Unauthorised request. Redirect to Sign In.")
        Future.successful(appConfig.redirectToLogin(rh.path))
      case _: NotFoundException =>
        notFoundTemplate(rh).map(html => Results.NotFound(html))
      case _ =>
        logger.error(s"[ErrorHandler][resolveError] Internal Server Error, (${rh.method})(${rh.uri})", ex)
        super.resolveError(rh, ex)
    }
  }

}
