/*
 * Copyright 2026 HM Revenue & Customs
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

package models.individual

import play.api.libs.json.{Reads, __}
import uk.gov.hmrc.http.InternalServerException

import scala.util.matching.Regex

sealed trait ObfuscatedIdentifier

object ObfuscatedIdentifier {

  private lazy val EmailRegex: Regex = """^(.+)@(.+)$""".r
  private lazy val SplitMailbox: Regex = """^(.)(.*)(.)$""".r

  case class ObfuscatedUserId(id: String) extends ObfuscatedIdentifier

  case class UserEmail(email: String) extends ObfuscatedIdentifier {
    lazy val obfuscatedEmail: String = email match {
      case EmailRegex(mailbox, domain) if mailbox.length < 3 =>
        s"${"*" * mailbox.length}@$domain"
      case EmailRegex(SplitMailbox(first, middle, last), domain) =>
        s"$first${"*" * middle.length}$last@$domain"
      case _ => throw new InternalServerException("Unable to obfuscate invalid email address")
    }
  }

  implicit val reads: Reads[ObfuscatedIdentifier] = {
    (__ \ "identityProviderType").read[String].flatMap {
      case "SCP" => (__ \ "obfuscatedUserId").read[String].map(ObfuscatedUserId.apply)
      case "ONE_LOGIN" => (__ \ "email").read[String].map(UserEmail.apply)
      case other => Reads.failed(s"Invalid type received: $other")
    }
  }

}
