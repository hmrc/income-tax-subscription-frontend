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

package testonly.controllers

import core.auth.BaseFrontendController
import core.config.AppConfig
import core.config.featureswitch.FeatureSwitch._
import core.config.featureswitch.{FeatureSwitch, FeatureSwitching}
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.AuthService
import testonly.connectors.{BackendFeatureSwitchConnector, EligibilityFeatureSwitchConnector}
import testonly.models.FeatureSwitchSetting
import testonly.views.html.feature_switch

import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}

class FeatureSwitchController @Inject()(val authService: AuthService,
                                        val messagesApi: MessagesApi,
                                        backendFeatureSwitchConnector: BackendFeatureSwitchConnector,
                                        eligibilityFeatureSwitchConnector: EligibilityFeatureSwitchConnector)
                                       (implicit val ec: ExecutionContext, appConfig: AppConfig) extends BaseFrontendController
  with FeatureSwitching with I18nSupport {

  private def view(switchNames: Map[FeatureSwitch, Boolean],
                   backendFeatureSwitches: Map[String, Boolean],
                   eligibilityFeatureSwitches: Map[String, Boolean]
                  )(implicit request: Request[_]): Html =

    feature_switch(
      switchNames = switchNames,
      backendFeatureSwitches = backendFeatureSwitches,
      eligibilityFeatureSwitches = eligibilityFeatureSwitches,
      testonly.controllers.routes.FeatureSwitchController.submit()
    )

  lazy val show: Action[AnyContent] = Action.async { implicit req =>
    for {
      backendFeatureSwitches <- backendFeatureSwitchConnector.getBackendFeatureSwitches
      eligibilityFeatureSwitches <- eligibilityFeatureSwitchConnector.getEligibilityFeatureSwitches
      featureSwitches = ListMap(switches.toSeq sortBy (_.displayText) map (switch => switch -> isEnabled(switch)): _*)
    } yield Ok(view(featureSwitches, backendFeatureSwitches, eligibilityFeatureSwitches))
  }

  lazy val submit: Action[AnyContent] = Action.async { implicit req =>
    val submittedData: Set[String] = req.body.asFormUrlEncoded match {
      case None => Set.empty
      case Some(data) => data.keySet
    }

    def settingsFromFeatureSwitchMap(featureSwitches: Future[Map[String, Boolean]]): Future[Set[FeatureSwitchSetting]] =
      featureSwitches map {
        _.keySet map {
          switchName => FeatureSwitchSetting(switchName, submittedData contains switchName)
        }
      }

    val frontendFeatureSwitches = submittedData flatMap FeatureSwitch.get

    switches.foreach(fs =>
      if (frontendFeatureSwitches.contains(fs)) enable(fs)
      else disable(fs)
    )

    for {
      backendFeatureSwitches <- settingsFromFeatureSwitchMap(backendFeatureSwitchConnector.getBackendFeatureSwitches)
      eligibilityFeatureSwitches <- settingsFromFeatureSwitchMap(eligibilityFeatureSwitchConnector.getEligibilityFeatureSwitches)
      _ <- backendFeatureSwitchConnector.submitBackendFeatureSwitches(backendFeatureSwitches)
      _ <- eligibilityFeatureSwitchConnector.submitEligibilityFeatureSwitches(eligibilityFeatureSwitches)
    } yield Redirect(testonly.controllers.routes.FeatureSwitchController.show())
  }

}
