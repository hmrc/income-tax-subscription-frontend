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

package services.agent

import models.audits.ClientMatchingAuditing.ClientMatchingAuditModel
import models.common.subscription.SubscriptionSuccess
import models.usermatching.UserDetailsModel
import play.api.mvc.{AnyContent, Request}
import services.{AuditingService, SubscriptionService, UserMatchingService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

sealed trait UnqualifiedAgent

case object NoClientMatched extends UnqualifiedAgent

case object ClientAlreadySubscribed extends UnqualifiedAgent

case object UnexpectedFailure extends UnqualifiedAgent

case class UnApprovedAgent(clientNino: String, clientUtr: Option[String]) extends UnqualifiedAgent

sealed trait QualifiedAgent {
  def clientNino: String

  def clientUtr: Option[String]
}

case class ApprovedAgent(clientNino: String, clientUtr: Option[String]) extends QualifiedAgent


@Singleton
class AgentQualificationService @Inject()(clientMatchingService: UserMatchingService,
                                          clientRelationshipService: ClientRelationshipService,
                                          subscriptionService: SubscriptionService,
                                          auditingService: AuditingService)
                                         (implicit ec: ExecutionContext) {

  type ReturnType = Either[UnqualifiedAgent, QualifiedAgent]

  private[services]
  def matchClient(clientDetails: UserDetailsModel, agentReferenceNumber: String)(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[ReturnType] = {
    clientMatchingService.matchUser(clientDetails)
      .collect {
        case Right(Some(matchedClient)) =>
          auditingService.audit(ClientMatchingAuditModel(agentReferenceNumber, clientDetails, isSuccess = true))
          Right(ApprovedAgent(matchedClient.nino, matchedClient.saUtr))
        case Right(None) =>
          auditingService.audit(ClientMatchingAuditModel(agentReferenceNumber, clientDetails, isSuccess = false))
          Left(NoClientMatched)
      }.recoverWith { case _ => Future.successful(Left(UnexpectedFailure)) }
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
  }.recoverWith { case _ => Future.successful(Left(UnexpectedFailure)) }

  private[services]
  def checkClientRelationship(agentReferenceNumber: String,
                              matchedClient: QualifiedAgent
                             )(implicit hc: HeaderCarrier): Future[ReturnType] = {
    for {
      isPreExistingRelationship <- clientRelationshipService.isPreExistingRelationship(agentReferenceNumber, matchedClient.clientNino)
    } yield
      if (isPreExistingRelationship) Right(matchedClient)
      else Left(UnApprovedAgent(matchedClient.clientNino, matchedClient.clientUtr))
  }.recoverWith { case _ => Future.successful(Left(UnexpectedFailure)) }

  private implicit class Util[A, B](first: Future[Either[A, B]]) {
    def flatMapRight(next: B => Future[Either[A, B]]): Future[Either[A, B]] =
      first.flatMap {
        case Right(v) => next(v)
        case left => Future.successful(left)
      }
  }

  def orchestrateAgentQualification(clientDetails: UserDetailsModel, agentReferenceNumber: String)
                                   (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[ReturnType] =
    matchClient(clientDetails, agentReferenceNumber)
      .flatMapRight(checkClientRelationship(agentReferenceNumber, _))
      .flatMapRight(checkExistingSubscription)
}


