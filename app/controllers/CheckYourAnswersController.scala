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
import auth.IncomeTaxSAUser
import config.BaseControllerConfig
import connectors.models.subscription.FESuccessResponse
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request, Result}
import services.{KeystoreService, SubscriptionService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future

@Singleton
class CheckYourAnswersController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val middleService: SubscriptionService,
                                           logging: Logging
                                          ) extends BaseController {

  import services.CacheUtil._

  lazy val backUrl: String = controllers.routes.TermsController.showTerms().url
  val show = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        Future.successful(
          Ok(views.html.check_your_answers(cache.getSummary,
            controllers.routes.CheckYourAnswersController.submit(),
            backUrl = backUrl
          ))
        )
  }(noCacheMapErrMessage = "User attempted to view 'Check Your Answers' without any keystore cached data")

  val submit = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        val nino = user.nino.get
        val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)

        middleService.submitSubscription(nino, cache.getSummary())(headerCarrier).flatMap {
          case Some(FESuccessResponse(Some(id))) =>
            keystoreService.saveSubscriptionId(id).map(_ => Redirect(controllers.routes.ConfirmationController.showConfirmation()))
          case _ =>
            error("Successful response not received from submission")
        }
  }(noCacheMapErrMessage = "User attempted to submit 'Check Your Answers' without any keystore cached data")

  private def journeySafeGuard(processFunc: IncomeTaxSAUser => Request[AnyContent] => CacheMap => Future[Result])
                              (noCacheMapErrMessage: String) =
    Authorised.async { implicit user =>
      implicit request =>
        keystoreService.fetchAll().flatMap {
          case Some(cache) if cache.getTerms.nonEmpty => processFunc(user)(request)(cache)
          case Some(_) => Future.successful(Redirect(controllers.routes.TermsController.showTerms()))
          case _ => error(noCacheMapErrMessage)
        }
    }

  def error(message: String): Future[Nothing] = {
    logging.warn(message)
    Future.failed(new InternalServerException(message))
  }
}
