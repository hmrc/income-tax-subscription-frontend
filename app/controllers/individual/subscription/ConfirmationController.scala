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

import auth.individual.PostSubmissionController
import config.AppConfig
import utilities.individual.CacheUtil._
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.AuthService
import services.individual.KeystoreService
import uk.gov.hmrc.http.InternalServerException
import utilities.ITSASessionKeys
import views.html.individual.incometax.subscription.sign_up_complete

import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(val authService: AuthService,
                                       val messagesApi: MessagesApi,
                                       keystoreService: KeystoreService)
                                      (implicit val ec: ExecutionContext, appConfig: AppConfig) extends PostSubmissionController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>


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
