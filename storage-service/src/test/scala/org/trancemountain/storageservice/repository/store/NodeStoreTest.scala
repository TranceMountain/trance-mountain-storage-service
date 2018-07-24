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

import java.nio.file.Files
import java.util.UUID

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
import org.trancemountain.storageservice.repository.RepositoryRevisionNumber
import org.trancemountain.storageservice.repository.search.IRepositorySearchService

import scala.language.postfixOps

/**
	* @author michaelcoddington
	*/
@RunWith(classOf[JUnitRunner])
@ContextConfiguration(loader = classOf[AnnotationConfigContextLoader],
	classes = Array(classOf[NodeStoreConfig]),
	initializers = Array(classOf[ConfigFileApplicationContextInitializer]))
class NodeStoreTest extends FlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with MockitoSugar {

	@Autowired
	val searchService: IRepositorySearchService = null

	@Autowired
	val nodeDataStore: INodeDataStore = null

	@Autowired
	val nodeMetadataStore: INodeMetadataStore = null

	@Autowired
	val nodeStore: INodeStore = null


	private val UNSTRUCTURED_NODE_TYPE = "tm:unstructured:1"

	override def beforeAll(): Unit = {
		// TODO: don't use an FS/Derby file store -- use memory
		val tmpFile = Files.createTempDirectory("tm-fs").toFile
		val dbFile = tmpFile.toPath.resolve("db").toFile
		val solrFile = tmpFile.toPath.resolve("solr").toFile

		System.setProperty(SpringConfigKeys.TM_BACKEND_DERBY_LOC, dbFile.getAbsolutePath)
		System.setProperty(SpringConfigKeys.TM_FILE_STORE_FS_LOC, tmpFile.getAbsolutePath)
		System.setProperty(SpringConfigKeys.TM_FILE_METADATA_STORE_TYPE, "derby")
		System.setProperty(SpringConfigKeys.TM_FILE_DATA_STORE_TYPE, "fs")
		System.setProperty(SpringConfigKeys.TM_NODE_METADATA_STORE_TYPE, "memory")
		System.setProperty(SpringConfigKeys.TM_NODE_DATA_STORE_TYPE, "memory")
		System.setProperty(SpringConfigKeys.TM_NODETYPE_DATA_STORE_TYPE, "memory")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_TYPE, "embedded")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_INDEXING_THREAD_COUNT, "10")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_EMBEDDED_CONTAINER_LOCATION, solrFile.getAbsolutePath)

		val mgr = new TestContextManager(this.getClass)
		mgr.prepareTestInstance(this)
	}

	override def afterEach(): Unit = {
		super.afterEach()
		nodeStore.reset()
	}

	private def randomNodeID = UUID.randomUUID().toString

	"A INodeStore" should "start at revision 1" in {
		nodeStore.revision should be(new RepositoryRevisionNumber(1))
	}

	it should "automatically create the root node on repository init" in {
		val rootNode = nodeStore.node("sessionA", new RepositoryRevisionNumber(1), "/")!;
		rootNode should not be null
	}

	it should "automatically index nodes on commit" in {
		val startCount = searchService.nodeDocumentCount
		val rev1 = nodeStore.revision
		nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE_TYPE, null)
		nodeStore.commit("sessionA")
		searchService.nodeDocumentCount shouldBe startCount + 1
	}

	it should "create a node revision change for new nodes" in {
		val rev1 = new RepositoryRevisionNumber(1)
		nodeStore.createNode("sessionA", rev1,  "/child1", UNSTRUCTURED_NODE_TYPE)
		val (cs, nodes) = nodeStore.prepareChangeSet("sessionA")
		cs.size should be(1)
		val op = cs(0)
		op shouldBe a[NodeCreationOperation]
		val create = op.asInstanceOf[NodeCreationOperation]
		create.nodePath should be("/child1")
		create.primaryNodeType should be(UNSTRUCTURED_NODE_TYPE)
		create.properties should be ('empty)
	}

	it should "create a node revision change for modified nodes" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val change = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE_TYPE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, change)
		nodeDataStore.applyChangeSet(changeSet)
		val node1 = nodeStore.node("sessionA", rev2, "/child1")!;
		node1("propA") = "hi there"
		val (cs, nodes) = nodeStore.prepareChangeSet("sessionA")
		cs.size should be(1)
		val op = cs(0)
		op shouldBe a[NodeChangeOperation]
		val changeOp = op.asInstanceOf[NodeChangeOperation]
		changeOp.nodePath should be("/child1")
		changeOp.addedOrChangedFields("propA") should be("hi there")
	}

	it should "create a node revision change for deleted nodes" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val change = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE_TYPE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, change)
		nodeDataStore.applyChangeSet(changeSet)
		nodeStore.deleteNode("sessionA", rev2, "/child1")
		val (cs, nodes) = nodeStore.prepareChangeSet("sessionA")
		cs.size should be(1)
		val op = cs(0)
		op shouldBe a[NodeDeletionOperation]
		val delOp = op.asInstanceOf[NodeDeletionOperation]
		delOp.nodePath should be("/child1")
	}

	it should "create a node revision change for a mixture of node changes" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val change1 = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE_TYPE, null, null)
		val change2 = NodeCreationOperation(randomNodeID, "/child2", UNSTRUCTURED_NODE_TYPE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, change1, change2)
		nodeDataStore.applyChangeSet(changeSet)
		val node1 = nodeStore.node("sessionA", rev2, "/child1")!;
		node1("propA") = "hi there"
		nodeStore.deleteNode("sessionA", rev2, "/child2")
		nodeStore.createNode("sessionA", rev2, "/child3", UNSTRUCTURED_NODE_TYPE, null)
		val (cs, nodes) = nodeStore.prepareChangeSet("sessionA")
		cs.size should be(3)
		val create = cs.operations.find(_.isInstanceOf[NodeCreationOperation]).get.asInstanceOf[NodeCreationOperation]
		val change = cs.operations.find(_.isInstanceOf[NodeChangeOperation]).get.asInstanceOf[NodeChangeOperation]
		val delete = cs.operations.find(_.isInstanceOf[NodeDeletionOperation]).get.asInstanceOf[NodeDeletionOperation]
		create should not be null
		change should not be null
		delete should not be null
		create.nodePath should be("/child3")
		create.primaryNodeType should be(UNSTRUCTURED_NODE_TYPE)
		create.properties should be ('empty)
		delete.nodePath should be("/child2")
		change.nodePath should be("/child1")
		change.addedOrChangedFields("propA") should be("hi there")
	}

	it should "write a node revision change to the node metadata store on commit" in {
		val rev1 = nodeStore.revision
		nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE_TYPE, null)
		nodeStore.commit("sessionA")
		nodeMetadataStore.currentRevision should be (rev1 + 1)
	}

	it should "apply a node revision change to the node data store" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val change1 = NodeCreationOperation(randomNodeID, "/child1", UNSTRUCTURED_NODE_TYPE, null, null)
		val change2 = NodeCreationOperation(randomNodeID, "/child2", UNSTRUCTURED_NODE_TYPE, null, null)
		val rev2 = rev1 + 1
		val changeSet = new NodeDataChangeSet(rev2, change1, change2)
		nodeDataStore.applyChangeSet(changeSet)
		val node1 = nodeStore.node("sessionA", rev2, "/child1")!;
		node1("propA") = "hi there"
		nodeStore.deleteNode("sessionA", rev2, "/child2")
		nodeStore.createNode("sessionA", rev2, "/child3", UNSTRUCTURED_NODE_TYPE, null)
		val rev3 = nodeStore.commit("sessionA")
		val child1 = nodeDataStore.node("/child1", rev3)
		child1.isDefined shouldBe true
		child1.get.properties("propA") shouldBe "hi there"
		nodeDataStore.exists("/child2", rev3) shouldBe false
		nodeDataStore.exists("/child3", rev3) shouldBe true
	}

	it should "increment its revision after a revision change set is applied" in {
		val rev1 = nodeStore.revision
		nodeStore.createNode("sessionA", rev1, "/child1", UNSTRUCTURED_NODE_TYPE, null)
		nodeStore.commit("sessionA")
		nodeStore.revision should be(rev1 + 1)
	}

}
