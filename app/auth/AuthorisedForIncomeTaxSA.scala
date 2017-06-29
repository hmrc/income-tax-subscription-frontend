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

package auth

import config.AppConfig
import controllers.ErrorPageRenderer
import controllers.ITSASessionKey._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.EnrolmentService
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait AuthorisedForIncomeTaxSA extends Actions with ErrorPageRenderer {

  val enrolmentService: EnrolmentService
  val applicationConfig: AppConfig
  val postSignInRedirectUrl: String
  lazy val alreadyEnrolledUrl: String = applicationConfig.alreadyEnrolledUrl

  private type PlayRequest = Request[AnyContent] => Result
  private type UserRequest = IncomeTaxSAUser => PlayRequest
  private type AsyncPlayRequest = Request[AnyContent] => Future[Result]
  private type AsyncUserRequest = IncomeTaxSAUser => AsyncPlayRequest

  // $COVERAGE-OFF$
  implicit private def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  // $COVERAGE-ON$

  lazy val visibilityPredicate = new IncomeTaxSACompositePageVisibilityPredicate

  class AuthorisedBy(regime: TaxRegime) {
    val authedBy: AuthenticatedBy = AuthorisedFor(regime, visibilityPredicate)

    def asyncCore(action: AsyncUserRequest): Action[AnyContent] =
      authedBy.async { authContext: AuthContext =>
        implicit request =>
          enrolmentService.getEnrolments.flatMap { enrolments =>
            request.session.get(GoHome) match {
              case Some(_) => action(IncomeTaxSAUser(authContext, enrolments))(request)
              case None => Redirect(controllers.routes.HomeController.index())
            }
          }
      }

    private def defaultPredicates(action: AsyncUserRequest
                                 )(implicit user: IncomeTaxSAUser,
                                   request: Request[AnyContent]
                                 ): Future[Result] =
      (user.nino, user.mtdItsaRef) match {
        case (None, _) => Future.successful(Redirect(controllers.routes.NoNinoController.showNoNino()))
        case (_, Some(_)) => Future.successful(Redirect(alreadyEnrolledUrl))
        case _ => action(user)(request)
      }

    def async(action: AsyncUserRequest): Action[AnyContent] =
      asyncCore {
        implicit authContext: IncomeTaxSAUser =>
          implicit request =>
            defaultPredicates(action)
      }

    def asyncForEnrolled(action: AsyncUserRequest): Action[AnyContent] =
      asyncCore {
        implicit user: IncomeTaxSAUser =>
          implicit request =>
            user.mtdItsaRef match {
              case Some(_) => action(user)(request)
              case _ => Future.successful(showNotFound)
            }
      }

    def asyncForHomeController(action: AsyncUserRequest): Action[AnyContent] =
      authedBy.async { authContext: AuthContext =>
        implicit request =>
          for {
            enrolments <- enrolmentService.getEnrolments
            result <- defaultPredicates(action)(IncomeTaxSAUser(authContext, enrolments), request)
          } yield result.addingToSession(GoHome -> "et")
      }
  }

  trait IncomeTaxSARegime extends TaxRegime {
    override def isAuthorised(accounts: Accounts): Boolean = true

    override def authenticationType: AuthenticationProvider = new GovernmentGatewayProvider(postSignInRedirectUrl, applicationConfig.ggSignInUrl)
  }

  object IncomeTaxSARegime extends IncomeTaxSARegime

  object Authorised extends AuthorisedBy(IncomeTaxSARegime)

}
