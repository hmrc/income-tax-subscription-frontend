/*
 * Copyright 2018 HM Revenue & Customs
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

package helpers.servicemocks

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json

object EnrolmentStoreStub extends WireMockMethods {
  def stubUpsertEnrolmentResult(enrolmentKey: String, status: Int): StubMapping =
    when(method = PUT, uri = s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey")
      .thenReturn(status = status, body = Json.obj())

  def stubAllocateEnrolmentResult(groupId: String, enrolmentKey: String, status: Int): StubMapping =
    when(method = POST, uri = s"/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments/$enrolmentKey")
      .thenReturn(status = status, body = Json.obj())
}
