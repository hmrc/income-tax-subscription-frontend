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

package controllers.individual.subscription

import auth.individual.SignUpController
import cats.data.EitherT
import cats.implicits._
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.ConnectorError
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.individual.SubscriptionOrchestrationService
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.SubscriptionDataKeys.MtditId

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimSubscriptionController @Inject()(val auditingService: AuditingService,
                                            val authService: AuthService,
                                            subscriptionDetailsService: SubscriptionDetailsService,
                                            subscriptionOrchestrationService: SubscriptionOrchestrationService)
                                           (implicit val ec: ExecutionContext,
                                            val appConfig: AppConfig,
                                            mcc: MessagesControllerComponents) extends SignUpController {

  val claim: Action[AnyContent] = Authenticated.async {
    implicit request =>
      user =>
        val res = for {
          mtditId <- EitherT(getMtditId)
          nino = user.nino.get
          _ <- EitherT(subscriptionOrchestrationService.enrolAndRefresh(mtditId, nino))
        } yield Ok(confirmationPage)

        res.valueOr(ex => throw new InternalServerException(ex.toString))
  }

  private def getMtditId(implicit hc: HeaderCarrier): Future[Either[ConnectorError, String]] = {
    subscriptionDetailsService.fetchSubscriptionId() map (_.toRight(left = SubscriptionDetailsMissingError(MtditId)))
  }

  private def confirmationPage(implicit request: Request[AnyContent]): Html = {
    views.html.individual.incometax.subscription.enrolled.claim_subscription()
  }

  case class SubscriptionDetailsMissingError(key: String) extends ConnectorError

}

