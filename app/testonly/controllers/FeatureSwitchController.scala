/*
 * Copyright 2017 HM Revenue & Customs
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

import auth.BaseFrontendController
import config.BaseControllerConfig
import config.featureswitch.FeatureSwitch._
import config.featureswitch.{FeatureSwitch, FeatureSwitching}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Request}
import play.twirl.api.Html
import services.AuthService

class FeatureSwitchController @Inject()(val messagesApi: MessagesApi,
                                        val baseConfig: BaseControllerConfig,
                                        val authService: AuthService)
  extends BaseFrontendController with FeatureSwitching with I18nSupport {
  private def view(switchNames: Map[FeatureSwitch, Boolean])(implicit request: Request[_]): Html = testonly.views.html.feature_switch(
    switchNames = switchNames,
    testonly.controllers.routes.FeatureSwitchController.submit()
  )

  lazy val show = Action { implicit req =>
    val featureSwitches = (switches map (switch => switch -> isEnabled(switch))).toMap
    Ok(view(featureSwitches))
  }

  lazy val submit = Action { implicit req =>
    val featureSwitches =
      req.body.asFormUrlEncoded.fold(Map.empty[FeatureSwitch, Boolean])(_.filterNot(_._1 == "csrfToken").map {
        case (k, v) => FeatureSwitch(k) -> v.head.toBoolean
      })

    switches.foreach(fs =>
      if (featureSwitches.contains(fs)) enable(fs)
      else disable(fs)
    )

    Redirect(testonly.controllers.routes.FeatureSwitchController.show())
  }

}
