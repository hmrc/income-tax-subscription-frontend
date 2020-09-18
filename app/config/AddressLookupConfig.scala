/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.i18n.{Lang, MessagesApi}

class AddressLookupConfig @Inject()(messagesApi: MessagesApi) {

  //scalastyle:off
  def config(continueUrl: String)(implicit language: Lang): String = {

    val cy = Lang("CY")

    s"""{
       |  "version": 2,
       |  "options": {
       |    "continueUrl": "$continueUrl",
       |    "showBackButtons": true,
       |    "includeHMRCBranding": true,
       |    "ukMode": true,
       |    "selectPageConfig": {
       |      "proposalListLimit": 50,
       |      "showSearchLinkAgain": true
       |    },
       |    "confirmPageConfig": {
       |      "showChangeLink": true,
       |      "showSubHeadingAndInfo": true,
       |      "showSearchAgainLink": false,
       |      "showConfirmChangeText": true
       |    },
       |    "timeoutConfig": {
       |      "timeoutAmount": 900,
       |      "timeoutUrl": "http://tax.service.gov.uk/income-tax-subscription-frontend/session-timeout"
       |    }
       |},
       |    "labels": {
       |      "en": {
       |        "selectPageLabels": {
       |          "title": "${messagesApi("addressLookup.selectPage.title")}",
       |          "heading": "${messagesApi("addressLookup.selectPage.heading")}"
       |        },
       |        "lookupPageLabels": {
       |          "title": "${messagesApi("addressLookup.lookupPage.title")}",
       |          "heading": "${messagesApi("addressLookup.lookupPage.heading")}"
       |        },
       |        "editPageLabels": {
       |          "title": "${messagesApi("addressLookup.editPage.title")}",
       |          "heading": "${messagesApi("addressLookup.editPage.heading")}",
       |          "postcodeLabel": "${messagesApi("addressLookup.editPage.postcodeLabel")}"
       |        },
       |        "confirmPageLabels": {
       |          "title": "${messagesApi("addressLookup.confirmPage.title")}",
       |          "heading": "${messagesApi("addressLookup.confirmPage.heading")}"
       |        }
       |      },
       |      "cy": {
       |        "selectPageLabels": {
       |          "title": "${messagesApi("addressLookup.selectPage.title")(cy)}",
       |          "heading": "${messagesApi("addressLookup.selectPage.heading")(cy)}"
       |        },
       |        "lookupPageLabels": {
       |          "title": "${messagesApi("addressLookup.lookupPage.title")(cy)}",
       |          "heading": "${messagesApi("addressLookup.lookupPage.heading")(cy)}"
       |        },
       |        "editPageLabels": {
       |          "title": "${messagesApi("addressLookup.editPage.title")(cy)}",
       |          "heading": "${messagesApi("addressLookup.editPage.heading")(cy)}",
       |          "postcodeLabel": "${messagesApi("addressLookup.editPage.postcodeLabel")(cy)}"
       |        },
       |        "confirmPageLabels": {
       |          "title": "${messagesApi("addressLookup.confirmPage.title")(cy)}",
       |          "heading": "${messagesApi("addressLookup.confirmPage.heading")(cy)}"
       |        }
       |      }
       |    }
       |  }""".stripMargin
  }
}
