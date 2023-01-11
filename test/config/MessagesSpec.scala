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

package config

import org.scalatestplus.play.PlaySpec
import utilities.MessagesMatcher

import scala.io.Source

class MessagesSpec extends PlaySpec with MessagesMatcher  {

  private val messageLinesEnglish: List[String] = getMessageLines("messages").toList
  private val messageKeysEnglish: List[String] = messageLinesEnglish map toKey
  private lazy val messageKeySetEnglish = messageKeysEnglish.toSet

  private val messageLinesWelsh: List[String] = getMessageLines("messages.cy").toList
  private val messageKeysWelsh: List[String] = messageLinesWelsh map toKey
  private lazy val messageKeySetWelsh = messageKeysWelsh.toSet

  "Messages present in Welsh (conf/messages.cy)" should {
    "also have an English translation (conf/messages)" in {
      messageKeySetWelsh must allBeIn(messageKeySetEnglish)
    }

    "not contain duplicate keys" in {
      messageKeysWelsh must containUniqueKeys
    }

    "contain only permitted characters" in {
      messageKeysWelsh must containOnlyPermittedCharacters
    }

    "contain a govuk-link class when a link is present" in {
      messageLinesWelsh must includeCorrectClassOnLinks
    }
  }

  "Messages present in English (conf/messages)" should {
    "also have a Welsh translation (conf/messages.cy)" in {
      messageKeySetEnglish must allBeIn(messageKeySetWelsh)
    }

    "not contain duplicate keys" in {
      messageKeysEnglish must containUniqueKeys
    }

    "contain only permitted characters" in {
      messageKeysEnglish must containOnlyPermittedCharacters
    }

    "contain a govuk-link class when a link is present" in {
      messageLinesEnglish must includeCorrectClassOnLinks
    }
  }

  private def getMessageLines(fileName: String) = {
    Source.fromResource(fileName)
      .getLines()
      .map(_.trim)
      .filterNot(_.startsWith("#"))
      .filter(_.nonEmpty)
  }

  private def toKey(line: String) = {
    line.split(' ').head
  }

}
