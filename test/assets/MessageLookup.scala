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

package assets

object MessageLookup {

  val continue = "Continue"

  object HelloWorld {
    val title = "Hello from income-tax-subscription-frontend"
    val heading = "Hello from income-tax-subscription-frontend !"
  }

  object BusinessName {
    val title = "What is your business name?"
    val heading = title
    val hint = "This does not mean trading name"
  }

  object Terms {
    val title = "Terms"
    val heading = title
  }

  object timeout {
    val title = "Session closed due to inactivity"
    val heading = "You've been signed out due to inactivity."
    val returnToHome = "You can start again from the <a href=\"{0}\" rel=\"external\">subscription</a> page."
  }

}
