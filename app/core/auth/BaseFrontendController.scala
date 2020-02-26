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

package core.auth

import core.auth.AuthPredicate._
import core.auth.JourneyState.{RequestFunctions, SessionFunctions}
import core.config.{AppConfig, BaseControllerConfig}
import core.services.AuthService
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole, Enrolments}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

trait BaseFrontendController extends FrontendController with I18nSupport with AuthPredicates {

  val authService: AuthService
  val baseConfig: BaseControllerConfig

  implicit val ec: ExecutionContext

  override lazy implicit val applicationConfig: AppConfig = baseConfig.applicationConfig

  type ActionBody[User <: IncomeTaxUser] = Request[AnyContent] => User => Future[Result]
  type AuthenticatedAction[User <: IncomeTaxUser] = ActionBody[User] => Action[AnyContent]

  protected trait AuthenticatedActions[User <: IncomeTaxUser] {

    def userApply: (Enrolments, Option[AffinityGroup], Option[CredentialRole], ConfidenceLevel) => User

    def apply(action: Request[AnyContent] => User => Result): Action[AnyContent] = async(action andThen (_ andThen Future.successful))

    protected def asyncInternal(predicate: AuthPredicate[User])(action: ActionBody[User]): Action[AnyContent] =
      Action.async { implicit request =>
        authService.authorised().retrieve(allEnrolments and affinityGroup and credentialRole and confidenceLevel) {
          case enrolments ~ affinity ~ role ~ confidence =>
            implicit val user: User = userApply(enrolments, affinity, role, confidence)

            predicate.apply(request)(user) match {
              case Right(AuthPredicateSuccess) => action(request)(user)
              case Left(failureResult) => failureResult
            }
        }
      }

    def async: AuthenticatedAction[User]

  }

  implicit class FormUtil[T](form: Form[T]) {
    def fill(data: Option[T]): Form[T] = data.fold(form)(form.fill)
  }

  implicit val cacheSessionFunctions: Session => SessionFunctions = JourneyState.SessionFunctions
  implicit val cacheRequestFunctions: Request[_] => RequestFunctions = JourneyState.RequestFunctions

}

