package org.kframework.kale.util

import org.kframework.kale._
import org.kframework.kale.standard.StandardEnvironment

import scala.language.implicitConversions

trait DSLMixin {
  protected val env: StandardEnvironment

  import env._

  def __ : Variable = Variable.freshVariable()

  implicit class RichVariable(v: Variable) {
    def ::(s: Sort) = Variable(v.name.str, s)

    def |=(t: Term) = BindMatch(v, t)

    def |!=(t: Term) = And(v, STRATEGY.doesNotMatch(t, v))
  }

  def ?(t: Term): Term = STRATEGY.orElseLeave(t)

  implicit class RichStandardTerm(t: Term) {
    def :=(tt: Term): Term = env.And.filterOutNext(env.unify(t, tt))

    def :==(tt: Term): Term = env.unify(t, tt)

    def ==>(tt: Term): Term = Rewrite(t, tt)

    def ?=>(tt: Term): Term = STRATEGY.orElseLeave(Rewrite(t, tt))

    def =:=(tt: Term): Term = env.And.onlyNext(env.unify(t, tt))

    def :::(tt: Term): Term = STRATEGY.compose(t, tt)

    def %(redex: Term) = Context(__, redex, t)

    def |(tt: Term) = Or(t, tt)

    def &(tt: Term) = And(t, tt)
  }

  implicit def symbolWithApp(s: Symbol)(env: Environment) = new {
    val label: Label = env.label(s.name)

    def apply[T](value: T): Term = label.asInstanceOf[LeafLabel[T]](value)

    def apply(_1: Term): Term = label.asInstanceOf[Label1](_1)

    def apply(_1: Term, _2: Term): Term = label.asInstanceOf[Label2](_1, _2)
  }
}

trait VariablesMixin {
  val env: StandardEnvironment

  import env._

  val Condition = Variable("Condition")
  val Then = Variable("Then")
  val Else = Variable("Else")
  val A = Variable("A")
  val B = Variable("B")
  val X = Variable("X")
  val Y = Variable("Y")
  val P = Variable("P")
  val S = Variable("S")
}

class dsl(implicit val env: StandardEnvironment) extends DSLMixin with VariablesMixin
