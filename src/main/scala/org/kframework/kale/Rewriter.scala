package org.kframework.kale

import org.kframework.kale.km.{MultisortedMixing, Z3Mixin}
import org.kframework.kale.standard.StandardEnvironment

import scala.collection.immutable.TreeSet
import scala.collection.mutable

object Rewriter {
  def apply(env: StandardEnvironment) = new {
    def apply(rules: Set[_ <: Term]): Rewriter = new Rewriter(env)(rules)
  }
}

class Rewriter(val env: StandardEnvironment)(val inputRules: Set[_ <: Term]) extends (Term => Stream[Term]) {
  assert(env.isSealed)
  assert(inputRules != null)

  private def lowerForAll(keep: Set[Variable]): Term => Term = {
    case v: Variable =>
      if (keep.contains(v))
        v
      else
        env.ForAll(v, v)

    case t =>
      val newKeep: Set[Variable] = t.variables filter (v => t.children.count(_.contains(v)) > 1)
      val kill = newKeep &~ keep

      val newLowerForAll = lowerForAll(keep | newKeep)
      val solvedChildren = t map0 newLowerForAll

      val withKill: Term = kill.foldLeft(solvedChildren) { (tt, v) => env.ForAll(v, tt) }
      withKill
  }


  val rules = inputRules map lowerForAll(Set())

  println(rules.mkString("\n"))

  val ruleHits = mutable.Map[Term, Int]()

  for (r <- rules)
    ruleHits += (r -> 0)

  var sortedRules = TreeSet[Term]()({ (r1, r2) =>
    if (r1 == r2)
      0
    else {
      val p = ruleHits(r2) - ruleHits(r1)
      if (p != 0)
        p
      else {
        val id = System.identityHashCode(r1) - System.identityHashCode(r2)
        if (id > 0)
          1
        else
          -1
      }
    }
  })

  sortedRules ++= rules

  // TODO: clean this
  val z3 = env match {
    case e: Environment with MultisortedMixing with Z3Mixin => new z3(e, Seq(Seq()))
    case _ => null
  }


  import env._

  def apply(t: Term): Stream[Term] = step(t)

  def step(obj: Term): Stream[Term] = {
    var tries = 0
    val res = (sortedRules.toStream map { r =>
      val m = unify(r, obj)
      tries += 1
      m match {
        case Or.set(ands) =>
          val afterSubstitution = env match {
            case env: StandardEnvironment =>
              import env._
              ands.toStream.collect({
                case And.withNext(_: Substitution, Some(Next(next))) => next
              }).headOption.getOrElse(Bottom)
          }
          //          if (afterSubstitution != Bottom) {
          //            println("   " + r)
          //            println("       " + oneGoodSub)
          //          }
          if (afterSubstitution != Bottom) {
            val prev = ruleHits(r)
            sortedRules -= r
            ruleHits.update(r, prev + 1)
            sortedRules += r
          }
          afterSubstitution
        case Bottom => Bottom
      }
    }).filterNot(_ == Bottom)
    res
  }

  def searchStep(obj: Term): Term = {
    val unificationRes: Set[Term] = rules.map(r => unify(r, obj))
    val finalRes = Or(unificationRes.flatMap({
      case Bottom => Set[Term]()
      case or =>
        val res = Or.asSet(or).flatMap(u => {
          val And.withNext(constraints@And.substitutionAndTerms(_, unresolvedConstraints), Some(Next(next))) = u

          if (unresolvedConstraints.nonEmpty && env.isInstanceOf[Z3Mixin] && !z3.sat(constraints)) {
            Set[Term]()
          } else {
            Set(And(next, constraints))
          }
        })
        res
    })
    )
    finalRes
  }
}

