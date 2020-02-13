package helpers.agent

import play.api.libs.json.{JsValue, Writes}

object ImplicitConversions {
  implicit def toJsValue[T](data: T)(implicit writer: Writes[T]): JsValue = writer.writes(data)
}
