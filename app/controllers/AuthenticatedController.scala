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

package controllers

import auth.IncomeTaxSAUser
import config.BaseControllerConfig
import controllers.AuthPredicates._
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.AuthService
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.Retrievals._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.http.SessionKeys._

import scala.concurrent.Future

trait AuthenticatedController extends FrontendController with I18nSupport {
  val authService: AuthService
  val baseConfig: BaseControllerConfig
  lazy implicit val applicationConfig = baseConfig.applicationConfig

  object Authenticated {
    def apply(action: Request[AnyContent] => IncomeTaxSAUser => Result): Action[AnyContent] = async(action andThen (_ andThen Future.successful))

    def async(action: Request[AnyContent] => IncomeTaxSAUser => Future[Result]): Action[AnyContent] = asyncHomeCheck(action)(defaultPredicates)

    def asyncEnrolled(action: Request[AnyContent] => IncomeTaxSAUser => Future[Result]): Action[AnyContent] = asyncHomeCheck(action)(confirmationPredicate)

    def asyncForHomeController(action: Request[AnyContent] => IncomeTaxSAUser => Future[Result]): Action[AnyContent] =
      Action.async { implicit request =>
        asyncInternal(action)(defaultPredicates)(request) map (_.addingToSession(ITSASessionKeys.GoHome -> "et"))
      }

    def asyncHomeCheck(action: Request[AnyContent] => IncomeTaxSAUser => Future[Result]
                      )(predicate: => (Enrolments => Future[Result]) => Enrolments => Future[Result]): Action[AnyContent] = Action.async { implicit request =>
      if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
        Future.successful(Redirect(timeoutRoute))
      }
      else if (request.session.get(ITSASessionKeys.GoHome).nonEmpty) {
        asyncInternal(action)(predicate)(request)
      }
      else Future.successful(Redirect(homeRoute))
    }

    def asyncInternal(action: Request[AnyContent] => IncomeTaxSAUser => Future[Result]
                     )(predicate: => (Enrolments => Future[Result]) => Enrolments => Future[Result]): Request[AnyContent] => Future[Result] = { implicit request =>
      authService.authorised().retrieve(allEnrolments).apply {
        enrolments =>
          predicate(action(request).compose(IncomeTaxSAUser.apply))(enrolments)
      }
    }

    lazy val timeoutRoute = controllers.routes.SessionTimeoutController.timeout()

    lazy val homeRoute = controllers.routes.HomeController.index()
  }

  implicit class FormUtil[T](form: Form[T]) {
    def fill(data: Option[T]): Form[T] = data.fold(form)(form.fill)
  }

}
