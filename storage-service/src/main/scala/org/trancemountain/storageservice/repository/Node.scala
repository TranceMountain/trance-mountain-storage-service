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

package org.trancemountain.storageservice.repository

import scala.collection.mutable

/**
	* @author michaelcoddington
	*/
protected[repository] class Node(nodeID: String, nodePath: String, primaryType: String, mixinTypes: Set[String], initialProperties: Map[String, Any], revisionNumber: RepositoryRevisionNumber) extends INode {

	private val nodeName = if (nodePath == "/") nodePath else nodePath.split("/").last

	private val nodeProperties = mutable.Map.empty[String, Any]
	if (initialProperties != null) nodeProperties ++= initialProperties

	private val nodeMixinTypes = mutable.Set.empty[String]
	if (mixinTypes != null) nodeMixinTypes ++= mixinTypes

	override def id: String = nodeID

	override def update(name: String, value: Any): Unit = {
		nodeProperties.get(name) match {
			case None => nodeProperties(name) = value
			case Some(obj) =>
				value match {
					case null => nodeProperties.remove(name)
					case _ =>
						if (obj.getClass != value.getClass) throw new RepositoryException(s"Cannot change value type for property $name for node $nodePath")
						nodeProperties(name) = value
				}
		}
	}

	override def apply(name: String): Option[Any] = {
		nodeProperties.get(name)
	}

	override def path: String = nodePath

	override def name: String = nodeName

	override def primaryNodeType: String = primaryType

	override def mixinNodeTypes: Set[String] = nodeMixinTypes.toSet

	override def addMixinNodeType(mixinType: String): Unit = nodeMixinTypes += mixinType

	override def removeMixinNodeType(mixinType: String): Unit = nodeMixinTypes -= mixinType

	override def revision: RepositoryRevisionNumber = revisionNumber

	override def toString: String = s"Node [path=$path, type=$primaryNodeType, revision=$revision]"

	override def properties: Map[String, Any] = nodeProperties.toMap

	override protected[repository] def setRevision(revision: RepositoryRevisionNumber): Unit = {
		throw new RuntimeException("not allowed")
	}

	override def isDeleted() = {
		throw new RuntimeException("not implemented")
	}
}
