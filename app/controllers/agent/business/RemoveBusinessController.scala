/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.agent.AuthenticatedController
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.ReferenceRetrieval
import forms.agent.RemoveBusinessForm
import models.common.business.{BusinessNameModel, BusinessTradeNameModel, SelfEmploymentData}
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc._
import services.{AuditingService, AuthService, RemoveBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import uk.gov.hmrc.play.bootstrap.controller.WithUrlEncodedOnlyFormBinding
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import views.html.agent.business.RemoveBusiness

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessController @Inject()(val removeBusinessView: RemoveBusiness,
                                         val auditingService: AuditingService,
                                         val authService: AuthService,
                                         val subscriptionDetailsService: SubscriptionDetailsService,
                                         val removeBusinessService: RemoveBusinessService,
                                         val incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector
                                        )(implicit val ec: ExecutionContext,
                                          val appConfig: AppConfig,
                                          mcc: MessagesControllerComponents
                                        ) extends AuthenticatedController with ReferenceRetrieval with WithUrlEncodedOnlyFormBinding {
  def show(businessId: String): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      withAgentReference { reference =>
        if (isEnabled(SaveAndRetrieve)) {
          withBusinessData(reference, businessId) { (maybeBusinessNameModel, maybeBusinessTradeNameModel) =>
            Future.successful(Ok(view(businessId, form, maybeBusinessNameModel, maybeBusinessTradeNameModel)))
          }
        } else {
          Future.failed(new NotFoundException("[RemoveBusinessController][show] - The save and retrieve feature switch is disabled"))
        }
      }
    }
  }

  def submit(businessId: String): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      withAgentReference { reference =>
        if (isEnabled(SaveAndRetrieve)) {
          form.bindFromRequest.fold(
            formWithErrors => {
              withBusinessData(reference, businessId) { (maybeBusinessNameModel, maybeBusinessTradeNameModel) =>
                Future.successful(BadRequest(view(businessId, formWithErrors, maybeBusinessNameModel, maybeBusinessTradeNameModel)))
              }
            },
            {
              case Yes => for {
                businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[SelfEmploymentData](reference, BusinessesKey)
                _ = removeBusinessService.deleteBusiness(reference, businessId, businesses)
              } yield Redirect(controllers.agent.routes.TaskListController.show())
              case No => Future.successful(Redirect(controllers.agent.routes.TaskListController.show()))
            }
          )
        } else {
          Future.failed(new NotFoundException("[RemoveBusinessController][submit] - The save and retrieve feature switch is disabled"))
        }
      }
    }
  }

  private def withBusinessData(
                                reference: String,
                                businessId: String
                              )(f: (Option[BusinessNameModel], Option[BusinessTradeNameModel]) => Future[Result])(
                                implicit hc: HeaderCarrier
                              ): Future[Result] = {
    fetchBusinessData(reference, businessId).flatMap {
      case Some(SelfEmploymentData(_, _, maybeBusinessNameModel, maybeBusinessTradeNameModel, _, _)) =>
        f(maybeBusinessNameModel, maybeBusinessTradeNameModel)
      case _ => Future.failed(new InternalServerException("[RemoveBusinessController] - Could not retrieve business details"))
    }
  }

  private def fetchBusinessData(reference: String, id: String)(implicit hc: HeaderCarrier): Future[Option[SelfEmploymentData]] = {
    incomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[SelfEmploymentData](reference, BusinessesKey).map {
      _.find(_.id == id)
    }
  }

  private def view(
                    businessId: String,
                    removeBusinessForm: Form[YesNo],
                    maybeBusinessNameModel: Option[BusinessNameModel],
                    maybeBusinessTradeNameModel: Option[BusinessTradeNameModel]
                  )(implicit request: Request[_]) =
    removeBusinessView(
      removeBusinessForm = removeBusinessForm,
      businessName = maybeBusinessNameModel.map(_.businessName),
      businessTradeName = maybeBusinessTradeNameModel.map(_.businessTradeName),
      postAction = controllers.agent.business.routes.RemoveBusinessController.submit(businessId),
      backUrl = controllers.agent.routes.TaskListController.show().url
    )

  private def form: Form[YesNo] = RemoveBusinessForm.removeBusinessForm()
}
