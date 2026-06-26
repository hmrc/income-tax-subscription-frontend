/*
 * Copyright 2026 HM Revenue & Customs
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

package connectors

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.after
import play.api.Logging
import uk.gov.hmrc.mdc.Mdc

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.util.Try

trait ConnectorRetries extends Logging {

  protected def actorSystem: ActorSystem

  protected def configuration: Config

  def retryWithIdempotency[A](label: String, initialIdempotencyKey: String)
                             (nextKey: PartialFunction[(A, String), String])
                             (block: String => Future[A])
                             (implicit ec: ExecutionContext): Future[A] = {

    def loop(remainingIntervals: Seq[FiniteDuration], idempotencyKey: String): Future[A] = {
      // Scheduling loses MDC data, so put it back before each scheduled retry.
      block(idempotencyKey).flatMap { result =>
        nextKey.lift((result, idempotencyKey)) match {
          case Some(updatedKey) if remainingIntervals.nonEmpty =>
            val delay = remainingIntervals.head
            logger.warn(s"Retrying [$label] in $delay due to error")
            val mdcData = Mdc.mdcData
            after(delay, actorSystem.scheduler) {
              Mdc.putMdc(mdcData)
              loop(remainingIntervals.tail, updatedKey)
            }
          case _ =>
            Future.successful(result)
        }
      }
    }

    loop(retryIntervals, initialIdempotencyKey)
  }

  private lazy val retryIntervals: Seq[FiniteDuration] =
    Try(configuration.getDurationList("retries.intervals").asScala.toSeq)
      .toOption.filter(_.nonEmpty)
      .getOrElse {
        logger.warn("[ConnectorRetries] - No retry intervals configured under retries.intervals")
        Seq.empty
      }
      .map(d => FiniteDuration(d.toMillis, TimeUnit.MILLISECONDS))
}
