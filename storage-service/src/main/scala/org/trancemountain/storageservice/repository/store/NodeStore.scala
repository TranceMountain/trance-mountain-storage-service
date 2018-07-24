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

package org.trancemountain.storageservice.repository.store

import java.io.InputStream
import java.util.UUID
import javax.annotation.PostConstruct

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.trancemountain.storageservice.repository._
import org.trancemountain.storageservice.repository.nodetype.{NodeTypeConstraintValidator, NodeTypeIdentifier}
import org.trancemountain.storageservice.repository.search.IRepositorySearchService

import scala.collection.mutable

/**
	* Holds new transient (uncommitted) nodes, changes to nodes, and deleted nodes.
	* For node data and metadata persistence, see INodeDataStore and INodeMetadataStore.
	*
	* @see [[org.trancemountain.storageservice.repository.store.INodeDataStore]]
	* @see [[org.trancemountain.storageservice.repository.store.INodeMetadataStore]]
	* @author michaelcoddington
	*/
@Lazy
@Service
class NodeStore extends INodeStore {

	private val LOG = LoggerFactory.getLogger(getClass)

	@Autowired
	private val metadataStore: INodeMetadataStore = null

	@Autowired
	private val dataStore: INodeDataStore = null

	@Autowired
	private val searchService: IRepositorySearchService = null

	@Autowired
	private val fileStore: IFileStore = null

	@Autowired
	private val nodeTypeStore: INodeTypeStore = null

	@Autowired
	private val nodeTypeValidator: NodeTypeConstraintValidator = null

	@Autowired
	private val sessionService: ISessionService = null

	override def revision: RepositoryRevisionNumber = metadataStore.currentRevision

	@PostConstruct
	private def init(): Unit = {
		// check to see that the root node exists and, if not, create it
		val rootNode = dataStore.node("/", revision)
		rootNode match {
			case Some(i: INode) =>
			case None =>
				val createOp = NodeCreationOperation(UUID.randomUUID().toString, "/", "tm:unstructured", null, null)
				val nextRevision = metadataStore.nextRevision
				val cs = new NodeDataChangeSet(nextRevision, createOp)
				LOG.debug(s"Initializing root node")
				dataStore.applyChangeSet(cs)
				metadataStore.commitNextRevision()
				LOG.debug("Done")
		}
		dataStore.pruneNodesUpToRevision(revision)
	}

	/**
		* Stores transient nodes, that hold data that won't be persisted until their session is committed
		*/
	private val transientNodeMap = mutable.Map.empty[(String, String), TransientNode]

	/**
		* Used to perform fast lookups for nodes that hold references to other nodes
		*/
	private val referenceManager = new ReferenceManager

	/**
		* Tracks the revisions for each active session in order to inform the persistent store when node revisions can be pruned
		*/
	private val sessionRevisionMap = mutable.Map.empty[String, RepositoryRevisionNumber]

	override def createNode(sessionID: String, sessionRevision: RepositoryRevisionNumber, path: String, primaryNodeType: String, mixinNodeTypes: Set[String], properties: Map[String, Any]): INode = {
		require(sessionID != null, "Null session ID")
		require(sessionRevision != null, "Null session revision")
		require(path != null, "Null path")

		// validate the primary node type
		if (nodeTypeStore.getNodeTypeDefinition(NodeTypeIdentifier.fromString(primaryNodeType)).isEmpty)
			throw new RepositoryException(s"Unknown primary node type $primaryNodeType")

		// validate the mixin node types
		if (mixinNodeTypes != null) {
			for (mixType <- mixinNodeTypes) {
				if (nodeTypeStore.getNodeTypeDefinition(NodeTypeIdentifier.fromString(mixType)).isEmpty)
					throw new RepositoryException(s"Unknown mixin node type $mixType")
			}
		}

		sessionRevisionMap(sessionID) = sessionRevision
		if (path == "/") throw new RepositoryException("Cannot create root node")
		else if (transientNodeMap.contains((sessionID, path)) || dataStore.exists(path, sessionRevision)) throw new RepositoryException(s"Cannot create existing node $path")
		else {
			val pPath = parentPath(path)
			if (!transientNodeMap.contains((sessionID, pPath)) && !dataStore.exists(pPath, sessionRevision)) throw new RepositoryException(s"Cannot create node at path $path under nonexistent parent path $pPath")
		}
		val newTransientNode = new TransientNode(UUID.randomUUID().toString, sessionID, sessionRevision, path, primaryNodeType, mixinNodeTypes, properties)
		transientNodeMap((sessionID, path)) = newTransientNode

		if (properties != null) {
			val referenceProperties = properties.filter( (propTuple: (String, Any)) => propTuple._2.isInstanceOf[StoredReference])
			for (refTuple <- referenceProperties) {
				refTuple._2 match {
					case sr: StoredStrongReference => referenceManager.addStrongReferenceTo((sessionID, sr.targetNodePath), sr)
					case wr: StoredWeakReference => referenceManager.addWeakReferenceTo((sessionID, wr.targetNodePath), wr)
				}
			}
		}

		newTransientNode
	}

	override def node(sessionID: String, sessionRevision: RepositoryRevisionNumber, path: String): Option[INode] = {
		require(sessionID != null, "Null session ID")
		require(sessionRevision != null, "Null session revision")
		require(path != null, "Null path")
		sessionRevisionMap(sessionID) = sessionRevision
		val transientNode = transientNodeMap.get((sessionID, path))
		transientNode match {
			case s @ Some(n: TransientNode) => if (n.isDeleted) None else s
			case None =>
				LOG.debug(s"Transient node $path not found for session $sessionID, retrieving from persistent store")
				val persistentNode = dataStore.node(path, sessionRevision)
				persistentNode match {
					case Some(n: INode) =>
						val newTransientNode = new TransientNode(n.id, sessionID, sessionRevision, path, n.primaryNodeType, n.mixinNodeTypes, Map.empty[String, Any])
						newTransientNode.persistentNode = Some(n)
						transientNodeMap.put((sessionID, path), newTransientNode)
						Some(newTransientNode)
					case None => None
				}
		}
	}

	override def deleteNode(sessionID: String, sessionRevision: RepositoryRevisionNumber, path: String): Unit = {
		sessionRevisionMap(sessionID) = sessionRevision
		deleteNode(sessionID, sessionRevision, path, recurse = true)
	}

	private def deleteNode(sessionID: String, sessionRevision: RepositoryRevisionNumber, path: String, recurse: Boolean): Unit = {
		sessionRevisionMap(sessionID) = sessionRevision
		if (path == "/") throw new RepositoryException("Cannot delete root node")
		val targetNodeTuple = (sessionID, path)

		val strongReferences = strongReferencesTo(sessionID, sessionRevision, path)
		if (strongReferences.isDefined) throw new RepositoryReferentialIntegrityException(s"Cannot delete node $path with ${strongReferences.get.size} strong references pointing to it.")
		val delnode = node(sessionID, sessionRevision, path)

		val weakReferences = weakReferencesTo(sessionID, sessionRevision, path)
		if (weakReferences.isDefined) {
			for (wr <- weakReferences.get) {
				LOG.debug(s"Removing weak reference $wr")
				val sourceNode = node(sessionID, sessionRevision, wr.sourceNodePath)
				sourceNode match {
					case Some(tn: TransientNode) =>
						tn(wr.propertyName) = null
						referenceManager.removeWeakReferenceFrom(targetNodeTuple, wr)
					case Some(in: INode) => throw new RepositoryException(s"Unexpectedly encountered persistent node ${wr.sourceNodePath} instead of transient node.")
					case None => throw new RepositoryException(s"Could not remove weak reference property ${wr.propertyName} from missing transient source node ${wr.sourceNodePath}")
				}
			}
		} else {
			LOG.debug(s"No weak references defined for target path $path")
		}

		delnode match {
			case Some(tn: TransientNode) =>
				tn.isDeleted = true
				val binaryReferences = tn.properties.filter((keyVal: (String, Any)) => keyVal._2.isInstanceOf[BinaryReference])
				for ((propertyName, value) <- binaryReferences) {
					fileStore.deleteFile(sessionID, sessionRevision, s"${tn.path}/$propertyName")
			}
			case Some(n: INode) => throw new RepositoryException(s"Found unexpected non-transient node $n")
			case None => throw new RepositoryException(s"Cannot delete nonexistent node $path")
		}

		if (recurse) {
			val childPaths = nodeChildPaths(sessionID, sessionRevision, path)
			for (childPath <- childPaths) deleteNode(sessionID, sessionRevision, childPath, recurse = false)
		}

		LOG.debug(s"Deleted node $path on session $sessionID at revision $sessionRevision")
	}

	override def parentNode(sessionID: String, sessionRevision: RepositoryRevisionNumber, path: String): INode = {
		sessionRevisionMap(sessionID) = sessionRevision
		if (path == "/") throw new RepositoryException("Cannot get parent node for root node")
		val parentNode = node(sessionID, sessionRevision, parentPath(path))
		parentNode match {
			case Some(n: INode) => n
			case None => throw new RepositoryException(s"Cannot locate parent node for node $path")
		}
	}

	override def nodeChildPaths(sessionID: String, sessionRevision: RepositoryRevisionNumber, path: String): Seq[String] = {
		sessionRevisionMap(sessionID) = sessionRevision
		val permChildPaths = dataStore.nodeChildPaths(sessionID, sessionRevision, path)
		val (existingChildPaths, removedChildPaths) = {
			val prefix = if (!path.endsWith("/")) s"$path/" else path
			LOG.debug(s"Searching for node prefix $prefix through keys ${transientNodeMap.keySet}")
			val (existingPaths, removedPaths) = transientNodeMap.view.filter(
				(keyVal: ((String, String), TransientNode)) => keyVal._1._1 == sessionID &&  keyVal._1._2.startsWith(prefix) && keyVal._1._2 != prefix)
				.partition(keyVal => !keyVal._2.isDeleted)

			( existingPaths.map(keyVal => keyVal._1._2).toSeq.sorted, removedPaths.map(keyVal => keyVal._1._2).toSeq.sorted )
		}
		(permChildPaths union existingChildPaths) diff removedChildPaths
	}

	private def joinReferences[T](transientRefs: Option[Set[T]], persistentRefs: Option[Set[T]]): Option[Set[T]] = {
		(transientRefs, persistentRefs) match {
			case (t @ Some(ts: Set[T]), p @ Some(ps: Set[T])) =>
				LOG.debug(s"Joining ${ts.size} transient and ${ps.size} persistent references.")
				Some(ts ++ ps)
			case (t @ Some(ts: Set[T]), None) =>
				LOG.debug("Returning only transient references.")
				Some(ts)
			case (None, p @ Some(ps: Set[T])) =>
				LOG.debug("Returning only persistent references.")
				Some(ps)
			case (None, None) =>
				LOG.debug("Found no transient or persistent references.")
				None
		}
	}

	def strongReferencesTo(sessionID: String, sessionRevision: RepositoryRevisionNumber, targetNodePath: String): Option[Set[StoredStrongReference]] = {
		sessionRevisionMap(sessionID) = sessionRevision
		LOG.debug(s"Seeking strong references to $targetNodePath")
		joinReferences[StoredStrongReference](referenceManager.strongReferencesTo((sessionID, targetNodePath)), dataStore.strongReferencesTo(targetNodePath))
	}

	def weakReferencesTo(sessionID: String, sessionRevision: RepositoryRevisionNumber, targetNodePath: String): Option[Set[StoredWeakReference]] = {
		sessionRevisionMap(sessionID) = sessionRevision
		LOG.debug(s"Seeking weak references to $targetNodePath")
		joinReferences[StoredWeakReference](referenceManager.weakReferencesTo((sessionID, targetNodePath)), dataStore.weakReferencesTo(targetNodePath))
	}

	def refresh(sessionID: String, keepChanges: Boolean): RepositoryRevisionNumber = {
		val retRevision = metadataStore.currentRevision
		if (!keepChanges) {
			clearDataForSession(sessionID)
		}
		sessionRevisionMap(sessionID) = retRevision
		retRevision
	}

	override def commit(sessionID: String): RepositoryRevisionNumber = commit(sessionID, asyncIndex = true)

	override def commit(sessionID: String, asyncIndex: Boolean): RepositoryRevisionNumber = {
		LOG.info(s"Committing on current revision $revision")
		val (changeSet, nodes) = prepareChangeSet(sessionID)
		metadataStore.saveChangeSet(changeSet)
		executeChangeSet(sessionID, changeSet)
		metadataStore.removeChangeSet(changeSet)
		val retRevision = metadataStore.currentRevision
		LOG.info(s"Returning new revision $retRevision")
		sessionRevisionMap(sessionID) = retRevision

		// update all affected nodes with the new repository revision number
		for (node <- nodes) node.setRevision(retRevision)
		searchService.indexNodes(nodes)

		retRevision
	}

	override protected[store] def prepareChangeSet(sessionID: String): (NodeDataChangeSet, Seq[INode]) = {
		val nodes = transientNodeMap.filterKeys((tuple: (String, String)) => tuple._1 == sessionID).values.toSeq
		val (liveNodes, deletedNodes) = nodes.partition((node: TransientNode) => !node.isDeleted)

		// perform node type checks on live nodes before building the changeset
		val session = sessionService.getSession(sessionID)
		for (baseNode <- liveNodes) {
			val sessionNode = new SessionNode(session, baseNode)
			nodeTypeValidator.validate(sessionNode)
		}

		val createdNodes = liveNodes.filter((node: TransientNode) => node.persistentNode.isEmpty)
		val modifiedNodes = liveNodes.filter((node: TransientNode) => node.isModified)

		val deleteOperations = deletedNodes.map(node => NodeDeletionOperation(node.id, node.path, node.revision))

		val createOperations = createdNodes.map(node => NodeCreationOperation(node.id, node.path, node.primaryNodeType, node.mixinNodeTypes, node.properties))
		val updateOperations = modifiedNodes.map(node => {
			val props = node.properties
			val (existingProps, nonExistingProps) = props.partition( (tuple: (String, Any)) => tuple._2 != null)
			NodeChangeOperation(node.path, node.mixinNodeTypes, existingProps, nonExistingProps)
		})

		val rev = metadataStore.nextRevision
		val cs = new NodeDataChangeSet(rev)
		cs ++= createOperations
		cs ++= updateOperations
		cs ++= deleteOperations
		(cs, nodes)
	}

	override protected[store] def applyExistingChangeSets(): Unit = {
		throw new RuntimeException("not implemented")
	}

	override protected[store] def executeChangeSet(sessionID: String, cs: NodeDataChangeSet): Unit = {
		val nextRevision = metadataStore.nextRevision
		fileStore.prepareCommit(sessionID, nextRevision)
		dataStore.applyChangeSet(cs)
		fileStore.commit(sessionID, nextRevision)

		val createNodeOperations = cs.operations.filter(_.isInstanceOf[NodeCreationOperation]).map(op => op.asInstanceOf[NodeCreationOperation])
		val createNodePaths = createNodeOperations.map(_.nodePath)
		for (path <- createNodePaths) {
			val transientNode = transientNodeMap(sessionID, path)
			transientNode.persistentNode = dataStore.node(path, nextRevision)
		}

		val changeNodeOperations = cs.operations.filter(_.isInstanceOf[NodeChangeOperation]).map(op => op.asInstanceOf[NodeChangeOperation])
		val changeNodePaths = changeNodeOperations.map(_.nodePath)
		for (path <- changeNodePaths) {
			val transientNode = transientNodeMap(sessionID, path)
			transientNode.isModified = false
		}

		metadataStore.commitNextRevision()
	}

	override def sessionClosed(sessionID: String): Unit = {
		val sessionRevision = sessionRevisionMap(sessionID)
		clearDataForSession(sessionID)
		val sessionRevisionCount = sessionRevisionMap.count((keyVal: (String, RepositoryRevisionNumber)) => keyVal._2 == sessionRevision)
		if (sessionRevisionCount == 0) {
			val sessionsBelowRevisionCount = sessionRevisionMap.count((keyVal: (String, RepositoryRevisionNumber)) => keyVal._2 < sessionRevision)
			if (sessionsBelowRevisionCount == 0) {
				LOG.debug(s"Closed the lowest current session revision $sessionRevision. Pruning up to that revision.")
				dataStore.pruneNodesUpToRevision(sessionRevision)
			} else {
				LOG.debug(s"Closed the last session at revision $sessionRevision but there are $sessionsBelowRevisionCount sessions below that revision. Pruning single revision.")
				dataStore.pruneNodesAtRevision(sessionRevision)
			}
		}
		referenceManager.sessionClosed(sessionID)
		fileStore.sessionClosed(sessionID)
	}

	/**
		* Clears all transient data for a session ID
		* @param sessionID the session ID for which transient data should be cleared
		*/
	private def clearDataForSession(sessionID: String): Unit = {
		val sessionKeys = transientNodeMap.keys.filter((key: (String, String)) => key._1 == sessionID)
		for (key <- sessionKeys) transientNodeMap.remove(key)
		sessionRevisionMap.remove(sessionID)
	}

	private def parentPath(nodePath: String): String = {
		val parentPathParts = nodePath.split("/").dropRight(1)
		if (parentPathParts.length == 1) "/" else parentPathParts.mkString("/")
	}

	override protected[repository] def reset(): Unit = {
		referenceManager.clear()
		transientNodeMap.clear()
		fileStore.clear()
		metadataStore.reset()
		dataStore.reset()
		searchService.reset()
		init()
	}

	/**
		* A node that is not persisted to permanent storage. Tracks changes to the "virtual" node at a given
		* path, and is used eventually to persist changes to permanent storage.
		*/
	protected[NodeStore] class TransientNode(nodeID: String, sessionID: String, sessionRevision: RepositoryRevisionNumber, _nodePath: String, _nodeType: String, _mixinNodeTypes: Set[String], startingProperties: Map[String, Any]) extends INode {

		var isDeleted = false
		var isModified = false

		var currentRevision = sessionRevision

		/**
			* The persistent node that is wrapped by this transient node.
			*/
		protected[NodeStore] var persistentNode: Option[INode] = None

		protected[NodeStore] val nodeMixinTypes = mutable.Set.empty[String]
		if (_mixinNodeTypes != null) nodeMixinTypes ++= _mixinNodeTypes

		/** Stores the new, changed, and deleted properties for this node. */
		private val transientProperties = mutable.Map.empty[String, Any]
		if (startingProperties != null && startingProperties.nonEmpty) {
			LOG.info(s"Adding new transient starting properties $startingProperties")
			for ((key, value) <- startingProperties) update(key, value)
		}


		override def id: String = nodeID

		override def update(propertyName: String, value: Any): Unit = {
			val existingValue = transientProperties.get(propertyName)
			LOG.debug(s"Existing property value for $propertyName is $existingValue")
			if (value != null) {
				existingValue match {
					case None =>
					case s @ Some(obj: Any) =>
						if (obj.getClass != value.getClass) throw new RepositoryException(s"Cannot change value type for property $propertyName for node ${_nodePath}")
				}
			}

			LOG.debug(s"Setting transient property $propertyName to $value")
			val valueToSave = value match {
				case is: InputStream =>
					fileStore.createFile(sessionID, revision, s"$path/$propertyName", is)
					new BinaryReference
				case sr: StoredStrongReference =>
					referenceManager.addStrongReferenceTo((sessionID, sr.targetNodePath), sr)
					LOG.debug(s"Cached strong reference to ${sr.targetNodePath} for session $sessionID from ${sr.sourceNodePath}, property ${sr.propertyName}")
					sr
				case wr: StoredWeakReference =>
					referenceManager.addWeakReferenceTo((sessionID, wr.targetNodePath), wr)
					LOG.debug(s"Cached weak reference to ${wr.targetNodePath} for session $sessionID from ${wr.sourceNodePath}, property ${wr.propertyName}")
					wr
				case null =>
						existingValue match {
							case s @ Some(br: BinaryReference) => fileStore.deleteFile(sessionID, sessionRevision, s"$path/$propertyName")
							case _ =>
						}
					  value
				case anythingelse => value
			}
			transientProperties(propertyName) = valueToSave
			if (persistentNode.isDefined) isModified = true
		}

		override def apply(propertyName: String): Option[Any] = {
			def propertyConversion(property: Option[Any]): Option[Any] = {
				property match {
					case Some(br: BinaryReference) =>
						val fileInputStreamOption = fileStore.getInputStreamForFile(sessionID, s"${_nodePath}/$propertyName")
						fileInputStreamOption match {
							case Some(is: InputStream) =>
								val binary = new IBinary {
									override def inputStream: InputStream = is
								}
								Some(binary)
							case None => throw new RuntimeException(s"Cannot locate file for binary property $propertyName for node ${_nodePath}")
						}
					case s @ Some(prop: Any) => s
					case sn @ Some(null) => None
					case None => None
				}
			}

			propertyConversion(transientProperties.get(propertyName)) match {
				case s @ Some(a: Any) => s
				case None =>
					persistentNode match {
						case p @ Some(n: INode) => propertyConversion(n(propertyName))
						case None => None
					}
			}
		}

		override def path: String = _nodePath

		override def name: String = if (_nodePath == "/") _nodePath else _nodePath.split("/").last

		override def revision: RepositoryRevisionNumber = currentRevision

		override def primaryNodeType: String = _nodeType

		override def mixinNodeTypes: Set[String] = nodeMixinTypes.toSet

		override def addMixinNodeType(mixinType: String): Unit = {
			nodeMixinTypes += mixinType
			if (persistentNode.isDefined) isModified = true
		}

		override def removeMixinNodeType(mixinType: String): Unit = {
			nodeMixinTypes -= mixinType
			if (persistentNode.isDefined) isModified = true
		}

		override def properties: Map[String, Any] = transientProperties.toMap

		override protected[repository] def setRevision(revision: RepositoryRevisionNumber): Unit = {
			currentRevision = revision
		}

		override def toString: String = s"TransientBaseNode[path=$path, primaryType=$primaryNodeType, mixinTypes=$mixinNodeTypes, sessionID=$sessionID, revision=$revision]"

	}

	/**
		* Keeps track of strong and weak references, both transient and persistent.
		*/
	protected[NodeStore] class ReferenceManager {
		/** Tracks strong references that point to nodes.
			* The key is a (sessionID, target node path) tuple and the value is a (sessionID, source node path, property name) tuple.
			*/
		val transientStrongReferenceTargetMap = mutable.Map.empty[(String, String), Set[StoredStrongReference]]
		val transientWeakReferenceTargetMap = mutable.Map.empty[(String, String), Set[StoredWeakReference]]

		def addStrongReferenceTo(targetNodeTuple: (String, String), ref: StoredStrongReference) {
			LOG.debug(s"Adding strong reference $ref to $targetNodeTuple")
			val refSet = transientStrongReferenceTargetMap.get(targetNodeTuple) match {
				case Some(s: Set[StoredStrongReference]) => s
				case None => Set.empty[StoredStrongReference]
			}
			transientStrongReferenceTargetMap(targetNodeTuple) = refSet + ref
		}

		def removeStrongReferenceFrom(targetNodeTuple: (String, String), ref: StoredStrongReference): Unit = {
			transientStrongReferenceTargetMap.get(targetNodeTuple) match {
				case Some(s: Set[StoredStrongReference]) => transientStrongReferenceTargetMap(targetNodeTuple) = s - ref
				case None =>
			}
		}

		def strongReferencesTo(targetNodeTuple: (String, String)): Option[Set[StoredStrongReference]] = transientStrongReferenceTargetMap.get(targetNodeTuple)

		def addWeakReferenceTo(targetNodeTuple: (String, String), ref: StoredWeakReference) {
			LOG.debug(s"Adding weak reference $ref to $targetNodeTuple")
			val refSet = transientWeakReferenceTargetMap.get(targetNodeTuple) match {
				case Some(s: Set[StoredWeakReference]) => s
				case None => Set.empty[StoredWeakReference]
			}
			transientWeakReferenceTargetMap(targetNodeTuple) = refSet + ref
		}

		def removeWeakReferenceFrom(targetNodeTuple: (String, String), ref: StoredWeakReference): Unit = {
			LOG.debug(s"Removing weak reference from $targetNodeTuple")
			transientWeakReferenceTargetMap.get(targetNodeTuple) match {
				case Some(s: Set[StoredWeakReference]) => transientWeakReferenceTargetMap(targetNodeTuple) = s - ref
				case None =>
					LOG.debug(s"No weak reference found from $targetNodeTuple to $ref")
			}
		}

		def weakReferencesTo(targetNodeTuple: (String, String)): Option[Set[StoredWeakReference]] = transientWeakReferenceTargetMap.get(targetNodeTuple)

		def sessionClosed(sessionID: String): Unit = {
			val strongRefSessionKeys = transientStrongReferenceTargetMap.keys.filter((key: (String, String)) => key._1 == sessionID)
			for (key <- strongRefSessionKeys) transientStrongReferenceTargetMap.remove(key)
			val weakRefSessionKeys = transientWeakReferenceTargetMap.keys.filter((key: (String, String)) => key._1 == sessionID)
			for (key <- weakRefSessionKeys) transientWeakReferenceTargetMap.remove(key)
		}

		def clear(): Unit = {
			transientStrongReferenceTargetMap.clear()
			transientWeakReferenceTargetMap.clear()
		}
	}
}
