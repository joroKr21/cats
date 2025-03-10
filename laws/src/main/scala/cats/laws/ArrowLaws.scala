/*
 * Copyright (c) 2015 Typelevel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package cats
package laws

import cats.arrow.Arrow
import cats.instances.function.*
import cats.syntax.arrow.*
import cats.syntax.compose.*
import cats.syntax.strong.*

/**
 * Laws that must be obeyed by any `cats.arrow.Arrow`.
 */
trait ArrowLaws[F[_, _]] extends CategoryLaws[F] with StrongLaws[F] {
  implicit override def F: Arrow[F]

  def arrowIdentity[A]: IsEq[F[A, A]] =
    F.lift(identity[A]) <-> F.id[A]

  def arrowComposition[A, B, C](f: A => B, g: B => C): IsEq[F[A, C]] =
    F.lift(f.andThen(g)) <-> (F.lift(f).andThen(F.lift(g)))

  def arrowExtension[A, B, C](g: A => B): IsEq[F[(A, C), (B, C)]] =
    F.lift(g).first[C] <-> F.lift(g.split(identity[C]))

  def arrowFunctor[A, B, C, D](f: F[A, B], g: F[B, C]): IsEq[F[(A, D), (C, D)]] =
    f.andThen(g).first[D] <-> (f.first[D].andThen(g.first[D]))

  def arrowExchange[A, B, C, D](f: F[A, B], g: C => D): IsEq[F[(A, C), (B, D)]] =
    (f.first[C].andThen(F.lift((identity[B] _).split(g)))) <-> (F.lift((identity[A] _).split(g)).andThen(f.first[D]))

  def arrowUnit[A, B, C](f: F[A, B]): IsEq[F[(A, C), B]] =
    (f.first[C].andThen(F.lift(fst[B, C]))) <-> (F.lift(fst[A, C]).andThen(f))

  def arrowAssociation[A, B, C, D](f: F[A, B]): IsEq[F[((A, C), D), (B, (C, D))]] =
    (f.first[C].first[D].andThen(F.lift(assoc[B, C, D]))) <-> (F.lift(assoc[A, C, D]).andThen(f.first[(C, D)]))

  def splitConsistentWithAndThen[A, B, C, D](f: F[A, B], g: F[C, D]): IsEq[F[(A, C), (B, D)]] =
    F.split(f, g) <-> (f.first.andThen(g.second))

  def mergeConsistentWithAndThen[A, B, C](f: F[A, B], g: F[A, C]): IsEq[F[A, (B, C)]] =
    F.merge(f, g) <-> (F.lift((x: A) => (x, x)).andThen(F.split(f, g)))

  private def fst[A, B](p: (A, B)): A = p._1

  private def assoc[A, B, C](p: ((A, B), C)): (A, (B, C)) = (p._1._1, (p._1._2, p._2))
}

object ArrowLaws {
  def apply[F[_, _]](implicit ev: Arrow[F]): ArrowLaws[F] =
    new ArrowLaws[F] { def F: Arrow[F] = ev }
}
