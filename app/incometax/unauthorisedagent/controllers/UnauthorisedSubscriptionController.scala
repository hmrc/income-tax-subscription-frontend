/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.unauthorisedagent.controllers

import javax.inject.{Inject, Singleton}

import core.ITSASessionKeys
import core.auth.UnauthorisedAgentSubscriptionController
import core.config.BaseControllerConfig
import agent.services.CacheUtil._
import core.ITSASessionKeys.AgentReferenceNumber
import core.services.{AuthService, KeystoreService}
import incometax.subscription.models.SubscriptionSuccess
import incometax.subscription.services.SubscriptionOrchestrationService
import incometax.unauthorisedagent.services.SubscriptionStoreRetrievalService
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future

@Singleton
class UnauthorisedSubscriptionController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi,
                                                   val authService: AuthService,
                                                   keystoreService: KeystoreService,
                                                   subscriptionStoreRetrievalService: SubscriptionStoreRetrievalService,
                                                   subscriptionOrchestrationService: SubscriptionOrchestrationService
                                                  ) extends UnauthorisedAgentSubscriptionController {


  def subscribeUnauthorised: Action[AnyContent] = Authenticated.async { implicit req =>
    implicit user =>
      keystoreService.fetchAll flatMap { cache =>
        val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> req.uri)

        subscriptionOrchestrationService.createSubscriptionFromUnauthorisedAgent(req.session(AgentReferenceNumber),
          user.nino.get, cache.getSummary())(headerCarrier) flatMap {
          case Right(SubscriptionSuccess(id)) =>
            for {
              _ <- keystoreService.saveSubscriptionId(id)
              _ <- subscriptionStoreRetrievalService.deleteSubscriptionData(user.nino.get)
            } yield Redirect(incometax.subscription.controllers.routes.ConfirmationController.show())
          case Left(failure) =>
            Future.failed(new InternalServerException(failure.toString))
        }
      }
  }

}
