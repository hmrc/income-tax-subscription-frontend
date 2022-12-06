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

package utilities

import org.scalatest.matchers.{MatchResult, Matcher}
import play.api.Logger

trait MessagesMatcher {
  val excludedKeys: Set[String] = Set.empty

  private val messagesMatcherLogger: Logger = Logger(getClass)

  def allBeIn(messages: Set[String]): Matcher[Set[String]] = (left: Set[String]) => {
    val diff = left -- messages -- getExcludedKeys
    MatchResult(
      diff.isEmpty,
      s"Missing ${diff.size} translation(s):\n  ${diff.mkString("\n  ")}",
      ""
    )
  }

  def containUniqueKeys: Matcher[Seq[String]] = (left: Seq[String]) => {
    val diff = left.diff(left.distinct).distinct
    MatchResult(
      diff.isEmpty,
      s"${diff.size} duplicate key(s):${diff.mkString("\n  ", "\n  ", "\n")}",
      ""
    )
  }

  def containOnlyPermittedCharacters: Matcher[Seq[String]] = (left: Seq[String]) => {
    val bad = left.filter(s => !s.matches("^[a-z0-9.-]*$"))
    MatchResult(
      bad.isEmpty,
      s"${bad.size} bad key(s):${bad.mkString("\n  ", "\n  ", "\n")}",
      ""
    )
  }

  // Only print the warning once by using a lazy fetch and check
  private lazy val getExcludedKeys = {
    if (excludedKeys.nonEmpty) {
      messagesMatcherLogger.warn(s"\n ---- WARNING: ${excludedKeys.size} translation key(s) excluded from the test ---- \n  ${excludedKeys.mkString("\n  ")}")
    }
    excludedKeys
  }
}
