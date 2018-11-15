package com.openshine.escova

import com.openshine.escova.nodes.TreeNode

/** CostConfig allows to configure the cost of queries for every node type.
  *
  * You should be able to inject an instance of this class from any escova-core
  * user. escova-core does not provide a mechanism for parsing or generating
  * configuration.
  *
  * @param default the default configuration for non-customized node types
  * @param custom a map of custom configuration for certain node types. Keys
  *               should be the name of the aggregates
  */
case class CostConfig(
    default: NodeCostConfig = NodeCostConfig(),
    custom: Map[String, NodeCostConfig] = Map()
)

object CostConfig {
  /** A default cost config is provided here in case no other is available
    */
  implicit val defaultCostConfig: CostConfig = CostConfig()
}

case class NodeCostConfig(
    /** Default node cost.
      *
      * Does not apply to certain types of nodes. E.g., date_histogram nodes
      * calculate their cost from the seconds requested
      */
    defaultNodeCost: Double = 1d,

    /** Cost per child of the current node
      */
    perChildCost: Double = 10d,

    /** Operation to aggregate the children costs to the current node's costs.
      */
    childrenCostOp: String = "mul",

    /** Operation to aggregate all the children costs together.
      *
      * Usually, this should be the sum or the average cost.
      */
    nodeCostOpAggChild: String = "sum",

    /** Custom configuration for certain types of nodes.
      *
      * This may be allowed in code paths for certain node types
      */
    customNodeConfig: Map[String, String] = Map(),

    /** The maximum number of allowed subaggregates from this node downwards.
      */
    maxTreeHeight: Int = 4,

    /** If non-empty, only these type of children will be allowed as subaggregates.
      */
    whitelistChildren: List[String] = List(),


    /** If non-empty these types of children will be disallowed as subaggregates.
      */
    blacklistChildren: List[String] = List(),
) {

  /** Determine whether a TreeNode fits the white/black lists.
    *
    * Take care also of the max height allowed.
    *
    * @param t the TreeNode to inlclude
    * @return whether the children is allowed at the current point
    */
  def isChildAllowed(t: TreeNode): Boolean = {
    val whitelisted =
      if (whitelistChildren.nonEmpty)
        whitelistChildren.contains(t.node.getType)
      else
        !blacklistChildren.contains(t.node.getType)

    whitelisted && t.height < maxTreeHeight
  }
}
