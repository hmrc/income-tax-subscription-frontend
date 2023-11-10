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

package controllers.utils

import common.Constants.ITSASessionKeys
import controllers.individual.business.routes
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.Future

trait TaxYearNavigationHelper {

  def handleUnableToSelectTaxYearIndividual(request: Request[AnyContent])(ableToSelect: Future[Result]): Future[Result] = {

    if (request.session.get(ITSASessionKeys.MANDATED_CURRENT_YEAR).contains("true") || request.session.get(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY).contains("true")) {
      Future.successful(Redirect(routes.TaskListController.show()))
    }
    else {
      ableToSelect
    }
  }

  def handleUnableToSelectTaxYearAgent(request: Request[AnyContent])(ableToSelect: Future[Result]): Future[Result] = {

    if (request.session.get(ITSASessionKeys.MANDATED_CURRENT_YEAR).contains("true") || request.session.get(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY).contains("true")) {
      Future.successful(Redirect(controllers.agent.routes.TaskListController.show()))
    }
    else {
      ableToSelect
    }
  }

}
