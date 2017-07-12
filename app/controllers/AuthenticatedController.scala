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
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.AuthService
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.Retrievals._
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait AuthenticatedController extends FrontendController with I18nSupport {
  val authService: AuthService
  val baseConfig: BaseControllerConfig
  lazy implicit val applicationConfig = baseConfig.applicationConfig

  object Authenticated {
    def apply(action: Request[AnyContent] => IncomeTaxSAUser => Result): Action[AnyContent] = async(action andThen (_ andThen Future.successful))

    def async(action: Request[AnyContent] => IncomeTaxSAUser => Future[Result]): Action[AnyContent] = asyncInternal(action)(defaultPredicates)

    def asyncEnrolled(action: Request[AnyContent] => IncomeTaxSAUser => Future[Result]): Action[AnyContent] = asyncInternal(action)(confirmationPredicate)

    def asyncInternal(action: Request[AnyContent] => IncomeTaxSAUser => Future[Result]
                     )(predicate: => (Enrolments => Future[Result]) => Enrolments => Future[Result]): Action[AnyContent] =
      Action.async { implicit request =>
        if (request.session.get(ITSASessionKeys.GoHome).nonEmpty || request.uri == homeRoute.url) {
          authService.authorised().retrieve(allEnrolments).apply {
            enrolments =>
              predicate(action(request).compose(IncomeTaxSAUser.apply))(enrolments)
          }
        }
        else Future.successful(Redirect(homeRoute))
      }

    lazy val homeRoute = controllers.routes.HomeController.index()
  }

  implicit class FormUtil[T](form: Form[T]) {
    def fill(data: Option[T]): Form[T] = data.fold(form)(form.fill)
  }

}
