package org.kframework.kale

import scala.Iterable
import scala.collection._
import scala.language.implicitConversions
import Util._

object UniqueId {
  var nextId = 0

  def apply(): Int = {
    nextId += 1
    nextId - 1
  }
}

trait UniqueId {
  val id = UniqueId()
}

trait Label extends MemoizedHashCode {
  val name: String
  val id: Int

  override def equals(other: Any) = other match {
    case that: Label => this.name == that.name
    case _ => false
  }

  override def computeHashCode = name.hashCode

  override def toString = name
}

trait NodeLabel extends Label {
  def unapplySeq(t: Term): Option[Seq[Term]] = t match {
    case t: Node if t.label == this => Some(t.iterator.toSeq)
    case _ => None
  }
}

trait LeafLabel[T] extends Label {
  def apply(t: T): Term

  def unapply(t: Term): Option[T] = t match {
    case t: Leaf[T] if t.label == this => Some(t.value)
    case _ => None
  }
}

sealed trait Term extends Iterable[Term] {
  def updateAt(i: Int)(t: Term): Term

  val label: Label

  private var att: Any = null

  def setHiddenAttDONOTUSE(att: Any) = this.att = att

  def getHiddenAttDONOTUSE = this.att

  def iterator(): Iterator[Term]

  override def hashCode = label.hashCode
}

trait Node extends Term with Product {
  val label: NodeLabel

  def updateAt(i: Int)(t: Term): Term = if (i <= 0 || i > productArity) {
    throw new IndexOutOfBoundsException(label + " has " + productArity + " children. Trying to update index _" + i)
  } else {
    innerUpdateAt(i, t)
  }

  protected def innerUpdateAt(i: Int, t: Term): Term

  def iterator: Iterator[Term]

  override def toString = label + "(" + iterator.mkString(", ") + ")"
}

trait Leaf[T] extends Term {
  def iterator = Iterator.empty

  def updateAt(i: Int)(t: Term): Term = throw new IndexOutOfBoundsException("Leaves have no children. Trying to update index _" + i)

  val label: LeafLabel[T]
  val value: T

  override def toString = label + "(" + value.toString + ")"
}

trait NameFromObject {
  val name = this.getClass.getName.drop(5).dropRight(1)
}

trait ConstantLabel[T] extends LeafLabel[T] with NameFromObject with UniqueId {
  def apply(v: T) = Constant(this, v)
}

case class Constant[T](label: ConstantLabel[T], value: T) extends Leaf[T] {
  override def toString = value.toString
}

trait Label0 extends Function0[Term] with NodeLabel {
  def apply(): Term
}

trait FreeLabel

object FreeLabel0 {
  def apply(name: String): FreeLabel0 = FreeLabel0(UniqueId(), name)
}

case class FreeLabel0(id: Int, name: String) extends Label0 with FreeLabel {
  def apply(): Term = FreeNode0(this)
}

trait Label1 extends (Term => Term) with NodeLabel {
  def apply(_1: Term): Term
}

object FreeLabel1 {
  def apply(name: String): FreeLabel1 = FreeLabel1(UniqueId(), name)
}

case class FreeLabel1(id: Int, name: String) extends Label1 with FreeLabel {
  def apply(_1: Term): Term = FreeNode1(this, _1)
}

trait Label2 extends ((Term, Term) => Term) with NodeLabel {
  def apply(_1: Term, _2: Term): Term
}

object FreeLabel2 {
  def apply(name: String): FreeLabel2 = FreeLabel2(UniqueId(), name)
}

case class FreeLabel2(id: Int, name: String) extends Label2 with FreeLabel {
  def apply(_1: Term, _2: Term): Term = FreeNode2(this, _1, _2)
}

trait Label3 extends NodeLabel {
  def apply(_1: Term, _2: Term, _3: Term): Term
}

object FreeLabel3 {
  def apply(name: String): FreeLabel3 = FreeLabel3(UniqueId(), name)
}

case class FreeLabel3(id: Int, name: String) extends Label3 with FreeLabel {
  def apply(_1: Term, _2: Term, _3: Term): Term = FreeNode3(this, _1, _2, _3)
}

trait Label4 extends NodeLabel {
  def apply(_1: Term, _2: Term, _3: Term, _4: Term): Term
}

object FreeLabel4 {
  def apply(name: String): FreeLabel4 = FreeLabel4(UniqueId(), name)
}

case class FreeLabel4(id: Int, name: String) extends Label4 with FreeLabel {
  def apply(_1: Term, _2: Term, _3: Term, _4: Term): Term = FreeNode4(this, _1, _2, _3, _4)
}

trait Node0 extends Node {
  val label: Label0

  def innerUpdateAt(i: Int, t: Term): Term = throw new AssertionError("unreachable code")

  def iterator = Iterator.empty
}

case class FreeNode0(label: Label0) extends Node0

trait Node1 extends Node with Product1[Term] {
  val label: Label1

  def innerUpdateAt(i: Int, t: Term): Term = i match {
    case 1 => label(t)
  }

  def iterator = Iterator(_1)
}

case class FreeNode1(label: Label1, _1: Term) extends Node1

trait Node2 extends Node with Product2[Term, Term] {
  val label: Label2

  def innerUpdateAt(i: Int, t: Term): Term = i match {
    case 1 => label(t, _2)
    case 2 => label(_1, t)
  }

  def iterator = Iterator(_1, _2)
}

case class FreeNode2(label: Label2, _1: Term, _2: Term) extends Node2

trait Node3 extends Node with Product3[Term, Term, Term] {
  val label: Label3

  def innerUpdateAt(i: Int, t: Term): Term = i match {
    case 1 => label(t, _2, _3)
    case 2 => label(_1, t, _3)
    case 3 => label(_1, _2, t)
  }

  def iterator = Iterator(_1, _2, _3)
}

case class FreeNode3(label: Label3, _1: Term, _2: Term, _3: Term) extends Node3

trait Node4 extends Node with Product4[Term, Term, Term, Term] {
  val label: Label4

  def innerUpdateAt(i: Int, t: Term): Term = i match {
    case 1 => label(t, _2, _3, _4)
    case 2 => label(_1, t, _3, _4)
    case 3 => label(_1, _2, t, _4)
    case 4 => label(_1, _2, _3, t)
  }

  def iterator = Iterator(_1, _2, _3, _4)
}

case class FreeNode4(label: Label4, _1: Term, _2: Term, _3: Term, _4: Term) extends Node4

object Variable extends LeafLabel[String] with NameFromObject with UniqueId {
  override def apply(name: String): Variable = SimpleVariable(name)
}

trait Variable extends Leaf[String] {
  val label = Variable
  val name: String
  lazy val value = name
}

case class SimpleVariable(name: String) extends Variable

trait Hooked {
  def f(t: Term): Term
}

object Truth extends LeafLabel[Boolean] with NameFromObject with UniqueId {
  def apply(v: Boolean) = if (v) Top else Bottom
}

class Truth(val value: Boolean) extends Leaf[Boolean] {
  val label = Truth
}

object Top extends Truth(true) with Substitution {
  override def get(v: Variable): Option[Term] = None

  override def toString = "⊤"
}

object Bottom extends Truth(false) {
  override def toString = "⊥"
}

object Equality extends Label2 with NameFromObject with UniqueId {
  override def apply(_1: Term, _2: Term): Term = bottomize(_1, _2) {
    _1.label match {
      case `Variable` => new Binding(_1.asInstanceOf[Variable], _2)
      case _ => new Equality(_1, _2)
    }
  }

  def unapply(t: Term): Option[(Term, Term)] = t match {
    case e: Equality => Some((e._1, e._2))
    case _ => None
  }
}

trait Substitution extends Term {
  def get(v: Variable): Option[Term]
}

private[kale] class Equality(val _1: Term, val _2: Term) extends Node2 {
  val label = Equality

  override def equals(other: Any) = other match {
    case that: Equality => this._1 == that._1 && this._2 == that._2
    case _ => false
  }
}

private[kale] class Binding(val variable: Variable, val term: Term) extends Equality(variable, term) with Substitution {
  assert(_1.isInstanceOf[Variable])

  def get(v: Variable) = if (_1 == v) Some(_2) else None
}

trait And extends Assoc

object And extends AssocLabel with NameFromObject with UniqueId {
  override def apply(_1: Term, _2: Term): Term = {
    if (_1 == Bottom || _2 == Bottom)
      Bottom
    else {
      val l1: (Substitution, Iterable[Term]) = unwrap(_1)
      val l2: (Substitution, Iterable[Term]) = unwrap(_2)
      Substitution(l1._1, l2._1) match {
        case `Bottom` => Bottom
        case s: Substitution => apply(s, l1._2 ++ l2._2)
        case _ => unreachable()
      }
    }
  }

  private def unwrap(t: Term): (Substitution, Iterable[Term]) = t match {
    case s: Substitution => (s, Iterable.empty)
    case and: AndOfSubstitutionAndTerms => (and.s, and.terms)
    case o => (Top, Iterable(o))
  }

  def apply(terms: Iterable[Term]): Term = {
    val bindings: Map[Variable, Term] = terms.collect({ case Equality(v: Variable, t) => v -> t }).toMap
    val pureSubstitution = Substitution(bindings)
    val others: Iterable[Term] = terms.filter({ case Equality(v: Variable, t) => false; case _ => true })
    apply(pureSubstitution, others)
  }

  def apply(pureSubstitution: Substitution, others: Iterable[Term]): Term = {
    if (others.isEmpty) {
      pureSubstitution
    } else if (pureSubstitution.isEmpty && others.size == 1) {
      others.head
    } else {
      new AndOfSubstitutionAndTerms(pureSubstitution, others.toSet)
    }
  }
}

private[kale] final class AndOfSubstitutionAndTerms(val s: Substitution, val terms: Set[Term]) extends Assoc {
  assert(!terms.contains(Bottom))
  val label = And

  lazy val _1: Term = terms.head
  lazy val _2: Term = And(s, terms.tail)
  override val list: Iterable[Term] = Substitution.asList(s) ++ terms
}

object Substitution extends AssocLabel with NameFromObject with UniqueId {
  override def apply(_1: Term, _2: Term): Term = {
    if (_1 == Bottom || _2 == Bottom)
      Bottom
    else {
      val m1 = unwrap(_1)
      val m2 = unwrap(_2)
      if ((m1.keys.toSet & m2.keys.toSet).forall(v => m1(v) == m2(v))) {
        apply(m1 ++ m2)
      } else {
        Bottom
      }
    }
  }

  private def unwrap(t: Term) = t match {
    case Top => Map[Variable, Term]()
    case Equality(_1: Variable, _2) => Map[Variable, Term](_1.asInstanceOf[Variable] -> _2)
    case Substitution(m) => m
  }

  def apply(l: Iterable[Term]) = l.foldLeft(Top: Term)(apply)

  def apply(m: Map[Variable, Term]): Substitution = m.size match {
    case 0 => Top
    case 1 => new Binding(m.head._1, m.head._2)
    case _ => new SubstitutionWithMultipleBindings(m)
  }

  def unapply(t: Substitution): Option[Map[Variable, Term]] = t match {
    case `Top` => Some(Map[Variable, Term]())
    case b: Binding => Some(Map(b.variable -> b.term))
    case s: SubstitutionWithMultipleBindings => Some(s.m)
  }
}

final class SubstitutionWithMultipleBindings(val m: Map[Variable, Term]) extends And with Substitution {
  assert(m.size >= 2)
  val label = Substitution
  lazy val _1 = Equality(m.head._1, m.head._2)
  lazy val _2 = Substitution(m.tail)

  override def equals(other: Any): Boolean = other match {
    case that: SubstitutionWithMultipleBindings => m == that.m
    case _ => false
  }

  override def hashCode(): Int = label.hashCode

  def get(v: Variable) = m.get(v)

  override val list: Iterable[Term] = Substitution.asList(this)
}

object Or extends AssocLabel with NameFromObject with UniqueId {
  def apply(_1: Term, _2: Term): Term =
    unwrap(_1) | unwrap(_2) match {
      case s if s.isEmpty => Bottom
      case s if s.size == 1 => s.head
      case s => new Or(s)
    }

  def unwrap(t: Term): Set[Term] = t match {
    case o: Or => o.terms
    case `Bottom` => Set()
    case o => Set(o)
  }

  def apply(l: Iterable[Term]): Term = l.foldLeft(Bottom: Term)(apply)

  def unapply(t: Term): Some[Set[Term]] = Some(unwrap(t))
}

class Or(val terms: Set[Term]) extends Assoc {
  assert(terms.size > 1)
  val label = Or

  lazy val _1 = terms.head
  lazy val _2 = Or(terms.tail.toSeq)
  override val list: Iterable[Term] = terms

  override def equals(other: Any): Boolean = other match {
    case that: Or => this.terms == that.terms
    case _ => false
  }
}

trait AssocLabel extends Label2 {
  def apply(l: Iterable[Term]): Term

  def apply(terms: Term*): Term = apply(terms)

  val thisthis = this

  def asList(t: Term) = t.label match {
    case `thisthis` => t.asInstanceOf[Assoc].list
    case _ => List(t)
  }

  val listUnapplier = new {
    def unapplySeq(t: Term): Iterable[Term] = asList(t)
  }
}

trait HasId {
  val identity: Term
}

trait AssocWithIdLabel extends AssocLabel with HasId {
  def apply(_1: Term, _2: Term) = {
    val l1 = unwrap(_1)
    val l2 = unwrap(_2)
    apply(l1 ++ l2)
  }

  def unwrap(t: Term) = t match {
    case `identity` => List[Term]()
    case x if x.label == this => x.asInstanceOf[Assoc].list
    case y => List(y)
  }

  def apply(l: Iterable[Term]): Term = l match {
    case l if l.isEmpty => identity
    case l if l.size == 1 => l.head
    case _ => construct(l)
  }

  def construct(l: Iterable[Term]): Term
}

trait AssocWithoutIdLabel extends AssocLabel {

}

trait Assoc extends Node2 {
  override val label: AssocLabel
  val list: Iterable[Term]
}

case class AssocWithIdListLabel(name: String, identity: Term) extends AssocWithIdLabel with UniqueId {
  override def construct(l: Iterable[Term]): Term = new AssocWithIdList(this, l)
}

case class AssocWithIdList(label: AssocLabel, list: Iterable[Term]) extends Assoc {
  override def _1: Term = list.head

  override def _2: Term = label(list.tail)
}

object Rewrite extends Label2 with NameFromObject with UniqueId

case class Rewrite(_1: Term, _2: Term) extends Node2 {
  override val label = Rewrite
}

trait UnifierFunction[Left <: Term, Right <: Term, Result <: Term] extends (DispatchState => ((Term, Term) => Term)) {
  def apply(solver: DispatchState) = { (a: Term, b: Term) => f(solver)(a.asInstanceOf[Left], b.asInstanceOf[Right]) }

  def f(solver: DispatchState)(a: Left, b: Right): Result
}

case class UnifierPiece(leftLabel: Label, rightLabel: Label, f: DispatchState => (Term, Term) => Term)

trait DispatchState {
  def apply(left: Term, right: Term): Term
}

class Dispatch(pieces: Set[UnifierPiece], maxId: Int) extends DispatchState {
  val arr: Array[Array[(Term, Term) => (Term)]] =
    (0 until maxId + 1).map({ i =>
      new Array[(Term, Term) => (Term)](maxId)
    }).toArray

  for (p <- pieces) {
    arr(p.leftLabel.id)(p.rightLabel.id) = p.f(this)
  }

  def apply(left: Term, right: Term): Term = {
    val u = arr(left.label.id)(right.label.id)
    val res = if (u != null)
      u(left, right)
    else
      Bottom

    // println(left + "\n:= " + right + "\n=== " + res)
    res
  }
}

trait UnaryState {
  def apply(t: Term): Term
}

trait UnaryFunction[Element <: Term, Result <: Term, US <: UnaryState] extends (US => Term => Term) {
  def apply(unaryState: US) = { t: Term => f(unaryState)(t.asInstanceOf[Element]) }

  def f(state: US)(t: Element): Result
}

case class UnaryPiece[US <: UnaryState](label: Label, f: US => Term => Term)

abstract class Application[US <: UnaryState](pieces: Set[UnaryPiece[US]], maxId: Int) extends UnaryState {
  this: US =>
  val arr = new Array[Term => Term](maxId)

  for (p <- pieces) {
    arr(p.label.id) = p.f(this)
  }

  def apply(t: Term): Term
}

object SubstitutionApplication {
  def apply(pieces: Set[UnaryPiece[SubstitutionApplication]], maxId: Int)(s: Substitution) = new SubstitutionApplication(pieces, maxId)(s)
}

class SubstitutionApplication(pieces: Set[UnaryPiece[SubstitutionApplication]], maxId: Int)(s: Substitution) extends Application[SubstitutionApplication](pieces, maxId) {
  def get(v: Variable): Option[Term] = s.get(v)

  def apply(t: Term) = arr(t.label.id) match {
    case null => Bottom
    case f => f(t)
  }
}

object ApplySubstitution {

  def apply(labels: Set[Label]): Substitution => SubstitutionApplication = {
    val maxId = labels.map(_.id).max + 1
    val setOfUnaryPieces = labels.map({
      case `Variable` => UnaryPiece(Variable, Var)
      case l: Label0 => UnaryPiece(l, Node0)
      case l: Label1 => UnaryPiece(l, Node1)
      case l: Label2 => UnaryPiece(l, Node2)
      case l: Label3 => UnaryPiece(l, Node3)
      case l: Label4 => UnaryPiece(l, Node4)
      case l: ConstantLabel[_] => UnaryPiece(l, Constant)
    })

    SubstitutionApplication(setOfUnaryPieces, maxId)
  }

  object Node0 extends UnaryFunction[Node0, Node0, SubstitutionApplication] {
    def f(solver: SubstitutionApplication)(t: Node0) = t
  }

  object Node1 extends UnaryFunction[Node1, Term, SubstitutionApplication] {
    def f(solver: SubstitutionApplication)(t: Node1) = t.label(solver(t._1))
  }

  object Node2 extends UnaryFunction[Node2, Term, SubstitutionApplication] {
    def f(solver: SubstitutionApplication)(t: Node2) = t.label(solver(t._1), solver(t._2))
  }

  object Node3 extends UnaryFunction[Node3, Term, SubstitutionApplication] {
    def f(solver: SubstitutionApplication)(t: Node3) = t.label(solver(t._1), solver(t._2), solver(t._3))
  }

  object Node4 extends UnaryFunction[Node4, Term, SubstitutionApplication] {
    def f(solver: SubstitutionApplication)(t: Node4) = t.label(solver(t._1), solver(t._2), solver(t._3), solver(t._4))
  }

  object Var extends UnaryFunction[Variable, Term, SubstitutionApplication] {
    def f(solver: SubstitutionApplication)(v: Variable) = solver.get(v).getOrElse(v)
  }

  object Constant extends UnaryFunction[Constant[_], Constant[_], SubstitutionApplication] {
    def f(solver: SubstitutionApplication)(a: Constant[_]) = a
  }

}

object SimpleMatcher {

  def apply(labels: Set[Label]): Dispatch = {
    val variableXlabel = labels.map(UnifierPiece(Variable, _, SimpleMatcher.VarLeft))
    val freeLikeLabelXfreeLikeLabel = labels.collect({
      case l: FreeLabel0 => UnifierPiece(l, l, SimpleMatcher.FreeNode0FreeNode0)
      case l: FreeLabel1 => UnifierPiece(l, l, SimpleMatcher.FreeNode1FreeNode1)
      case l: FreeLabel2 => UnifierPiece(l, l, SimpleMatcher.FreeNode2FreeNode2)
      case l: FreeLabel3 => UnifierPiece(l, l, SimpleMatcher.FreeNode3FreeNode3)
      case l: FreeLabel4 => UnifierPiece(l, l, SimpleMatcher.FreeNode4FreeNode4)
      case l: ConstantLabel[_] => UnifierPiece(l, l, SimpleMatcher.Constants)
    })

    val assoc = labels.flatMap({
      case l: AssocLabel =>
        labels.collect({ case ll if !ll.isInstanceOf[Variable] => UnifierPiece(l, ll, SimpleMatcher.AssocTerm) })
      case _ => Set[UnifierPiece]()
    }).toSet

    val anywhereContextMatchers = labels.map(UnifierPiece(AnywhereContext, _, AnywhereContextMatcher))

    new Dispatch(variableXlabel | freeLikeLabelXfreeLikeLabel | assoc | anywhereContextMatchers, labels.map(_.id).max + 1)
  }

  object FreeNode0FreeNode0 extends UnifierFunction[Node0, Node0, Top.type] {
    def f(solver: DispatchState)(a: Node0, b: Node0) = Top
  }

  object FreeNode1FreeNode1 extends UnifierFunction[Node1, Node1, Term] {
    def f(solver: DispatchState)(a: Node1, b: Node1) = solver(a._1, b._1)
  }

  object FreeNode2FreeNode2 extends UnifierFunction[Node2, Node2, Term] {
    def f(solver: DispatchState)(a: Node2, b: Node2) = And(solver(a._1, b._1), solver(a._2, b._2))
  }

  object FreeNode3FreeNode3 extends UnifierFunction[Node3, Node3, Term] {
    def f(solver: DispatchState)(a: Node3, b: Node3) = And(List(solver(a._1, b._1), solver(a._2, b._2), solver(a._3, b._3)))
  }

  object FreeNode4FreeNode4 extends UnifierFunction[Node4, Node4, Term] {
    def f(solver: DispatchState)(a: Node4, b: Node4) = And(List(solver(a._1, b._1), solver(a._2, b._2), solver(a._3, b._3), solver(a._4, b._4)))
  }

  def matchContents(l: AssocLabel, ksLeft: Iterable[Term], ksRight: Iterable[Term])(implicit solver: DispatchState): Term = {
    val res = (ksLeft, ksRight) match {
      case (Seq(), Seq()) => Top
      case ((v: Variable) :: tailL, ksR) =>
        (0 to ksR.size)
          .map { index => (ksR.take(index), ksR.drop(index)) }
          .map { case (prefix, suffix) => And(Equality(v, l(prefix)), matchContents(l, tailL, suffix)) }
          .fold(Bottom)({ (a, b) => Or(a, b) })
      case (left, right) if left.nonEmpty && right.nonEmpty => And(solver(left.head, right.head), matchContents(l, left.tail, right.tail): Term)
      case other => Bottom
    }
    res
  }


  object AssocTerm extends UnifierFunction[Assoc, Term, Term] {
    def f(solver: DispatchState)(a: Assoc, b: Term) = {
      val asList = a.label.asList _
      val l1 = asList(a)
      val l2 = asList(b)
      matchContents(a.label, l1, l2)(solver)
    }
  }

  object TermAssoc extends UnifierFunction[Term, Assoc, Term] {
    def f(solver: DispatchState)(a: Term, b: Assoc) = {
      val asList = b.label.asList _
      val l1 = asList(a)
      val l2 = asList(b)
      matchContents(b.label, l1, l2)(solver)
    }
  }

  object VarLeft extends UnifierFunction[Variable, Term, Term] {
    def f(solver: DispatchState)(a: Variable, b: Term) = Equality(a.asInstanceOf[Variable], b)
  }

  object Constants extends UnifierFunction[Constant[_], Constant[_], Term] {
    override def f(solver: DispatchState)(a: Constant[_], b: Constant[_]) =
      Truth(a.value == b.value)
  }

}

object Rewriter {
  def apply(substitutioner: Substitution => SubstitutionApplication, matcher: Dispatch)(rules: Set[Rewrite]) =
    new Rewriter(substitutioner, matcher, rules)
}

class Rewriter(substitutioner: Substitution => SubstitutionApplication, matcher: Dispatch, rules: Set[Rewrite]) {
  def executionStep(obj: Term): Term = {
    rules.toStream.map(r => (matcher(r._1, obj), r._2)).find(_._1 != Bottom) match {
      case Some((substitutions, rhs)) =>
        val oneSubstitutuion = Or.unwrap(substitutions).head.asInstanceOf[Substitution]
        substitutioner(oneSubstitutuion).apply(rhs)
      case None => Bottom
    }
  }

  def searchStep(obj: Term): Term = {
    Or(rules.map(r => (matcher(r._1, obj), r._2)).flatMap({
      case (Bottom, _) => Set[Term]()
      case (or, rhs) =>
        val substitutions: Set[Substitution] = Or.unwrap(or).asInstanceOf[Set[Substitution]]
        substitutions.map(substitutioner).map(_ (rhs))
    }))
  }
}
