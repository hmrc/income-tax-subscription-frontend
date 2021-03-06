/*
 * Copyright 2021 HM Revenue & Customs
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

import auth.agent.{AuthPredicates, IncomeTaxAgentUser, StatelessController}
import auth.individual.AuthPredicate.AuthPredicate
import config.AppConfig
import config.featureswitch.FeatureSwitch.{ReleaseFour, RemoveCovidPages}
import config.featureswitch.FeatureSwitching
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import utilities.UserMatchingSessionUtil.UserMatchingSessionResultUtil

import scala.concurrent.ExecutionContext

@Singleton
class AddAnotherClientController @Inject()(val auditingService: AuditingService,
                                           val authService: AuthService,
                                           val appConfig: AppConfig,
                                           subscriptionDetailsService: SubscriptionDetailsService)
                                          (implicit val ec: ExecutionContext,
                                           mcc: MessagesControllerComponents) extends StatelessController with FeatureSwitching {


  override val statelessDefaultPredicate: AuthPredicate[IncomeTaxAgentUser] = AuthPredicates.defaultPredicates

  def addAnother(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(RemoveCovidPages)) {
      subscriptionDetailsService.deleteAll() map { _ =>
        Redirect(eligibility.routes.OtherSourcesOfIncomeController.show())
          .removingFromSession(ITSASessionKeys.JourneyStateKey)
          .removingFromSession(ITSASessionKeys.clientData: _*)
          .clearUserName
      }
  } else {
        subscriptionDetailsService.deleteAll() map { _ =>
          Redirect(eligibility.routes.Covid19ClaimCheckController.show())
            .removingFromSession(ITSASessionKeys.JourneyStateKey)
            .removingFromSession(ITSASessionKeys.clientData: _*)
            .clearUserName
        }
      }
  }

}
