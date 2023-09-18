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

package controllers.agent.business

import auth.agent.{AuthenticatedController, IncomeTaxAgentUser}
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.SPSEntityId
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionSuccess}
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.GetCompleteDetailsService.CompleteDetails
import services.agent.SubscriptionOrchestrationService
import services.{AuditingService, AuthService, GetCompleteDetailsService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.agent.business.GlobalCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GlobalCheckYourAnswersController @Inject()(val auditingService: AuditingService,
                                                 val authService: AuthService,
                                                 subscriptionService: SubscriptionOrchestrationService,
                                                 val subscriptionDetailsService: SubscriptionDetailsService,
                                                 getCompleteDetailsService: GetCompleteDetailsService,
                                                 globalCheckYourAnswers: GlobalCheckYourAnswers)
                                                (implicit val ec: ExecutionContext,
                                                 val appConfig: AppConfig,
                                                 mcc: MessagesControllerComponents) extends AuthenticatedController with ReferenceRetrieval {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        withCompleteDetails(reference) { completeDetails =>
          Future.successful(Ok(view(
            postAction = routes.GlobalCheckYourAnswersController.submit,
            backUrl = controllers.agent.routes.TaskListController.show().url,
            completeDetails = completeDetails
          )))
        }
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        withCompleteDetails(reference) { completeDetails =>
          signUp(completeDetails)(
            onSuccessfulSignUp = id => Redirect(controllers.agent.routes.ConfirmationAgentController.show).addingToSession(ITSASessionKeys.MTDITID -> id)
          )
        }
      }
  }

  private def signUp(completeDetails: CompleteDetails)
                    (onSuccessfulSignUp: String => Result)
                    (implicit request: Request[AnyContent], user: IncomeTaxAgentUser): Future[Result] = {
    val nino = user.clientNino.get
    val utr = user.clientUtr.get
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
    val session = request.session

    subscriptionService.createSubscriptionFromTaskList(
      arn = user.arn,
      nino = nino,
      utr = utr,
      createIncomeSourcesModel = CreateIncomeSourcesModel.createIncomeSources(nino, completeDetails)
    )(headerCarrier) map {
      case Right(SubscriptionSuccess(id)) =>
        onSuccessfulSignUp(id)
      case Left(failure) =>
        throw new InternalServerException(
          s"[GlobalCheckYourAnswersController][submit] - failure response received from submission: ${failure.toString}"
        )
    }
  }


  private def view(postAction: Call,
                   backUrl: String,
                   completeDetails: CompleteDetails)
                  (implicit request: Request[AnyContent]): Html = {
    globalCheckYourAnswers(
      postAction = postAction,
      backUrl = backUrl,
      completeDetails = completeDetails
    )
  }

  private def withCompleteDetails(reference: String)
                                 (f: CompleteDetails => Future[Result])
                                 (implicit request: Request[AnyContent]): Future[Result] = {
    getCompleteDetailsService.getCompleteSignUpDetails(reference) flatMap {
      case Right(completeDetails) => f(completeDetails)
      case Left(_) => Future.successful(Redirect(controllers.agent.routes.TaskListController.show()))
    }
  }

}

