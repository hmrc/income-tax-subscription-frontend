/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.httpparser.RetrieveReferenceHttpParser.{InvalidJsonFailure, UnexpectedStatusFailure}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import controllers.agent.ITSASessionKeys
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.mvc.{AnyContent, Request, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout, status}
import services.SubscriptionDetailsService
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

class ReferenceRetrievalSpec extends PlaySpec with MustMatchers with MockSubscriptionDetailsService with Results {

  object TestReferenceRetrieval extends ReferenceRetrieval {
    override val subscriptionDetailsService: SubscriptionDetailsService = MockSubscriptionDetailsService
    override implicit val ec: ExecutionContext = executionContext
  }

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  implicit val individualUser: IncomeTaxSAUser = IncomeTaxSAUser(Enrolments(Set()), None, None, ConfidenceLevel.L200, "userId")
  implicit val agentUser: IncomeTaxAgentUser = IncomeTaxAgentUser(Enrolments(Set()), None, ConfidenceLevel.L50)

  val utr: String = "1234567890"
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
          mockRetrieveReferenceSuccess(utr)(reference)

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
        "the reference is not in session and we call out to retrieve the reference successfully and add the reference to the session" in {
          mockRetrieveReferenceSuccess(utr)(reference)

          implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

          val result = TestReferenceRetrieval.withReference(utr) { reference =>
            Future.successful(Ok(reference))
          }

          status(result) mustBe OK
        }
      }
    }
  }

  "withAgentReference" should {
    "return an exception" when {
      "the clients utr is not in session" in {
        implicit val request: Request[AnyContent] = FakeRequest().withSession()

        intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { reference =>
          Future.successful(Ok(reference))
        })).message mustBe "[ReferenceRetrieval][withAgentReference] - Unable to retrieve clients utr"
      }
      "reference is not already in session and the retrieval returns an InvalidJson error" in {
        mockRetrieveReference(utr)(Left(InvalidJsonFailure))

        implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

        intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { reference =>
          Future.successful(Ok(reference))
        })).message mustBe "[ReferenceRetrieval][withAgentReference] - Unable to parse json returned"
      }
      "reference is not already in session and the retrieval returns an UnexpectedStatus error" in {
        mockRetrieveReference(utr)(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

        intercept[InternalServerException](await(TestReferenceRetrieval.withAgentReference { reference =>
          Future.successful(Ok(reference))
        })).message mustBe s"[ReferenceRetrieval][withAgentReference] - Unexpected status returned: $INTERNAL_SERVER_ERROR"
      }
    }
    "pass the reference through to the provided function" when {
      "the reference is already in session" in {
        implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr, ITSASessionKeys.REFERENCE -> reference)

        val result = TestReferenceRetrieval.withAgentReference { reference =>
          Future.successful(Ok(reference))
        }

        status(result) mustBe OK
      }
      "the reference is not in session and we call out to retrieve the reference successfully and add the reference to the session" in {
        mockRetrieveReferenceSuccess(utr)(reference)

        implicit val request: Request[AnyContent] = FakeRequest().withSession(ITSASessionKeys.UTR -> utr)

        val result = TestReferenceRetrieval.withAgentReference { reference =>
          Future.successful(Ok(reference))
        }

        status(result) mustBe OK
      }
    }
  }

}
