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
import models.common.subscription.*
import models.common.subscription.SignUpFailureResponse.UnprocessableSignUp
import services.SPSService
import services.individual.SignUpOrchestrationService.*
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

  def orchestrateSignUp(nino: String,
                        utr: String,
                        taxYear: AccountingYear,
                        incomeSources: CreateIncomeSourcesModel,
                        maybeEntityId: Option[String])
                       (implicit hc: HeaderCarrier): Future[SignUpOrchestrationResponse] = {

    signUp(nino = nino, utr = utr, taxYear = taxYear).flatMap {
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
      case Left(failure) =>
        Future.successful(Left(failure))
    }

  }

  private def signUp(nino: String, utr: String, taxYear: AccountingYear)
                    (implicit hc: HeaderCarrier): Future[Either[SignUpOrchestrationFailure, SignUpSuccessful]] = {
    signUpConnector.signUp(nino, utr, taxYear) map {
      case Right(value) =>
        Right(value)
      case Left(UnprocessableSignUp(ALREADY_SIGNED_UP, _)) =>
        Left(AlreadySignedUp)
      case Left(UnprocessableSignUp(ID_NOT_FOUND | BUSINESS_PARTNER_CATEGORY_ORGANISATION | MULTIPLE_BUSINESS_PARTNERS_FOUND, _)) =>
        Left(HandledUnprocessableSignUp)
      case Left(_) =>
        Left(UnhandledSignUpError)
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

  val ID_NOT_FOUND = "002"
  val BUSINESS_PARTNER_CATEGORY_ORGANISATION = "815"
  val MULTIPLE_BUSINESS_PARTNERS_FOUND = "816"
  val ALREADY_SIGNED_UP = "820"

  type SignUpOrchestrationResponse = Either[SignUpOrchestrationFailure, SignUpOrchestrationSuccessful.type]

  case object SignUpOrchestrationSuccessful

  sealed trait SignUpOrchestrationFailure

  case object AlreadySignedUp extends SignUpOrchestrationFailure

  case object HandledUnprocessableSignUp extends SignUpOrchestrationFailure

  case object UnhandledSignUpError extends SignUpOrchestrationFailure

  case object CreateIncomeSourcesFailure extends SignUpOrchestrationFailure

}
