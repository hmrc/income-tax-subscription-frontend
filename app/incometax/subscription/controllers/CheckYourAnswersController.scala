/*
 * Copyright 2018 HM Revenue & Customs
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

import core.ITSASessionKeys
import core.audit.Logging
import core.auth.{IncomeTaxSAUser, Registration, SignUpController}
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import incometax.business.forms.MatchTaxYearForm
import incometax.business.models.MatchTaxYearModel
import incometax.incomesource.forms.IncomeSourceForm
import incometax.subscription.models.SubscriptionSuccess
import incometax.subscription.services.SubscriptionOrchestrationService
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future

@Singleton
class CheckYourAnswersController @Inject()(val baseConfig: BaseControllerConfig,
                                           val messagesApi: MessagesApi,
                                           val keystoreService: KeystoreService,
                                           val subscriptionService: SubscriptionOrchestrationService,
                                           val authService: AuthService,
                                           logging: Logging
                                          ) extends SignUpController {

  import core.services.CacheUtil._

  lazy val backUrl: String = incometax.subscription.controllers.routes.TermsController.show().url
  val show = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        Future.successful(
          Ok(incometax.subscription.views.html.check_your_answers(
            cache.getSummary,
            isRegistration = request.isInState(Registration),
            incometax.subscription.controllers.routes.CheckYourAnswersController.submit(),
            backUrl = backUrl
          ))
        )
  }(noCacheMapErrMessage = "User attempted to view 'Check Your Answers' without any keystore cached data")

  val submit = journeySafeGuard { implicit user =>
    implicit request =>
      cache =>
        val nino = user.nino.get
        val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)

        subscriptionService.createSubscription(nino, cache.getSummary())(headerCarrier).flatMap {
          case Right(SubscriptionSuccess(id)) =>
            keystoreService.saveSubscriptionId(id).map(_ => Redirect(incometax.subscription.controllers.routes.ConfirmationController.show()))
          case _ =>
            error("Successful response not received from submission")
        }
  }(noCacheMapErrMessage = "User attempted to submit 'Check Your Answers' without any keystore cached data")

  private def journeySafeGuard(processFunc: IncomeTaxSAUser => Request[AnyContent] => CacheMap => Future[Result])
                              (noCacheMapErrMessage: String) =
    Authenticated.async { implicit request =>
      implicit user =>
        keystoreService.fetchAll().flatMap {
          case Some(cache) => cache.getTerms match {
            case Some(true) =>
              if (cache.getIncomeSource().fold(false)(_.source == IncomeSourceForm.option_property))
                processFunc(user)(request)(cache)
              else
              (cache.getMatchTaxYear(), cache.getAccountingPeriodDate()) match {
                case (Some(MatchTaxYearModel(MatchTaxYearForm.option_yes)), _) | (Some(MatchTaxYearModel(MatchTaxYearForm.option_no)), Some(_)) =>
                  processFunc(user)(request)(cache)
                case (Some(MatchTaxYearModel(MatchTaxYearForm.option_no)), _) =>
                  Future.successful(Redirect(incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true, editMatch = true)))
              }
            case Some(false) => Future.successful(Redirect(incometax.subscription.controllers.routes.TermsController.show(editMode = true)))
            case _ => Future.successful(Redirect(incometax.subscription.controllers.routes.TermsController.show()))
          }
          case _ => error(noCacheMapErrMessage)
        }
    }

  def error(message: String): Future[Nothing] = {
    logging.warn(message)
    Future.failed(new InternalServerException(message))
  }
}
