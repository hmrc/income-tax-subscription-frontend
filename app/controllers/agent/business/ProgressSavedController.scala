/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.agent.business

import auth.agent.AuthenticatedController
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.utils.ReferenceRetrieval
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import utilities.CacheExpiryDateProvider
import views.html.agent.business.ProgressSaved

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProgressSavedController @Inject()(val progressSavedView: ProgressSaved,
                                        val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val subscriptionDetailsService: SubscriptionDetailsService,
                                        val cacheExpiryDateProvider: CacheExpiryDateProvider)
                                      (implicit val ec: ExecutionContext,
                                        val appConfig: AppConfig,
                                        val config: Configuration,
                                        val env: Environment,
                                        mcc: MessagesControllerComponents) extends AuthenticatedController with AuthRedirects with ReferenceRetrieval {
  def show(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        withAgentReference { reference =>
          if (isEnabled(SaveAndRetrieve)) {
            subscriptionDetailsService.fetchLastUpdatedTimestamp(reference) map {
              case Some(timestamp) => Ok(progressSavedView(cacheExpiryDateProvider.expiryDateOf(timestamp.dateTime), signInUrl))
              case None => throw new InternalServerException("[ProgressSavedController][show] - The last updated timestamp cannot be retrieved")
            }
          } else {
            Future.failed(new NotFoundException("[ProgressSavedController][show] - The save and retrieve feature switch is disabled"))
          }
        }
  }

  private val signInUrl: String = ggLoginUrl
}
