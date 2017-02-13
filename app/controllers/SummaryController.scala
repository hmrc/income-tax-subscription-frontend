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

import javax.inject.Inject

import config.BaseControllerConfig
import connectors.models.subscription.{FESuccessResponse, IncomeSourceType}
import play.api.i18n.MessagesApi
import services.{KeystoreService, SubscriptionService}

import scala.concurrent.Future

class SummaryController @Inject()(val baseConfig: BaseControllerConfig,
                                  val messagesApi: MessagesApi,
                                  val keystoreService: KeystoreService,
                                  val middleService: SubscriptionService
                                 ) extends BaseController {

  import services.CacheUtil._

  val showSummary = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchAll() map {
        case Some(cache) =>
          Ok(views.html.summary_page(cache.getSummary,
            controllers.routes.SummaryController.submitSummary(),
            backUrl = backUrl
          ))
      }
  }

  val submitSummary = Authorised.async { implicit user =>
    implicit request =>
//        keystoreService.fetchIncomeSource() flatMap {
        keystoreService.fetchAll() flatMap {
        case Some(source) =>
          val nino = user.nino.fold("")(x => x)
          middleService.submitSubscription(nino, source.getSummary()).flatMap {
            case Some(FESuccessResponse(id)) =>
              keystoreService.saveSubscriptionId(id).map(_ => Redirect(controllers.routes.ConfirmationController.showConfirmation()))
            case _ =>
              Future.successful(InternalServerError("Submission failed"))
          }
      }
  }

  lazy val backUrl: String = controllers.routes.TermsController.showTerms().url

}
