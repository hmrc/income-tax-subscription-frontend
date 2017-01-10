package forms.validation

import play.api.data.validation.{Constraint, ValidationResult}


object ConstraintUtil {

  def constraint[A](f: A => ValidationResult): Constraint[A] = Constraint[A]("")(f)

}

