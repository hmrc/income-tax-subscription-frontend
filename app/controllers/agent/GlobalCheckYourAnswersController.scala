/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.agent

import common.Constants.ITSASessionKeys
import config.AppConfig
import config.featureswitch.FeatureSwitching
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionSuccess}
import models.requests.agent.ConfirmedClientRequest
import play.api.mvc._
import play.twirl.api.Html
import services.GetCompleteDetailsService.CompleteDetails
import services._
import services.agent.SubscriptionOrchestrationService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.UserMatchingSessionUtil.ClientDetails
import views.html.agent.GlobalCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GlobalCheckYourAnswersController @Inject()(globalCheckYourAnswers: GlobalCheckYourAnswers,
                                                 identify: IdentifierAction,
                                                 journeyRefiner: ConfirmedClientJourneyRefiner,
                                                 getCompleteDetailsService: GetCompleteDetailsService,
                                                 ninoService: NinoService,
                                                 utrService: UTRService,
                                                 subscriptionService: SubscriptionOrchestrationService)
                                                (val appConfig: AppConfig,
                                                  val subscriptionDetailsService: SubscriptionDetailsService,
                                                 val sessionDataService: SessionDataService)
                                                (implicit val ec: ExecutionContext,
                                                 mcc: MessagesControllerComponents) extends SignUpBaseController with FeatureSwitching {

  def show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withCompleteDetails(request.reference) { completeDetails =>
      Future.successful(
        Ok(
          view(
            postAction = routes.GlobalCheckYourAnswersController.submit,
            backUrl = backUrl,
            completeDetails = completeDetails,
            clientDetails = request.clientDetails
          )
        )
      )
    }
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withCompleteDetails(request.reference) { completeDetails =>
      signUp(completeDetails) {
        case Some(id) =>
          Redirect(controllers.agent.routes.ConfirmationController.show)
            .addingToSession(ITSASessionKeys.MTDITID -> id)
        case None =>
          Redirect(controllers.agent.routes.ConfirmationController.show)
            .addingToSession(ITSASessionKeys.MTDITID -> "already-signed-up")
      }
    }
  }

  def backUrl: String = {
    tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
  }

  private def signUp(completeDetails: CompleteDetails)
                    (onSuccessfulSignUp: Option[String] => Result)
                    (implicit request: ConfirmedClientRequest[AnyContent]): Future[Result] = {
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)

    ninoService.getNino flatMap { nino =>
      utrService.getUTR flatMap { utr =>
        subscriptionService.createSubscriptionFromTaskList(
          arn = request.arn,
          utr = utr,
          createIncomeSourcesModel = CreateIncomeSourcesModel.createIncomeSources(nino, completeDetails)
        )(headerCarrier) map {
          case Right(Some(SubscriptionSuccess(id))) =>
            onSuccessfulSignUp(Some(id))
          case Right(None) =>
            onSuccessfulSignUp(None)
          case Left(failure) =>
            throw new InternalServerException(
              s"[GlobalCheckYourAnswersController][submit] - failure response received from submission: ${failure.toString}"
            )
        }
      }
    }
  }


  private def view(postAction: Call,
                   backUrl: String,
                   completeDetails: CompleteDetails,
                   clientDetails: ClientDetails)
                  (implicit request: Request[AnyContent]): Html = {
    globalCheckYourAnswers(
      postAction = postAction,
      backUrl = backUrl,
      completeDetails = completeDetails,
      clientDetails = clientDetails
    )
  }

  private def withCompleteDetails(reference: String)
                                 (f: CompleteDetails => Future[Result])
                                 (implicit request: Request[AnyContent]): Future[Result] = {
    getCompleteDetailsService.getCompleteSignUpDetails(reference) flatMap {
      case Right(completeDetails) => f(completeDetails)
      case Left(_) => Future.successful(Redirect(backUrl))
    }
  }

}

