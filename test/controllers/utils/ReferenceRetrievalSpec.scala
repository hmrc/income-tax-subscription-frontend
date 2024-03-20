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

package controllers.utils

import auth.agent.IncomeTaxAgentUser
import auth.individual.IncomeTaxSAUser
import common.Constants.ITSASessionKeys
import connectors.httpparser.RetrieveReferenceHttpParser
import models.audits.SignupRetrieveAuditing.SignupRetrieveAuditModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, defaultAwaitTimeout, status}
import services.mocks.{MockSessionDataService, MockSubscriptionDetailsService}
import services.{AuditingService, SessionDataService, SubscriptionDetailsService}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.{ExecutionContext, Future}

class ReferenceRetrievalSpec extends PlaySpec with MockSubscriptionDetailsService with MockSessionDataService {

  val mockAuditingService: AuditingService = mock[AuditingService]

  override def beforeEach(): Unit = {
    reset(mockAuditingService)
    super.beforeEach()
  }

  object TestReferenceRetrieval extends ReferenceRetrieval {
    override val subscriptionDetailsService: SubscriptionDetailsService = MockSubscriptionDetailsService
    override val auditingService: AuditingService = mockAuditingService
    override val sessionDataService: SessionDataService = mockSessionDataService
    override implicit val ec: ExecutionContext = executionContext
  }

  val utr: String = "1234567890"
  val nino: String = "AA111111A"
  val arn: String = "123456"
  val reference: String = "test-reference"

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  implicit val individualUser: IncomeTaxSAUser = new IncomeTaxSAUser(Enrolments(Set()), None, None, ConfidenceLevel.L200, "userId")
  implicit val agentUser: IncomeTaxAgentUser = new IncomeTaxAgentUser(
    Enrolments(Set(Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("ARN", arn)), "activated", None)))
    , None, ConfidenceLevel.L50)

  implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr, ITSASessionKeys.NINO -> nino)
  val requestWithoutUTR: Request[AnyContent] = FakeRequest()

  "withIndividualReference" should {
    "return the reference requested" when {
      "the reference was successfully returned from the session store" in {
        mockFetchReferenceSuccess(Some(reference))

        val result = TestReferenceRetrieval.withIndividualReference { reference =>
          Future.successful(Ok(reference))
        }

        status(result) mustBe OK
        contentAsString(result) mustBe reference
      }
      "the reference was not found in the session store so retrieved existing reference from subscription data" in {
        mockFetchReferenceSuccess(None)
        mockRetrieveReferenceSuccess(utr, RetrieveReferenceHttpParser.Existing)(reference)
        when(mockAuditingService.audit(ArgumentMatchers.eq(SignupRetrieveAuditModel(None, utr, Some(nino))))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(AuditResult.Success))
        mockSaveReferenceSuccess(reference)

        val result = TestReferenceRetrieval.withIndividualReference { reference =>
          Future.successful(Ok(reference))
        }

        status(result) mustBe OK
        contentAsString(result) mustBe reference
      }
      "the reference was not found in the session store so retrieved a newly created reference" in {
        mockFetchReferenceSuccess(None)
        mockRetrieveReferenceSuccess(utr, RetrieveReferenceHttpParser.Created)(reference)
        mockSaveReferenceSuccess(reference)

        val result = TestReferenceRetrieval.withIndividualReference { reference =>
          Future.successful(Ok(reference))
        }

        status(result) mustBe OK
        contentAsString(result) mustBe reference
      }
    }
    "throw an InternalServerException" when {
      "utr was not available" in {
        intercept[InternalServerException](await(TestReferenceRetrieval.withIndividualReference { _ =>
          Future.successful(Ok("test-utr-failure"))
        }(implicitly, requestWithoutUTR, implicitly))).message mustBe s"[ReferenceRetrieval][withIndividualReference] - Unable to retrieve users utr"
      }
      "an unexpected status error was returned from the session store" in {
        mockFetchReferenceStatusFailure(INTERNAL_SERVER_ERROR)

        intercept[InternalServerException](await(TestReferenceRetrieval.withIndividualReference { _ =>
          Future.successful(Ok("test-status-failure"))
        })).message mustBe s"[ReferenceRetrieval][withReference] - Error occurred when fetching reference from session. Status: $INTERNAL_SERVER_ERROR"
      }
      "a json parse error was returned from the session store" in {
        mockFetchReferenceJsonFailure()

        intercept[InternalServerException](await(TestReferenceRetrieval.withIndividualReference { _ =>
          Future.successful(Ok("test-json-failure"))
        })).message mustBe s"[ReferenceRetrieval][withReference] - Unable to parse json returned from session"
      }
      "an unexpected status error was returned from the subscription data" in {
        mockFetchReferenceSuccess(None)
        mockRetrieveReference(utr)(Left(RetrieveReferenceHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(TestReferenceRetrieval.withIndividualReference { _ =>
          Future.successful(Ok("test-reference-status-failure"))
        })).message mustBe s"[ReferenceRetrieval][handleReferenceNotFound] - Unexpected status returned: $INTERNAL_SERVER_ERROR"
      }
      "a json parse error was returned when retrieving the reference from subscription data" in {
        mockFetchReferenceSuccess(None)
        mockRetrieveReference(utr)(Left(RetrieveReferenceHttpParser.InvalidJsonFailure))

        intercept[InternalServerException](await(TestReferenceRetrieval.withIndividualReference { _ =>
          Future.successful(Ok("test-reference-json-failure"))
        })).message mustBe s"[ReferenceRetrieval][handleReferenceNotFound] - Unable to parse json returned"
      }
      "an unexpected status error was returned when saving the reference to session" in {
        mockFetchReferenceSuccess(None)
        mockRetrieveReferenceSuccess(utr, RetrieveReferenceHttpParser.Created)(reference)
        mockSaveReferenceStatusFailure(reference)(INTERNAL_SERVER_ERROR)

        intercept[InternalServerException](await(TestReferenceRetrieval.withIndividualReference { _ =>
          Future.successful(Ok("test-reference-save-status-failure"))
        })).message mustBe s"[ReferenceRetrieval][saveReferenceToSession] - Unexpected status returned: $INTERNAL_SERVER_ERROR"
      }
    }
  }

  "withAgentReference" should {
    "return the reference requested" when {
      "the reference was successfully returned from the session store" in {
        mockFetchReferenceSuccess(Some(reference))

        val result = TestReferenceRetrieval.withAgentReference { reference =>
          Future.successful(Ok(reference))
        }

        status(result) mustBe OK
        contentAsString(result) mustBe reference
      }
      "the reference was not found in the session store so retrieved existing reference from subscription data" in {
        mockFetchReferenceSuccess(None)
        mockRetrieveReferenceSuccess(utr, RetrieveReferenceHttpParser.Existing)(reference)
        when(mockAuditingService.audit(ArgumentMatchers.eq(SignupRetrieveAuditModel(Some(arn), utr, Some(nino))))(
          ArgumentMatchers.any(), ArgumentMatchers.any()
        )).thenReturn(Future.successful(AuditResult.Success))
        mockSaveReferenceSuccess(reference)

        val result = TestReferenceRetrieval.withAgentReference { reference =>
          Future.successful(Ok(reference))
        }

        status(result) mustBe OK
        contentAsString(result) mustBe reference
      }
      "the reference was not found in the session store so retrieved a newly created reference" in {
        mockFetchReferenceSuccess(None)
        mockRetrieveReferenceSuccess(utr, RetrieveReferenceHttpParser.Created)(reference)
        mockSaveReferenceSuccess(reference)

        val result = TestReferenceRetrieval.withAgentReference { reference =>
          Future.successful(Ok(reference))
        }

        status(result) mustBe OK
        contentAsString(result) mustBe reference
      }
    }
    "throw an InternalServerException" when {
      "utr was not available" in {
        intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { _ =>
          Future.successful(Ok("test-utr-failure"))
        }(implicitly, requestWithoutUTR, implicitly))).message mustBe s"[ReferenceRetrieval][withAgentReference] - Unable to retrieve clients utr"
      }
      "an unexpected status error was returned from the session store" in {
        mockFetchReferenceStatusFailure(INTERNAL_SERVER_ERROR)

        intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { _ =>
          Future.successful(Ok("test-status-failure"))
        })).message mustBe s"[ReferenceRetrieval][withReference] - Error occurred when fetching reference from session. Status: $INTERNAL_SERVER_ERROR"
      }

      "a json parse error was returned from the session store" in {
        mockFetchReferenceJsonFailure()

        intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { _ =>
          Future.successful(Ok("test-json-failure"))
        })).message mustBe s"[ReferenceRetrieval][withReference] - Unable to parse json returned from session"
      }
      "an unexpected status error was returned from the subscription data" in {
        mockFetchReferenceSuccess(None)
        mockRetrieveReference(utr)(Left(RetrieveReferenceHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { _ =>
          Future.successful(Ok("test-reference-status-failure"))
        })).message mustBe s"[ReferenceRetrieval][handleReferenceNotFound] - Unexpected status returned: $INTERNAL_SERVER_ERROR"
      }
      "a json parse error was returned when retrieving the reference from subscription data" in {
        mockFetchReferenceSuccess(None)
        mockRetrieveReference(utr)(Left(RetrieveReferenceHttpParser.InvalidJsonFailure))

        intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { _ =>
          Future.successful(Ok("test-reference-json-failure"))
        })).message mustBe s"[ReferenceRetrieval][handleReferenceNotFound] - Unable to parse json returned"
      }
      "an unexpected status error was returned when saving the reference to session" in {
        mockFetchReferenceSuccess(None)
        mockRetrieveReferenceSuccess(utr, RetrieveReferenceHttpParser.Created)(reference)
        mockSaveReferenceStatusFailure(reference)(INTERNAL_SERVER_ERROR)

        intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { _ =>
          Future.successful(Ok("test-reference-save-status-failure"))
        })).message mustBe s"[ReferenceRetrieval][saveReferenceToSession] - Unexpected status returned: $INTERNAL_SERVER_ERROR"
      }
    }
  }

}
