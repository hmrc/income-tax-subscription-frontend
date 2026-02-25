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

import auth.individual.IncomeTaxSAUser
import config.AppConfig
import config.featureswitch.FeatureSwitch.TaxYear26To27Plus
import config.featureswitch.FeatureSwitching
import connectors.individual.PreferencesFrontendConnector
import controllers.SignUpBaseController
import controllers.individual.actions.{ConfirmationJourneyRefiner, IdentifierAction}
import models.Next
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.*
import utilities.ImplicitDateFormatterImpl
import views.html.individual.confirmation.SignUpConfirmation

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(identify: IdentifierAction,
                                       journeyRefiner: ConfirmationJourneyRefiner,
                                       preferencesFrontendConnector: PreferencesFrontendConnector,
                                       subscriptionDetailsService: SubscriptionDetailsService,
                                       signedUpDateService: SignedUpDateService,
                                       view: SignUpConfirmation,
                                       val appConfig: AppConfig)
                                      (implicit mcc: MessagesControllerComponents,
                                       ec: ExecutionContext,
                                       implicitDateFormatter: ImplicitDateFormatterImpl) extends SignUpBaseController with FeatureSwitching {

  def show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    for {
      signedUpDate <- signedUpDateService.getSignedUpDate(request.sessionData)
      preference <- preferencesFrontendConnector.getOptedInStatus
      selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(request.reference)
    } yield {
      val taxYearSelectionIsNext = selectedTaxYear.map(_.accountingYear).contains(Next)

      Ok(view(
        mandatedCurrentYear = request.mandationStatus.currentYearStatus.isMandated,
        taxYearSelectionIsNext = taxYearSelectionIsNext,
        individualUserNameMaybe = IncomeTaxSAUser.fullName,
        individualUserNino = request.nino,
        preference = preference,
        usingSoftwareStatus = request.usingSoftware,
        signedUpDate = signedUpDate,
        showHelp = isDisabled(TaxYear26To27Plus)
      ))
    }
  }

  def submit: Action[AnyContent] = identify { _ =>
    Redirect(controllers.routes.SignOutController.signOut)
  }

}
