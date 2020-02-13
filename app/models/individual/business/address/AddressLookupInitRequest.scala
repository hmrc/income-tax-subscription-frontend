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

package models.individual.business.address

import core.connectors.models.ConnectorError
import play.api.libs.json.Json

case class ConfirmPage(title: Option[String] = None,
                       heading: Option[String] = None,
                       showSubHeadingAndInfo: Option[Boolean] = Some(false),
                       infoSubheading: Option[String] = None,
                       infoMessage: Option[String] = None,
                       submitLabel: Option[String] = None,
                       showSearchAgainLink: Option[Boolean] = Some(true),
                       searchAgainLinkText: Option[String] = None,
                       showChangeLink: Option[Boolean] = Some(false),
                       changeLinkText: Option[String] = None)

case class LookupPage(title: Option[String] = None,
                      heading: Option[String] = None,
                      filterLabel: Option[String] = None,
                      postcodeLabel: Option[String] = None,
                      submitLabel: Option[String] = None,
                      resultLimitExceededMessage: Option[String] = None,
                      noResultsFoundMessage: Option[String] = None,
                      manualAddressLinkText: Option[String] = None)

case class SelectPage(title: Option[String] = None,
                      heading: Option[String] = None,
                      proposalListLabel: Option[String] = None,
                      submitLabel: Option[String] = None,
                      proposalListLimit: Option[Int] = None,
                      showSearchAgainLink: Option[Boolean] = Some(true),
                      searchAgainLinkText: Option[String] = None,
                      editAddressLinkText: Option[String] = None)

case class EditPage(title: Option[String] = None,
                    heading: Option[String] = None,
                    line1Label: Option[String] = None,
                    line2Label: Option[String] = None,
                    line3Label: Option[String] = None,
                    townLabel: Option[String] = None,
                    postcodeLabel: Option[String] = None,
                    countryLabel: Option[String] = None,
                    submitLabel: Option[String] = None,
                    showSearchAgainLink: Option[Boolean] = Some(true),
                    searchAgainLinkText: Option[String] = None)

case class AddressLookupInitRequest(continueUrl: String,
                                    lookupPage: Option[LookupPage] = Some(LookupPage()),
                                    selectPage: Option[SelectPage] = Some(SelectPage()),
                                    confirmPage: Option[ConfirmPage] = Some(ConfirmPage()),
                                    editPage: Option[EditPage] = Some(EditPage()),
                                    homeNavHref: Option[String] = None,
                                    navTitle: Option[String] = None,
                                    additionalStylesheetUrl: Option[String] = None,
                                    showPhaseBanner: Option[Boolean] = Some(false), // if phase banner is shown, it will default to "beta" unless ...
                                    alphaPhase: Option[Boolean] = Some(false), // ... you set "alpha" to be true,
                                    phaseFeedbackLink: Option[String] = None,
                                    phaseBannerHtml: Option[String] = None,
                                    showBackButtons: Option[Boolean] = Some(true),
                                    includeHMRCBranding: Option[Boolean] = Some(true),
                                    deskProServiceName: Option[String] = None)


object EditPage {
  implicit val format = Json.format[EditPage]
}

object SelectPage {
  implicit val format = Json.format[SelectPage]
}

object LookupPage {
  implicit val format = Json.format[LookupPage]
}

object ConfirmPage {
  implicit val format = Json.format[ConfirmPage]
}

object AddressLookupInitRequest {
  implicit val format = Json.format[AddressLookupInitRequest]
}


case class AddressLookupInitFailureResponse(status: Int) extends ConnectorError