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

package controllers.agent

import agent.audit.Logging
import agent.auth.AuthenticatedController
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.config.featureswitch.{AgentPropertyCashOrAccruals, EligibilityPagesFeature, FeatureSwitching}
import core.services.AuthService
import incometax.subscription.models.{Both, Business, Property}
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class OtherIncomeErrorController @Inject()(implicit val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val authService: AuthService,
                                           val logging: Logging
                                          ) extends AuthenticatedController with FeatureSwitching {

  val show: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(agent.views.html.other_income_error(postAction = controllers.agent.routes.OtherIncomeErrorController.submit(), backUrl)))
  }

  val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchIncomeSource() map {
        case Some(Business | Both) =>
          Redirect(controllers.agent.business.routes.MatchTaxYearController.show())
        case Some(Property) =>
          if (isEnabled(AgentPropertyCashOrAccruals)) {
            Redirect(controllers.agent.business.routes.PropertyAccountingMethodController.show())
          } else if (isEnabled(EligibilityPagesFeature)) {
            Redirect(controllers.agent.routes.CheckYourAnswersController.show())
          } else {
            Redirect(controllers.agent.routes.TermsController.show())
          }
        case _ =>
          logging.info("Tried to submit 'other income error' when no data found in Keystore for 'income source'")
          throw new InternalServerException("Other Income Error controller, no income source found")
      }
  }

  lazy val backUrl: String = controllers.agent.routes.OtherIncomeController.show().url


}
