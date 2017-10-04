package org.kframework.kale

import org.kframework.kale.standard.{ReferenceLabel, ScalaLibraryMixin}

trait Convert[A, B] {
  def convert(a: A): B
}

trait Up[O] {
  def apply(o: O): Term
}

trait Down[O] {
  def unapply(t: Term): Option[O]
}

object UpDown {
  final def apply[O](implicit instance: Up[O]): Up[O] = instance

  final def apply[O](f: O => Term): Up[O] = new Up[O] {
    override def apply(o: O): Term = f(o)
  }

  final implicit def updownFromLeafLabel[O](implicit l: LeafLabel[O]): UpDown[O] = new UpDown[O] {
    override def unapply(t: Term) = l.unapply(t)

    override def apply(o: O) = l.apply(o)
  }
}

trait UpDown[O] extends Up[O] with Down[O]

trait MonoidLabeled[O[_]] {
  def monoidLabel: MonoidLabel
}

trait DefineMixin extends Mixin {
  _: Environment =>

  def define[A: UpDown, B: UpDown, R: UpDown](name: String, f: (A, B) => R): PrimitiveFunction2[A, B, R] =
    PrimitiveFunction2(name, implicitly[UpDown[A]], implicitly[UpDown[B]], implicitly[Up[R]], f)

  def define[A: UpDown, B: UpDown, C: UpDown, R: UpDown](name: String, f: (A, B, C) => R): PrimitiveFunction3[A, B, C, R] =
    PrimitiveFunction3(name, implicitly[UpDown[A]], implicitly[UpDown[B]], implicitly[UpDown[C]], implicitly[Up[R]], f)

  def define[A: UpDown, R: UpDown](name: String, f: A => R): PrimitiveFunction1[A, R] =
    PrimitiveFunction1(name, implicitly[UpDown[A]], implicitly[Up[R]], f)

  def define[T](name: String)(implicit ConvertFromString: Convert[String, T]): ReferenceLabel[T] = new ReferenceLabel[T](name) {
    override protected[this] def internalInterpret(s: String): T = ConvertFromString.convert(s)
  }

  import cats._

  implicit def autoUp[O](o: O)(implicit up: UpDown[O]): Term = up(o)

  implicit def autoDown[O](t: Term)(implicit down: UpDown[O]): O = down.unapply(t).get

  implicit def upMonoidLabeled[O[_] : MonoidLabeled : Traverse : Applicative : MonoidK, E: UpDown]: UpDown[O[E]] = new UpDown[O[E]] {
    val eUpDown = implicitly[UpDown[E]]
    val fTraverse = implicitly[Traverse[O]]
    val label = implicitly[MonoidLabeled[O]].monoidLabel
    val oMonoid = implicitly[MonoidK[O]].algebra[E]
    val oApplicative = implicitly[Applicative[O]]

    override def apply(o: O[E]): Term = {
      fTraverse.foldMap(o)(eUpDown.apply)(label)
    }

    override def unapply(t: Term): Option[O[E]] = t match {
      case label.iterable(seq: Iterable[Term]) =>
        import cats.implicits._
        def ff(t: Term) = oApplicative.pure(eUpDown.unapply(t).get)

        Some(implicitly[Traverse[scala.List]].foldMap(seq.toList)(ff)(oMonoid))
    }
  }
}
