/*
 * Copyright 2023 HM Revenue & Customs
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


sealed trait CannotGoBack {
  def key: String
  def toMessageString(implicit messages: Messages): String = messages(s"agent.cannot-go-back-previous-client.$key")
}

object CannotGoBack {

  case object AgentServiceAccount extends CannotGoBack {
    val key = "agent-service-account"

    override def toString: String = key
  }

  case object ReenterClientDetails extends CannotGoBack {
    val key = "re-enter-client-details"

    override def toString: String = key
  }

  case object SignUpAnotherClient extends CannotGoBack {
    val key = "sign-up-another-client"

    override def toString: String = key
  }

}

