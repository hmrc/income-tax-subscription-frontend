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

package controllers.individual.subscription

import config.AppConfig
import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import connectors.IncomeTaxSubscriptionConnector
import forms.individual.business.AddAnotherBusinessForm.addAnotherBusinessForm
import javax.inject.{Inject, Singleton}
import models.{No, Yes}
import models.individual.business.{AddAnotherBusinessModel, SelfEmploymentData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.AuthService
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utilities.SubscriptionDataKeys.BusinessesKey
import utilities.individual.ImplicitDateFormatterImpl
import views.html.individual.incometax.subscription.self_employments_check_your_answers

import scala.concurrent.ExecutionContext

@Singleton
class SelfEmploymentsCYAController @Inject()(authService: AuthService,
                                             incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                             mcc: MessagesControllerComponents)
                                            (implicit val ec: ExecutionContext, val appConfig: AppConfig, dateFormatter: ImplicitDateFormatterImpl)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def view(addAnotherBusinessForm: Form[AddAnotherBusinessModel], businesses: Seq[SelfEmploymentData])(implicit request: Request[AnyContent]): Html = {
    if(isEnabled(ReleaseFour)) {
      self_employments_check_your_answers(
        addAnotherBusinessForm = addAnotherBusinessForm,
        answers = businesses,
        postAction = controllers.individual.subscription.routes.SelfEmploymentsCYAController.submit(),
        implicitDateFormatter = dateFormatter
      )
    } else {
      throw new InternalServerException("Page is disabled")
    }
  }

  def show: Action[AnyContent] = Action.async { implicit request =>
    authService.authorised() {
      incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey).map {
        case Some(businesses) if businesses.exists(_.isComplete) =>
          Ok(view(addAnotherBusinessForm(businesses.size, appConfig.limitOnNumberOfBusinesses), businesses.filter(_.isComplete)))
        case _ => Redirect(controllers.individual.incomesource.routes.IncomeSourceController.show())

      }.recoverWith {
        case ex: Exception => throw new InternalServerException(
          s"[SelfEmploymentsCYAController][show] - getSelfEmployments connection failure, error: ${ex.getMessage}")
      }
    }
  }

  def submit(): Action[AnyContent] = Action.async { implicit request =>
    authService.authorised() {
      incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey).map {
        case Some(businesses) if businesses.exists(_.isComplete) =>
          addAnotherBusinessForm(businesses.size, appConfig.limitOnNumberOfBusinesses).bindFromRequest.fold(
            formWithErrors => BadRequest(view(formWithErrors, businesses)),
            businessNameData => businessNameData.addAnotherBusiness match {
              case Yes => Redirect(controllers.individual.business.routes.InitialiseController.initialise())
              case No => Redirect(controllers.individual.business.routes.BusinessAccountingMethodController.show())
            }
          )
        case _ => Redirect(controllers.individual.incomesource.routes.IncomeSourceController.show())
      }.recoverWith {
        case ex: Exception => throw new InternalServerException(
          s"[SelfEmploymentsCYAController][submit] - getSelfEmployments connection failure, error: ${ex.getMessage}")
      }
    }
  }
}
