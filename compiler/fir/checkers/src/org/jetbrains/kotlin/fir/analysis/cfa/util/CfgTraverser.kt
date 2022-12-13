/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*

// ------------------------------ Graph Traversal ------------------------------

enum class TraverseDirection {
    Forward, Backward
}

fun <D> ControlFlowGraph.traverse(
    direction: TraverseDirection,
    visitor: ControlFlowGraphVisitor<*, D>,
    data: D
) {
    for (node in getNodesInOrder(direction)) {
        node.accept(visitor, data)
        (node as? CFGNodeWithSubgraphs<*>)?.subGraphs?.forEach { it.traverse(direction, visitor, data) }
    }
}

fun ControlFlowGraph.traverse(
    direction: TraverseDirection,
    visitor: ControlFlowGraphVisitorVoid
) {
    traverse(direction, visitor, null)
}

// ---------------------- Path-sensitive data collection -----------------------

fun <I : PathAwareControlFlowInfo<I, *>> ControlFlowGraph.collectDataForNode(
    direction: TraverseDirection,
    initialInfo: I,
    visitor: PathAwareControlFlowGraphVisitor<I>,
    visitSubGraphs: Boolean = true
): Map<CFGNode<*>, I> {
    val nodeMap = LinkedHashMap<CFGNode<*>, I>()
    val startNode = getEnterNode(direction)
    nodeMap[startNode] = initialInfo

    var shouldContinue: Boolean
    do {
        shouldContinue = collectDataForNodeInternal(direction, initialInfo, visitor, nodeMap, visitSubGraphs)
    } while (shouldContinue)

    return nodeMap
}

private fun <I : PathAwareControlFlowInfo<I, *>> ControlFlowGraph.collectDataForNodeInternal(
    direction: TraverseDirection,
    initialInfo: I,
    visitor: PathAwareControlFlowGraphVisitor<I>,
    nodeMap: MutableMap<CFGNode<*>, I>,
    visitSubGraphs: Boolean = true
): Boolean {
    var changed = false
    val nodes = getNodesInOrder(direction)
    for (node in nodes) {
        if (visitSubGraphs && direction == TraverseDirection.Backward && node is CFGNodeWithSubgraphs<*>) {
            node.subGraphs.forEach { changed = changed or it.collectDataForNodeInternal(direction, initialInfo, visitor, nodeMap) }
        }
        val previousNodes = when (direction) {
            TraverseDirection.Forward -> node.previousCfgNodes
            TraverseDirection.Backward -> node.followingCfgNodes
        }
        val previousData =
            previousNodes.mapNotNull {
                val k = when (direction) {
                    TraverseDirection.Forward -> node.edgeFrom(it)
                    TraverseDirection.Backward -> node.edgeTo(it)
                }
                val v = nodeMap[it] ?: return@mapNotNull null
                visitor.visitEdge(it, node, k, v)
            }
        val reduced = if (node is UnionNodeMarker)
            previousData.reduceOrNull { a, b -> a.plus(b) }
        else
            previousData.reduceOrNull { a, b -> a.merge(b) }
        val data = nodeMap[node]
        val newData = node.accept(visitor, reduced ?: visitor.emptyInfo)
        val hasChanged = newData != data
        changed = changed or hasChanged
        if (hasChanged) {
            nodeMap[node] = newData
        }
        if (visitSubGraphs && direction == TraverseDirection.Forward && node is CFGNodeWithSubgraphs<*>) {
            node.subGraphs.forEach { changed = changed or it.collectDataForNodeInternal(direction, initialInfo, visitor, nodeMap) }
        }
    }
    return changed
}
