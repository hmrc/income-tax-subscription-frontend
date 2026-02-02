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

import common.Constants.{mtdItsaEnrolmentIdentifierKey, mtdItsaEnrolmentName}
import models.common.subscription.EnrolmentKey
import models.status.GetITSAStatus.Annual
import models.{Channel, HmrcLedUnconfirmed, SessionData}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.GetITSAStatusService
import services.agent.CheckEnrolmentAllocationService
import services.agent.CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AlreadySignedUpResolver @Inject()(
  checkEnrolmentService: CheckEnrolmentAllocationService,
  getITSAStatusService: GetITSAStatusService
)(implicit ec: ExecutionContext) {

  def resolve(
    sessionData: SessionData,
    mtdItId: String,
    channel: Option[Channel]
  )(implicit hc: HeaderCarrier): Future[Result] = {
    val enrolmentKey = EnrolmentKey(mtdItsaEnrolmentName, mtdItsaEnrolmentIdentifierKey -> mtdItId)
    checkEnrolmentService.getGroupIdForEnrolment(enrolmentKey).flatMap {
      case Right(_) =>
        Future.successful(Redirect(controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show))
      case Left(EnrolmentAlreadyAllocated(_)) =>
        channel match {
          case Some(HmrcLedUnconfirmed) =>
            Future.successful(Redirect(controllers.individual.handoffs.routes.CheckIncomeSourcesController.show))
          case _ =>
            getITSAStatusService.getITSAStatus(sessionData).map { model => model.status match {
              case Annual => Redirect(controllers.individual.handoffs.routes.OptedOutController.show)
              case _ => Redirect(controllers.individual.matching.routes.AlreadyEnrolledController.show)
            }
        }}
      case _ =>
        throw new Exception("Error checking enrolment")
    }
  }

}
