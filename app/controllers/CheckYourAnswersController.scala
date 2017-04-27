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

package controllers

import javax.inject.{Inject, Singleton}

import audit.Logging
import config.BaseControllerConfig
import connectors.models.subscription.FESuccessResponse
import play.api.i18n.MessagesApi
import services.{KeystoreService, SubscriptionService}

import scala.concurrent.Future

@Singleton
class CheckYourAnswersController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val middleService: SubscriptionService,
                                           logging: Logging
                                 ) extends BaseController {

  import services.CacheUtil._

  val show = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchAll() map {
        case Some(cache) =>
          Ok(views.html.check_your_answers(cache.getSummary,
            controllers.routes.CheckYourAnswersController.submit(),
            backUrl = backUrl
          ))
        case _ =>
          logging.info("User attempted to view 'Check Your Answers' without any keystore cached data")
          InternalServerError
      }
  }

  val submit = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchAll() flatMap {
        case Some(source) =>
          val nino = user.nino.fold("")(x => x)
          middleService.submitSubscription(nino, source.getSummary()).flatMap {
            case Some(FESuccessResponse(Some(id))) =>
              keystoreService.saveSubscriptionId(id).map(_ => Redirect(controllers.routes.ConfirmationController.showConfirmation()))
            case _ =>
              logging.warn("Successful response not received from submission")
              Future.successful(InternalServerError("Submission failed"))
          }
        case _ =>
          logging.info("User attempted to submit 'Check Your Answers' without any keystore cached data")
          Future.successful(InternalServerError)
      }
  }

  lazy val backUrl: String = controllers.routes.TermsController.showTerms().url

}
