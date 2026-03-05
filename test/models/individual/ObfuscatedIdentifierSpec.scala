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

import models.individual.ObfuscatedIdentifier.{ObfuscatedUserId, UserEmail}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.*
import uk.gov.hmrc.http.InternalServerException

class ObfuscatedIdentifierSpec extends PlaySpec {

  val obfuscatedIdJson: JsObject = Json.obj(
    "identityProviderType" -> "SCP",
    "obfuscatedUserId" -> "*****123"
  )

  val emailJson: JsObject = Json.obj(
    "identityProviderType" -> "ONE_LOGIN",
    "email" -> "test@email.com"
  )

  "ObfuscatedIdentifier" must {
    "successfully read from json" when {
      "identityProviderType has the value SCP and obfuscatedUserId is present in the json" in {
        Json.fromJson[ObfuscatedIdentifier](obfuscatedIdJson) mustBe JsSuccess(ObfuscatedUserId("*****123"), __ \ "obfuscatedUserId")
      }
      "identityProviderType has the value ONE_LOGIN and email is present in the json" in {
        Json.fromJson[ObfuscatedIdentifier](emailJson) mustBe JsSuccess(UserEmail("test@email.com"), __ \ "email")
      }
    }
    "fail to read from json" when {
      "identityProviderType has the value SCP and obfuscatedUserId not present in the json" in {
        Json.fromJson[ObfuscatedIdentifier](obfuscatedIdJson - "obfuscatedUserId") mustBe JsError(__ \ "obfuscatedUserId", "error.path.missing")
      }
      "identityProviderType has the value ONE_LOGIN and email is not present in the json" in {
        Json.fromJson[ObfuscatedIdentifier](emailJson - "email") mustBe JsError(__ \ "email", "error.path.missing")
      }
      "identityProviderType is not present in the json" in {
        Json.fromJson[ObfuscatedIdentifier](obfuscatedIdJson - "identityProviderType") mustBe JsError(__ \ "identityProviderType", "error.path.missing")
      }
      "identityProviderType has an invalid value in the json" in {
        Json.fromJson[ObfuscatedIdentifier](Json.obj("identityProviderType" -> "INVALID")) mustBe JsError(__, "Invalid type received: INVALID")
      }
    }
  }

  "UserEmail" must {
    "provide an obfuscated version" when {
      "the email's mailbox is very short" in {
        UserEmail("t@email.com").obfuscatedEmail mustBe "*@email.com"
      }
      "the email's mailbox is long enough to include original characters" in {
        UserEmail("test@email.com").obfuscatedEmail mustBe "t**t@email.com"
      }
    }
    "throw an exception" when {
      "the email is not valid" in {
        intercept[InternalServerException](UserEmail("invalid").obfuscatedEmail)
          .message mustBe "Unable to obfuscate invalid email address"
      }
    }
  }

}
