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

package models

import play.api.i18n.Messages
import play.api.libs.json.Reads._
import play.api.libs.json._

sealed trait YesNo {
  def toMessageString(implicit messages: Messages): String
}

object Yes extends YesNo {
  private[models] val YES = "Yes"

  override def toString: String = YES

  override def toMessageString(implicit messages: Messages): String = messages("base.yes")
}

object No extends YesNo {
  private[models] val NO = "No"

  override def toString: String = NO

  override def toMessageString(implicit messages: Messages): String = messages("base.no")
}

object YesNo {

  import No.NO
  import Yes.YES

  private val reads: Reads[YesNo] = new Reads[YesNo] {
    override def reads(json: JsValue): JsResult[YesNo] =
      json.validate[String] map {
        case YES => Yes
        case NO => No
      }
  }

  private val writes: Writes[YesNo] = new Writes[YesNo] {
    override def writes(o: YesNo): JsValue = o match {
      case Yes => JsString(YES)
      case No => JsString(NO)
    }
  }

  implicit val format: Format[YesNo] = Format[YesNo](reads, writes)
}
