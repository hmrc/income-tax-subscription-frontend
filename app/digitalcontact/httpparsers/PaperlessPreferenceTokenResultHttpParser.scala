/*
 * Copyright 2019 HM Revenue & Customs
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

package digitalcontact.httpparsers

import digitalcontact.models.PaperlessPreferenceTokenResult._
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object PaperlessPreferenceTokenResultHttpParser {
  implicit object PaperlessPreferenceTokenResultHttpReads extends HttpReads[PaperlessPreferenceTokenResult] {
    override def read(method: String, url: String, response: HttpResponse): PaperlessPreferenceTokenResult = {
      response.status match {
        case CREATED => Right(PaperlessPreferenceTokenSuccess)
        case _ => Left(PaperlessPreferenceTokenFailure)
      }
    }
  }
}
