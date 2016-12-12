/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.Future

trait AuthorisedForIncomeTaxSA extends Actions {

  val applicationConfig: AppConfig
  val postSignInRedirectUrl: String

  private type PlayRequest = Request[AnyContent] => Result
  private type UserRequest = IncomeTaxSAUser => PlayRequest
  private type AsyncPlayRequest = Request[AnyContent] => Future[Result]
  private type AsyncUserRequest = IncomeTaxSAUser => AsyncPlayRequest

  implicit private def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  lazy val visibilityPredicate = new IncomeTaxSACompositePageVisibilityPredicate(
    postSignInRedirectUrl,
    applicationConfig.notAuthorisedRedirectUrl,
    applicationConfig.ivUpliftUrl,
    applicationConfig.ivRegisterUrl,
    applicationConfig.twoFactorUrl
  )

  class AuthorisedBy(regime: TaxRegime) {
    val authedBy: AuthenticatedBy = AuthorisedFor(regime, visibilityPredicate)

    def async(action: AsyncUserRequest): Action[AnyContent] = {
      authedBy.async {
        authContext: AuthContext => implicit request =>
          action(IncomeTaxSAUser(authContext))(request)
      }
    }

    def apply(action: UserRequest): Action[AnyContent] = async(user => request => Future.successful(action(user)(request)))
  }

  trait IncomeTaxSARegime extends TaxRegime {
    override def isAuthorised(accounts: Accounts): Boolean = true
    override def authenticationType: AuthenticationProvider = new GovernmentGatewayProvider(postSignInRedirectUrl, applicationConfig.ggSignInUrl)
  }

  object IncomeTaxSARegime extends IncomeTaxSARegime
  object Authorised extends AuthorisedBy(IncomeTaxSARegime)

}
