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

package controllers.individual.matching

import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataSuccessResponse
import controllers.individual.ControllerBaseSpec
import controllers.individual.actions.PreSignUpJourneyRefiner
import controllers.individual.actions.mocks.MockIdentifierAction
import controllers.individual.resolvers.{AlreadyEnrolledResolver, MockAlreadySignedUpResolver}
import models.requests.individual.{IdentifierRequest, PreSignUpRequest}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.mocks.*

import scala.concurrent.Future

class HomeControllerSpec extends ControllerBaseSpec
  with MockIdentifierAction
  with MockAuditingService
  with MockReferenceRetrieval
  with MockSessionDataService
  with MockThrottlingService
  with MockPrePopDataService
  with MockCitizenDetailsService
  with MockGetEligibilityStatusService
  with MockAlreadySignedUpResolver
  with MockSubscriptionService {

  class MockRefiner extends PreSignUpJourneyRefiner(app.injector.instanceOf[AlreadyEnrolledResolver]) {
    override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, PreSignUpRequest[A]]] = {
      Future.successful(Right(PreSignUpRequest(request, request.nino, request.utr)))
    }
  }

  private def testHomeController() = new HomeController(
    fakeIdentifierAction,
    new MockRefiner(),
    mockSessionDataService,
    mockCitizenDetailsService,
    mockThrottlingService,
    mockSubscriptionService,
    mockAuditingService,
    mockGetEligibilityStatusService,
    mockReferenceRetrieval,
    mockResolver,
    mockPrePopDataService
  )(mockMessagesControllerComponents, executionContext)

  override val controllerName: String = "HomeController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map.empty

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockGetAllSessionData()
    mockSaveUTR(utr)(Right(SaveSessionDataSuccessResponse))
    setupMockGetSubscriptionFound(nino)
    mockResolverNoChannel()
  }

  "Already Signed Up users are redirected to the resolver" in {
    val result = testHomeController().index()(FakeRequest())
    status(result) mustBe SEE_OTHER
    redirectLocation(result) mustBe Some(resolverUrl)
  }
}
