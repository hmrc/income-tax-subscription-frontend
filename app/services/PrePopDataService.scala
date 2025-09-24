/*
 * Copyright 2024 HM Revenue & Customs
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

package services

import connectors.PrePopConnector
import models.common.business.SelfEmploymentData
import models.prepop.{PrePopData, PrePopSelfEmployment}
import play.api.Logging
import services.PrePopDataService.PrePopResult
import services.PrePopDataService.PrePopResult.{PrePopFailure, PrePopSuccess}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UUIDProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PrePopDataService @Inject()(prePopConnector: PrePopConnector,
                                  subscriptionDetailsService: SubscriptionDetailsService,
                                  uuidProvider: UUIDProvider)
                                 (implicit ec: ExecutionContext) extends Logging {

  def prePopIncomeSources(reference: String, nino: String)(implicit hc: HeaderCarrier): Future[PrePopResult] = {
    handlePrePopFlag(reference) {
      retrievePrePopData(reference, nino) { prePopData =>
        savePrePopSelfEmployments(reference)(prePopData.selfEmployment)
      }
    }
  }

  private def handlePrePopFlag(reference: String)
                              (f: => Future[PrePopResult])
                              (implicit hc: HeaderCarrier): Future[PrePopResult] = {
    subscriptionDetailsService.fetchPrePopFlag(reference) flatMap {
      case Some(_) => Future.successful(PrePopSuccess)
      case None => f
    }
  }

  private def retrievePrePopData(reference: String, nino: String)
                                (f: PrePopData => Future[PrePopResult])
                                (implicit hc: HeaderCarrier): Future[PrePopResult] = {
    prePopConnector.getPrePopData(nino).flatMap {
      case Left(error) => Future.successful(PrePopFailure(error.toString))
      case Right(prePopData) =>
        subscriptionDetailsService.savePrePopFlag(reference, prepop = true) flatMap {
          case Left(error) => Future.successful(PrePopFailure(error.toString))
          case Right(_) => f(prePopData)
        }
    }
  }

  private def savePrePopSelfEmployments(reference: String)
                                       (maybePrePopSelfEmployments: Option[Seq[PrePopSelfEmployment]])
                                       (implicit hc: HeaderCarrier): Future[PrePopResult] = {
    maybePrePopSelfEmployments match {
      case Some(prePopSelfEmployments) =>
        val selfEmployments: Seq[SelfEmploymentData] = prePopSelfEmployments.map(_.toSelfEmploymentData(
          id = uuidProvider.getUUID
        ))
        subscriptionDetailsService.saveBusinesses(reference, selfEmployments) map {
          case Left(error) =>
            logger.error(s"[PrePopDataService][savePrePopSelfEmployments] - Error saving self employment businesses. Error: $error")
            PrePopFailure(error.toString)
          case Right(_) =>
            PrePopSuccess
        }
      case None => Future.successful(PrePopSuccess)
    }
  }

}

object PrePopDataService {

  sealed trait PrePopResult

  object PrePopResult {
    case object PrePopSuccess extends PrePopResult

    case class PrePopFailure(error: String) extends PrePopResult
  }

}