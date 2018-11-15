package com.openshine.escova

import com.openshine.escova.functional.CostMeasure
import org.elasticsearch.search.aggregations.AggregationBuilder

package nodes {

  sealed trait AnalysisTree extends CostMeasure[Double] {
    def addChild(child: TreeNode): AnalysisTree

    /** Synonym for addChild
      *
      * @param child the child to add to the AnalysisTree
      * @return a new AnalysisTree accounting for the added child
      */
    def :+(child: TreeNode): AnalysisTree = addChild(child)

    override def value: Double = cost

    def cost: Double
  }

  sealed trait TreeNode extends AnalysisTree {
    lazy val height: Int =
      if (children.isEmpty)
        1
      else
        1 + children.map(_.height).max

    override def addChild(child: TreeNode): TreeNode

    override def :+(child: TreeNode): TreeNode = addChild(child)

    def node: AggregationBuilder

    def children: Seq[TreeNode]
  }

  case class LeafAggregation(override val node: AggregationBuilder,
                             override val cost: Double)
      extends AnalysisTree
      with TreeNode {
    override def addChild(child: TreeNode): TreeNode =
      SubAggregation(node, cost, Vector(child))

    override def children: Seq[TreeNode] = Vector.empty
  }

  case class SubAggregation(override val node: AggregationBuilder,
                            override val cost: Double,
                            override val children: Vector[TreeNode])
      extends AnalysisTree
      with TreeNode {

    override def addChild(child: TreeNode): TreeNode =
      SubAggregation(node, cost, children :+ child)
  }

  object SubAggregation {
    def apply(node: AggregationBuilder,
              children: Vector[TreeNode]): SubAggregation =
      new SubAggregation(node, children.map(_.cost).sum, children)
  }

  case class RootAggregation(children: Vector[TreeNode] = Vector(),
                             override val cost: Double = Double.NaN)
      extends AnalysisTree {

    override def addChild(child: TreeNode): RootAggregation =
      RootAggregation(children :+ child, cost)

    override def :+(child: TreeNode): RootAggregation = addChild(child)
  }
}
