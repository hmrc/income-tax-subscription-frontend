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

import uk.gov.hmrc.play.http.HttpResponse
import play.api.http.Status._

sealed trait PaperlessState {
  val redirection: Option[String]
}

object PaperlessState {

  import utils.Implicits.{EitherUtilLeft, EitherUtilRight, optionWrapperUtil}

  val parseResponse: HttpResponse => String = (response: HttpResponse) => (response.json \ "redirectUserTo").get.as[String]

  def apply(response: HttpResponse): Either[Int, PaperlessState] = response.status match {
    case OK => Activated //200
    case CONFLICT => Declined //409
    case PRECONDITION_FAILED => Unset //412
    case x => x
  }

}

case object Activated extends PaperlessState {
  override val redirection: Option[String] = None
}

case object Declined extends PaperlessState {
  override val redirection: Option[String] = None
}

case object Unset extends PaperlessState {
  override val redirection: Option[String] = None
}

