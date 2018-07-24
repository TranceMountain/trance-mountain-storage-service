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

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.Date

import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.backend.DerbyService
import org.trancemountain.storageservice.repository.nodetype.NodeTypeConstraintValidator
import org.trancemountain.storageservice.repository.search.EmbeddedSolrRepositorySearchService
import org.trancemountain.storageservice.repository.store.derby.DerbyFileMetadataStore
import org.trancemountain.storageservice.repository.store.fs.FSFileDataStore
import org.trancemountain.storageservice.repository.store.memory.{MemoryNodeDataStore, MemoryNodeMetadataStore, MemoryNodeTypeDataStore}
import org.trancemountain.storageservice.repository.store._

@Configuration
class SessionNodeConfig {
	@Bean
	def propConfig(): PropertyPlaceholderConfigurer = {
		val placeholderConfigurer = new PropertyPlaceholderConfigurer()
		placeholderConfigurer.setSearchSystemEnvironment(true)
		placeholderConfigurer
	}
}

/**
	* @author michaelcoddington
	*/
@RunWith(classOf[JUnitRunner])
@ContextConfiguration(
	loader = classOf[AnnotationConfigContextLoader],
	classes = Array(classOf[SessionNodeConfig], classOf[SessionService],
		classOf[NodeStore], classOf[MemoryNodeDataStore], classOf[MemoryNodeMetadataStore],
		classOf[FileStore], classOf[FSFileDataStore], classOf[DerbyFileMetadataStore],
		classOf[DerbyService], classOf[Session], classOf[NodeTypeStore],
		classOf[NodeTypeConstraintValidator], classOf[MemoryNodeTypeDataStore], classOf[EmbeddedSolrRepositorySearchService]),
	initializers = Array(classOf[ConfigFileApplicationContextInitializer]))
class SessionNodeTest extends FlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with MockitoSugar {

	@Autowired
	private val sessionService: ISessionService = null

	@Autowired
	private var nodeStore: INodeStore = _

	private var session: ISession = _

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

	override def beforeEach(): Unit = {
		nodeStore.reset()
		session = sessionService.getAdminSession()
	}

	"A SessionNode" should "report its path" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> 1))
		val n1 = session.node("/a").get
		ns1.path shouldBe ("/a")
	}

	it should "report its name" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> 1))
		val n1 = session.node("/a").get
		n1.name shouldBe "a"
	}

	it should "report its primary node type" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> 1))
		val n1 = session.node("/a").get
		n1.primaryNodeType shouldBe "tm:unstructured:1"
	}

	it should "report its mixin node types" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", Set("mix:lockable:1"), Map("prop1" -> 1))
		val n1 = session.node("/a").get
		n1.mixinNodeTypes shouldBe Set("mix:lockable:1")
	}

	it should "return its parent" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> 1))
		val n1 = session.node("/a").get
		val parentNode = n1.parent
		parentNode.path shouldBe "/"
	}

	it should "return its children" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> 1))
		val ns2 = nodeStore.createNode(session.sessionID, session.revision, "/a/b", "tm:unstructured:1", null, Map("prop1" -> 1))
		val n1 = session.node("/a").get
		val n2 = session.node("/a/b").get
		n2.path shouldBe "/a/b"
		n2.parent shouldBe ns1
		val childrenOpt = n1.children
		childrenOpt.isDefined shouldBe true
		childrenOpt.get.next() shouldBe n2
	}

	it should "read an int property" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> 1))
		val n1 = session.node("/a").get
		n1.int("prop1") shouldBe 1
	}

	it should "write an int property" in {
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		n1("prop") = 1
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		val propOpt = ns1("prop")
		propOpt.isDefined shouldBe true
		propOpt.get shouldBe 1
	}

	it should "read an int array property" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> Array(1, 2, 3)))
		val n1 = session.node("/a").get
		n1.intArray("prop1") shouldBe Array(1, 2, 3)
	}

	it should "write an int array property" in {
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		n1("prop") = Array(1, 2)
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		val propOpt = ns1("prop")
		propOpt.isDefined shouldBe true
		propOpt.get shouldBe Array(1, 2)
	}

	it should "read a string property" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> "hi"))
		val n1 = session.node("/a").get
		n1.string("prop1") shouldBe "hi"
	}

	it should "write a string property" in {
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		n1("prop") = "test"
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		val propOpt = ns1("prop")
		propOpt.isDefined shouldBe true
		propOpt.get shouldBe "test"
	}

	it should "read a string array property" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> Array("a", "b")))
		val n1 = session.node("/a").get
		n1.stringArray("prop1") shouldBe Array("a", "b")
	}

	it should "write a string array property" in {
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		n1("prop") = Array("test", "ing")
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		val propOpt = ns1("prop")
		propOpt.isDefined shouldBe true
		propOpt.get shouldBe Array("test", "ing")
	}

	it should "read a long property" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> 1L))
		val n1 = session.node("/a").get
		n1.long("prop1") shouldBe 1L
	}

	it should "write a long property" in {
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		n1("prop") = 1L
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		val propOpt = ns1("prop")
		propOpt.isDefined shouldBe true
		propOpt.get shouldBe 1L
	}

	it should "read a long array property" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> Array(1L, 2L)))
		val n1 = session.node("/a").get
		n1.longArray("prop1") shouldBe Array(1L, 2L)
	}

	it should "write a long array property" in {
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		n1("prop") = Array(1L, 3L)
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		val propOpt = ns1("prop")
		propOpt.isDefined shouldBe true
		propOpt.get shouldBe Array(1L, 3L)
	}

	it should "read a double property" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> 1.0))
		val n1 = session.node("/a").get
		n1.double("prop1") shouldBe 1.0
	}

	it should "write a double property" in {
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		n1("prop") = 1.0
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		val propOpt = ns1("prop")
		propOpt.isDefined shouldBe true
		propOpt.get shouldBe 1.0
	}

	it should "read a double array property" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> Array(1.0, 2.0)))
		val n1 = session.node("/a").get
		n1.doubleArray("prop1") shouldBe Array(1.0, 2.0)
	}

	it should "write a double array property" in {
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		n1("prop") = Array(1.0, 42.0)
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		val propOpt = ns1("prop")
		propOpt.isDefined shouldBe true
		propOpt.get shouldBe Array(1.0, 42.0)
	}

	it should "read a date property" in {
		val d = new Date
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> d))
		val n1 = session.node("/a").get
		n1.date("prop1") shouldBe d
	}

	it should "write a date property" in {
		val d = new Date
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		n1("prop") = d
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		val propOpt = ns1("prop")
		propOpt.isDefined shouldBe true
		propOpt.get shouldBe d
	}

	it should "read a boolean property" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> true))
		val n1 = session.node("/a").get
		n1.boolean("prop1") shouldBe true
	}

	it should "write a boolean property" in {
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		n1("prop") = true
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		val propOpt = ns1("prop")
		propOpt.isDefined shouldBe true
		propOpt.get shouldBe true
	}

	it should "read a binary property" in {
		val bytes = Array[Byte](1, 2, 3)
		val bais = new ByteArrayInputStream(bytes)
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null, Map("prop1" -> bais))
		val n1 = session.node("/a").get
		val binary = n1.binary("prop1")
		val inStream = binary.inputStream
		val retBytes = IOUtils.toByteArray(inStream)
		retBytes shouldBe bytes
	}

	it should "write a binary property" in {
		val bytes = Array[Byte](1, 2, 3)
		val bais = new ByteArrayInputStream(bytes)
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		n1("prop") = bais
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		val propOpt = ns1("prop")
		propOpt.isDefined shouldBe true
		val binary = propOpt.get.asInstanceOf[IBinary]
		val is = binary.inputStream
		val isBytes = IOUtils.toByteArray(is)
		isBytes shouldBe bytes
	}

	it should "read a strong reference property" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null)
		val ns2 = nodeStore.createNode(session.sessionID, session.revision, "/b", "tm:unstructured:1", null)
		ns1("ref") = StoredStrongReference("/a", "ref", "/b")
		val n1 = session.node("/a").get
		val n2 = session.node("/b").get
		val sr = n1.strongReference("ref")
		sr shouldBe StrongReference(n2)
	}

	it should "write a strong reference property" in {
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		val n2 = session.createNode("/b", "tm:unstructured:1", null, null)
		n1("ref") = StrongReference(n2)
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		ns1("ref").get shouldBe StoredStrongReference("/a", "ref", "/b")
	}

	it should "read a weak reference property" in {
		val ns1 = nodeStore.createNode(session.sessionID, session.revision, "/a", "tm:unstructured:1", null)
		val ns2 = nodeStore.createNode(session.sessionID, session.revision, "/b", "tm:unstructured:1", null)
		ns1("ref") = StoredWeakReference("/a", "ref", "/b")
		val n1 = session.node("/a").get
		val n2 = session.node("/b").get
		val sr = n1.weakReference("ref")
		sr shouldBe WeakReference(n2)
	}

	it should "write a weak reference property" in {
		val n1 = session.createNode("/a", "tm:unstructured:1", null, null)
		val n2 = session.createNode("/b", "tm:unstructured:1", null, null)
		n1("ref") = WeakReference(n2)
		val ns1 = nodeStore.node(session.sessionID, session.revision, "/a").get
		ns1("ref").get shouldBe StoredWeakReference("/a", "ref", "/b")
	}



}
