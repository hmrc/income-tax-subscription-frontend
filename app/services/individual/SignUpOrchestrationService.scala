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

package services.individual

import connectors.{CreateIncomeSourcesConnector, SignUpConnector}
import models.AccountingYear
import models.common.subscription.SignUpSuccessResponse.{AlreadySignedUp, SignUpSuccessful}
import models.common.subscription._
import services.SPSService
import services.individual.UpsertAndAllocateEnrolmentService.UpsertAndAllocateEnrolmentResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignUpOrchestrationService @Inject()(signUpConnector: SignUpConnector,
                                           createIncomeSourcesConnector: CreateIncomeSourcesConnector,
                                           upsertAndAllocateEnrolmentService: UpsertAndAllocateEnrolmentService,
                                           spsService: SPSService)
                                          (implicit ec: ExecutionContext) {

  import services.individual.SignUpOrchestrationService._
  def orchestrateSignUp(nino: String,
                        utr: String,
                        taxYear: AccountingYear,
                        incomeSources: CreateIncomeSourcesModel,
                        maybeEntityId: Option[String])
                       (implicit hc: HeaderCarrier): Future[SignUpOrchestrationResponse] = {

    signUpConnector.signUp(
      nino = nino,
      utr = utr,
      taxYear = taxYear
    ) flatMap {
      case Right(SignUpSuccessful(mtdbsa)) =>
        val createIncomeSourcesRequest = createIncomeSourcesConnector.createIncomeSources(mtdbsa, incomeSources)
        val upsertAndAllocateEnrolmentRequest = upsertAndAllocateEnrolmentService.upsertAndAllocate(mtdbsa, nino)

        for {
          createIncomeSourcesResponse <- createIncomeSourcesRequest
          upsertAndAllocateEnrolmentResponse <- upsertAndAllocateEnrolmentRequest
          _ <- confirmPreferencesIfEnrolled(mtdbsa, maybeEntityId)(upsertAndAllocateEnrolmentResponse)
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

  private def confirmPreferencesIfEnrolled(mtditid: String, maybeEntityId: Option[String])
                                          (upsertAndAllocateEnrolmentResponse: UpsertAndAllocateEnrolmentResponse)
                                          (implicit hc: HeaderCarrier) = {
    upsertAndAllocateEnrolmentResponse match {
      case Right(_) => spsService.confirmPreferences(
        itsaId = mtditid,
        maybeSpsEntityId = maybeEntityId
      )
      case Left(_) => Future.successful(())
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
