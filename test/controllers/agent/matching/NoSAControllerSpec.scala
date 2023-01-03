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

package controllers.agent.matching

import assets.MessageLookup
import controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.test.Helpers._
import views.html.agent.ClientNoSa

class NoSAControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "NoSAController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private def withController(testCode: NoSAController => Any): Unit = {
    val mockClientNoSaView: ClientNoSa = app.injector.instanceOf[ClientNoSa]

    implicit lazy val mockMessagesControllerComponents: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

    val controller = new NoSAController(
      mockClientNoSaView,
      mockMessagesControllerComponents
    )

    testCode(controller)
  }

  "Calling the show action of the NoSAController" should withController { controller =>

    lazy val result = controller.show(subscriptionRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    s"have the title '${MessageLookup.NoSA.Agent.title}'" in {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      document.title() must be(MessageLookup.NoSA.Agent.title + serviceNameGovUk)
    }
  }

}
