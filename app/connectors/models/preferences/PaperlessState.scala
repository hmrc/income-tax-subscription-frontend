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

package connectors.models.preferences

import connectors.models.ConnectorError
import uk.gov.hmrc.play.http.HttpResponse
import play.api.http.Status._
import play.api.libs.json.{JsResult, JsSuccess, JsValue, Reads}

sealed trait PaperlessState

object PaperlessState {

  implicit object PaperlessStateJsonReads extends Reads[PaperlessState] {
    override def reads(json: JsValue): JsResult[PaperlessState] = {
      (json \ "optedIn").validate[Boolean] map {
        case true => Activated
        case false => Declined
      }
    }
  }

}

case object Activated extends PaperlessState

case object Declined extends PaperlessState

case object Unset extends PaperlessState

case object PaperlessPreferenceError extends ConnectorError

