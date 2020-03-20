/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import models.audits.ClientMatchingAuditing.ClientMatchingAuditModel
import models.individual.subscription.SubscriptionSuccess
import models.usermatching.UserDetailsModel
import play.api.mvc.{AnyContent, Request}
import services.{AuditingService, SubscriptionService, UserMatchingService}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.Implicits._

import scala.concurrent.{ExecutionContext, Future}

sealed trait UnqualifiedAgent

case object NoClientDetails extends UnqualifiedAgent

case object NoClientMatched extends UnqualifiedAgent

case object ClientAlreadySubscribed extends UnqualifiedAgent

case object UnexpectedFailure extends UnqualifiedAgent

sealed trait QualifiedAgent {
  def clientNino: String

  def clientUtr: Option[String]
}

case class ApprovedAgent(clientNino: String, clientUtr: Option[String]) extends QualifiedAgent

case class UnApprovedAgent(clientNino: String, clientUtr: Option[String]) extends QualifiedAgent

@Singleton
class AgentQualificationService @Inject()(clientMatchingService: UserMatchingService,
                                          clientRelationshipService: ClientRelationshipService,
                                          subscriptionService: SubscriptionService,
                                          auditingService: AuditingService)
                                         (implicit ec: ExecutionContext) {

  import utilities.UserMatchingSessionUtil._

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
              auditingService.audit(ClientMatchingAuditModel(arn, cd, isSuccess = true))
              Right(ApprovedAgent(matchedClient.nino, matchedClient.saUtr))
            case Right(None) =>
              auditingService.audit(ClientMatchingAuditModel(arn, cd, isSuccess = false))
              Left(NoClientMatched)
          }.recoverWith { case _ => Left(UnexpectedFailure) }
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


