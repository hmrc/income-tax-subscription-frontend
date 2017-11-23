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

package incometax.subscription.controllers

import javax.inject.{Inject, Singleton}

import cats.data.EitherT
import cats.implicits._
import core.auth.SignUpController
import core.config.BaseControllerConfig
import core.connectors.models.{ConnectorError, KeystoreMissingError}
import core.services.CacheConstants.MtditId
import core.services.{AuthService, KeystoreService}
import incometax.subscription.services.SubscriptionOrchestrationService
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future

@Singleton
class ClaimSubscriptionController @Inject()(val baseConfig: BaseControllerConfig,
                                            val messagesApi: MessagesApi,
                                            val authService: AuthService,
                                            val subscriptionOrchestrationService: SubscriptionOrchestrationService,
                                            val keystoreService: KeystoreService
                                           ) extends SignUpController {
  val claim: Action[AnyContent] = Authenticated.async {
    implicit request =>
      user =>
        val res = for {
          mtditId <- EitherT(getMtditId())
          nino = user.nino.get
          subscriptionResult <- EitherT(subscriptionOrchestrationService.enrolAndRefresh(mtditId, nino))
        } yield Ok(confirmationPage(mtditId))

        res.valueOr(ex => throw new InternalServerException(ex.toString))
  }

  private def getMtditId()(implicit hc: HeaderCarrier): Future[Either[ConnectorError, String]] =
    keystoreService.fetchSubscriptionId() map (_.toRight(left = KeystoreMissingError(MtditId)))

  private def confirmationPage(id: String)(implicit request: Request[AnyContent]) =
    incometax.subscription.views.html.enrolled.already_enrolled(
      subscriptionId = id,
      core.controllers.SignOutController.signOut(routes.ClaimSubscriptionController.claim())
    )
}
