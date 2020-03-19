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

package za.co.absa.spline.common.graph

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.scalatest.WhitespaceNormalizations._
import za.co.absa.spline.common.graph.DAGTraversals._
import za.co.absa.spline.common.graph.DAGTraversalsTest._

class DAGTraversalsTest extends AnyFlatSpec with Matchers {
  behavior of "DFS"

  it should "return visit every node, depth-first" in {
    val res = dfs[Int, String](1, "", treeGen(3, 3), _ => true, csvCollectUntilDepth(Int.MaxValue))
    res should equal(
      """
        |1,
        | 11,
        |   111, 112, 113,
        | 12,
        |   121, 122, 123,
        | 13,
        |   131, 132, 133,
        |""".stripMargin)(after being whiteSpaceRemoved)
  }

  it should "only collect even vertices" in {
    val res = dfs[Int, String](1, "", treeGen(3, 3), _ % 2 == 0, csvCollectUntilDepth(Int.MaxValue))
    res should equal("112, 12, 122, 132,")(after being whiteSpaceRemoved)
  }

  it should "limit traversing depth" in {
    val res = dfs[Int, String](1, "", treeGen(3, 3), _ => true, csvCollectUntilDepth(2))
    res should equal("1, 11, 12, 13,")(after being whiteSpaceRemoved)
  }
}

object DAGTraversalsTest {

  private def treeGen(width: Int, depth: Int)(i: Int): Seq[Int] =
    if (i < math.pow(10, depth - 1)) 1 to width map (_ + i * 10)
    else Nil

  private def csvCollectUntilDepth(threshold: Int)(z: String, i: Int) =
    (s"$z$i,", i < math.pow(10, threshold - 1))
}
