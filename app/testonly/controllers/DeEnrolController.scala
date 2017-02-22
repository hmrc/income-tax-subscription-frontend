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

//$COVERAGE-OFF$Disabling scoverage on this test only controller as it is only required by our acceptance test

package testonly.controllers

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent}
import testonly.connectors.{AuthenticatorConnector, DeEnrolmentConnector}
import uk.gov.hmrc.play.frontend.controller.FrontendController

@Singleton
class DeEnrolController @Inject()(deEnrolmentConnector: DeEnrolmentConnector,
                                  authenticatorConnector: AuthenticatorConnector) extends FrontendController {

  def deEnrol: Action[AnyContent] = Action.async { implicit request =>
    for {
      ggStubResponse <- deEnrolmentConnector.deEnrol()
      authRefreshed <- authenticatorConnector.refreshProfile()
    } yield ggStubResponse.status match {
      case OK => Ok("Successfully De-enrolled")
      case _ => BadRequest("Failed to De-enrol")
    }
  }

}

// $COVERAGE-ON$
