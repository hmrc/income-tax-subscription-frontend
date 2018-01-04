/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import core.auth.BaseFrontendController
import core.config.BaseControllerConfig
import core.config.featureswitch.FeatureSwitch._
import core.config.featureswitch.{FeatureSwitch, FeatureSwitching}
import core.services.AuthService
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Request}
import play.twirl.api.Html
import testonly.connectors.BackendFeatureSwitchConnector
import testonly.models.FeatureSwitchSetting

import scala.collection.immutable.ListMap

class FeatureSwitchController @Inject()(val messagesApi: MessagesApi,
                                        val baseConfig: BaseControllerConfig,
                                        val authService: AuthService,
                                        featureSwitchConnector: BackendFeatureSwitchConnector)
  extends BaseFrontendController with FeatureSwitching with I18nSupport {
  private def view(switchNames: Map[FeatureSwitch, Boolean], backendFeatureSwitches: Map[String, Boolean])(implicit request: Request[_]): Html =
    testonly.views.html.feature_switch(
      switchNames = switchNames,
      backendFeatureSwitches = backendFeatureSwitches,
      testonly.controllers.routes.FeatureSwitchController.submit()
    )

  lazy val show = Action.async { implicit req =>
    for {
      backendFeatureSwitches <- featureSwitchConnector.getBackendFeatureSwitches
      featureSwitches =     ListMap(switches.toSeq sortBy(_.displayText) map (switch => switch -> isEnabled(switch)):_*)
    } yield Ok(view(featureSwitches, backendFeatureSwitches))
  }

  lazy val submit = Action.async { implicit req =>
    val submittedData: Set[String] = req.body.asFormUrlEncoded match {
      case None => Set.empty
      case Some(data) => data.keySet
    }

    val frontendFeatureSwitches = submittedData flatMap FeatureSwitch.get

    switches.foreach(fs =>
      if (frontendFeatureSwitches.contains(fs)) enable(fs)
      else disable(fs)
    )

    featureSwitchConnector.getBackendFeatureSwitches map {
      _.keySet map { switchName =>
        FeatureSwitchSetting(
          feature = switchName,
          enable = submittedData contains switchName
        )
      }
    } flatMap featureSwitchConnector.submitBackendFeatureSwitches map {
      _ => Redirect(testonly.controllers.routes.FeatureSwitchController.show())
    }
  }

}
