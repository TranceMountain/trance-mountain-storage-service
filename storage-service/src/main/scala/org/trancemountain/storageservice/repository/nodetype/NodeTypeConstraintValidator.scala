/*
 * Trance Mountain: A scalable digital asset management system
 *
 * Copyright (C) 2016  Michael Coddington
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.trancemountain.storageservice.repository.nodetype

import java.util.Date

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.trancemountain.storageservice.repository._
import org.trancemountain.storageservice.repository.store.INodeTypeStore

import scala.collection.mutable

/**
	* @author michaelcoddington
	*/
object NodeTypeConstraintValidator {
	private val LOG = LoggerFactory.getLogger(getClass)
}


@Component
class NodeTypeConstraintValidator {

	import NodeTypeConstraintValidator.LOG

	@Autowired
	private val nodeTypeStore: INodeTypeStore = null

	@throws[NodeTypeConstraintViolationException]
	def validate(node: ISessionNode): Unit = {

		val primaryNodeTypeDef: INodeTypeDefinition = nodeTypeStore.getNodeTypeDefinition(NodeTypeIdentifier.fromString(node.primaryNodeType)) match {
			case s @ Some(ntd: INodeTypeDefinition) => ntd
			case None => throw new RepositoryException(s"Cannot locate primary node type defintion ${node.primaryNodeType} for node $node")
		}
		val mixinTypeDefs: Set[INodeTypeDefinition] = node.mixinNodeTypes.map(typeName => nodeTypeStore.getNodeTypeDefinition(NodeTypeIdentifier.fromString(typeName)) match {
			case s @ Some(ntd: INodeTypeDefinition) => ntd
			case None => throw new RepositoryException(s"Cannot locate mixin node type defintion $typeName for node $node")
		})

		val nodeTypeDefinitions: Set[INodeTypeDefinition] = mixinTypeDefs + primaryNodeTypeDef

		val requiredPropertyDefinitions = mutable.Map.empty[String, INodePropertyDefinition]
		val optionalPropertyDefinitions = mutable.Map.empty[String, INodePropertyDefinition]

		val namedChildDefinitions = mutable.Map.empty[String, INodeChildDefinition]
		val unnamedTypedChildDefinitions = mutable.Map.empty[String, INodeChildDefinition]
		var wildcardNodeChildDefinition: INodeChildDefinition = null

		for (nodeTypeDef <- nodeTypeDefinitions) {
			nodeTypeDef.propertyDefinitions match {
				case Some(s: Set[INodePropertyDefinition]) =>
					for (propDef <- s) {
						if (propDef.required) requiredPropertyDefinitions(propDef.name) = propDef
						else optionalPropertyDefinitions(propDef.name) = propDef
					}
				case None =>
 			}
			nodeTypeDef.childDefinitions match {
				case Some(s: Set[INodeChildDefinition]) =>
					for (childDef <- s) {
						childDef match {
							case WildcardNodeChildDefinition => wildcardNodeChildDefinition = childDef
							case cd: NodeChildDefinition =>
								childDef.name match {
									case Some(name: String) =>
										LOG.debug(s"Adding named child definition $childDef")
										namedChildDefinitions.put(name, childDef)
									case None =>
										LOG.debug(s"Adding typed child definition $childDef")
										unnamedTypedChildDefinitions(childDef.nodeTypeIdentifier.toString) = childDef
								}
						}
					}
				case None =>
			}
		}

		def checkPropType(propertyType: PropertyType, value: Any): Boolean = {
			propertyType match {
				case PropertyType.INT => value.isInstanceOf[Int]
				case PropertyType.INT_ARRAY => value.isInstanceOf[Array[Int]]
				case PropertyType.BINARY => value.isInstanceOf[IBinary]
				case PropertyType.BOOLEAN => value.isInstanceOf[Boolean]
				case PropertyType.DATE => value.isInstanceOf[Date]
				case PropertyType.DOUBLE => value.isInstanceOf[Double]
				case PropertyType.DOUBLE_ARRAY => value.isInstanceOf[Array[Double]]
				case PropertyType.LONG => value.isInstanceOf[Long]
				case PropertyType.LONG_ARRAY => value.isInstanceOf[Array[Long]]
				case PropertyType.STRING => value.isInstanceOf[String]
				case PropertyType.STRING_ARRAY => value.isInstanceOf[Array[String]]
				case PropertyType.STRONG_REF => value.isInstanceOf[StrongReference]
				case PropertyType.WEAK_REF => value.isInstanceOf[WeakReference]
			}
		}

		val nodeProperties = node.properties

		for ((propName, propDef) <- requiredPropertyDefinitions) {
			val nodeProp = nodeProperties.get(propName)
			nodeProp match {
				case Some(a: Any) =>
					val typeMatches = checkPropType(propDef.propertyType, a)
					if (!typeMatches) throw new NodeTypeConstraintViolationException(s"Node $node has value $a for property $propName. Expected ${propDef.propertyType}.")
				case None => throw new NodeTypeConstraintViolationException(s"Node $node is missing required property $propName")
			}
		}

		for ((propName, propDef) <- optionalPropertyDefinitions) {
			val nodeProp = nodeProperties.get(propName)
			nodeProp match {
				case Some(a: Any) =>
					val typeMatches = checkPropType(propDef.propertyType, a)
					if (!typeMatches) throw new NodeTypeConstraintViolationException(s"Node $node has value $a for property $propName. Expected ${propDef.propertyType}.")
				case None =>
			}
		}

		// check node children
		val evaluatedChildren = mutable.Set.empty[ISessionNode]

		// first, check the named children
		for ((childName, childDef) <- namedChildDefinitions) {
			val nodeChild = node.child(childName)
			nodeChild match {
				case Some(n: ISessionNode) =>
					require(childDef.nodeTypeIdentifier.toString == n.primaryNodeType,
						s"Node child $n has incorrect type ${n.primaryNodeType} Expected ${childDef.nodeTypeIdentifier}.")
					evaluatedChildren += n
				case None =>
					childDef.minCount match {
						case Some(0) =>
						case None | Some(1) => throw new NodeTypeConstraintViolationException(s"Node $node is missing required child $childDef")
						case _ => throw new RepositoryException(s"Unknown min count ${childDef.minCount} found for named child definition $childDef")
					}
			}
		}

		if (node.children.isDefined) {
			// then check the unnamed, typed children
			val unevaluatedChildren = mutable.Set.empty[ISessionNode]
			unevaluatedChildren ++= node.children.get.toSet.diff(evaluatedChildren)
			LOG.debug(s"Checking ${unevaluatedChildren.size} unevaluated children")

			// map node types to the nodes that conform to those types
			val childTypeMap: Map[String, Set[ISessionNode]] = unevaluatedChildren.toSet.groupBy(_.primaryNodeType)

			LOG.debug(s"Evaluating typed children $childTypeMap")

			for ((childType, childNodes) <- childTypeMap) {
				val typedChildDef = unnamedTypedChildDefinitions.get(childType)
				typedChildDef match {
					case None =>
						if (wildcardNodeChildDefinition == null) throw new NodeTypeConstraintViolationException(s"Invalid unnamed/typed children: $childNodes")
					case Some(childDef: INodeChildDefinition) =>
						val minCount = childDef.minCount match {
							case None => 1
							case Some(i: Int) => i
						}
						val maxCount = childDef.maxCount match {
							case None => 1
							case Some(i: Int) => i
						}
						if (childNodes.size < minCount || childNodes.size > maxCount) {
							throw new NodeTypeConstraintViolationException(s"Invalid typed child count for nodetype $childTypeMap: ${childNodes.size}")
						} else {
							unevaluatedChildren --= childNodes
						}
				}
			}

			// if there are any children left to evaluate and there's no wildcard child definition,
			// that's an error
			if (unevaluatedChildren.nonEmpty && wildcardNodeChildDefinition == null) {
				throw new NodeTypeConstraintViolationException(s"Found invalid children: $unevaluatedChildren")
			}
		} else {
			LOG.debug(s"No children defined for node $node")
			// if node has no children, but has typed child definitions, we need to check those definition
			// minCounts to see if that's ok
			for ((childType, childDef) <- unnamedTypedChildDefinitions) {
				val minCount = childDef.minCount match {
					case None | Some(1) => 1
					case Some(i: Int) => i
				}
				if (minCount > 0) throw new NodeTypeConstraintViolationException(s"No child found for required typed-child definition $childDef")
			}
		}

	}

}
