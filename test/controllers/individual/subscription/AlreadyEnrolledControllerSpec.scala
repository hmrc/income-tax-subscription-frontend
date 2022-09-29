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

package controllers.individual.subscription

import controllers.ControllerBaseSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks.MockAuditingService
import views.html.individual.incometax.subscription.enrolled.AlreadyEnrolled

import scala.concurrent.Future

class AlreadyEnrolledControllerSpec extends ControllerBaseSpec with MockAuditingService {

  val mockAlreadyEnrolledView: AlreadyEnrolled = mock[AlreadyEnrolled]
  when(mockAlreadyEnrolledView()(ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(HtmlFormat.empty)

  object TestAlreadyEnrolledController extends AlreadyEnrolledController(
    mockAuditingService,
    mockAuthService,
    mockAlreadyEnrolledView
  )

  override val controllerName: String = "AlreadyEnrolledController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "enrolled" -> TestAlreadyEnrolledController.show()
  )

  "Calling the enrolled action of the AlreadyEnrolledController with an enrolled Authenticated User" should {

    def call(request: Request[AnyContent]): Future[Result] = TestAlreadyEnrolledController.show(request)

    "return an OK with the error page" in {
      mockAuthEnrolled()

      lazy val result = call(subscriptionRequest)

      status(result) must be(Status.OK)
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }
  }

  authorisationTests()
}
