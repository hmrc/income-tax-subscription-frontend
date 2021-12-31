/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.Logging
import play.api.http.{Status => HttpStatus}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials._
import views.html.{Feedback, FeedbackThankyou}

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeedbackController @Inject()(val feedback: Feedback, val feedbackThankYou: FeedbackThankyou, http: HttpClient)
                                  (implicit formPartialRetriever: FormPartialRetriever,
                                   cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever,
                                   appConfig: AppConfig,
                                   ec: ExecutionContext,
                                   mcc: MessagesControllerComponents,
                                   headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter) extends FrontendController(mcc) with Logging {

  private val TICKET_ID = "ticketId"

  def contactFormReferer(implicit request: Request[AnyContent]): String = request.headers.get(REFERER).getOrElse("")

  def localSubmitUrl(implicit request: Request[AnyContent]): String = routes.FeedbackController.submit.url

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
        case (None, Some(ref)) => Ok(feedback(feedbackFormPartialUrl, None)).addingToSession(REFERER -> ref)
        case _ => Ok(feedback(feedbackFormPartialUrl, None))
      }
  }

  def submit: Action[AnyContent] = Action.async {
    implicit request =>
      val partialHeaderCarrier = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)
      request.body.asFormUrlEncoded.map { formData =>
        http.POSTForm[HttpResponse](feedbackHmrcSubmitPartialUrl, formData)(
          rds = readPartialsForm, hc = partialHeaderCarrier, implicitly[ExecutionContext]).map {
          resp =>
            resp.status match {
              case HttpStatus.OK => Redirect(routes.FeedbackController.thankyou).addingToSession(TICKET_ID -> resp.body)
              case HttpStatus.BAD_REQUEST => BadRequest(feedback(feedbackFormPartialUrl, Some(Html(resp.body))))
              case status => logger.error(s"Unexpected status code from feedback form: $status")
                throw new InternalServerException("feedback controller, submit call failed")
            }
        }
      }.getOrElse {
        logger.error("Trying to submit an empty feedback form")
        Future.failed(new InternalServerException("feedback controller, tried to submit empty feedback form"))
      }
  }

  def thankyou: Action[AnyContent] = Action {
    implicit request =>
      val ticketId = request.session.get(TICKET_ID).getOrElse("N/A")
      val referer = request.session.get(REFERER).getOrElse("/")
      Ok(feedbackThankYou(feedbackThankYouPartialUrl(ticketId), referer)).removingFromSession(REFERER)
  }

  def urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

  implicit val readPartialsForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }
}
