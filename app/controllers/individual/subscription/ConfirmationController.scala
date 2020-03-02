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

package controllers.individual.subscription

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import core.ITSASessionKeys
import core.audit.Logging
import core.auth.PostSubmissionController
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.incometax.subscription.sign_up_complete

import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val logging: Logging,
                                       val authService: AuthService
                                      )(implicit val ec: ExecutionContext) extends PostSubmissionController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      import core.services.CacheUtil._

      val startTime = LocalDateTime.parse(request.session.get(ITSASessionKeys.StartTime).get)
      val endTime = java.time.LocalDateTime.now()
      val journeyDuration = ChronoUnit.MILLIS.between(startTime, endTime).toInt

      keystoreService.fetchAll() map (_.getSummary()) map { summary =>
        summary.incomeSource match {
          case Some(_) =>
            Ok(sign_up_complete(journeyDuration, summary))
          case _ =>
            throw new InternalServerException("Confirmation Controller, call to show confirmation with invalid income source")
        }
      }
  }

}
