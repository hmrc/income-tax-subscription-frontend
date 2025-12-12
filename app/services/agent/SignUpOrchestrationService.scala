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

import config.AppConfig
import config.featureswitch.FeatureSwitch.AgentRelationshipSingleCall
import config.featureswitch.FeatureSwitching
import connectors.agent.AgentSPSConnector
import connectors.{CreateIncomeSourcesConnector, SignUpConnector}
import models.AccountingYear
import models.common.subscription.CreateIncomeSourcesModel
import models.common.subscription.SignUpSuccessResponse.{AlreadySignedUp, SignUpSuccessful}
import services.agent.AutoEnrolmentService.AutoClaimEnrolmentResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignUpOrchestrationService @Inject()(signUpConnector: SignUpConnector,
                                           createIncomeSourcesConnector: CreateIncomeSourcesConnector,
                                           autoEnrolmentService: AutoEnrolmentService,
                                           clientRelationshipService: ClientRelationshipService,
                                           agentSPSConnector: AgentSPSConnector,
                                           val appConfig: AppConfig)
                                          (implicit ec: ExecutionContext) extends FeatureSwitching {

  import services.agent.SignUpOrchestrationService._
  def orchestrateSignUp(arn: String,
                        nino: String,
                        utr: String,
                        taxYear: AccountingYear,
                        incomeSources: CreateIncomeSourcesModel)
                       (implicit hc: HeaderCarrier): Future[SignUpOrchestrationResponse] = {

    signUpConnector.signUp(
      nino = nino,
      utr = utr,
      taxYear = taxYear
    ) flatMap {
      case Right(SignUpSuccessful(mtdbsa)) =>
        val createIncomeSourcesRequest = createIncomeSourcesConnector.createIncomeSources(mtdbsa, incomeSources)
        val autoClaimEnrolmentRequest = autoEnrolmentService.autoClaimEnrolment(utr = utr, nino = nino, mtditid = mtdbsa)
        val checkClientRelationshipsRequest = checkClientRelationships(arn = arn, nino = nino)

        for {
          createIncomeSourcesResponse <- createIncomeSourcesRequest
          enrolResponse <- autoClaimEnrolmentRequest
          _ <- checkClientRelationshipsRequest
          _ <- confirmAgentEnrolmentToSpsIfEnrolled(arn, nino, utr, mtdbsa)(enrolResponse)
        } yield {
          createIncomeSourcesResponse match {
            case Right(_) => Right(SignUpOrchestrationSuccessful)
            case Left(_) => Left(CreateIncomeSourcesFailure)
          }
        }
      case Right(AlreadySignedUp) =>
        Future.successful(Right(SignUpOrchestrationSuccessful))
      case Left(_) => Future.successful(Left(SignUpFailure))
    }

  }

  private def confirmAgentEnrolmentToSpsIfEnrolled(arn: String, nino: String, sautr: String, mtditId: String)
                                                  (autoClaimEnrolmentResponse: AutoClaimEnrolmentResponse)
                                                  (implicit hc: HeaderCarrier): Future[Unit] = {
    autoClaimEnrolmentResponse match {
      case Right(_) => agentSPSConnector.postSpsConfirm(
        arn = arn,
        nino = nino,
        sautr = sautr,
        itsaId = mtditId
      )
      case Left(_) => Future.successful(())
    }

  }

  private def checkClientRelationships(arn: String, nino: String)
                                      (implicit hc: HeaderCarrier): Future[Unit] = {
    if (isEnabled(AgentRelationshipSingleCall)) {
      clientRelationshipService.isMTDAgentRelationship(nino).map(_ => ())
    } else {
      clientRelationshipService.isMTDPreExistingRelationship(arn, nino) flatMap {
        case Right(false) => clientRelationshipService.isMTDSuppAgentRelationship(arn, nino).map(_ => ())
        case _ => Future.successful(())
      }
    }
  }
}


object SignUpOrchestrationService {

  type SignUpOrchestrationResponse = Either[SignUpOrchestrationFailure, SignUpOrchestrationSuccessful.type]

  case object SignUpOrchestrationSuccessful

  sealed trait SignUpOrchestrationFailure

  case object SignUpFailure extends SignUpOrchestrationFailure

  case object CreateIncomeSourcesFailure extends SignUpOrchestrationFailure

}