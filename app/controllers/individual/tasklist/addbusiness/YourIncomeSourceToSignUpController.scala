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

package controllers.individual.tasklist.addbusiness

import config.AppConfig
import config.featureswitch.FeatureSwitch.DisplayName
import config.featureswitch.FeatureSwitching
import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import models.Current
import models.common.SoleTraderBusinesses
import models.common.business.SelfEmploymentData
import play.api.mvc.*
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.addbusiness.YourIncomeSourceToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YourIncomeSourceToSignUpController @Inject()(identify: IdentifierAction,
                                                   journeyRefiner: SignUpJourneyRefiner,
                                                   view: YourIncomeSourceToSignUp,
                                                   subscriptionDetailsService: SubscriptionDetailsService
                                                  )(val appConfig: AppConfig)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents) extends SignUpBaseController  with FeatureSwitching {

  def show: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    for {
      incomeSources <- subscriptionDetailsService.fetchAllIncomeSources(request.reference)
      maybePrePop <- subscriptionDetailsService.fetchPrePopFlag(request.reference)
      taxYearSelection <- subscriptionDetailsService.fetchSelectedTaxYear(request.reference)
    } yield {
      val isCurrent: Boolean = taxYearSelection.map(_.accountingYear).contains(Current)
      Ok(view(
        postAction = routes.YourIncomeSourceToSignUpController.submit,
        incomeSources,
        maybePrePop.contains(true),
        taxYearSelection = isCurrent
      ))
    }
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    subscriptionDetailsService.fetchAllIncomeSources(request.reference) flatMap { incomeSources =>
      if (incomeSources.isComplete) {
        subscriptionDetailsService.saveIncomeSourcesConfirmation(request.reference) map {
          case Right(_) =>
            if (thereAreDuplicateBusinessNames(incomeSources.selfEmployments) && isEnabled(DisplayName)) {
              Redirect(controllers.individual.tasklist.addbusiness.routes.AddDisplayNamesController.show)
            } else {
              Redirect(controllers.individual.sps.routes.SPSHandoffController.redirectToSPS)
            }
          case Left(_) => throw new InternalServerException("[YourIncomeSourceToSignUpController][submit] - failed to save income sources confirmation")
        }
      } else {
        Future.successful(Redirect(controllers.individual.tasklist.routes.IncomeSourcesIncompleteController.show))
      }
    }
  }

  private def thereAreDuplicateBusinessNames(selfEmployments: Seq[SelfEmploymentData]): Boolean = {
    SoleTraderBusinesses(selfEmployments.map(_.toSoleTraderBusiness)).getBusinessesWithDuplicateNames.nonEmpty
  }
}
