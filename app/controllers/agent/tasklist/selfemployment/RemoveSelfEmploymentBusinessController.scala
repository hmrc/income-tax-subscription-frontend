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

package controllers.agent.tasklist.selfemployment

import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.RemoveBusinessForm
import models.common.business.{BusinessNameModel, BusinessTradeNameModel, SelfEmploymentData}
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.HtmlFormat
import services._
import uk.gov.hmrc.http.HeaderCarrier
import views.html.agent.tasklist.selfemployment.RemoveSelfEmploymentBusiness

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveSelfEmploymentBusinessController @Inject()(removeBusinessView: RemoveSelfEmploymentBusiness,
                                                       identify: IdentifierAction,
                                                       journeyRefiner: ConfirmedClientJourneyRefiner,
                                                       subscriptionDetailsService: SubscriptionDetailsService,
                                                       removeBusinessService: RemoveBusinessService)
                                                      (implicit val ec: ExecutionContext,
                                                       mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show(businessId: String): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    withBusinessData(request.reference, businessId) { (maybeBusinessNameModel, maybeBusinessTradeNameModel) =>
      Future.successful(Ok(view(businessId, form, maybeBusinessNameModel, maybeBusinessTradeNameModel)))
    }
  }

  def submit(businessId: String): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    form.bindFromRequest().fold(
      formWithErrors => {
        withBusinessData(request.reference, businessId) { (maybeBusinessNameModel, maybeBusinessTradeNameModel) =>
          Future.successful(BadRequest(view(businessId, formWithErrors, maybeBusinessNameModel, maybeBusinessTradeNameModel)))
        }
      },
      {
        case Yes => fetchBusinessesAndRemoveThisBusiness(businessId, request.reference)
        case No => Future.successful(Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show))
      }
    )
  }

  private def fetchBusinessesAndRemoveThisBusiness(businessId: String, reference: String)(implicit headerCarrier: HeaderCarrier) = {
    subscriptionDetailsService.fetchAllSelfEmployments(reference)
      .flatMap { case (businesses, accountingMethod) => removeBusinessService.deleteBusiness(reference, businessId, businesses, accountingMethod) }
      .map {
        case Right(_) => Redirect(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
        case Left(reason) => throw new RuntimeException(reason.toString)
      }
  }

  private def withBusinessData(reference: String,
                               businessId: String)
                              (f: (Option[BusinessNameModel], Option[BusinessTradeNameModel]) => Future[Result])
                              (implicit hc: HeaderCarrier): Future[Result] = {
    fetchBusinessData(reference, businessId).flatMap {
      case Some(SelfEmploymentData(_, _, _, maybeBusinessNameModel, maybeBusinessTradeNameModel, _, _)) =>
        f(maybeBusinessNameModel, maybeBusinessTradeNameModel)
      case _ => Future.successful(Redirect(controllers.agent.tasklist.addbusiness.routes.BusinessAlreadyRemovedController.show().url))
    }
  }

  private def fetchBusinessData(reference: String, id: String)(implicit hc: HeaderCarrier): Future[Option[SelfEmploymentData]] = {
    subscriptionDetailsService.fetchAllSelfEmployments(reference).map { case (businesses, _) =>
      businesses.find(_.id == id)
    }
  }

  private def view(businessId: String,
                   removeBusinessForm: Form[YesNo],
                   maybeBusinessNameModel: Option[BusinessNameModel],
                   maybeBusinessTradeNameModel: Option[BusinessTradeNameModel])
                  (implicit request: Request[_]): HtmlFormat.Appendable =
    removeBusinessView(
      removeBusinessForm = removeBusinessForm,
      businessName = maybeBusinessNameModel.map(_.businessName),
      businessTradeName = maybeBusinessTradeNameModel.map(_.businessTradeName),
      postAction = controllers.agent.tasklist.selfemployment.routes.RemoveSelfEmploymentBusinessController.submit(businessId),
      backUrl = controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    )

  private def form: Form[YesNo] = RemoveBusinessForm.removeBusinessForm()
}
