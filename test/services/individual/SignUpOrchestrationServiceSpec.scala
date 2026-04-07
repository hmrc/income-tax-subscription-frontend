/*
 * Copyright 2025 HM Revenue & Customs
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

import connectors.httpparser.CreateIncomeSourcesResponseHttpParser
import connectors.mocks.{MockCreateIncomeSourcesConnector, MockSignUpConnector}
import models.common.subscription.SignUpFailureResponse.UnprocessableSignUp
import models.common.subscription.{CreateIncomeSourcesModel, SignUpFailureResponse, SignUpSuccessful, UkProperty}
import models.{AccountingYear, Current, DateModel}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.individual.SignUpOrchestrationService.{ALREADY_SIGNED_UP, BUSINESS_PARTNER_CATEGORY_ORGANISATION, ID_NOT_FOUND, MULTIPLE_BUSINESS_PARTNERS_FOUND}
import services.individual.mocks.MockUpsertAndAllocateEnrolmentService
import services.mocks.MockSpsService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.AccountingPeriodUtil

import scala.concurrent.ExecutionContext.Implicits.global

class SignUpOrchestrationServiceSpec extends PlaySpec
  with MockSignUpConnector
  with MockCreateIncomeSourcesConnector
  with MockUpsertAndAllocateEnrolmentService
  with MockSpsService {


  /*
  *   case object SignUpOrchestrationSuccessful

  sealed trait SignUpOrchestrationFailure

  case object AlreadySignedUp extends SignUpOrchestrationFailure

  case object HandledUnprocessableSignUp extends SignUpOrchestrationFailure

  case object UnhandledSignUpError extends SignUpOrchestrationFailure

  case object CreateIncomeSourcesFailure extends SignUpOrchestrationFailure*/


  "orchestrateSignUp" must {
    "return a sign up orchestration success response" when {
      "sign up and creation of income sources was successful" in {
        mockSignUp(nino, utr, taxYear)(Right(SignUpSuccessful(mtditid)))
        mockCreateIncomeSources(mtditid, createIncomeSourcesModel)(Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess))
        mockUpsertAndAllocateEnrolment(mtditid, nino)(Right(UpsertAndAllocateEnrolmentService.UpsertAndAllocateEnrolmentSuccess))
        mockConfirmPreference(entityId, mtditid)

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Right(SignUpOrchestrationService.SignUpOrchestrationSuccessful)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel)
        verifyUpsertAndAllocateEnrolment(mtditid, nino)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid)
      }
      "there was a problem upserting and allocating the enrolment" in {
        mockSignUp(nino, utr, taxYear)(Right(SignUpSuccessful(mtditid)))
        mockCreateIncomeSources(mtditid, createIncomeSourcesModel)(Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess))
        mockUpsertAndAllocateEnrolment(mtditid, nino)(Left(UpsertAndAllocateEnrolmentService.AllocateEnrolmentFailure))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Right(SignUpOrchestrationService.SignUpOrchestrationSuccessful)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel)
        verifyUpsertAndAllocateEnrolment(mtditid, nino)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid, count = 0)
      }
    }
    "return an already signed up result" when {
      s"signing up returned an unprocessable sign up response with a code of $ALREADY_SIGNED_UP" in {
        mockSignUp(nino, utr, taxYear)(Left(UnprocessableSignUp(ALREADY_SIGNED_UP, "Customer already signed up to MTD ITSA")))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Left(SignUpOrchestrationService.AlreadySignedUp)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel, count = 0)
        verifyUpsertAndAllocateEnrolment(mtditid, nino, count = 0)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid, count = 0)
      }
    }
    "return a handled unprocessable sign up result" when {
      s"signing up returned an unprocessable sign up response with a code of $ID_NOT_FOUND" in {
        mockSignUp(nino, utr, taxYear)(Left(UnprocessableSignUp(ID_NOT_FOUND, "Customer already signed up to MTD ITSA")))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Left(SignUpOrchestrationService.HandledUnprocessableSignUp)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel, count = 0)
        verifyUpsertAndAllocateEnrolment(mtditid, nino, count = 0)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid, count = 0)
      }
      s"signing up returned an unprocessable sign up response with a code of $BUSINESS_PARTNER_CATEGORY_ORGANISATION" in {
        mockSignUp(nino, utr, taxYear)(Left(UnprocessableSignUp(BUSINESS_PARTNER_CATEGORY_ORGANISATION, "Customer already signed up to MTD ITSA")))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Left(SignUpOrchestrationService.HandledUnprocessableSignUp)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel, count = 0)
        verifyUpsertAndAllocateEnrolment(mtditid, nino, count = 0)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid, count = 0)
      }
      s"signing up returned an unprocessable sign up response with a code of $MULTIPLE_BUSINESS_PARTNERS_FOUND" in {
        mockSignUp(nino, utr, taxYear)(Left(UnprocessableSignUp(MULTIPLE_BUSINESS_PARTNERS_FOUND, "Customer already signed up to MTD ITSA")))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Left(SignUpOrchestrationService.HandledUnprocessableSignUp)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel, count = 0)
        verifyUpsertAndAllocateEnrolment(mtditid, nino, count = 0)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid, count = 0)
      }
    }
    "return a unhandled sign up error" when {
      "signing up returned an unprocessable sign up response with a code which we do not handle" in {
        mockSignUp(nino, utr, taxYear)(Left(UnprocessableSignUp("500", "Unhandled")))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Left(SignUpOrchestrationService.UnhandledSignUpError)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel, count = 0)
        verifyUpsertAndAllocateEnrolment(mtditid, nino, count = 0)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid, count = 0)
      }
      "an unexpected status error is returned from the sign up" in {
        mockSignUp(nino, utr, taxYear)(Left(SignUpFailureResponse.UnexpectedStatus(INTERNAL_SERVER_ERROR)))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Left(SignUpOrchestrationService.UnhandledSignUpError)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel, count = 0)
        verifyUpsertAndAllocateEnrolment(mtditid, nino, count = 0)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid, count = 0)
      }
    }
    "return a create income sources failure" when {
      "an error was returned from the create income sources connector" in {
        mockSignUp(nino, utr, taxYear)(Right(SignUpSuccessful(mtditid)))
        mockCreateIncomeSources(mtditid, createIncomeSourcesModel)(Left(CreateIncomeSourcesResponseHttpParser.UnexpectedStatus(INTERNAL_SERVER_ERROR)))
        mockUpsertAndAllocateEnrolment(mtditid, nino)(Right(UpsertAndAllocateEnrolmentService.UpsertAndAllocateEnrolmentSuccess))
        mockConfirmPreference(entityId, mtditid)

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Left(SignUpOrchestrationService.CreateIncomeSourcesFailure)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel)
        verifyUpsertAndAllocateEnrolment(mtditid, nino)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid)
      }
    }
  }

  lazy val nino: String = "test-nino"
  lazy val utr: String = "test-utr"
  lazy val mtditid: String = "test-mtditid"
  lazy val taxYear: AccountingYear = Current
  lazy val createIncomeSourcesModel: CreateIncomeSourcesModel = CreateIncomeSourcesModel(
    nino = nino,
    ukProperty = Some(UkProperty(
      startDateBeforeLimit = Some(true),
      accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
      tradingStartDate = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)
    ))
  )
  lazy val entityId: String = "test-entity-id"

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  object TestSignUpOrchestrationService extends SignUpOrchestrationService(
    mockSignUpConnector,
    mockCreateIncomeSourcesConnector,
    mockUpsertAndAllocateEnrolmentService,
    mockSpsService
  )

}
