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

package cats.bench

import cats.{Bitraverse, Parallel, Traverse}
import cats.instances.int.*
import cats.instances.either.*
import cats.instances.vector.*
import org.openjdk.jmh.annotations.{Benchmark, Scope, State}

/**
 * Results on OpenJDK 64-Bit Server VM (build 25.232-b09, mixed mode)
 *
 * bench/jmh:run -i 10 -wi 10 -f 5 -t 1 cats.bench.ParTraverseBench
 *
 * Benchmark                                       Mode  Cnt         Score        Error  Units
 * ParTraverseBench.eitherParBitraversePointfree  thrpt   50  20201469.796 ± 143402.778  ops/s
 * ParTraverseBench.eitherParBitraversePointfull  thrpt   50  24742265.071 ± 133253.733  ops/s
 * ParTraverseBench.eitherParTraversePointfree    thrpt   50   1150877.660 ±  10162.432  ops/s
 * ParTraverseBench.eitherParTraversePointfull    thrpt   50   1221809.128 ±   9997.474  ops/s
 */
@State(Scope.Benchmark)
class ParTraverseBench {

  val xs: Vector[Int] = (1 to 10).toVector
  val t: (Int, Int) = (1, 2)
  val f: Int => Either[Int, Int] = Right(_)

  def parTraversePointfree[T[_]: Traverse, M[_], A, B](ta: T[A])(f: A => M[B])(implicit P: Parallel[M]): M[T[B]] = {
    val gtb: P.F[T[B]] = Traverse[T].traverse(ta)(f.andThen(P.parallel.apply(_)))(using P.applicative)
    P.sequential(gtb)
  }

  def parTraversePointfull[T[_]: Traverse, M[_], A, B](ta: T[A])(f: A => M[B])(implicit P: Parallel[M]): M[T[B]] = {
    val gtb: P.F[T[B]] = Traverse[T].traverse(ta)(a => P.parallel(f(a)))(using P.applicative)
    P.sequential(gtb)
  }

  def parBitraversePointfree[T[_, _]: Bitraverse, M[_], A, B, C, D](
    tab: T[A, B]
  )(f: A => M[C], g: B => M[D])(implicit P: Parallel[M]): M[T[C, D]] = {
    val ftcd: P.F[T[C, D]] =
      Bitraverse[T].bitraverse(tab)(f.andThen(P.parallel.apply(_)), g.andThen(P.parallel.apply(_)))(using P.applicative)
    P.sequential(ftcd)
  }

  def parBitraversePointfull[T[_, _]: Bitraverse, M[_], A, B, C, D](
    tab: T[A, B]
  )(f: A => M[C], g: B => M[D])(implicit P: Parallel[M]): M[T[C, D]] = {
    val ftcd: P.F[T[C, D]] =
      Bitraverse[T].bitraverse(tab)(a => P.parallel.apply(f(a)), b => P.parallel.apply(g(b)))(using P.applicative)
    P.sequential(ftcd)
  }

  @Benchmark
  def eitherParTraversePointfree: Either[Int, Vector[Int]] = parTraversePointfree(xs)(f)

  @Benchmark
  def eitherParTraversePointfull: Either[Int, Vector[Int]] = parTraversePointfull(xs)(f)

  @Benchmark
  def eitherParBitraversePointfree: Either[Int, (Int, Int)] = parBitraversePointfree(t)(f, f)

  @Benchmark
  def eitherParBitraversePointfull: Either[Int, (Int, Int)] = parBitraversePointfull(t)(f, f)
}
