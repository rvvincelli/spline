/*
 * Copyright 2020 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.consumer.service.internal

import java.util.UUID

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.spline.consumer.service.internal.model.OperationWithSchema
import za.co.absa.spline.consumer.service.model.{AttributeNode, AttributeTransition}

import scala.collection.JavaConverters._

class AttributeDependencySolverSpec extends AnyFlatSpec with Matchers {

  import za.co.absa.spline.consumer.service.internal.AttributeDependencySolverSpec._

  behavior of "AttributeDependencySolver"

  it should "resolve basic select with sum" in {

    val in = toInput(
      0,
      Seq(A, B))

    val op = toSelect(
      1,
      Seq(0),
      Seq(attrOperation(Seq(attrRef(A), attrRef(B)))),
      Seq(C))

    val out = toOutput(
      2,
      Seq(1),
      Seq(C)
    )

    val operations = Seq(op, in, out)
    assertResolvedEquals(operations, A, Seq.empty, Seq((A, 0, Nil)))
    assertResolvedEquals(operations, B, Seq.empty, Seq((B, 0, Nil)))
    assertResolvedEquals(operations, C, Seq(C -> A, C -> B), Seq((C, 1, Nil), (A, 0, Nil), (B, 0, Nil)))
  }

  it should "resolve three dependent operation" in {

    val in = toInput(
      0,
      Seq(A))

    val op1 = toSelect(
      1,
      Seq(0),
      Seq(attrRef(A)),
      Seq(B))

    val op2 = toSelect(
      2,
      Seq(1),
      Seq(attrRef(B)),
      Seq(C))

    val op3 = toSelect(
      3,
      Seq(2),
      Seq(attrRef(C)),
      Seq(D))

    val out = toOutput(
      4,
      Seq(3),
      Seq(D)
    )

    val operations = Seq(out, op1, op2, op3, in)
    assertResolvedEquals(operations, A, Seq.empty, Seq((A, 0, Nil)))
    assertResolvedEquals(operations, B, Seq(B -> A), Seq((A, 0, Nil), (B, 1, Nil)))
    assertResolvedEquals(operations, C, Seq(C -> B, B -> A), Seq((A, 0, Nil), (B, 1, Nil), (C, 2, Nil)))
    assertResolvedEquals(operations, D, Seq(D -> C, C -> B, B -> A), Seq((A, 0, Nil), (B, 1, Nil), (C, 2, Nil), (D, 3, Seq(4))))
  }


  it should "resolve chain of several operations including an expression" in {

    val in = toInput(
      0,
      Seq(A, B))

    val op1 = toSelect(
      1,
      Seq(0),
      Seq(attrRef(A), attrRef(B)),
      Seq(C, D))

    val op2 = toSelect(
      2,
      Seq(1),
      Seq(attrOperation(Seq(attrRef(C), attrRef(D)))),
      Seq(E))

    val op3 = toSelect(
      3,
      Seq(2),
      Seq(attrRef(E)),
      Seq(F))

    val out = toOutput(
      4,
      Seq(3),
      Seq(F)
    )

    val operations = Seq(out, in, op1, op2, op3)

    assertResolvedEquals(operations, C, Seq(C -> A), Seq((A, 0, Nil), (C, 1, Nil)))
    assertResolvedEquals(operations, D, Seq(D -> B), Seq((B, 0, Nil), (D, 1, Nil)))
    assertResolvedEquals(operations, E, Seq(E -> C, E -> D, C -> A, D -> B),
      Seq((A, 0, Nil), (B, 0, Nil), (C, 1, Nil), (D, 1, Nil), (E, 2, Nil)))
    assertResolvedEquals(operations, F, Seq(F -> E, E -> C, E -> D, C -> A, D -> B),
      Seq((A, 0, Nil), (B, 0, Nil), (C, 1, Nil), (D, 1, Nil), (E, 2, Nil), (F, 3, Seq(4))))
  }

  it should "resolve aggregation" in {

    val in = toInput(
      0,
      Seq(A, B))

    val op = toAggregate(
      1,
      Seq(0),
      Seq(attrRef(A), attrRef(B)),
      Seq(C, D))

    val out = toOutput(
      2,
      Seq(1),
      Seq(C, D)
    )

    val operations = Seq(op, in, out)

    assertResolvedEquals(operations, C, Seq(C -> A), Seq((A, 0, Nil), (C, 1, Nil)))
    assertResolvedEquals(operations, D, Seq(D -> B), Seq((B, 0, Nil), (D, 1, Nil)))
  }

  it should "resolve generation" in {

    val in = toInput(
      0,
      Seq(A))

    val op = toGenerate(
      1,
      Seq(0),
      attrRef(A),
      B)

    val out = toOutput(
      2,
      Seq(1),
      Seq(B)
    )

    val operations = Seq(op, in, out)

    assertResolvedEquals(operations, B, Seq(B -> A), Seq((A, 0, Nil), (B, 1, Nil)))
  }

  it should "resolve subquery alias" in {

    val in = toInput(
      0,
      Seq(A, B))

    val op = toSubqueryAlias(
      1,
      Seq(0),
      Seq(C, D))

    val out = toOutput(
      2,
      Seq(1),
      Seq(C, D)
    )

    val operations = Seq(op, in, out)

    assertResolvedEquals(operations, B, Seq.empty, Seq((B, 0, Nil)))
    assertResolvedEquals(operations, C, Seq(C -> A), Seq((A, 0, Nil), (C, 1, Nil)))
    assertResolvedEquals(operations, D, Seq(D -> B), Seq((B, 0, Nil), (D, 1, Nil)))
  }

  it should "resolve io operation correctly" in {

    val in = toInput(
      1,
      Seq(A, B, C))

    val op = toSelect(
      2,
      Seq(1),
      Seq(attrOperation(Seq(attrRef(A), attrRef(C)))),
      Seq(D))

    val out = toOutput(
      3,
      Seq(2),
      Seq(D))

    val operations = Seq(op, in, out)

    assertResolvedEquals(operations, A, Seq.empty, Seq((A, 1, Nil)))
    assertResolvedEquals(operations, B, Seq.empty, Seq((B, 1, Nil)))
    assertResolvedEquals(operations, C, Seq.empty, Seq((C, 1, Nil)))
    assertResolvedEquals(operations, D, Seq(D -> A, D -> C), Seq((A, 1, Nil), (C, 1, Nil), (D, 2, Nil)))
  }


  it should "resolve filter operation correctly" in {

    val in = toInput(
      0,
      Seq(A))

    val op1 = toFilter(
      1,
      Seq(0),
      Seq(A)
    )

    val op2 = toSelect(
      2,
      Seq(1),
      Seq(attrRef(A)),
      Seq(B))

    val op3 = toFilter(
      3,
      Seq(2),
      Seq(B)
    )

    val op4 = toFilter(
      4,
      Seq(3),
      Seq(B)
    )

    val out = toOutput(
      5,
      Seq(4),
      Seq(B))


    val operations = Seq(out, op1, op2, op3, op4, in)

    assertResolvedEquals(operations, A, Seq.empty, Seq((A, 0, Seq(1))))
    assertResolvedEquals(operations, B, Seq(B -> A), Seq((A, 0, Seq(1)), (B, 2, Seq(5, 4, 3))))
  }

  /**
   *
   *  (A)      (B)   (C)
   *   \       /     /
   *   (D)   (E)    /
   *    \    /     /
   *    (D, E)    /
   *      |      /
   *     (F)   (G)
   *       \   /
   *      (F, G)
   *
   */
  it should "resolve operation with several sources correctly" in {

    val inA = toInput(
      0,
      Seq(A))

    val inB = toInput(
      1,
      Seq(B))

    val inC = toInput(
      2,
      Seq(C))

    val opD = toSelect(
      3,
      Seq(0),
      Seq(attrRef(A)),
      Seq(D))

    val opE = toSelect(
      4,
      Seq(1),
      Seq(attrRef(B)),
      Seq(E))

    val joinDE = toJoin(
      5,
      Seq(opD._id.toInt, opE._id.toInt),
      Seq(D, E))

    val opF = toSelect(
      6,
      Seq(joinDE._id.toInt),
      Seq(attrOperation(Seq(attrRef(D), attrRef(E)))),
      Seq(F))

    val opG = toSelect(
      7,
      Seq(inC._id.toInt),
      Seq(attrRef(C)),
      Seq(G))

    val joinFG = toJoin(
      8,
      Seq(opF._id.toInt, opG._id.toInt),
      Seq(F, G))

    val out = toOutput(
      9,
      Seq(joinFG._id.toInt),
      Seq(F, G))


    val operations = Seq(out, opD, opE, joinDE, opF, opG, joinFG, inA, inB, inC)

    assertResolvedEquals(operations, D, Seq(D -> A), Seq((D, 3, Seq(5)), (A, 0, Nil)))
    assertResolvedEquals(operations, E, Seq(E -> B), Seq((B, 1, Nil), (E, 4, Seq(5))))
    assertResolvedEquals(operations, F, Seq(F -> E, F -> D, D -> A, E -> B),
      Seq((A, 0, Nil), (B, 1, Nil), (D, 3, Seq(5)), (E, 4, Seq(5)), (F, 6, Seq(9,8))))
    assertResolvedEquals(operations, G, Seq(G -> C), Seq((C, 2, Nil), (G, 7, Seq(9,8))))
  }

  /**
   *     (A)
   *    /  \
   *  (A)  (B)
   *    \  /
   *   (A,B)
   *     |
   *   (A,B)
   */
  it should "resolve diamond correctly" in {

    val in = toInput(
      0,
      Seq(A))

    val op1 = toFilter(
      1,
      Seq(0),
      Seq(A)
    )

    val op2 = toSelect(
      2,
      Seq(0),
      Seq(attrRef(A)),
      Seq(B))

    val join = toJoin(
      3,
      Seq(1, 2),
      Seq(A, B))

    val out = toOutput(
      4,
      Seq(3),
      Seq(A, B))


    val operations = Seq(out, op1, op2, join, in)

    assertResolvedEquals(operations, A, Seq.empty, Seq((A, 0, Seq(4, 3, 1))))
    assertResolvedEquals(operations, B, Seq(B -> A), Seq((A, 0, Nil), (B, 2, Seq(4, 3))))
  }

}

object AttributeDependencySolverSpec extends Matchers {
  private val A, B, C, D, E, F, G = UUID.randomUUID()

  private def assertResolvedEquals(
    operations: Seq[OperationWithSchema],
    forAttribute: UUID,
    edges: Seq[(UUID, UUID)],
    nodes: Seq[(UUID, Int, Seq[Int])]
  ): Unit = {

    val maybeGraph = AttributeDependencySolver.resolveDependencies(operations, forAttribute)
    val graph = maybeGraph.getOrElse(fail())

    val expected = edges.map { case (a, b) => AttributeTransition(a.toString, b.toString) }
    val expectedNodes = nodes.map { case (a, b, c) => AttributeNode(a.toString, b.toString, c.map(_.toString)) }

    graph.edges.toSet shouldEqual expected.toSet
    graph.nodes.toSet shouldEqual expectedNodes.toSet
  }

  private def exprAsJava(expressions: Any): Any = expressions match {
    case e: Seq[Any] => e.map(exprAsJava).asJava
    case e: Map[String, Any] => mapToJava(e)
    case e: Any => e
  }

  private def mapToJava(m: Map[String, Any]): java.util.Map[String, Any] = m
    .map { case (k, v) => k -> exprAsJava(v) }
    .asJava

  private def toInput(id: Int, schema: Seq[UUID]) =
    OperationWithSchema(id.toString, schema.toArray, Map("name" -> "Read"), Map.empty, Seq.empty)

  private def toOutput(id: Int, childIds: Seq[Int], schema: Seq[UUID]) =
    OperationWithSchema(id.toString, schema.toArray, Map("name" -> "Write"), Map.empty, childIds.map(_.toString))

  private def toSelect(id: Int, childIds: Seq[Int], expressions: Seq[Map[String, Any]], schema: Seq[UUID]) =
    OperationWithSchema(id.toString, schema.toArray,
      Map("name" -> "Project"),
      Map("projectList" -> exprAsJava(expressions)),
      childIds.map(_.toString))

  private def toAggregate(id: Int, childIds: Seq[Int], expressions: Seq[Map[String, Any]], schema: Seq[UUID]) =
    OperationWithSchema(id.toString, schema.toArray,
      Map("name" -> "Aggregate"),
      Map("aggregateExpressions" -> exprAsJava(expressions)),
      childIds.map(_.toString))

  private def toGenerate(id: Int, childIds: Seq[Int], generator: Map[String, Any], outputId: UUID) =
    OperationWithSchema(id.toString, Array(outputId),
      Map("name" -> "Generate"),
      Map("generator" -> exprAsJava(generator), "generatorOutput" -> exprAsJava(Seq(attrRef(outputId)))),
      childIds.map(_.toString))

  private def toFilter(id: Int, childIds: Seq[Int], schema: Seq[UUID]) =
    OperationWithSchema(id.toString, schema.toArray, Map("name" -> "Filter"), Map.empty, childIds.map(_.toString))

  private def toJoin(id: Int, childIds: Seq[Int], schema: Seq[UUID]) =
    OperationWithSchema(id.toString, schema.toArray, Map("name" -> "Join"), Map.empty, childIds.map(_.toString))

  private def toSubqueryAlias(id: Int, childIds: Seq[Int], schema: Seq[UUID]) =
    OperationWithSchema(id.toString, schema.toArray, Map("name" -> "SubqueryAlias"), Map.empty, childIds.map(_.toString))

  private def attrRef(attr: UUID) =
    Map("_typeHint" -> "expr.AttrRef", "refId" -> attr.toString)

  private def attrOperation(children: Seq[Any]) =
    Map("_typeHint" -> "dummt", "children" -> children)
}
