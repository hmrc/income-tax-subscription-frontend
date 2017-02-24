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

package connectors.mocks

import config.AppConfig
import connectors.models.preferences.PaperlessState
import connectors.preferences.PreferenceFrontendConnector
import play.api.http.Status._
import play.api.i18n.MessagesApi
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, Request}
import utils.JsonUtils._
import utils.UnitTestTrait

trait MockPreferenceFrontendConnector extends UnitTestTrait
  with MockHttp {

  object TestPreferenceFrontendConnector extends PreferenceFrontendConnector(
    app.injector.instanceOf[AppConfig],
    mockHttpGet,
    mockHttpPut,
    app.injector.instanceOf[MessagesApi]
  )

  def setupCheckPaperless(tuple: (Int, Option[JsValue]))(implicit request: Request[AnyContent]): Unit =
    setupMockCheckPaperless(tuple._1, tuple._2)

  def setupMockCheckPaperless(status: Int, response: Option[JsValue])(implicit request: Request[AnyContent]): Unit =
    setupMockHttpPut[String](url = TestPreferenceFrontendConnector.checkPaperlessUrl, "")(status, response)


  private final val okResponseJson: Boolean => JsValue =
    (paperless: Boolean) => s"""{ "${PaperlessState.Paperless}": $paperless }""".stripMargin


  val paperlessActivated: ((Int, Option[JsValue])) = (OK, okResponseJson(true))

  val paperlessDeclined: ((Int, Option[JsValue])) = (OK, okResponseJson(false))

  val paperlessPreconditionFailed: ((Int, Option[JsValue])) = (PRECONDITION_FAILED, None)

}
