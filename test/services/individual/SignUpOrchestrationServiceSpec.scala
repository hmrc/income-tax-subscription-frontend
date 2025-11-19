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
import models.common.subscription.SignUpSuccessResponse.{AlreadySignedUp, SignUpSuccessful}
import models.common.subscription.{CreateIncomeSourcesModel, SignUpFailureResponse, UkProperty}
import models.{AccountingYear, Current, DateModel}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.individual.mocks.MockUpsertAndAllocateEnrolmentService
import services.individual.SignUpOrchestrationServiceModel._
import services.individual.UpsertAndAllocateEnrolmentServiceModel._
import services.mocks.MockSpsService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.AccountingPeriodUtil

import scala.concurrent.ExecutionContext.Implicits.global

class SignUpOrchestrationServiceSpec extends PlaySpec
  with MockSignUpConnector
  with MockCreateIncomeSourcesConnector
  with MockUpsertAndAllocateEnrolmentService
  with MockSpsService {

  "orchestrateSignUp" must {
    "return a sign up orchestration success response" when {
      "attempting to sign up the user and they are already signed up" in {
        mockSignUp(nino, utr, taxYear)(Right(AlreadySignedUp))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Right(SignUpOrchestrationServiceModel.SignUpOrchestrationSuccessful)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel, count = 0)
        verifyUpsertAndAllocateEnrolment(mtditid, nino, count = 0)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid, count = 0)
      }
      "sign up and creation of income sources was successful" in {
        mockSignUp(nino, utr, taxYear)(Right(SignUpSuccessful(mtditid)))
        mockCreateIncomeSources(mtditid, createIncomeSourcesModel)(Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess))
        mockUpsertAndAllocateEnrolment(mtditid, nino)(Right(UpsertAndAllocateEnrolmentServiceModel.UpsertAndAllocateEnrolmentSuccess))
        mockConfirmPreference(entityId, mtditid)

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Right(SignUpOrchestrationServiceModel.SignUpOrchestrationSuccessful)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel)
        verifyUpsertAndAllocateEnrolment(mtditid, nino)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid)
      }
      "there was a problem upserting and allocating the enrolment" in {
        mockSignUp(nino, utr, taxYear)(Right(SignUpSuccessful(mtditid)))
        mockCreateIncomeSources(mtditid, createIncomeSourcesModel)(Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess))
        mockUpsertAndAllocateEnrolment(mtditid, nino)(Left(UpsertAndAllocateEnrolmentServiceModel.AllocateEnrolmentFailure))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Right(SignUpOrchestrationServiceModel.SignUpOrchestrationSuccessful)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel)
        verifyUpsertAndAllocateEnrolment(mtditid, nino)
        verifyConfirmPreferencesPostSpsConfirm(entityId, mtditid, count = 0)
      }
    }
    "return a sign up failure" when {
      "an error was returned from the sign up connector" in {
        mockSignUp(nino, utr, taxYear)(Left(SignUpFailureResponse.UnexpectedStatus(INTERNAL_SERVER_ERROR)))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Left(SignUpOrchestrationServiceModel.SignUpFailure)

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
        mockUpsertAndAllocateEnrolment(mtditid, nino)(Right(UpsertAndAllocateEnrolmentServiceModel.UpsertAndAllocateEnrolmentSuccess))
        mockConfirmPreference(entityId, mtditid)

        val result = TestSignUpOrchestrationService.orchestrateSignUp(nino, utr, taxYear, createIncomeSourcesModel, Some(entityId))

        await(result) mustBe Left(SignUpOrchestrationServiceModel.CreateIncomeSourcesFailure)

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
