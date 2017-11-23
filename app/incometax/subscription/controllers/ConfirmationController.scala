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

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime}
import javax.inject.{Inject, Singleton}

import core.ITSASessionKeys
import core.audit.Logging
import core.auth.PostSubmissionController
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import core.models.DateModel.dateConvert
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class ConfirmationController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val logging: Logging,
                                       val authService: AuthService
                                      ) extends PostSubmissionController {

  val showConfirmation: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val startTime = LocalDateTime.parse(request.session.get(ITSASessionKeys.StartTime).get)
      val endTime = java.time.LocalDateTime.now()
      val journeyDuration = ChronoUnit.MILLIS.between(startTime, endTime).toInt
      keystoreService.fetchIncomeSource.flatMap {
        case Some(incomeSource) =>
          keystoreService.fetchSubscriptionId.map {
            case Some(id) =>
              Ok(incometax.subscription.views.html.confirmation(
                subscriptionId = id,
                submissionDate = dateConvert(LocalDate.now()),
                signOutAction = core.controllers.SignOutController.signOut(routes.ConfirmationController.showConfirmation()),
                journeyDuration,
                incomeSource.source
              ))
            case _ =>
              logging.info("User attempted to view confirmation with no subscriptionId stored in Keystore")
              throw new InternalServerException("Confirmation Controller, call to view confirmation with no subscription ID")
          }
        case _ =>
          logging.info("User attempted to view confirmation with no incomeSource stored in Keystore")
          Future.failed(new InternalServerException("Confirmation Controller, call to show confirmation with no income source"))
      }
  }

}
