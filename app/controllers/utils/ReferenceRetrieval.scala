/*
 * Copyright 2021 HM Revenue & Customs
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

import auth.agent.IncomeTaxAgentUser
import auth.individual.IncomeTaxSAUser
import connectors.httpparser.RetrieveReferenceHttpParser.{InvalidJsonFailure, UnexpectedStatusFailure}
import controllers.agent.ITSASessionKeys
import play.api.mvc.{AnyContent, Request, Result}
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

trait ReferenceRetrieval {

  val subscriptionDetailsService: SubscriptionDetailsService
  implicit val ec: ExecutionContext

  def withReference(f: String => Future[Result])
                   (implicit request: Request[AnyContent],
                    hc: HeaderCarrier,
                    user: IncomeTaxSAUser): Future[Result] = {
    withReference(
      utr = user.utr.getOrElse(
        throw new InternalServerException("[ReferenceRetrieval][withReference] - Unable to retrieve users utr")
      )
    )(f)
  }

  def withReference(utr: String)
                   (f: String => Future[Result])
                   (implicit request: Request[AnyContent],
                    hc: HeaderCarrier,
                    user: IncomeTaxSAUser): Future[Result] = {
    user.reference match {
      case Some(reference) =>
        f(reference)
      case None =>
        subscriptionDetailsService.retrieveReference(utr).flatMap {
          case Left(InvalidJsonFailure) =>
            throw new InternalServerException(s"[ReferenceRetrieval][withReference] - Unable to parse json returned")
          case Left(UnexpectedStatusFailure(status)) =>
            throw new InternalServerException(s"[ReferenceRetrieval][withReference] - Unexpected status returned: $status")
          case Right(value) =>
            f(value).map(_.addingToSession(ITSASessionKeys.REFERENCE -> value))
        }
    }
  }

  def withAgentReference(f: String => Future[Result])
                        (implicit request: Request[AnyContent],
                         hc: HeaderCarrier,
                         user: IncomeTaxAgentUser): Future[Result] = {
    user.clientReference match {
      case Some(reference) =>
        f(reference)
      case None =>
        val clientsUtr: String = user.clientUtr.getOrElse(
          throw new InternalServerException("[ReferenceRetrieval][withAgentReference] - Unable to retrieve clients utr")
        )
        subscriptionDetailsService.retrieveReference(clientsUtr).flatMap {
          case Left(InvalidJsonFailure) =>
            throw new InternalServerException(s"[ReferenceRetrieval][withAgentReference] - Unable to parse json returned")
          case Left(UnexpectedStatusFailure(status)) =>
            throw new InternalServerException(s"[ReferenceRetrieval][withAgentReference] - Unexpected status returned: $status")
          case Right(value) =>
            f(value).map(_.addingToSession(ITSASessionKeys.REFERENCE -> value))
        }
    }
  }

}