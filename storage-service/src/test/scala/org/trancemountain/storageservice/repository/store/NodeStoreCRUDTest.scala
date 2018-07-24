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

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.{Date, UUID}

import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import org.trancemountain.OptionalUnpacker
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.nodetype.NodeTypeConstraintViolationException
import org.trancemountain.storageservice.repository.{IBinary, RepositoryException, RepositoryReferentialIntegrityException, RepositoryRevisionNumber}

import scala.language.postfixOps

/**
	* @author michaelcoddington
	*/
@RunWith(classOf[JUnitRunner])
@ContextConfiguration(loader = classOf[AnnotationConfigContextLoader], classes = Array(classOf[NodeStoreConfig]), initializers = Array(classOf[ConfigFileApplicationContextInitializer]))
class NodeStoreCRUDTest extends FlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with MockitoSugar  {

	@Autowired
	private val nodeStore: INodeStore = null

	@Autowired
	private val nodeDataStore: INodeDataStore = null

	@Autowired
	private val nodeMetadataStore: INodeMetadataStore = null

	@Autowired
	private val fileStore: IFileStore = null

	@Autowired
	private val nodeTypeStore: INodeTypeStore = null

	private val UNSTRUCTURED_NODE = "tm:unstructured:1"

	override def beforeAll(): Unit = {
		// TODO: don't use an FS/Derby file store -- use memory
		val tmpFile = Files.createTempDirectory("tm-fs").toFile
		val dbFile = tmpFile.toPath.resolve("db").toFile
		val solrFile = tmpFile.toPath.resolve("solr").toFile

		System.setProperty(SpringConfigKeys.TM_BACKEND_DERBY_LOC, dbFile.getAbsolutePath)
		System.setProperty(SpringConfigKeys.TM_FILE_STORE_FS_LOC, tmpFile.getAbsolutePath)
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_EMBEDDED_CONTAINER_LOCATION, solrFile.getAbsolutePath)
		System.setProperty(SpringConfigKeys.TM_FILE_METADATA_STORE_TYPE, "derby")
		System.setProperty(SpringConfigKeys.TM_FILE_DATA_STORE_TYPE, "fs")
		System.setProperty(SpringConfigKeys.TM_NODE_METADATA_STORE_TYPE, "memory")
		System.setProperty(SpringConfigKeys.TM_NODE_DATA_STORE_TYPE, "memory")
		System.setProperty(SpringConfigKeys.TM_NODETYPE_DATA_STORE_TYPE, "memory")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_TYPE, "embedded")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_INDEXING_THREAD_COUNT, "10")

		val mgr = new TestContextManager(this.getClass)
		mgr.prepareTestInstance(this)
	}

	override def afterEach(): Unit = {
		super.afterEach()
		nodeStore.reset()
	}

	private def randomNodeID = UUID.randomUUID().toString

	"A INodeStore" should "retrieve a node from permanent storage" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val change = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, Set("mix:lockable"), null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, change)
		nodeDataStore.applyChangeSet(changeSet)
		val opt = nodeStore.node("sessionA", rev2, "/child1")
		opt should not be None
		val node = opt.get
		node.mixinNodeTypes shouldBe Set("mix:lockable")
	}

	it should "create a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		nodeStore.node("sessionA", rev1, "/child1") should not be null
	}

	it should "create a node with properties" in {
		val rev1 = new RepositoryRevisionNumber(1)
		nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null, Map("string" -> "str", "int" -> 1))
		val retNode = nodeStore.node("sessionA", rev1, "/child1").get
		retNode("string") shouldBe Some("str")
		retNode("int") shouldBe Some(1)
	}

	it should "disallow the creation of a node for a session if its parent doesn't exist" in {
		val rev1 = new RepositoryRevisionNumber(1)
		assertThrows[RepositoryException] {
			nodeStore.createNode("sessionA", rev1, "/child1/child2", UNSTRUCTURED_NODE)
		}
	}

	it should "disallow the creation of a node for a session if a transient node at that path exists" in {
		val rev1 = new RepositoryRevisionNumber(1)
		nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		assertThrows[RepositoryException] {
			nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		}
	}

	it should "disallow the creation of a node if a permanent node at that path exists" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		val nodeCreate = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, nodeCreate)
		nodeDataStore.applyChangeSet(changeSet)
		assertThrows[RepositoryException] {
			nodeStore.createNode(sessionID, rev2, "/child1", UNSTRUCTURED_NODE)
		}
	}

	it should "not allow the deletion of the root node" in {
		assertThrows[RepositoryException] {
			nodeStore.deleteNode("sessionA", new RepositoryRevisionNumber(1), "/")
		}
	}

	it should "delete the transient children of a transient node when the node is deleted" in {
		val rev1 = new RepositoryRevisionNumber(1)
		nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		nodeStore.createNode("sessionA", rev1, "/child1/child2", UNSTRUCTURED_NODE)
		nodeStore.createNode("sessionA", rev1, "/child1/child2/child3", UNSTRUCTURED_NODE)
		nodeStore.createNode("sessionA", rev1, "/child1/child3", UNSTRUCTURED_NODE)
		nodeStore.deleteNode("sessionA", rev1, "/child1")
		for (path <- Array("/child1", "/child1/child2", "/child1/child2/child3", "/child1/child3")) {
			nodeStore.node("sessionA", rev1, path) shouldBe None
		}
	}

	it should "delete the permanent children of a permanent node when the node is deleted" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val props = Map[String, Any]("prop1" -> 1, "prop2" -> 2)
		val create1 = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, props)
		val create2 = NodeCreationOperation(randomNodeID, "/child1/childA", UNSTRUCTURED_NODE, null, props)
		val create3 = NodeCreationOperation(randomNodeID, "/child1/childA/child3", UNSTRUCTURED_NODE, null, props)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, create1, create2, create3)
		nodeDataStore.applyChangeSet(changeSet)
		nodeStore.deleteNode("sessionA", rev2, "/child1")
		nodeStore.node("sessionA", rev2, "/child1") shouldBe None
		nodeStore.node("sessionA", rev2, "/child1/childA") shouldBe None
		nodeStore.node("sessionA", rev2, "/child1/childA/child3") shouldBe None
	}

	it should "delete the transient children of a permanent node when the node is deleted" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val props = Map[String, Any]("prop1" -> 1, "prop2" -> 2)
		val create1 = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, props)
		val create2 = NodeCreationOperation(randomNodeID, "/child1/childA", UNSTRUCTURED_NODE, null, props)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, create1, create2)
		nodeDataStore.applyChangeSet(changeSet)
		nodeStore.createNode("sessionA", rev2, "/child1/childA/child3", UNSTRUCTURED_NODE, null)
		nodeStore.deleteNode("sessionA", rev2, "/child1")
		nodeStore.node("sessionA", rev2, "/child1") shouldBe None
		nodeStore.node("sessionA", rev2, "/child1/childA") shouldBe None
		nodeStore.node("sessionA", rev2, "/child1/childA/child3") shouldBe None
	}

	it should "disallow the deletion of transient child nodes that are targets of strong references" in {
		val rev1 = new RepositoryRevisionNumber(1)
		nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		nodeStore.createNode("sessionA", rev1, "/child1/child2", UNSTRUCTURED_NODE)
		nodeStore.createNode("sessionA", rev1, "/child1/child2/child3", UNSTRUCTURED_NODE)
		nodeStore.createNode("sessionA", rev1, "/child1/child3", UNSTRUCTURED_NODE, null, Map[String, Any]("strongRef" -> StoredStrongReference("/child1/child3", "strongRef", "/child1/child2")))
		assertThrows[RepositoryReferentialIntegrityException] {
			nodeStore.deleteNode("sessionA", rev1, "/child1")
		}
	}

	it should "disallow the deletion of permanent child nodes that are targets of strong references" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val child1 = "/child1"
		val child1a = "/child1/childA"
		val child2 = "/child2"
		val create1 = NodeCreationOperation(randomNodeID, child1, UNSTRUCTURED_NODE, null, null)
		val create2 = NodeCreationOperation(randomNodeID, child1a, UNSTRUCTURED_NODE, null, null)
		val create3 = NodeCreationOperation(randomNodeID, child2, UNSTRUCTURED_NODE, null, Map[String, Any]("strongRef" -> StoredStrongReference(child2, "strongRef", child1a)))

		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, create1, create2, create3)
		nodeDataStore.applyChangeSet(changeSet)

		assertThrows[RepositoryReferentialIntegrityException] {
			nodeStore.deleteNode("sessionA", rev2, child1)
		}
	}

	it should "access properties that came from an underlying node in permanent storage" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val props = Map[String, Any]("prop1" -> 1, "prop2" -> 2)
		val change = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, props)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, change)
		nodeDataStore.applyChangeSet(changeSet)
		val opt = nodeStore.node("sessionA", rev2, "/child1")
		opt.isDefined shouldBe true
		val node = opt!;
		(node("prop1")!) shouldBe 1
		(node("prop2")!) shouldBe 2
	}

	it should "set a string property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		node("prop1") = "string"
	}

	it should "set a string array property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		node("prop1") = Array("s1", "s2")
	}

	it should "set an int property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		node("prop1") = 1
	}

	it should "set an int array property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		node("prop1") = Array(1, 2, 3)
	}

	it should "set a long property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		node("prop1") = 1L
	}

	it should "set a long array property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		node("prop1") = Array(1L, 2L)
	}

	it should "set a double property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		node("prop1") = 1.0
	}

	it should "set a double array property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		node("prop1") = Array(1.0, 2.0)
	}

	it should "set a boolean property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		node("prop1") = true
	}

	it should "set a date property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		node("prop1") = new Date()
	}

	it should "set a binary property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		val bytes = Array[Byte](1, 2, 3)
		node("prop1") = new ByteArrayInputStream(bytes)
	}

	it should "commit a binary property to permanent file storage when a session is committed" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		val bytes = Array[Byte](1, 2, 3)
		node("prop1") = new ByteArrayInputStream(bytes)
		nodeStore.commit("sessionA")
		val committedFile = fileStore.committedFile("/child1/prop1")
		committedFile.isDefined shouldBe true
	}

	it should "not remove a binary property from permanent file storage if there are any remaining nodes using it" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		val bytes = Array[Byte](1, 2, 3)
		node("prop1") = new ByteArrayInputStream(bytes)
		nodeStore.commit("sessionA")
		val node2 = nodeStore.createNode("sessionB", rev1, "/child2", UNSTRUCTURED_NODE)
		node2("prop1") = new ByteArrayInputStream(bytes)
		nodeStore.commit("sessionB")
		val committedFile1 = fileStore.committedFile("/child1/prop1")
		committedFile1.isDefined shouldBe true
		val committedFile2 = fileStore.committedFile("/child2/prop1")
		committedFile2.isDefined shouldBe true
		nodeStore.deleteNode("sessionA", rev1 + 1, "/child1")
		nodeStore.commit("sessionA")
		val committedFile1b = fileStore.committedFile("/child1/prop1")
		committedFile1b.isDefined shouldBe false
		val committedFile2b = fileStore.committedFile("/child2/prop1")
		committedFile2b.isDefined shouldBe true
	}

	it should "remove a binary property from permanent file storage when the last node to use it no longer references it" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		val bytes = Array[Byte](1, 2, 3)
		node("prop1") = new ByteArrayInputStream(bytes)
		nodeStore.commit("sessionA")
		val committedFile = fileStore.committedFile("/child1/prop1")
		committedFile.isDefined shouldBe true
		nodeStore.deleteNode("sessionA", rev1 + 1, "/child1")
		nodeStore.commit("sessionA")
		val rev2CommittedFile = fileStore.committedFile("/child1/prop1")
		rev2CommittedFile.isDefined shouldBe false
	}

	it should "remove a binary property from permanent file storage when the property is set to null" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		val bytes = Array[Byte](1, 2, 3)
		node("prop1") = new ByteArrayInputStream(bytes)
		nodeStore.commit("sessionA")
		val committedFile = fileStore.committedFile("/child1/prop1")
		committedFile.isDefined shouldBe true
		node("prop1") = null
		nodeStore.commit("sessionA")
		val committedFile2 = fileStore.committedFile("/child1/prop1")
		committedFile2.isDefined shouldBe false
	}

	it should "get a string property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null)
		node1("prop1") = "hi"
		(node1("prop1")!) shouldBe "hi"
	}

	it should "get a string array property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null)
		node1("prop1") = Array("hi", "there")
		(node1("prop1")!) shouldBe Array("hi", "there")
	}

	it should "get an int property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null)
		node1("prop1") = 42
		(node1("prop1")!) shouldBe 42
	}

	it should "get an int array property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null)
		node1("prop1") =  Array[Int](5, 6)
		(node1("prop1")!) shouldBe Array[Int](5, 6)
	}

	it should "get a long property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null)
		node1("prop1") = 2L
		(node1("prop1")!) shouldBe 2L
	}

	it should "get a long array property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null)
		node1("prop1") = Array(1L, 2L)
		(node1("prop1")!) shouldBe Array(1L, 2L)
	}

	it should "get a boolean property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null)
		node1("prop1") = false
		(node1("prop1")!) shouldBe false
	}

	it should "get a date property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null)
		val d = new Date()
		node1("prop1") = d
		(node1("prop1")!) shouldBe d
	}

	it should "get a binary property for a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null)
		val bytes = Array[Byte](1, 2, 3)
		node1("prop1") = new ByteArrayInputStream(bytes)
		val bin = node1("prop1")!
		val stream = bin.asInstanceOf[IBinary].inputStream
		val compBytes = IOUtils.toByteArray(stream)
		compBytes shouldBe bytes
	}

	it should "not allow the deletion of a transient node with a strong reference pointing to it" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		nodeStore.createNode("sessionA", rev1, "/child2", UNSTRUCTURED_NODE)
		node1("strongRef") = StoredStrongReference("/child1", "strongRef", "/child2")
		assertThrows[RepositoryException] {
			nodeStore.deleteNode("sessionA", rev1, "/child2")
		}
	}

	it should "remove any weak reference properties from transient nodes when the targeted transient node is removed" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		nodeStore.createNode("sessionA", rev1, "/child2", UNSTRUCTURED_NODE)
		node1("weakRef") = StoredWeakReference("/child1", "weakRef", "/child2")
		nodeStore.deleteNode("sessionA", rev1, "/child2")
		node1("weakRef") shouldBe None
	}

	it should "set a strong reference property for a transient node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		val node2 = nodeStore.createNode("sessionA", rev1, "/child2", UNSTRUCTURED_NODE)
		node1("strongRef") = StoredStrongReference("/child1", "strongRef", "/child2")
		val sr = node1("strongRef")
		sr.isDefined shouldBe true
		sr.get shouldBe StoredStrongReference("/child1", "strongRef", "/child2")
	}

	it should "set a weak reference property for a transient node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		val node2 = nodeStore.createNode("sessionA", rev1, "/child2", UNSTRUCTURED_NODE)
		node1("weakRef") = StoredWeakReference("/child1", "weakRef", "/child2")
		val wr = node1("weakRef")
		wr.isDefined shouldBe true
		wr.get shouldBe StoredWeakReference("/child1", "weakRef", "/child2")
	}

	it should "not allow the deletion of a permanent node with a transient node's strong reference pointing to it" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val op = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, op)
		nodeDataStore.applyChangeSet(changeSet)
		val node2 = nodeStore.createNode("sessionA", rev2, "/child2", UNSTRUCTURED_NODE)
		node2("strongRef") = StoredStrongReference("/child2", "strongRef", "/child1")
		assertThrows[RepositoryReferentialIntegrityException] {
			nodeStore.deleteNode("sessionA", rev2, "/child1")
		}
	}

	it should "remove any weak reference properties from transient nodes when the targeted permanent node is removed" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val op = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, op)
		nodeDataStore.applyChangeSet(changeSet)
		val node2 = nodeStore.createNode("sessionA", rev2, "/child2", UNSTRUCTURED_NODE)
		node2("weakRef") = StoredWeakReference("/child2", "weakRef", "/child1")
		nodeStore.deleteNode("sessionA", rev2, "/child1")
		nodeStore.commit("sessionA")
		node2("weakRef") shouldBe None
	}

	it should "get the transient nodes that strongly reference a given transient node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null)
		nodeStore.createNode("sessionA", rev1, "/child2", UNSTRUCTURED_NODE, null, Map[String, Any]("strongRef" -> StoredStrongReference("/child2", "strongRef", "/child1")))
		nodeStore.strongReferencesTo("sessionA", rev1, "/child1") shouldBe Some(Set(StoredStrongReference("/child2", "strongRef", "/child1")))
	}

	it should "get the transient nodes that weakly reference a given transient node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, null)
		nodeStore.createNode("sessionA", rev1, "/child2", UNSTRUCTURED_NODE, null, Map[String, Any]("weakRef" -> StoredWeakReference("/child2", "weakRef", "/child1")))
		nodeStore.weakReferencesTo("sessionA", rev1, "/child1") shouldBe Some(Set(StoredWeakReference("/child2", "weakRef", "/child1")))
	}

	it should "get the persistent nodes that strongly reference a given transient node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val props = Map[String, Any]("strongRef" -> StoredStrongReference("/child1", "strongRef", "/child2"))
		val op = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, props)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, op)
		nodeDataStore.applyChangeSet(changeSet)
		nodeStore.strongReferencesTo("sessionA", rev1, "/child2") shouldBe Some(Set(StoredStrongReference("/child1", "strongRef", "/child2")))
	}

	it should "get the persistent nodes that weakly reference a given transient node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val props = Map[String, Any]("weakRef" -> StoredWeakReference("/child1", "weakRef", "/child2"))
		val op = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, props)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, op)
		nodeDataStore.applyChangeSet(changeSet)
		nodeStore.weakReferencesTo("sessionA", rev2, "/child2") shouldBe Some(Set(StoredWeakReference("/child1", "weakRef", "/child2")))
	}

	it should "not allow a change to the data type of an existing property" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val node1 = nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE)
		node1("prop1") = 1
		assertThrows[RepositoryException] {
			node1("prop1") = "s"
		}
	}

	it should "get the transient children of a transient node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		nodeStore.createNode(sessionID, rev1, "/child1", UNSTRUCTURED_NODE)
		nodeStore.createNode(sessionID, rev1, "/child1/child2", UNSTRUCTURED_NODE)
		val paths = nodeStore.nodeChildPaths(sessionID, rev1, "/child1")
		paths.size shouldBe 1
		paths should contain ("/child1/child2")
	}

	it should "not get the deleted transient children of a transient node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		nodeStore.createNode(sessionID, rev1, "/child1", UNSTRUCTURED_NODE)
		nodeStore.createNode(sessionID, rev1, "/child1/child2", UNSTRUCTURED_NODE)
		nodeStore.createNode(sessionID, rev1, "/child1/child3", UNSTRUCTURED_NODE)
		nodeStore.deleteNode(sessionID, rev1, "/child1/child2")
		val paths = nodeStore.nodeChildPaths(sessionID, rev1, "/child1")
		paths.size shouldBe 1
		paths should contain ("/child1/child3")
	}

	it should "get the transient children of a permanent node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		val nodeCreate = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, nodeCreate)
		nodeDataStore.applyChangeSet(changeSet)
		nodeStore.createNode(sessionID, rev2, "/child1/child2", UNSTRUCTURED_NODE)
		val paths = nodeStore.nodeChildPaths(sessionID, rev2, "/child1")
		paths.size shouldBe 1
		paths should contain ("/child1/child2")
	}

	it should "get the transient and permanent children of a permanent node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		val nodeCreate1 = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, null)
		val nodeCreate2 = NodeCreationOperation(randomNodeID, "/child1/child2", UNSTRUCTURED_NODE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, nodeCreate1, nodeCreate2)
		nodeDataStore.applyChangeSet(changeSet)
		nodeStore.createNode(sessionID, rev2, "/child1/child3", UNSTRUCTURED_NODE)
		val paths = nodeStore.nodeChildPaths(sessionID, rev2, "/child1")
		paths.size shouldBe 2
		paths should contain ("/child1/child2")
		paths should contain ("/child1/child3")
	}

	it should "get the parent of a node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		nodeStore.createNode("sessionA", rev1,  "/child1", UNSTRUCTURED_NODE)
		val parentNode = nodeStore.parentNode("sessionA", rev1, "/child1")
		parentNode.path shouldBe "/"
	}

	it should "delete a node for a session" in {
		val rev1 = new RepositoryRevisionNumber(1)
		nodeStore.createNode("sessionA", rev1,  "/child1", UNSTRUCTURED_NODE)
		nodeStore.deleteNode("sessionA", rev1, "/child1")
		nodeStore.node("sessionA", rev1, "/child1") shouldBe None
	}

	it should "delete a permanent node that hasn't yet been loaded" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val change = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, change)
		nodeDataStore.applyChangeSet(changeSet)
		nodeStore.deleteNode("sessionA", rev2, "/child1")
	}

	it should "only allow a session to see permanent nodes at or below that session's revision number" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val rev2 = rev1 + 1
		val rev3 = rev2 + 1
		val change1 = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, null)
		val cs1 = new NodeDataChangeSet(rev2, change1)
		nodeDataStore.applyChangeSet(cs1) // child1 now exists at revision 2
		val change2 = NodeCreationOperation(randomNodeID, "/child2", UNSTRUCTURED_NODE, null, null)
		val cs2 = new NodeDataChangeSet(rev3, change2)
		nodeDataStore.applyChangeSet(cs2) // child2 now exists at revision 3
		val childPaths = nodeStore.nodeChildPaths("sessionB", rev2, "/")
		childPaths.length should be(1)
		childPaths.head shouldBe "/child1"
		nodeStore.node("sessionB", rev2, "/child2") shouldBe None
	}

	it should "add a new transient mixin node type to a transient node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		val node = nodeStore.createNode(sessionID, rev1, "/child1", UNSTRUCTURED_NODE) // no mixins yet
		node.addMixinNodeType("mix:lockable:1")
		node.mixinNodeTypes shouldBe Set("mix:lockable:1")
	}

	it should "add a new transient mixin node type to a persistent node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		val nodeCreate = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, nodeCreate)
		nodeDataStore.applyChangeSet(changeSet)
		val node = nodeStore.node(sessionID, rev2, "/child1").get
		node.addMixinNodeType("mix:lockable:1")
		node.mixinNodeTypes shouldBe Set("mix:lockable:1")
		val persistentNode = nodeDataStore.node("/child1", rev2).get
		persistentNode.mixinNodeTypes shouldBe Set.empty[String]
	}

	it should "transiently remove a mixin node type from a persistent node" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		val nodeCreate = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, Set("mix:lockable:1"), null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, nodeCreate)
		nodeDataStore.applyChangeSet(changeSet)
		val node = nodeStore.node(sessionID, rev2, "/child1").get
		node.removeMixinNodeType("mix:lockable:1")
		node.mixinNodeTypes shouldBe Set.empty[String]
		val persistentNode = nodeDataStore.node("/child1", rev2).get
		persistentNode.mixinNodeTypes shouldBe Set("mix:lockable:1")
	}

	it should "persist a new transient mixin node type when the node is persisted" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		val nodeCreate = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, nodeCreate)
		nodeDataStore.applyChangeSet(changeSet)
		val node = nodeStore.node(sessionID, rev2, "/child1").get
		node.addMixinNodeType("mix:lockable:1")
		node("locked") = true
		node("lockedBy") = "joeblow"
		val rev3 = nodeStore.commit(sessionID)
		val persistentNode = nodeDataStore.node("/child1", rev3).get
		persistentNode.mixinNodeTypes shouldBe Set("mix:lockable:1")
	}

	it should "not allow the creation of a node with an invalid primary node type" in {
		val rev1 = new RepositoryRevisionNumber(1)
		assertThrows[RepositoryException] {
			nodeStore.createNode("sessionA", rev1, "/child1", "tm:bogus:1")
		}
	}

	it should "not allow the creation of a node with an invalid mixin node type" in {
		val rev1 = new RepositoryRevisionNumber(1)
		assertThrows[RepositoryException] {
			nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE, Set("tm:not_a_mixin:1"))
		}
	}

	it should "not allow a node to be persisted if its properties violate its primary node type definition" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		nodeStore.createNode(sessionID, rev1, "/file", "tm:file:1", Set(), Map("badprop" -> "blah"))
		assertThrows[NodeTypeConstraintViolationException] {
			nodeStore.commit(sessionID)
		}
	}

	it should "not allow a node to be persisted if its children violate its primary node type definition" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		nodeStore.createNode(sessionID, rev1, "/file", "tm:folder:1", Set())
		nodeStore.createNode(sessionID, rev1, "/file/child", UNSTRUCTURED_NODE, Set())
		assertThrows[NodeTypeConstraintViolationException] {
			nodeStore.commit(sessionID)
		}
	}

	it should "not allow a node to be persisted if its properties violate one of its mixin node type definitions" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		nodeStore.createNode(sessionID, rev1, "/file", UNSTRUCTURED_NODE, Set("mix:lockable:1"), Map("lockable" -> true, "badprop" -> "blah"))
		assertThrows[NodeTypeConstraintViolationException] {
			nodeStore.commit(sessionID)
		}
	}

	it should "not allow a node to be persisted if its children violate one of its mixin node type definitions" in {
		val mixinWithChildren =
			"""nodetype: "mix:test:1"
				|mixin: true
				|children:
				|  - name: child1
				|    nodetype: "tm:folder:1"
				|    minCount: 0""".stripMargin
		nodeTypeStore.createNodeType(mixinWithChildren)
		val rev1 = new RepositoryRevisionNumber(1)
		val sessionID = "sessionA"
		nodeStore.createNode(sessionID, rev1, "/test", "tm:folder:1", Set("mix:test:1"))
		nodeStore.createNode(sessionID, rev1, "/test/child1", UNSTRUCTURED_NODE, Set())
		assertThrows[NodeTypeConstraintViolationException] {
			nodeStore.commit(sessionID)
		}
	}


}
