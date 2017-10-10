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

package auth

import auth.AuthPredicates.registrationPredicates
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.Future

trait RegistrationController extends BaseFrontendController {

  object Authenticated extends AuthenticatedActions {
    private val registrationUnavailableMessage = "This page for registration is not yet available to the public: "

    def async: AuthenticatedAction =
      if (applicationConfig.enableRegistration) asyncInternal(registrationPredicates)
      else _ =>
        Action.async(request => Future.failed(new NotFoundException(registrationUnavailableMessage + request.uri)))

  }

}
