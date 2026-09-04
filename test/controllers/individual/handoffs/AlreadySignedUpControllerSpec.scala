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

package controllers.individual.handoffs

import controllers.individual.ControllerBaseSpec
import controllers.individual.actions.mocks.MockIdentifierAction
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import services.mocks.MockAuditingService
import views.html.individual.handoffs.AlreadySignedUp

import scala.concurrent.Future

class AlreadySignedUpControllerSpec extends ControllerBaseSpec with MockAuditingService with MockIdentifierAction {

  val mockAlreadyEnrolledView: AlreadySignedUp = mock[AlreadySignedUp]
  when(mockAlreadyEnrolledView(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
    .thenReturn(HtmlFormat.empty)

  class testAlreadyEnrolledController(noEnrolment: Boolean) extends AlreadySignedUpController(
    fakeIdentifierAction(noEnrolment),
    mockAlreadyEnrolledView
  )

  override val controllerName: String = "AlreadyEnrolledController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "Calling the enrolled action of the AlreadyEnrolledController with an enrolled Authenticated User" should {

    def show(noEnrolment: Boolean): Future[Result] = new testAlreadyEnrolledController(noEnrolment).show(fakeRequest)
    def submit(noEnrolment: Boolean): Future[Result] = new testAlreadyEnrolledController(noEnrolment).submit(fakeRequest)

    "return an OK with the error page" in {
      Seq(false, true).foreach { noEnrolment =>
        lazy val result = show(noEnrolment)

        status(result) must be(Status.OK)
        contentType(result) must be(Some("text/html"))
        charset(result) must be(Some("utf-8"))
      }
    }

    "redirect to" should {
      "V&C" in { // HO04C
        lazy val result = submit(false)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) must be(Some(appConfig.getVAndCUrl))
      }

      "tax account after log-out" in { // HO06C
        lazy val result = submit(true)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) must be(Some(appConfig.ggSignOutUrl(appConfig.getAccountUrl)))
      }
    }
  }
}
