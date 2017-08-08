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

package connectors.models.iv

import play.api.libs.json.Json

case class IVJourneyResultModel(result: String, token: String)

object IVJourneyResultModel {
  implicit val format = Json.format[IVJourneyResultModel]
}

sealed trait IVJourneyResult

object IVJourneyResult {
  def apply(iVJourneyResultModel: IVJourneyResultModel): IVJourneyResult = iVJourneyResultModel match {
    case IVJourneyResultModel("Success", _) => IVSuccess
    case IVJourneyResultModel("Timeout", _) => IVTimeout
    case _ => IVFailure
  }
}

case object IVSuccess extends IVJourneyResult

case object IVTimeout extends IVJourneyResult

case object IVFailure extends IVJourneyResult