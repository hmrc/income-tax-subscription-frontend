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

package services.agent

import config.AppConfig
import config.featureswitch.FeatureSwitch.AgentRelationshipSingleCall
import config.featureswitch.FeatureSwitching
import connectors.httpparser.CreateIncomeSourcesResponseHttpParser
import connectors.mocks.{MockCreateIncomeSourcesConnector, MockSignUpConnector}
import models.common.subscription.SignUpSuccessResponse.{AlreadySignedUp, SignUpSuccessful}
import models.common.subscription.{CreateIncomeSourcesModel, SignUpFailureResponse, UkProperty}
import models.{AccountingYear, Current, DateModel}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.agent.mocks.{MockAgentSPSConnector, MockClientRelationshipService}
import services.mocks.MockAutoEnrolmentService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.AccountingPeriodUtil

import scala.concurrent.ExecutionContext.Implicits.global

class SignUpOrchestrationServiceSpec extends PlaySpec
  with MockSignUpConnector
  with MockCreateIncomeSourcesConnector
  with MockAutoEnrolmentService
  with MockClientRelationshipService
  with MockAgentSPSConnector
  with FeatureSwitching {

  "orchestrateSignUp" must {
    "return a sign up orchestration success response" when {
      "attempting to sign up the user and they are already signed up" in {
        mockSignUp(nino, utr, taxYear)(Right(AlreadySignedUp))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(arn, nino, utr, taxYear, createIncomeSourcesModel)

        await(result) mustBe Right(SignUpOrchestrationService.SignUpOrchestrationSuccessful)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel, count = 0)
        verifyAutoClaimEnrolment(utr, nino, mtditid, count = 0)
        verifyCheckPreExistingMTDRelationship(arn, nino, count = 0)
        verifyAgentSpsConnector(arn, utr, nino, mtditid, count = 0)
        verifyCheckMTDAgentRelationship(nino, 0)
      }
      "sign up and creation of income sources was successful" in {
        mockSignUp(nino, utr, taxYear)(Right(SignUpSuccessful(mtditid)))
        mockCreateIncomeSources(mtditid, createIncomeSourcesModel)(Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess))
        mockAutoClaimEnrolment(utr, nino, mtditid)(Right(AutoEnrolmentService.EnrolSuccess))
        preExistingMTDRelationship(arn, nino)(isPreExistingMTDRelationship = true)
        mockAgentSpsConnectorSuccess(arn, utr, nino, mtditid)

        val result = TestSignUpOrchestrationService.orchestrateSignUp(arn, nino, utr, taxYear, createIncomeSourcesModel)

        await(result) mustBe Right(SignUpOrchestrationService.SignUpOrchestrationSuccessful)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel)
        verifyAutoClaimEnrolment(utr, nino, mtditid)
        verifyCheckPreExistingMTDRelationship(arn, nino)
        verifyAgentSpsConnector(arn, utr, nino, mtditid)
        verifyCheckMTDAgentRelationship(nino, 0)
      }
      "the auto claim enrolment process was not successful" in {
        mockSignUp(nino, utr, taxYear)(Right(SignUpSuccessful(mtditid)))
        mockCreateIncomeSources(mtditid, createIncomeSourcesModel)(Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess))
        mockAutoClaimEnrolment(utr, nino, mtditid)(Left(AutoEnrolmentService.EnrolmentNotAllocated))
        preExistingMTDRelationship(arn, nino)(isPreExistingMTDRelationship = true)

        val result = TestSignUpOrchestrationService.orchestrateSignUp(arn, nino, utr, taxYear, createIncomeSourcesModel)

        await(result) mustBe Right(SignUpOrchestrationService.SignUpOrchestrationSuccessful)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel)
        verifyAutoClaimEnrolment(utr, nino, mtditid)
        verifyCheckPreExistingMTDRelationship(arn, nino)
        verifyAgentSpsConnector(arn, utr, nino, mtditid, count = 0)
        verifyCheckMTDAgentRelationship(nino, 0)
      }
      "there was no relationship found for mtd" in {
        mockSignUp(nino, utr, taxYear)(Right(SignUpSuccessful(mtditid)))
        mockCreateIncomeSources(mtditid, createIncomeSourcesModel)(Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess))
        mockAutoClaimEnrolment(utr, nino, mtditid)(Right(AutoEnrolmentService.EnrolSuccess))
        preExistingMTDRelationship(arn, nino)(isPreExistingMTDRelationship = false)
        suppAgentRelationship(arn, nino)(isMTDSuppAgentRelationship = true)
        mockAgentSpsConnectorSuccess(arn, utr, nino, mtditid)

        val result = TestSignUpOrchestrationService.orchestrateSignUp(arn, nino, utr, taxYear, createIncomeSourcesModel)

        await(result) mustBe Right(SignUpOrchestrationService.SignUpOrchestrationSuccessful)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel)
        verifyAutoClaimEnrolment(utr, nino, mtditid)
        verifyCheckPreExistingMTDRelationship(arn, nino)
        verifyCheckMTDSuppAgentRelationship(arn, nino)
        verifyAgentSpsConnector(arn, utr, nino, mtditid)
        verifyCheckMTDAgentRelationship(nino, 0)
      }
      "there was no relationship found for mtd supporting" in {
        mockSignUp(nino, utr, taxYear)(Right(SignUpSuccessful(mtditid)))
        mockCreateIncomeSources(mtditid, createIncomeSourcesModel)(Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess))
        mockAutoClaimEnrolment(utr, nino, mtditid)(Right(AutoEnrolmentService.EnrolSuccess))
        preExistingMTDRelationship(arn, nino)(isPreExistingMTDRelationship = false)
        suppAgentRelationship(arn, nino)(isMTDSuppAgentRelationship = false)
        mockAgentSpsConnectorSuccess(arn, utr, nino, mtditid)

        val result = TestSignUpOrchestrationService.orchestrateSignUp(arn, nino, utr, taxYear, createIncomeSourcesModel)

        await(result) mustBe Right(SignUpOrchestrationService.SignUpOrchestrationSuccessful)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel)
        verifyAutoClaimEnrolment(utr, nino, mtditid)
        verifyCheckPreExistingMTDRelationship(arn, nino)
        verifyCheckMTDSuppAgentRelationship(arn, nino)
        verifyAgentSpsConnector(arn, utr, nino, mtditid)
        verifyCheckMTDAgentRelationship(nino, 0)
      }
      "the single call feature switch is on" in {
        enable(AgentRelationshipSingleCall)
        mockSignUp(nino, utr, taxYear)(Right(SignUpSuccessful(mtditid)))
        mockCreateIncomeSources(mtditid, createIncomeSourcesModel)(Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess))
        mockAutoClaimEnrolment(utr, nino, mtditid)(Right(AutoEnrolmentService.EnrolSuccess))
        mockAgentSpsConnectorSuccess(arn, utr, nino, mtditid)
        agentRelationship(nino)(isMTDAgentRelationship = false)

        val result = TestSignUpOrchestrationService.orchestrateSignUp(arn, nino, utr, taxYear, createIncomeSourcesModel)

        await(result) mustBe Right(SignUpOrchestrationService.SignUpOrchestrationSuccessful)

        verifyCheckPreExistingMTDRelationship(arn, nino, 0)
        verifyCheckMTDSuppAgentRelationship(arn, nino, 0)
        verifyCheckMTDAgentRelationship(nino)
      }
    }
    "return a sign up failure" when {
      "an error was returned from the sign up connector" in {
        mockSignUp(nino, utr, taxYear)(Left(SignUpFailureResponse.UnexpectedStatus(INTERNAL_SERVER_ERROR)))

        val result = TestSignUpOrchestrationService.orchestrateSignUp(arn, nino, utr, taxYear, createIncomeSourcesModel)

        await(result) mustBe Left(SignUpOrchestrationService.SignUpFailure)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel, count = 0)
        verifyAutoClaimEnrolment(utr, nino, mtditid, count = 0)
        verifyCheckPreExistingMTDRelationship(arn, nino, count = 0)
        verifyCheckMTDSuppAgentRelationship(arn, nino, count = 0)
        verifyAgentSpsConnector(arn, utr, nino, mtditid, count = 0)
        verifyCheckMTDAgentRelationship(nino, 0)
      }
    }
    "return a create income sources failure" when {
      "an error was returned from the create income sources connector" in {
        mockSignUp(nino, utr, taxYear)(Right(SignUpSuccessful(mtditid)))
        mockCreateIncomeSources(mtditid, createIncomeSourcesModel)(Left(CreateIncomeSourcesResponseHttpParser.UnexpectedStatus(INTERNAL_SERVER_ERROR)))
        mockAutoClaimEnrolment(utr, nino, mtditid)(Right(AutoEnrolmentService.EnrolSuccess))
        preExistingMTDRelationship(arn, nino)(isPreExistingMTDRelationship = false)
        suppAgentRelationship(arn, nino)(isMTDSuppAgentRelationship = true)
        mockAgentSpsConnectorSuccess(arn, utr, nino, mtditid)

        val result = TestSignUpOrchestrationService.orchestrateSignUp(arn, nino, utr, taxYear, createIncomeSourcesModel)

        await(result) mustBe Left(SignUpOrchestrationService.CreateIncomeSourcesFailure)

        verifySignUp(nino, utr, taxYear)
        verifyCreateIncomeSources(mtditid, createIncomeSourcesModel)
        verifyAutoClaimEnrolment(utr, nino, mtditid)
        verifyCheckPreExistingMTDRelationship(arn, nino)
        verifyCheckMTDSuppAgentRelationship(arn, nino)
        verifyAgentSpsConnector(arn, utr, nino, mtditid)
        verifyCheckMTDAgentRelationship(nino, 0)
      }
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentRelationshipSingleCall)
  }

  lazy val nino: String = "test-nino"
  lazy val utr: String = "test-utr"
  lazy val mtditid: String = "test-mtditid"
  lazy val arn: String = "test-arn"
  lazy val taxYear: AccountingYear = Current
  lazy val createIncomeSourcesModel: CreateIncomeSourcesModel = CreateIncomeSourcesModel(
    nino = nino,
    ukProperty = Some(UkProperty(
      startDateBeforeLimit = Some(true),
      accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
      tradingStartDate = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)
    ))
  )

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  object TestSignUpOrchestrationService extends SignUpOrchestrationService(
    mockSignUpConnector,
    mockCreateIncomeSourcesConnector,
    mockAutoEnrolmentService,
    mockClientRelationshipService,
    mockAgentSpsConnector,
    appConfig
  )

  override val appConfig: AppConfig = mock[AppConfig]
}
