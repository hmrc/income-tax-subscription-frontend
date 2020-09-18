/*
 * Copyright 2020 HM Revenue & Customs
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

import config.AppConfig
import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import forms.individual.business.BusinessTradeNameForm.businessTradeNameValidationForm
import javax.inject.{Inject, Singleton}
import models.individual.business.{BusinessTradeNameModel, SelfEmploymentData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{AuthService, MultipleSelfEmploymentsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.individual.incometax.business.business_trade_name

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class BusinessTradeNameController @Inject()(mcc: MessagesControllerComponents,
                                            multipleSelfEmploymentsService: MultipleSelfEmploymentsService,
                                            authService: AuthService)
                                           (implicit val ec: ExecutionContext, val appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def view(businessTradeNameForm: Form[BusinessTradeNameModel], id: String, isEditMode: Boolean)(implicit request: Request[AnyContent]): Html = {
    if(isEnabled(ReleaseFour)) {
      business_trade_name(
        businessTradeNameForm = businessTradeNameForm,
        postAction = controllers.individual.business.routes.BusinessTradeNameController.submit(id, isEditMode),
        isEditMode,
        backUrl = backUrl(id, isEditMode)
      )
    } else {
      throw new InternalServerException("Page is disabled")
    }
  }

  def show(id: String, isEditMode: Boolean): Action[AnyContent] = Action.async { implicit request =>
    authService.authorised() {
      withAllBusinesses { businesses =>
        val excludedBusinessTradeNames = getExcludedBusinessTradeNames(id, businesses)
        val currentBusiness = businesses.find(_.id == id)
        (currentBusiness.flatMap(_.businessName), currentBusiness.flatMap(_.businessTradeName)) match {
          case (None, _) => Future.successful(Redirect(routes.BusinessNameController.show(id)))
          case (_, Some(trade)) =>
            Future.successful(Ok(view(businessTradeNameValidationForm(excludedBusinessTradeNames).fill(trade), id, isEditMode)))
          case (_, None) =>
            Future.successful(Ok(view(businessTradeNameValidationForm(excludedBusinessTradeNames), id, isEditMode)))
        }
      }
    }
  }


  def submit(id: String, isEditMode: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      authService.authorised() {
        withAllBusinesses { businesses =>
          val excludedBusinessTradeNames = getExcludedBusinessTradeNames(id, businesses)
          businessTradeNameValidationForm(excludedBusinessTradeNames).bindFromRequest.fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, id, isEditMode = isEditMode))),
            businessTradeNameData =>
              multipleSelfEmploymentsService.saveBusinessTrade(id, businessTradeNameData).map(_ =>
                if (isEditMode) {
                  Redirect(controllers.individual.subscription.routes.SelfEmploymentsCYAController.show())
                } else {
                  Redirect(
                    controllers.individual.business.routes.AddressLookupRoutingController.initialiseAddressLookupJourney(id))
                }
              )
          )
        }
      }
  }

  private def getExcludedBusinessTradeNames(id: String, businesses: Seq[SelfEmploymentData]): Seq[BusinessTradeNameModel] = {
    val currentBusinessName = businesses.find(_.id == id).flatMap(_.businessName)
    businesses.filterNot(_.id == id).filter {
      case SelfEmploymentData(_, _, Some(name),_, _) if currentBusinessName contains name => true
      case _ => false
    }.flatMap(_.businessTradeName)
  }

  private def withAllBusinesses(f: Seq[SelfEmploymentData] => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    multipleSelfEmploymentsService.fetchAllBusinesses.flatMap {
      case businesses => f(businesses)
    }.recoverWith {
      case ex: Exception => throw new InternalServerException(
        s"[BusinessTradeNameController][withAllBusinesses] - Error retrieving businesses, error: ${ex.getMessage}")
    }
  }

  def backUrl(id: String, isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.individual.subscription.routes.SelfEmploymentsCYAController.show().url
    } else {
      controllers.individual.business.routes.BusinessNameController.show(id).url
    }
  }

}
