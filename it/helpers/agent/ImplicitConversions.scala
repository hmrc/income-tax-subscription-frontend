
package helpers.agent

import play.api.libs.json.{JsValue, Writes}
import scala.language.implicitConversions

object ImplicitConversions {
  implicit def toJsValue[T](data: T)(implicit writer: Writes[T]): JsValue = writer.writes(data)
}
