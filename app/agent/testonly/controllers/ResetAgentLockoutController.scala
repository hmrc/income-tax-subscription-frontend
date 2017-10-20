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

package agent.testonly.controllers

import javax.inject.{Inject, Singleton}

import agent.config.BaseControllerConfig
import play.api.mvc.{Action, AnyContent}
import agent.testonly.connectors.ResetAgentLockoutConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

@Singleton
class ResetAgentLockoutController @Inject()(val baseConfig: BaseControllerConfig,
                                            val resetAgentLockoutConnector: ResetAgentLockoutConnector) extends FrontendController {

  val resetLockout: Action[AnyContent] = Action.async { implicit request =>
    for {
      reset <- resetAgentLockoutConnector.resetLockout
    } yield reset.status match {
      case OK => Ok("Agent lockout has been reset")
      case status => InternalServerError(s"Unexpected failure returned status=$status body=${reset.body}")
    }
  }

}
