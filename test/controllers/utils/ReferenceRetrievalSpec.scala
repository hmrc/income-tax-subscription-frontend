/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.httpparser.RetrieveReferenceHttpParser
import connectors.httpparser.RetrieveReferenceHttpParser.{Existing, InvalidJsonFailure, UnexpectedStatusFailure}
import controllers.agent.ITSASessionKeys
import models.audits.SignupRetrieveAuditing.SignupRetrieveAuditModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, times, verify}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.{AnyContent, Request, Results, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout, session, status}
import services.mocks.MockSubscriptionDetailsService
import services.{AuditModel, AuditingService, SubscriptionDetailsService}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{Await, ExecutionContext, Future}

class ReferenceRetrievalSpec extends PlaySpec with Matchers with MockSubscriptionDetailsService with Results {

  val mockAuditingService: AuditingService = mock[AuditingService]

  override def beforeEach(): Unit = {
    reset(mockAuditingService)
    super.beforeEach()
  }

  object TestReferenceRetrieval extends ReferenceRetrieval {
    override val subscriptionDetailsService: SubscriptionDetailsService = MockSubscriptionDetailsService
    override val auditingService: AuditingService = mockAuditingService
    override implicit val ec: ExecutionContext = executionContext
  }

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  implicit val individualUser: IncomeTaxSAUser = IncomeTaxSAUser(Enrolments(Set()), None, None, ConfidenceLevel.L200, "userId")
  implicit val agentUser: IncomeTaxAgentUser = IncomeTaxAgentUser(
    Enrolments(Set(Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("ARN", "123456")), "activated", None)))
    , None, ConfidenceLevel.L50)

  val utr: String = "1234567890"
  val arn: String = "test-arn"
  val reference: String = "test-reference"

  "withReference" when {
    "not provided with a utr directly" should {
      "return an exception" when {
        "the user's utr is not in session" in {
          implicit val request: Request[AnyContent] = FakeRequest().withSession()

          intercept[InternalServerException](await(TestReferenceRetrieval.withReference { reference =>
            Future.successful(Ok(reference))
          })).message mustBe "[ReferenceRetrieval][withReference] - Unable to retrieve users utr"
        }
        "reference is not already in session and the retrieval returns an InvalidJson error" in {
          mockRetrieveReference(utr)(Left(InvalidJsonFailure))

          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

          intercept[InternalServerException](await(TestReferenceRetrieval.withReference { reference =>
            Future.successful(Ok(reference))
          })).message mustBe "[ReferenceRetrieval][withReference] - Unable to parse json returned"
        }
        "reference is not already in session and the retrieval returns an UnexpectedStatus error" in {
          mockRetrieveReference(utr)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

          intercept[InternalServerException](await(TestReferenceRetrieval.withReference { reference =>
            Future.successful(Ok(reference))
          })).message mustBe s"[ReferenceRetrieval][withReference] - Unexpected status returned: $INTERNAL_SERVER_ERROR"
        }
      }
      "pass the reference through to the provided function" when {
        "the reference is already in session" in {
          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr, ITSASessionKeys.REFERENCE -> reference)

          val result = TestReferenceRetrieval.withReference { reference =>
            Future.successful(Ok(reference))
          }

          status(result) mustBe OK
        }
        "the reference is not in session and we call out to retrieve the reference successfully and add the reference to the session" in {
          mockRetrieveReferenceSuccess(utr, RetrieveReferenceHttpParser.Created)(reference)

          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

          val result = TestReferenceRetrieval.withReference { reference =>
            Future.successful(Ok(reference))
          }

          status(result) mustBe OK
        }
      }
    }

    "provided with a utr directly" should {
      "return an exception" when {
        "reference is not already in session and the retrieval returns an InvalidJson error" in {
          mockRetrieveReference(utr)(Left(InvalidJsonFailure))

          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

          intercept[InternalServerException](await(TestReferenceRetrieval.withReference(utr) { reference =>
            Future.successful(Ok(reference))
          })).message mustBe "[ReferenceRetrieval][withReference] - Unable to parse json returned"
        }
        "reference is not already in session and the retrieval returns an UnexpectedStatus error" in {
          mockRetrieveReference(utr)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

          intercept[InternalServerException](await(TestReferenceRetrieval.withReference(utr) { reference =>
            Future.successful(Ok(reference))
          })).message mustBe s"[ReferenceRetrieval][withReference] - Unexpected status returned: $INTERNAL_SERVER_ERROR"
        }
      }
      "pass the reference through to the provided function" when {
        "the reference is already in session" in {
          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr, ITSASessionKeys.REFERENCE -> reference)

          val result = TestReferenceRetrieval.withReference(utr) { reference =>
            Future.successful(Ok(reference))
          }

          status(result) mustBe OK
        }
        "the reference is not in session and we retrieve the reference successfully" when {
          "there is an existing reference" should {
            "return OK, do the signupRetrieve auditing and add the reference to the session" in {

              val auditModel: SignupRetrieveAuditModel = SignupRetrieveAuditModel(userType = "individual", None, utr, None)
              mockRetrieveReference(utr)(Right(Existing(reference)))

              implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

              val result = TestReferenceRetrieval.withReference(utr) { reference =>
                Future.successful(Ok(reference))
              }.futureValue

              session(result) mustBe Session(Map("UTR" -> utr, "reference" -> "test-reference"))

              verify(TestReferenceRetrieval.auditingService, times(1))
                .audit(ArgumentMatchers.eq(auditModel))(any(), ArgumentMatchers.eq(request))

              status(result) mustBe OK
            }
          }
          "there is not an existing reference" should {
            "return OK and add the reference to the session" in {

              mockRetrieveReference(utr)(Right(RetrieveReferenceHttpParser.Created(reference)))

              implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

              val result = TestReferenceRetrieval.withReference(utr) { reference =>
                Future.successful(Ok(reference))
              }.futureValue

              session(result) mustBe Session(Map("UTR" -> utr, "reference" -> "test-reference"))

              verify(TestReferenceRetrieval.auditingService, never).audit(any[AuditModel]())(any(), any())
              status(result) mustBe OK
            }
          }
        }
      }
    }
  }

  "withAgentReference" when {

    "not provided with a client's utr directly" should {
      "return an exception" when {
        "the client's utr is not in session" in {
          implicit val request: Request[AnyContent] = FakeRequest().withSession()

          intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { reference =>
            Future.successful(Ok(reference))
          })).message mustBe "[ReferenceRetrieval][withAgentReference] - Unable to retrieve clients utr"
        }
        "the client's reference is not already in session and the retrieval returns an InvalidJson error" in {
          mockRetrieveReference(utr)(Left(InvalidJsonFailure))

          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

          intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { reference =>
            Future.successful(Ok(reference))
          })).message mustBe "[ReferenceRetrieval][withAgentReference] - Unable to parse json returned"
        }
        "the client's reference is not already in session and the retrieval returns an UnexpectedStatus error" in {
          mockRetrieveReference(utr)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

          intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { reference =>
            Future.successful(Ok(reference))
          })).message mustBe s"[ReferenceRetrieval][withAgentReference] - Unexpected status returned: $INTERNAL_SERVER_ERROR"
        }
      }
      "pass the client's reference through to the provided function" when {
        "the reference is already in session" in {
          implicit val request: Request[AnyContent] = FakeRequest()
            .withSession(ITSASessionKeys.REFERENCE -> reference, ITSASessionKeys.UTR -> utr)

          val result = TestReferenceRetrieval.withAgentReference { reference =>
            Future.successful(Ok(reference))
          }

          status(result) mustBe OK
        }
        "the client's reference is not in session and we call out to retrieve the reference successfully and add the reference to the session" in {
          mockRetrieveReferenceSuccess(utr, RetrieveReferenceHttpParser.Created)(reference)

          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

          val result = TestReferenceRetrieval.withAgentReference { reference =>
            Future.successful(Ok(reference))
          }

          status(result) mustBe OK
        }
      }
    }
    "provided with a client's utr directly" should {
      "return an exception" when {
        "thee client's reference is not already in session and the retrieval returns an InvalidJson error" in {
          mockRetrieveReference(utr)(Left(InvalidJsonFailure))

          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

          intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference(utr) { reference =>
            Future.successful(Ok(reference))
          })).message mustBe "[ReferenceRetrieval][withAgentReference] - Unable to parse json returned"
        }
        "the client's reference is not already in session and the retrieval returns an UnexpectedStatus error" in {
          mockRetrieveReference(utr)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

          intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference(utr) { reference =>
            Future.successful(Ok(reference))
          })).message mustBe s"[ReferenceRetrieval][withAgentReference] - Unexpected status returned: $INTERNAL_SERVER_ERROR"
        }
      }
      "pass the client's reference through to the provided function" when {
        "the client's reference is already in session" in {
          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr, ITSASessionKeys.REFERENCE -> reference)

          val result = TestReferenceRetrieval.withAgentReference(utr) { reference =>
            Future.successful(Ok(reference))
          }

          status(result) mustBe OK
        }
        "the client's reference is not in session and we retrieve the reference successfully" when {
          "there is an existing client's reference" should {
            "return OK, do the signupRetrieve auditing and add the reference to the session for the client" in {

              val auditModel: SignupRetrieveAuditModel = SignupRetrieveAuditModel(userType = "agent", Some("123456"), utr, None)
              mockRetrieveReference(utr)(Right(Existing(reference)))

              implicit val request: Request[AnyContent] = FakeRequest()

              val result = TestReferenceRetrieval.withAgentReference(utr) { reference =>
                Future.successful(Ok(reference))
              }.futureValue

              status(result) mustBe OK
              result.session(request) mustBe Session(Map("reference" -> "test-reference"))

              verify(TestReferenceRetrieval.auditingService, times(1))
                .audit(ArgumentMatchers.eq(auditModel))(any(), ArgumentMatchers.eq(request))
            }
          }
          "there is not an existing client's reference" should {
            "return OK and add the reference to the session for the client" in {

              mockRetrieveReference(utr)(Right(RetrieveReferenceHttpParser.Created(reference)))

              implicit val request: Request[AnyContent] = FakeRequest()

              val result = TestReferenceRetrieval.withAgentReference(utr) { reference =>
                Future.successful(Ok(reference))
              }.futureValue

              status(result) mustBe OK
              result.session(request) mustBe Session(Map("reference" -> "test-reference"))

              verify(TestReferenceRetrieval.auditingService, never).audit(any[AuditModel]())(any(), any())
            }
          }
        }
      }
    }
  }

}
