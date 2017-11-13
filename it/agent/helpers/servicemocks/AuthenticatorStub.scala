///*
// * Copyright 2017 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package agent.helpers.servicemocks
//
//import _root_.agent.connectors.models.matching.{ClientMatchFailureResponseModel, ClientMatchRequestModel, ClientMatchSuccessResponseModel}
//import _root_.agent.helpers.IntegrationTestModels
//import play.api.http.Status
//
//object AuthenticatorStub extends WireMockMethods {
//  def stubMatchFound(returnedNino: String, returnedUtr: Option[String]): Unit = {
//    val model = ClientMatchRequestModel.requestConvert(IntegrationTestModels.testClientDetails)
//
//    val returnMessage = ClientMatchSuccessResponseModel(nino = returnedNino, saUtr = returnedUtr)
//    when(method = POST, uri = "/authenticator/match", body = model)
//      .thenReturn(status = Status.OK, returnMessage)
//  }
//
//  def stubMatchNotFound(): Unit = {
//    val model = ClientMatchRequestModel.requestConvert(IntegrationTestModels.testClientDetails)
//
//    val returnMessage = ClientMatchFailureResponseModel("")
//
//    when(method = POST, uri = "/authenticator/match", body = model)
//      .thenReturn(status = Status.UNAUTHORIZED, returnMessage)
//  }
//}