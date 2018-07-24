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

package org.trancemountain.storageservice.repository.store.memory

import java.util.UUID

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.store._
import org.trancemountain.storageservice.repository.{Node, INode, RepositoryReferentialIntegrityException, RepositoryRevisionNumber}

import scala.collection.mutable

/**
	* @author michaelcoddington
	*/
@Lazy
@Service
@ConditionalOnProperty(name = Array(SpringConfigKeys.TM_NODE_DATA_STORE_TYPE), havingValue = "memory")
class MemoryNodeDataStore extends INodeDataStore {

	private val LOG = LoggerFactory.getLogger(getClass)

	/**
		* Holds persistent(committed) nodes.
		*/
	protected[memory] val persistentNodeMap: mutable.Map[String, Seq[INode]] = scala.collection.mutable.Map.empty[String, Seq[INode]]
	LOG.debug("Initialized persistent node map")

	private val referenceManager = new ReferenceManager

	/**
		* Returns the node for the given path with the highest revision number that is less than or equal to the given revision number.
		* @param nodePath the node path to search for
		* @param revisionNumber the highest allowable revision number to use
		*/
	private def lastNodeAvailableForRevision(nodePath: String, revisionNumber: RepositoryRevisionNumber): Option[INode] = {
		persistentNodeMap.get(nodePath) match {
			case None => None
			case Some(seq: Seq[INode]) =>
				val availableNodes = seq.filter(_.revision <= revisionNumber)
				LOG.debug(s"Found available nodes $availableNodes at path $nodePath for revision number $revisionNumber")
				if (availableNodes.nonEmpty) availableNodes.lastOption
				else None
		}
	}

	override def exists(path: String, sessionRevision: RepositoryRevisionNumber): Boolean = {
		lastNodeAvailableForRevision(path, sessionRevision) match {
			case None => false
			case Some(n: INode) => !n.isInstanceOf[DeletedNode]
		}
	}

	override def node(path: String, revision: RepositoryRevisionNumber): Option[INode] = {
		LOG.debug(s"Seeking node $path, revision $revision in persistent node map")
		val nodeAtPath = lastNodeAvailableForRevision(path, revision)
		nodeAtPath match {
			case None => None
			case Some(d: DeletedNode) => None
			case s @ Some(n: INode) => s
		}
	}

	override def availableRevisionsOfNode(targetNodePath: String): Option[Seq[INode]] = {
		persistentNodeMap.get(targetNodePath)
	}

	override def pruneNodesAtRevision(revision: RepositoryRevisionNumber): Unit = {
		for ((nodePath, nodeRevisionSeq) <- persistentNodeMap) {
			val nodeAtRevision = nodeRevisionSeq.find((node: INode) => node.revision == revision)
			nodeAtRevision match {
				case None =>
				case Some(n: INode) =>
					if (nodeRevisionSeq.last != n || n.isInstanceOf[DeletedNode]) {
						LOG.debug(s"Pruning node ${n.path} at revision $revision")
						val newSeq = nodeRevisionSeq.filter(_ != n)
						if (newSeq.nonEmpty) persistentNodeMap(nodePath) = newSeq
						else persistentNodeMap.remove(nodePath)
					}
			}
		}
	}

	override def pruneNodesUpToRevision(revision: RepositoryRevisionNumber): Unit = {
		for ((nodePath, nodeRevisionSeq) <- persistentNodeMap) {
			val lastRevisionOfNode = nodeRevisionSeq.last
			val nodesToKeep = nodeRevisionSeq.filter(node => node.revision > revision || (node == lastRevisionOfNode && !node.isInstanceOf[DeletedNode]))
			if (nodesToKeep.nonEmpty) {
				LOG.debug(s"Pruning node $nodePath through $revision, retaining ${nodesToKeep.size} revisions.")
				persistentNodeMap(nodePath) = nodesToKeep
			} else {
				LOG.debug(s"Pruning all revisions of node $nodePath")
				persistentNodeMap.remove(nodePath)
			}
		}
	}

	override def applyChangeSet(changeSet: NodeDataChangeSet): Unit = {
		LOG.debug(s"Applying change set of size ${changeSet.size}")

		def setReferences(node: INode): Unit = {
			if (node.properties != null) {
				val referencePropertyMap = node.properties.filter( (tuple:(String, Any)) => tuple._2.isInstanceOf[StoredReference])
				for ((propName, refValue) <- referencePropertyMap) {
					refValue match {
						case sr: StoredStrongReference => referenceManager.addStrongReferenceTo(sr.targetNodePath, sr)
						case wr: StoredWeakReference => referenceManager.addWeakReferenceTo(wr.targetNodePath, wr)
					}
				}
			}
		}

		for (change <- changeSet) {
			LOG.debug(s"Applying change $change")
			change match {
				case create: NodeCreationOperation =>
					require(!persistentNodeMap.contains(create.nodePath))
					val node = new Node(create.nodeID, create.nodePath, create.primaryNodeType, create.mixinNodeTypes, create.properties, changeSet.revision)
					persistentNodeMap(create.nodePath) = Seq[INode](node)
					setReferences(node)
					LOG.debug(s"Added node $node to persistent node map at path ${create.nodePath}")
				case change:  NodeChangeOperation =>
					require(persistentNodeMap.contains(change.nodePath))
					val persistentNodeSeq: Seq[INode] = persistentNodeMap(change.nodePath)
					val lastNode = persistentNodeSeq.last
					val newPersistentNode = new Node(UUID.randomUUID().toString, lastNode.path, lastNode.primaryNodeType, change.mixinNodeTypes, lastNode.properties, changeSet.revision)
					for (ac <- change.addedOrChangedFields) newPersistentNode(ac._1) = ac._2
					for (rc <- change.removedFields) newPersistentNode(rc._1) = null
					persistentNodeMap(change.nodePath) = persistentNodeSeq :+ newPersistentNode
					setReferences(newPersistentNode)
				case delete: NodeDeletionOperation =>
					val refsTo = referenceManager.strongReferencesTo(delete.nodePath)
					if (refsTo.isDefined && refsTo.get.nonEmpty) throw new RepositoryReferentialIntegrityException(s"Cannot delete persistent node ${delete.nodePath} with ${refsTo.get.size} strong references pointing to it.")
					require(persistentNodeMap.contains(delete.nodePath))
					val nodeRevisions = persistentNodeMap(delete.nodePath)
					if (nodeRevisions.size == 1 && nodeRevisions.last.isInstanceOf[DeletedNode]) {
						LOG.debug(s"Evicting node ${delete.nodePath} since last remaining node is a DELETED node.")
						persistentNodeMap.remove(delete.nodePath)
					} else {
						LOG.debug(s"Appending DELETED node for path ${delete.nodePath}")
						persistentNodeMap(delete.nodePath) = nodeRevisions :+ new DeletedNode(delete.nodeID, delete.nodePath, changeSet.revision)
					}
			}
		}
	}

	override def nodeChildPaths(sessionID: String, revision: RepositoryRevisionNumber, nodePath: String): Seq[String] = {
		val prefix = if (!nodePath.endsWith("/")) s"$nodePath/" else nodePath
		LOG.debug(s"Searching for node prefix $prefix through keys ${persistentNodeMap.keySet}")
		val childEntries = persistentNodeMap.view.filter( (keyVal: (String, Seq[INode])) => keyVal._1.startsWith(prefix) && keyVal._1 != prefix)
		val lastAvailableChildEntries = childEntries.map( keyVal => (keyVal._1, lastNodeAvailableForRevision(keyVal._1, revision)))
		val lastExistingChildEntries = lastAvailableChildEntries.filter(keyVal => keyVal._2.isDefined && !keyVal._2.get.isInstanceOf[DeletedNode])
		lastExistingChildEntries.map(_._1).toList.sorted
	}

	def strongReferencesTo(targetNodePath: String): Option[Set[StoredStrongReference]] = {
		referenceManager.strongReferencesTo(targetNodePath)
	}

	def weakReferencesTo(targetNodePath: String): Option[Set[StoredWeakReference]] = {
		referenceManager.weakReferencesTo(targetNodePath)
	}

	override def reset(): Unit = {
		persistentNodeMap.clear()
		referenceManager.clear()
	}

	/**
		* Keeps track of strong and weak references, both transient and persistent.
		*/
	protected[MemoryNodeDataStore] class ReferenceManager {
		/** Tracks strong references that point to nodes.
			* The key is a target node path  and the value is a (sessionID, source node path, property name) tuple.
			*/
		val transientStrongReferenceTargetMap = mutable.Map.empty[String, Set[StoredStrongReference]]
		val transientWeakReferenceTargetMap = mutable.Map.empty[String, Set[StoredWeakReference]]

		def addStrongReferenceTo(targetNodePath: String, ref: StoredStrongReference) {
			LOG.debug(s"Adding strong reference $ref")
			val refSet = transientStrongReferenceTargetMap.get(targetNodePath) match {
				case Some(s: Set[StoredStrongReference]) => s
				case None => Set.empty[StoredStrongReference]
			}
			transientStrongReferenceTargetMap(targetNodePath) = refSet + ref
		}

		def removeStrongReferenceFrom(targetNodePath: String, ref: StoredStrongReference): Unit = {
			LOG.debug(s"Removing strong reference $ref")
			val refSet = transientStrongReferenceTargetMap.get(targetNodePath) match {
				case Some(s: Set[StoredStrongReference]) => transientStrongReferenceTargetMap(targetNodePath) = s - ref
				case None =>
			}
		}

		def strongReferencesTo(targetNodePath: String): Option[Set[StoredStrongReference]] = transientStrongReferenceTargetMap.get(targetNodePath)

		def addWeakReferenceTo(targetNodePath: String, ref: StoredWeakReference) {
			LOG.debug(s"Adding weak reference $ref")
			val refSet = transientWeakReferenceTargetMap.get(targetNodePath) match {
				case Some(s: Set[StoredWeakReference]) => s
				case None => Set.empty[StoredWeakReference]
			}
			transientWeakReferenceTargetMap(targetNodePath) = refSet + ref
		}

		def removeWeakReferenceFrom(targetNodePath: String, ref: StoredWeakReference): Unit = {
			LOG.debug(s"Removing weak reference $ref")
			val refSet = transientWeakReferenceTargetMap.get(targetNodePath) match {
				case Some(s: Set[StoredWeakReference]) => transientWeakReferenceTargetMap(targetNodePath) = s - ref
				case None =>
			}
		}

		def weakReferencesTo(targetNodePath: String): Option[Set[StoredWeakReference]] = transientWeakReferenceTargetMap.get(targetNodePath)

		def clear(): Unit = {
			transientStrongReferenceTargetMap.clear()
			transientWeakReferenceTargetMap.clear()
		}
	}
}
