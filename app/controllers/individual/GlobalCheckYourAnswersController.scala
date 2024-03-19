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

package controllers.individual

import auth.individual.{IncomeTaxSAUser, SignUpController}
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.SPSEntityId
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.common.subscription.CreateIncomeSourcesModel
import play.api.mvc._
import play.twirl.api.Html
import services.GetCompleteDetailsService.CompleteDetails
import services._
import services.individual.SubscriptionOrchestrationService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.individual.GlobalCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GlobalCheckYourAnswersController @Inject()(subscriptionService: SubscriptionOrchestrationService,
                                                 getCompleteDetailsService: GetCompleteDetailsService,
                                                 globalCheckYourAnswers: GlobalCheckYourAnswers)
                                                (val auditingService: AuditingService,
                                                 val authService: AuthService,
                                                 val subscriptionDetailsService: SubscriptionDetailsService,
                                                 val sessionDataService: SessionDataService,
                                                 val appConfig: AppConfig)
                                                (implicit val ec: ExecutionContext,
                                                 mcc: MessagesControllerComponents) extends SignUpController with ReferenceRetrieval {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withIndividualReference { reference =>
        withCompleteDetails(reference) { completeDetails =>
          Future.successful(Ok(view(
            postAction = routes.GlobalCheckYourAnswersController.submit,
            backUrl = tasklist.routes.TaskListController.show().url,
            completeDetails = completeDetails
          )))
        }
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withIndividualReference { reference =>
        withCompleteDetails(reference) { completeDetails =>
          signUp(completeDetails)(
            onSuccessfulSignUp = Redirect(controllers.individual.routes.ConfirmationController.show)
          )
        }
      }
  }

  private def signUp(completeDetails: CompleteDetails)
                    (onSuccessfulSignUp: Result)
                    (implicit request: Request[AnyContent], user: IncomeTaxSAUser): Future[Result] = {
    val nino = user.nino.get
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
    val session = request.session

    subscriptionService.signUpAndCreateIncomeSourcesFromTaskList(
      nino = nino,
      createIncomeSourceModel = CreateIncomeSourcesModel.createIncomeSources(nino, completeDetails),
      maybeSpsEntityId = session.get(SPSEntityId)
    )(headerCarrier) map {
      case Right(_) =>
        onSuccessfulSignUp
      case Left(failure) =>
        throw new InternalServerException(
          s"[GlobalCheckYourAnswersController][submit] - failure response received from submission: ${failure.toString}"
        )
    }
  }


  private def view(
                    postAction: Call,
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
      case Left(_) => Future.successful(Redirect(tasklist.routes.TaskListController.show()))
    }
  }

}

