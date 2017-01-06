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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}


trait ControllerBaseSpec extends PlaySpec with OneAppPerTest {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val controllerName: String
  val authorisedRoutes: Map[String, Action[AnyContent]]

  def authorisationTests = {
    authorisedRoutes.foreach {
      case (name, call) =>
        s"Calling the $name action of the $controllerName with an unauthorised user" should {

          lazy val result = call(FakeRequest())

          "return 303" in {
            status(result) must be(Status.SEE_OTHER)
          }
        }
    }
  }
}
