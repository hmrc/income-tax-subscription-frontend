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

package usermatching.controllers

import javax.inject.{Inject, Singleton}

import core.ITSASessionKeys
import core.services.KeystoreService
import core.ITSASessionKeys._
import core.auth.AuthenticatedController
import core.config.BaseControllerConfig
import core.services.AuthService
import core.services.CacheUtil._
import incometax.subscription.models.SubscriptionSuccess
import incometax.subscription.services.SubscriptionOrchestrationService
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import usermatching.userjourneys.ConfirmAgentSubscription

import scala.concurrent.Future

@Singleton
class ConfirmAgentSubscriptionController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi,
                                                   val authService: AuthService,
                                                   keystoreService: KeystoreService,
                                                   subscriptionOrchestrationService: SubscriptionOrchestrationService
                                                  ) extends AuthenticatedController[ConfirmAgentSubscription.type] {
  def show(): Action[AnyContent] = Authenticated {
    implicit req =>
      user =>
        val arn = req.session(AgentReferenceNumber)
        Ok(usermatching.views.html.confirm_agent_subscription(arn))
  }

  def submit(): Action[AnyContent] = Authenticated.async {
    implicit req =>
      implicit user =>
        keystoreService.fetchAll flatMap {
          case Some(cache) =>
            val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> req.uri)

            subscriptionOrchestrationService.createSubscription(user.nino.get, cache.getSummary())(headerCarrier) flatMap {
              case Right(SubscriptionSuccess(id)) =>
                keystoreService.saveSubscriptionId(id) map {
                  _ => Redirect(incometax.subscription.controllers.routes.ConfirmationController.showConfirmation())
                }
              case Left(failure) =>
                Future.failed(new InternalServerException(failure.toString))
            }
        }
  }
}
