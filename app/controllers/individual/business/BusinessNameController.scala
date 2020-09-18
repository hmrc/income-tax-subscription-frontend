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

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import forms.individual.business.BusinessNameForm
import forms.individual.business.BusinessNameForm.businessNameValidationForm
import javax.inject.{Inject, Singleton}
import models.common.{BusinessNameModel, IncomeSourceModel}
import models.individual.business.SelfEmploymentData
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import play.twirl.api.Html
import services.{AuthService, MultipleSelfEmploymentsService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.SubscriptionDataUtil._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessNameController @Inject()(val authService: AuthService,
                                       subscriptionDetailsService: SubscriptionDetailsService,
                                       multipleSelfEmploymentsService: MultipleSelfEmploymentsService)
                                      (implicit val ec: ExecutionContext, appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {

  def view(businessNameForm: Form[BusinessNameModel], id: String, isEditMode: Boolean)(implicit request: Request[AnyContent]): Html = {
    views.html.individual.incometax.business.business_name(
      businessNameForm = businessNameForm,
      postAction = controllers.individual.business.routes.BusinessNameController.submit(id, editMode = isEditMode),
      isEditMode,
      backUrl = backUrl(isEditMode)
    )
  }

  def show(id: String, isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(ReleaseFour)) {
        withAllBusinesses { businesses =>
          val excludedBusinessNames = getExcludedBusinessNames(id, businesses)
          val currentBusinessName = businesses.find(_.id == id).flatMap(_.businessName)
          Future.successful(Ok(
            view(businessNameValidationForm(excludedBusinessNames).fill(currentBusinessName), id, isEditMode = isEditMode)
          ))
        }
      } else {
        for {
          businessName <- subscriptionDetailsService.fetchBusinessName()
        } yield Ok(view(BusinessNameForm.businessNameForm().form.fill(businessName), id, isEditMode = isEditMode))
      }
  }

  def submit(id: String, isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(ReleaseFour)) {
        withAllBusinesses { businesses =>
          val excludedBusinessNames = getExcludedBusinessNames(id, businesses)
          businessNameValidationForm(excludedBusinessNames).bindFromRequest.fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, id, isEditMode = isEditMode))),
            businessNameData =>
              multipleSelfEmploymentsService.saveBusinessName(id, businessNameData).map(_ =>
                if (isEditMode) {
                  Redirect(controllers.individual.subscription.routes.SelfEmploymentsCYAController.show())
                } else {
                  Redirect(controllers.individual.business.routes.BusinessTradeNameController.show(id))
                }
              )
          )
        }
      } else {
        BusinessNameForm.businessNameForm().bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, id, isEditMode = isEditMode))),
          businessName =>
            subscriptionDetailsService.saveBusinessName(businessName) flatMap { _ =>
              if (isEditMode) {
                Future.successful(Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show()))
              } else {
                for {
                  cacheMap <- subscriptionDetailsService.fetchAll()
                } yield cacheMap.getIncomeSource match {
                  case Some(IncomeSourceModel(true, false, _)) =>
                    Redirect(controllers.individual.business.routes.WhatYearToSignUpController.show())
                  case _ =>
                    Redirect(controllers.individual.business.routes.BusinessAccountingMethodController.show())
                }
              }
            }
        )
      }
  }


  private def getExcludedBusinessNames(id: String, businesses: Seq[SelfEmploymentData]): Seq[BusinessNameModel] = {
    val currentBusinessTrade = businesses.find(_.id == id).flatMap(_.businessTradeName)
    businesses.filterNot(_.id == id).filter {
      case SelfEmploymentData(_, _, _, Some(trade), _) if currentBusinessTrade contains trade => true
      case _ => false
    }.flatMap(_.businessName)
  }

  private def withAllBusinesses(f: Seq[SelfEmploymentData] => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    multipleSelfEmploymentsService.fetchAllBusinesses.flatMap {
      case businesses => f(businesses)
    }.recoverWith {
      case ex: Exception => throw new InternalServerException(
        s"[BusinessTradeNameController][withAllBusinesses] - Error retrieving businesses, error: ${ex.getMessage}")
    }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode && isEnabled(ReleaseFour)) {
      controllers.individual.subscription.routes.SelfEmploymentsCYAController.show().url
    } else if (isEditMode && !isEnabled(ReleaseFour)){
      controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    } else {
      controllers.individual.incomesource.routes.IncomeSourceController.show().url
    }
  }

}
