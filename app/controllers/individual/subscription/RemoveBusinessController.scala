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
import connectors.IncomeTaxSubscriptionConnector
import connectors.httpparser.PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure
import javax.inject.{Inject, Singleton}
import models.individual.business.SelfEmploymentData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthService
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utilities.SubscriptionDataKeys.BusinessesKey

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessController @Inject()(authService: AuthService,
                                         incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                         mcc: MessagesControllerComponents)
                                        (implicit val ec: ExecutionContext, val appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport {

  def show(id: String): Action[AnyContent] = Action.async { implicit request =>
    authService.authorised() {
      incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey).flatMap {
        case Some(businesses) if businesses.exists(_.isComplete) => {
          val updatedBusinesses: Seq[SelfEmploymentData] = businesses.filter(_.isComplete).filterNot(_.id == id)

          incomeTaxSubscriptionConnector.saveSubscriptionDetails(BusinessesKey, updatedBusinesses) map {
            case Right(_) => {
              if(updatedBusinesses.size == 0) Redirect(controllers.individual.incomesource.routes.IncomeSourceController.show())
              else Redirect(controllers.individual.subscription.routes.SelfEmploymentsCYAController.show())
            }
            case Left(UnexpectedStatusFailure(status)) =>
              throw new InternalServerException(s"saveSelfEmployments failure, status: $status")
          }
        }
        case _ => Future.successful(Redirect(controllers.individual.business.routes.InitialiseController.initialise()))
      }.recoverWith {
        case ex: Exception => throw new InternalServerException(
          s"[RemoveBusinessController][show] - incomeTaxSubscriptionConnector connection failed, error: ${ex.getMessage}")
      }
    }
  }

}
