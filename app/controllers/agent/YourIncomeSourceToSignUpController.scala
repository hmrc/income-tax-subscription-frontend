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

import auth.agent.AuthenticatedController
import auth.agent.IncomeTaxAgentUser
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import views.html.agent.YourIncomeSourceToSignUp
import forms.agent.HaveYouCompletedThisSectionForm
import models.{No, Yes, YesNo}
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import models.common.business.SelfEmploymentData
import models.common.{OverseasPropertyModel, PropertyModel}
import play.api.data.Form
import uk.gov.hmrc.http.InternalServerException
import views.html.helper.form

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YourIncomeSourceToSignUpController @Inject()(val yourIncomeSourceToSignUp: YourIncomeSourceToSignUp,
                                                   val subscriptionDetailsService: SubscriptionDetailsService,
                                                   val auditingService: AuditingService,
                                                   val authService: AuthService
                                                  )(implicit val ec: ExecutionContext,
                                                    val appConfig: AppConfig,
                                                    mcc: MessagesControllerComponents) extends AuthenticatedController
  
  with ReferenceRetrieval {
  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        for {
          businesses <- subscriptionDetailsService.fetchAllSelfEmployments(reference)
          ukProperty <- subscriptionDetailsService.fetchProperty(reference)
          foreignProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
        } yield {
          Ok(view(
            form = HaveYouCompletedThisSectionForm.form,
            selfEmployments = businesses,
            ukProperty = ukProperty,
            foreignProperty = foreignProperty
          ))
        }
      }

  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        handleForm(reference)(
          onYes = Redirect(controllers.agent.routes.TaskListController.show()),
          onNo = Redirect(controllers.agent.routes.TaskListController.show())
        )
      }
  }

  private def handleForm(reference: String)(onYes: Result, onNo: Result)
                        (implicit request: Request[AnyContent], user: IncomeTaxAgentUser): Future[Result] = {
    HaveYouCompletedThisSectionForm.form.bindFromRequest().fold(
      hasErrors => for {
        businesses <- subscriptionDetailsService.fetchAllSelfEmployments(reference)
        ukProperty <- subscriptionDetailsService.fetchProperty(reference)
        foreignProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
      } yield {
        BadRequest(view(
          form = hasErrors,
          selfEmployments = businesses,
          ukProperty = ukProperty,
          foreignProperty = foreignProperty
        ))
      },
      {
        case Yes =>
          subscriptionDetailsService.saveIncomeSourcesConfirmation(reference).map{
            case Right(_) => onYes
            case Left(_) => throw new InternalServerException("[YourIncomeSourceToSignUpController] [Failed to save income sources]")
          }
        case No =>
          Future.successful(onNo)
      }
    )
  }

  def backUrl: String = controllers.agent.routes.TaskListController.show().url

  private def view(form: Form[YesNo],
                   selfEmployments: Seq[SelfEmploymentData],
                   ukProperty: Option[PropertyModel],
                   foreignProperty: Option[OverseasPropertyModel])(implicit request: Request[AnyContent]): Html =
    yourIncomeSourceToSignUp(
      postAction = routes.YourIncomeSourceToSignUpController.submit,
      backUrl = backUrl,
      haveYouCompletedThisSectionForm = form,
      clientDetails = request.clientDetails,
      selfEmployments,
      ukProperty,
      foreignProperty,
    )
}
