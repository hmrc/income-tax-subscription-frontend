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

package views.html.helpers


case class LabelParams(
                        label: Option[Boolean] = None,
                        labelAfter: Option[Boolean] = None,
                        labelTextClass: Option[String] = None,
                        labelHighlight: Option[Boolean] = None,
                        labelClass: Option[String] = None,
                        labelDataAttributes: Option[String] = None
                      )

object LabelParams {

  implicit class quickAccess(params: Option[LabelParams]) {
    def labelAfter: Option[Boolean] = params.flatMap(x => x.labelAfter)

    def labelHighlight: Option[Boolean] = params.flatMap(x => x.labelHighlight)

    def labelClass: Option[String] = params.flatMap(x => x.labelClass)

    def labelDataAttributes: Option[String] = params.flatMap(x => x.labelDataAttributes)

    def labelTextClass: Option[String] = params.flatMap(x => x.labelTextClass)

    def label: Option[Boolean] = params.flatMap(x => x.label)
  }

}
