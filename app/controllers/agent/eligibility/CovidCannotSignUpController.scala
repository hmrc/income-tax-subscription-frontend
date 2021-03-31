/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.agent.eligibility

import auth.agent.StatelessController
import config.AppConfig
import config.featureswitch.FeatureSwitch.RemoveCovidPages
import config.featureswitch.FeatureSwitching
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.eligibility.covid_cannot_sign_up

import scala.concurrent.ExecutionContext


@Singleton
class CovidCannotSignUpController @Inject()(val auditingService: AuditingService,
                                            val authService: AuthService)
                                           (implicit val ec: ExecutionContext,
                                            mcc: MessagesControllerComponents,
                                            val appConfig: AppConfig) extends StatelessController with FeatureSwitching {

  val show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      if (isEnabled(RemoveCovidPages)) {
        Redirect(routes.OtherSourcesOfIncomeController.show())
      } else {
        Ok(covid_cannot_sign_up(postAction = routes.Covid19ClaimCheckController.show(), backUrl))
      }
  }

  def backUrl: String = {
    if (isEnabled(RemoveCovidPages)) {
      throw new InternalServerException("Remove Covid Pages Feature Switch - Enabled")
  } else {
      routes.Covid19ClaimCheckController.show().url
    }
  }

}
