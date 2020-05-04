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

package controllers

import java.net.URLEncoder

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.{Status => HttpStatus}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, RequestHeader}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.partials._
import views.html.feedback_thankyou

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeedbackController @Inject()(http: HttpClient, sessionCookieCrypto: SessionCookieCrypto)(
  implicit appConfig: AppConfig, ec: ExecutionContext, mcc: MessagesControllerComponents) extends FrontendController(mcc) {

  private val TICKET_ID = "ticketId"

  implicit val cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever = new CachedStaticHtmlPartialRetriever {
    override val httpGet: HttpGet = http
  }

  implicit val formPartialRetriever: FormPartialRetriever = new FormPartialRetriever {
    override def httpGet: HttpGet = http

    def crypto: (String) => String = cookie => sessionCookieCrypto.crypto.encrypt(PlainText(cookie)).value
  }

  def contactFormReferer(implicit request: Request[AnyContent]): String = request.headers.get(REFERER).getOrElse("")

  def localSubmitUrl(implicit request: Request[AnyContent]): String = routes.FeedbackController.submit().url

  protected def loadPartial(url: String)(implicit request: RequestHeader): HtmlPartial = ???

  private def feedbackFormPartialUrl(implicit request: Request[AnyContent]) =
    s"${appConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form/?submitUrl=${urlEncode(localSubmitUrl)}" +
      s"&service=${urlEncode(appConfig.contactFormServiceIdentifier)}&referer=${urlEncode(contactFormReferer)}"

  private def feedbackHmrcSubmitPartialUrl(implicit request: Request[AnyContent]) =
    s"${appConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form?resubmitUrl=${urlEncode(localSubmitUrl)}"

  private def feedbackThankYouPartialUrl(ticketId: String)(implicit request: Request[AnyContent]) =
    s"${appConfig.contactFrontendPartialBaseUrl}/contact/beta-feedback/form/confirmation?ticketId=${urlEncode(ticketId)}"

  def show: Action[AnyContent] = Action {
    implicit request =>
      (request.session.get(REFERER), request.headers.get(REFERER)) match {
        case (None, Some(ref)) => Ok(views.html.feedback(feedbackFormPartialUrl, None)).addingToSession(REFERER -> ref)
        case _ => Ok(views.html.feedback(feedbackFormPartialUrl, None))
      }
  }

  def submit: Action[AnyContent] = Action.async {
    implicit request =>
      request.body.asFormUrlEncoded.map { formData =>
        http.POSTForm[HttpResponse](feedbackHmrcSubmitPartialUrl, formData)(
          rds = readPartialsForm, hc = partialsReadyHeaderCarrier,implicitly[ExecutionContext]).map {
          resp =>
            resp.status match {
              case HttpStatus.OK => Redirect(routes.FeedbackController.thankyou()).addingToSession(TICKET_ID -> resp.body)
              case HttpStatus.BAD_REQUEST => BadRequest(views.html.feedback(feedbackFormPartialUrl, Some(Html(resp.body))))
              case status => Logger.error(s"Unexpected status code from feedback form: $status")
                throw new InternalServerException("feedback controller, submit call failed")
            }
        }
      }.getOrElse {
        Logger.error("Trying to submit an empty feedback form")
        Future.failed(new InternalServerException("feedback controller, tried to submit empty feedback form"))
      }
  }

  def thankyou: Action[AnyContent] = Action {
    implicit request =>
      val ticketId = request.session.get(TICKET_ID).getOrElse("N/A")
      val referer = request.session.get(REFERER).getOrElse("/")
      Ok(feedback_thankyou(feedbackThankYouPartialUrl(ticketId), referer)).removingFromSession(REFERER)
  }

  def urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

  private def partialsReadyHeaderCarrier(implicit request: Request[_]): HeaderCarrier = {
    val hc1 = PlaHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    PlaHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc1)
  }

  object PlaHeaderCarrierForPartialsConverter extends HeaderCarrierForPartialsConverter {
    override val crypto: String => String = identity
  }

  implicit val readPartialsForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }
}
