package forms.validation.utils


object Patterns {

  // ISO 8859-1 standard
  // ASCII range {32 to 126} + {160 to 255} all values inclusive
  val iso8859_1Regex = """^([\x20-\x7E\xA0-\xFF])*$"""

  def validText(text: String): Boolean = text matches iso8859_1Regex

}
