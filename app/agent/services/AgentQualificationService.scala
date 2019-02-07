/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.services

import javax.inject.{Inject, Singleton}

import agent.audit.AuditingService
import agent.audit.models.ClientMatchingAuditing.ClientMatchingAuditModel
import core.utils.Implicits._
import incometax.subscription.models.SubscriptionSuccess
import incometax.subscription.services.SubscriptionService
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier
import usermatching.models.UserDetailsModel
import usermatching.services.UserMatchingService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait UnqualifiedAgent

case object NoClientDetails extends UnqualifiedAgent

case object NoClientMatched extends UnqualifiedAgent

case object ClientAlreadySubscribed extends UnqualifiedAgent

case object UnexpectedFailure extends UnqualifiedAgent

trait QualifiedAgent{
  def clientNino: String
  def clientUtr: Option[String]
}

case class ApprovedAgent(clientNino: String, clientUtr: Option[String]) extends QualifiedAgent
case class UnApprovedAgent(clientNino: String, clientUtr: Option[String]) extends QualifiedAgent

@Singleton
class AgentQualificationService @Inject()(clientMatchingService: UserMatchingService,
                                          clientRelationshipService: ClientRelationshipService,
                                          subscriptionService: SubscriptionService,
                                          auditingService: AuditingService) {

  import usermatching.utils.UserMatchingSessionUtil._

  type ReturnType = Either[UnqualifiedAgent, QualifiedAgent]

  private[services]
  def matchClient(arn: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[ReturnType] = {
    val clientDetails: Future[Either[UnqualifiedAgent, UserDetailsModel]] = request.fetchUserDetails match {
      case Some(cd) => Right(cd)
      case _ => Left(NoClientDetails)
    }

    clientDetails.flatMap {
      case Left(x) => Left(x)
      case Right(cd) =>
        clientMatchingService.matchUser(cd)
          .collect {
            case Right(Some(matchedClient)) =>
              auditingService.audit(ClientMatchingAuditModel(arn, cd, isSuccess = true), agent.controllers.matching.routes.ConfirmClientController.submit().url)
              Right(ApprovedAgent(matchedClient.nino, matchedClient.saUtr))
            case Right(None) =>
              auditingService.audit(ClientMatchingAuditModel(arn, cd, isSuccess = false), agent.controllers.matching.routes.ConfirmClientController.submit().url)
              Left(NoClientMatched)
          }
          .recoverWith { case _ => Left(UnexpectedFailure) }
    }
  }

  private[services]
  def checkExistingSubscription(matchedClient: QualifiedAgent)(implicit hc: HeaderCarrier): Future[ReturnType] = {
    for {
      agentClientResponse <- subscriptionService.getSubscription(matchedClient.clientNino)
        .collect {
          case Right(None) => Right(matchedClient)
          case Right(Some(SubscriptionSuccess(_))) => Left(ClientAlreadySubscribed)
        }
    } yield agentClientResponse
  }.recoverWith { case _ => Left(UnexpectedFailure) }

  private[services]
  def checkClientRelationship(arn: String,
                              matchedClient: QualifiedAgent
                             )(implicit hc: HeaderCarrier): Future[ReturnType] = {
    for {
      isPreExistingRelationship <- clientRelationshipService.isPreExistingRelationship(arn, matchedClient.clientNino)
    } yield
      if (isPreExistingRelationship) Right(matchedClient)
      else Right(UnApprovedAgent(matchedClient.clientNino, matchedClient.clientUtr))
  }.recoverWith { case _ => Left(UnexpectedFailure) }

  private implicit class Util[A, B](first: Future[Either[A, B]]) {
    def flatMapRight(next: B => Future[Either[A, B]]): Future[Either[A, B]] =
      first.flatMap {
        case Right(v) => next(v)
        case left => left
      }
  }

  def orchestrateAgentQualification(arn: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[ReturnType] =
    matchClient(arn)
      .flatMapRight(checkExistingSubscription)
      .flatMapRight(checkClientRelationship(arn, _))
}


