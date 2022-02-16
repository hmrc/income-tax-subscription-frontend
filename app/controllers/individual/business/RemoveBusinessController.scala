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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.IncomeTaxSubscriptionConnector
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsResponse
import controllers.utils.ReferenceRetrieval
import forms.individual.business.RemoveBusinessForm
import models.common.business.{BusinessNameModel, BusinessTradeNameModel, SelfEmploymentData}
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc._
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import utilities.SubscriptionDataKeys.BusinessesKey
import views.html.individual.incometax.business.RemoveBusiness

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessController @Inject()(val removeBusinessView: RemoveBusiness,
                                         val auditingService: AuditingService,
                                         val authService: AuthService,
                                         val subscriptionDetailsService: SubscriptionDetailsService,
                                         val incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector
                                        )(implicit val ec: ExecutionContext,
                                          val appConfig: AppConfig,
                                          mcc: MessagesControllerComponents
                                        ) extends SignUpController  with ReferenceRetrieval {
  def show(businessId: String): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      withReference { reference =>
        if (isEnabled(SaveAndRetrieve)) {
          withBusinessData(reference, businessId) { (businessNameModel, maybeBusinessTradeNameModel) =>
            Future.successful(Ok(view(businessId, form, businessNameModel, maybeBusinessTradeNameModel)))
          }
        } else {
          Future.failed(new NotFoundException("[RemoveBusinessController][show] - The save and retrieve feature switch is disabled"))
        }
      }
    }
  }

  def submit(businessId: String): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      withReference { reference =>
        if (isEnabled(SaveAndRetrieve)) {
          form.bindFromRequest.fold(
            formWithErrors => {
              withBusinessData(reference, businessId) { (businessNameModel, maybeBusinessTradeNameModel) =>
                Future.successful(BadRequest(view(businessId, formWithErrors, businessNameModel, maybeBusinessTradeNameModel)))
              }
            },
            {
              case Yes => for {
                businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](reference, BusinessesKey)
                _ = deleteBusiness(reference, businessId, businesses)
              } yield Redirect(controllers.individual.business.routes.TaskListController.show())
              case No => Future.successful(Redirect(controllers.individual.business.routes.TaskListController.show()))
            }
          )
        } else {
          Future.failed(new NotFoundException("[RemoveBusinessController][submit] - The save and retrieve feature switch is disabled"))
        }
      }
    }
  }

  private def deleteBusiness(reference: String, businessId: String, maybeBusinesses: Option[Seq[SelfEmploymentData]])(
    implicit hc: HeaderCarrier
  ): Option[Future[PostSubscriptionDetailsResponse]] = {
    maybeBusinesses map { businesses =>
      incomeTaxSubscriptionConnector
        .saveSubscriptionDetails[Seq[SelfEmploymentData]](reference, BusinessesKey, businesses.filterNot(_.id == businessId))
    }
  }

  private def withBusinessData(
                                reference: String,
                                businessId: String
                              )(f: (BusinessNameModel, Option[BusinessTradeNameModel]) => Future[Result])(
    implicit hc: HeaderCarrier
  ): Future[Result] = {
    fetchBusinessData(reference, businessId).flatMap {
        case Some(SelfEmploymentData(_, _, Some(businessNameModel), maybeBusinessTradeNameModel, _, _)) =>
          f(businessNameModel, maybeBusinessTradeNameModel)
        case _ => Future.failed(new InternalServerException("[RemoveBusinessController] - Could not retrieve business details"))
      }
  }

  private def fetchBusinessData(reference: String, id: String)(implicit hc: HeaderCarrier): Future[Option[SelfEmploymentData]] = {
    incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](reference, BusinessesKey).map {
      case Some(businesses) =>
        businesses.find(_.id == id)
      case None => None
    }
  }

  private def view(
                    businessId: String,
                    removeBusinessForm: Form[YesNo],
                    businessNameModel: BusinessNameModel,
                    maybeBusinessTradeNameModel: Option[BusinessTradeNameModel]
                  )(implicit request: Request[_]) =
    removeBusinessView(
      removeBusinessForm = removeBusinessForm,
      businessName = businessNameModel.businessName,
      businessTradeName = maybeBusinessTradeNameModel.map(_.businessTradeName),
      postAction = controllers.individual.business.routes.RemoveBusinessController.submit(businessId),
      backUrl = controllers.individual.business.routes.TaskListController.show().url
    )

  private def form: Form[YesNo] = RemoveBusinessForm.removeBusinessForm()
}
