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

package config

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

import scala.io.Source

class MessagesSpec extends AnyFunSuite {

  private val messageKeysEnglish: List[String] = getMessageKeys("messages").toList
  lazy val messageKeySetEnglish = messageKeysEnglish.toSet

  private val messageKeysWelsh: List[String] = getMessageKeys("messages.cy").toList
  lazy val messageKeySetWelsh = messageKeysWelsh.toSet

  test("Messages present in Welsh (conf/messages.cy) should also have an English translation (conf/messages)") {
    val keysInWelshNotEnglish = messageKeySetWelsh -- messageKeySetEnglish
    keysInWelshNotEnglish foreach println
    keysInWelshNotEnglish.size mustBe 0
  }

  test("Messages present in English (conf/messages) should also have a Welsh translation (conf/messages.cy)") {
    val keysInEnglishNotWelsh = messageKeySetEnglish -- messageKeySetWelsh
    keysInEnglishNotWelsh foreach println
    keysInEnglishNotWelsh.size mustBe 0
  }

  test("No duplicate keys in English") {
    val duplicateMessagesEnglish = messageKeysEnglish.diff(messageKeysEnglish.distinct).distinct
    duplicateMessagesEnglish foreach println
    duplicateMessagesEnglish.size mustBe (0)
  }

  test("No duplicate keys in Welsh") {
    val duplicateMessagesWelsh = messageKeysWelsh.diff(messageKeysWelsh.distinct).distinct
    duplicateMessagesWelsh foreach println
    duplicateMessagesWelsh.size mustBe (0)
  }

  private def getMessageKeys(fileName: String) = {
    Source.fromResource(fileName)
      .getLines()
      .map(_.trim)
      .filter(!_.startsWith("#"))
      .filter(_.nonEmpty)
      .map(_.split(' ').head)
  }
}
