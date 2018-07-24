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

package org.trancemountain.storageservice.repository.search

import java.nio.file.Files
import java.util.UUID

import org.apache.solr.client.solrj.SolrQuery
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.store.{BinaryReference, StoredStrongReference, StoredWeakReference}
import org.trancemountain.storageservice.repository.{INode, RepositoryRevisionNumber, StrongReference}

/**
	* @author michaelcoddington
	*/
@RunWith(classOf[JUnitRunner])
@ContextConfiguration(loader = classOf[AnnotationConfigContextLoader],
	classes = Array(classOf[EmbeddedSolrRepositorySearchService]),
	initializers = Array(classOf[ConfigFileApplicationContextInitializer]))
class RepositorySearchServiceTest extends FlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with MockitoSugar {

	@Autowired
	private val searchService: IRepositorySearchService = null

	private def getDefaultTestINode(name: String, path: String, revision: Int, deleted: Boolean = false): INode = {
		val props = Map.empty[String, Any]
		val nodeType = "tm:unstructured:1"
		val mixins = Set.empty[String]
		getTestINode(name, path, revision, nodeType, mixins, props, deleted)
	}

	private def getTestINode(name: String, path: String, revision: Int, nodeType: String, mixins: Set[String], properties: Map[String, Any], deleted: Boolean = false): INode = {
		val node = mock[INode]
		when(node.id).thenReturn(UUID.randomUUID().toString)
		when(node.name).thenReturn(name)
		when(node.path).thenReturn(path)
		when(node.isDeleted()).thenReturn(deleted)
		when(node.revision).thenReturn(new RepositoryRevisionNumber(revision))
		when(node.primaryNodeType).thenReturn(nodeType)
		when(node.mixinNodeTypes).thenReturn(mixins)
		when(node.properties).thenReturn(properties)
		node
	}

	override def beforeAll(): Unit = {
		val tmpFile = Files.createTempDirectory("tm-solr").toFile

		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_TYPE, "embedded")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_INDEXING_THREAD_COUNT, "10")
		System.setProperty(SpringConfigKeys.TM_SEARCH_SERVICE_EMBEDDED_CONTAINER_LOCATION, tmpFile.getAbsolutePath)

		val mgr = new TestContextManager(this.getClass)
		mgr.prepareTestInstance(this)
	}

	override def beforeEach(): Unit = {
		searchService.reset()
	}

	"A repository search service" should "index a node" in {
		val docCount = searchService.nodeDocumentCount
		val props = Map[String, Any](
			"int" -> 3,
			"int_array" -> Array(1, 2, 3),
			"string" -> "string",
			"string_array" -> Array("a", "b", "c"),
			"long" -> 1L,
			"long_array" -> Array(1L, 2L, 3L),
			"double" -> 2.4,
			"double_array" -> Array(1.0, 2.0, 3.0),
			"boolean" -> true,
			"binary" -> new BinaryReference,
			"strongRef" -> StoredStrongReference("/test1", "strongRef", "/test2"),
		  "weakRef" -> StoredWeakReference("/test1", "weakRef", "/test3")
		)
		val node = getTestINode("test", "/test", 1, "tm:unstructured:1", Set.empty[String], properties = props)

		searchService.indexNodes(Seq(node))
		searchService.nodeDocumentCount shouldBe docCount + 1
	}

	it should "create different entries for multiple revisions of a node" in {
		val node1 = getDefaultTestINode("test", "/test", 1)
		searchService.indexNodes(Seq(node1))
		val docCount = searchService.nodeDocumentCount
		val node2 = getDefaultTestINode("test", "/test", 2)
		searchService.indexNodes(Seq(node2))
		searchService.nodeDocumentCount shouldBe docCount + 1
	}

	it should "disallow multiple entries for a single revision of a node" in {
		val node1 = getDefaultTestINode("test", "/test", 1)
		searchService.indexNodes(Seq(node1))
		assertThrows[SearchIndexUniqueConstraintException] {
			searchService.indexNodes(Seq(node1))
		}
	}

	it should "retain entries for deleted nodes as deleted-node documents" in {
		val node1 = getDefaultTestINode("test", "/test", 1)
		searchService.indexNodes(Seq(node1))
		val node2 = getDefaultTestINode("test", "/test", 2)
		searchService.indexNodes(Seq(node2))
		val docCount = searchService.nodeDocumentCount
		docCount shouldBe 2
	}

	it should "not show search results for nodes at a revision higher than a given revision when searching" in {
		val node1 = getDefaultTestINode("test", "/test1", 1)
		val node2 = getDefaultTestINode("test", "/test2", 2)
		searchService.indexNodes(Seq(node1, node2))
		val query = new SolrQuery("*:*")
		val nodeIDs = searchService.nodeIDs(query, new RepositoryRevisionNumber(1))
		nodeIDs.size shouldBe 1
	}

	it should "only show search results for the latest revision of a node" in {
		val node1 = getDefaultTestINode("test", "/test1", 1)
		val node2 = getDefaultTestINode("test", "/test1", 2)
		searchService.indexNodes(Seq(node1, node2))
		val query = new SolrQuery("*:*")
		val nodeIDs = searchService.nodeIDs(query, new RepositoryRevisionNumber(2))
		nodeIDs.size shouldBe 1
	}

	it should "not show search results for deleted nodes at the current revision" in {
		val node1 = getDefaultTestINode("test", "/test1", 1)
		val node2 = getDefaultTestINode("test", "/test1", 2, deleted = true)
		searchService.indexNodes(Seq(node1, node2))
		val query = new SolrQuery("*:*")
		val nodeIDs = searchService.nodeIDs(query, new RepositoryRevisionNumber(2))
		nodeIDs.size shouldBe 0
	}

	it should "show search results for deleted nodes at a previous revision" in {
		val node1 = getDefaultTestINode("test", "/test1", 1)
		val node2 = getDefaultTestINode("test", "/test1", 2, deleted = true)
		searchService.indexNodes(Seq(node1, node2))
		val query = new SolrQuery("*:*")
		val nodeIDs = searchService.nodeIDs(query, new RepositoryRevisionNumber(1))
		nodeIDs.size shouldBe 1
	}

	it should "be able to remove all documents for nodes up to a given revision number" in {
		val node1 = getDefaultTestINode("test", "/test1", 1)
		val node2 = getDefaultTestINode("test", "/test1", 2, deleted = true)
		searchService.indexNodes(Seq(node1, node2))
		searchService.truncateToMinimumRevision(new RepositoryRevisionNumber(2))
		searchService.nodeDocumentCount shouldBe 1
	}

}
