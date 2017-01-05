/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import akka.actor._
import akka.stream._
import assets.MessageLookup
import auth._
import config.{FrontendAppConfig, FrontendAuthConnector}
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}

class HelloWorldControllerSpec extends PlaySpec with OneAppPerTest {

  object HelloWorldTestController extends HelloWorldController {
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val postSignInRedirectUrl = MockConfig.ggSignInContinueUrl
  }

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  "The HelloWorld controller" should {
    "use the correct applicationConfig" in {
      HelloWorldController.applicationConfig must be (FrontendAppConfig)
    }
    "use the correct authConnector" in {
      HelloWorldController.authConnector must be (FrontendAuthConnector)
    }
    "use the correct postSignInRedirectUrl" in {
      HelloWorldController.postSignInRedirectUrl must be (FrontendAppConfig.ggSignInContinueUrl)
    }
  }

  "Calling the helloWorld action of the HelloWorldController with an authorised user" should {

    lazy val result = HelloWorldTestController.helloWorld(authenticatedFakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be (Status.OK)
    }

    "return HTML" in {
      contentType(result) must be (Some("text/html"))
      charset(result) must be (Some("utf-8"))
    }

    s"have the title '${MessageLookup.HelloWorld.title}'" in {
      document.title() must be (MessageLookup.HelloWorld.title)
    }
  }

  "Calling the helloWorld action of the HelloWorldController with an unauthorised user" should {

    lazy val result = HelloWorldTestController.helloWorld(FakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 303" in {
      status(result) must be (Status.SEE_OTHER)
    }
  }
}
