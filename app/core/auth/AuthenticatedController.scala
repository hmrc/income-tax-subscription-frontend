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
import core.config.BaseControllerConfig
import core.services.AuthService
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthenticatedController[T <: UserJourney[IncomeTaxSAUser]] extends FrontendController with I18nSupport {
  type User = IncomeTaxSAUser

  val authService: AuthService
  val baseConfig: BaseControllerConfig

  lazy implicit val applicationConfig = baseConfig.applicationConfig

  type ActionBody = Request[AnyContent] => User => Future[Result]

  protected object Authenticated {
    def apply(action: Request[AnyContent] => User => Result)(implicit state: T): Action[AnyContent] =
      async(action andThen (_ andThen Future.successful))

    def async(action: ActionBody)(implicit state: T): Action[AnyContent] =
      Action.async { implicit request =>
        if (state.isEnabled) {
          authService.authorised().retrieve(allEnrolments and affinityGroup and credentialRole and confidenceLevel) {
            case enrolments ~ affinity ~ role ~ confidence =>
              implicit val user = IncomeTaxSAUser(enrolments, affinity, role, confidence)

              state.authPredicates.apply(request)(user) match {
                case Right(AuthPredicateSuccess) => action(request)(user)
                case Left(failureResult) => failureResult
              }
          }
        } else {
          Future.failed(new NotFoundException(s"$state not enabled: ${request.uri}"))
        }
      }
  }

  implicit val cacheSessionFunctions: (Session) => SessionFunctions = JourneyState.SessionFunctions
  implicit val cacheRequestFunctions: (Request[_]) => RequestFunctions = JourneyState.RequestFunctions
}

