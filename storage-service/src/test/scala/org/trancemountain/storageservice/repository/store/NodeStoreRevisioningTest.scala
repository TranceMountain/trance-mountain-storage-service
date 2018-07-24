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
import java.util
import java.util.concurrent.{Callable, ExecutorService, Executors}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.RepositoryRevisionNumber
import org.trancemountain.OptionalUnpacker

import scala.language.postfixOps

/**
	* @author michaelcoddington
	*/
@RunWith(classOf[JUnitRunner])
@ContextConfiguration(loader = classOf[AnnotationConfigContextLoader], classes = Array(classOf[NodeStoreConfig]), initializers = Array(classOf[ConfigFileApplicationContextInitializer]))
class NodeStoreRevisioningTest extends FlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with MockitoSugar {

	@Autowired
	val nodeStore: INodeStore = null

	@Autowired
	val nodeDataStore: INodeDataStore = null

	@Autowired
	val nodeMetadataStore: INodeMetadataStore = null

	private var executor: ExecutorService = _

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
		System.setProperty(SpringConfigKeys.TM_NODETYPE_DATA_STORE_TYPE, "memory")
		System.setProperty(SpringConfigKeys.TM_NODE_DATA_STORE_TYPE, "memory")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_TYPE, "embedded")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_INDEXING_THREAD_COUNT, "10")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_EMBEDDED_CONTAINER_LOCATION, solrFile.getAbsolutePath)

		val mgr = new TestContextManager(this.getClass)
		mgr.prepareTestInstance(this)
	}

	override def beforeEach(): Unit = {
		executor = Executors.newFixedThreadPool(10)
	}

	override def afterEach(): Unit = {
		super.afterEach()
		executor.shutdown()
		nodeStore.reset()
	}

	private def runConcurrently(funcs: Seq[()=> Unit]): Unit = {
		val callableList = new util.ArrayList[Callable[Unit]]
		for (func <- funcs) {
			val c = new Callable[Unit] {
				override def call() { func() }
			}
			callableList.add(c)
		}
		executor.invokeAll(callableList)
	}

	"A INodeStore" should "only allow a session to access nodes at or below that session's revision number" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val rev2 = new RepositoryRevisionNumber(2)
		val sessionA = "sessionA"
		val sessionB = "sessionB"
		nodeStore.createNode(sessionA, rev1, "/child1", UNSTRUCTURED_NODE_TYPE, null)
		nodeStore.commit("sessionA")
		// the repo should now be at revision 2
		nodeStore.revision shouldBe rev2
		// session B, at revision 1, should not see /child1
		nodeStore.node(sessionB, rev1, "/child1") shouldBe None
		// session A, at revision 2, should see /child1
		val rev1Node = nodeStore.node(sessionA, rev2, "/child1")
		rev1Node.isDefined shouldBe true
	}

	it should "maintain a consistent view of persistent nodes for a session at a given revision" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val rev2 = rev1 + 1
		val rev3 = rev2 + 1
		val sessionA = "sessionA"
		val sessionB = "sessionB"
		nodeStore.revision shouldBe rev1
		nodeStore.createNode(sessionA, rev1, "/child1", UNSTRUCTURED_NODE_TYPE, null)
		nodeStore.commit(sessionA) shouldBe rev2 // session A should be at revision 2 now
		nodeDataStore.node("/child1", rev2).isDefined shouldBe true
		val nodea = nodeStore.node(sessionA, rev2, "/child1")
		val nodeb = nodeStore.node(sessionB, rev2, "/child1")!;
		nodeb("prop1") = "hi"
		nodeStore.commit("sessionB") // session B should be at revision 3 now
		nodeStore.node(sessionB, rev3, "/child1").get("prop1") shouldBe Some("hi")
		nodeStore.node(sessionA, rev2, "/child1").get("prop1") shouldBe None
	}

	it should "maintain an older persistent version of a node until all sessions at that revision are closed" in {
		val rev1 = new RepositoryRevisionNumber(1)
		val rev2 = rev1 + 1
		val rev3 = rev2 + 1
		val sessionA = "sessionA"
		val sessionB = "sessionB"
		nodeStore.createNode(sessionA, rev1, "/child1", UNSTRUCTURED_NODE_TYPE, null)
		nodeStore.commit(sessionA) shouldBe rev2 // session A should be at revision 2 now
		nodeDataStore.node("/child1", rev2).isDefined shouldBe true
		nodeStore.node(sessionB, rev2, "/child1").get // now sessionB at rev 2 is active
		nodeStore.node(sessionA, rev2, "/child1").get("prop1") = "hi"
		nodeStore.commit(sessionA) // session A is at rev 3 now
		nodeDataStore.node("/child1", rev2).isDefined shouldBe true // for session B
		nodeDataStore.node("/child1", rev3).isDefined shouldBe true // for session A
		nodeStore.sessionClosed(sessionB)
		nodeDataStore.node("/child1", rev2).isDefined shouldBe false // because session B was the last session at rev 1
		nodeStore.sessionClosed(sessionA)
		nodeDataStore.node("/child1", rev3).isDefined shouldBe true // because rev2 is the latest version of the node and is not deleted
	}

	it should "maintain an older persistent version of a node until all sessions at or below that revision are closed" in {
		val rev1 =  new RepositoryRevisionNumber(1)
		val rev2 = rev1 + 1
		val rev3 = rev2 + 1
		val sessionA = "sessionA"
		val sessionB = "sessionB"
		val sessionC = "sessionC"
		nodeStore.createNode(sessionA, rev1, "/child1", UNSTRUCTURED_NODE_TYPE, null)
		nodeStore.commit(sessionA) shouldBe rev2 // session A should be at revision 2 now
		nodeDataStore.node("/child1", rev2).isDefined shouldBe true
		nodeStore.node(sessionB, rev2, "/child1").get // now sessionB at rev 2 is active
		nodeStore.node(sessionA, rev2, "/child1").get("prop1") = "hi"
		nodeStore.commit(sessionA) shouldBe rev3 // session A is at rev 3 now
		nodeDataStore.node("/child1", rev2).isDefined shouldBe true // for session B
		nodeDataStore.node("/child1", rev3).isDefined shouldBe true // for session A
		nodeStore.sessionClosed(sessionB)
		nodeDataStore.node("/child1", rev2).isDefined shouldBe false // because session B was the last session at rev 2
		nodeStore.sessionClosed(sessionA)
		nodeDataStore.node("/child1", rev3).isDefined shouldBe true // because rev3 is the latest version of the node and is not deleted
		val cNode = nodeStore.node(sessionC, rev3, "/child1").get
		cNode("prop2") = "hello"
		val rev4 = nodeStore.commit(sessionC)
		rev4 shouldBe new RepositoryRevisionNumber(4)
		nodeDataStore.node("/child1", rev3).isDefined shouldBe true
		nodeDataStore.node("/child1", rev4).isDefined shouldBe true
		nodeStore.sessionClosed(sessionC)
		// there were no session active below revision 4, so we should prune nodes
		// below rev 4 that aren't the latest version
		nodeDataStore.node("/child1", rev3).isDefined shouldBe false
	}

	it should "be able to prune all nodes to their latest non-deleted revision" in {
		val sessionA = "sessionA"
		val rev1 = new RepositoryRevisionNumber(1)
		nodeStore.createNode(sessionA, rev1, "/child1", UNSTRUCTURED_NODE_TYPE, null)
		val rev2 = nodeStore.commit(sessionA)
		nodeDataStore.node("/child1", rev2).isDefined shouldBe true
		nodeStore.node(sessionA, rev2, "/child1").get("prop") = "hi"
		val rev3 = nodeStore.commit(sessionA)
		nodeDataStore.node("/child1", rev3).isDefined shouldBe true
		nodeStore.createNode(sessionA, rev3, "/child2", UNSTRUCTURED_NODE_TYPE, null)
		val rev4 = nodeStore.commit(sessionA)
		nodeDataStore.node("/child2", rev4).isDefined shouldBe true
		nodeStore.deleteNode(sessionA, rev4, "/child2")
		val rev5 = nodeStore.commit(sessionA)
		nodeDataStore.node("/child2", rev5) shouldBe None
		nodeDataStore.pruneNodesUpToRevision(rev5)
		nodeDataStore.node("/child1", rev2) shouldBe None
		nodeDataStore.node("/child1", rev3).isDefined shouldBe true
		nodeDataStore.node("/child2", rev4) shouldBe None
	}



}
