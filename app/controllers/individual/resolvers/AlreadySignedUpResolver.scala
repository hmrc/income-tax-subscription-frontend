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

package controllers.individual.resolvers

import models.status.GetITSAStatus.Annual
import models.{Channel, HmrcLedUnconfirmed, SessionData}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Call, Request, Result}
import services.GetITSAStatusService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AlreadySignedUpResolver @Inject()(
  service: GetITSAStatusService
)(implicit ec: ExecutionContext) {

  def resolve(
    sessionData: SessionData,
    hasEnrolment: Boolean,
    channel: Option[Channel]
  )(implicit hc: HeaderCarrier): Future[Result] = {
    (hasEnrolment, channel) match {
      case (false, None) =>
        Future.successful(Redirect(controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show))
      case (true, None) =>
        Future.successful(Redirect(controllers.individual.matching.routes.AlreadyEnrolledController.show))
      case (false, _) =>
        Future.successful(Redirect(controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show))
      case (true, Some(HmrcLedUnconfirmed)) =>
        Future.successful(Redirect(controllers.individual.handoffs.routes.CheckIncomeSourcesController.show))
      case (_, _) =>
        service.getITSAStatus(sessionData).map { model => model.status match {
          case Annual => Redirect(controllers.individual.handoffs.routes.OptedOutController.show)
          case _ => Redirect(controllers.individual.matching.routes.AlreadyEnrolledController.show)
        }}
    }
  }

}
