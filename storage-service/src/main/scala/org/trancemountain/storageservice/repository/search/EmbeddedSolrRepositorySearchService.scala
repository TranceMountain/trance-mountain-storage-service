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
import java.io.File
import java.util.concurrent.{Callable, LinkedBlockingDeque, ThreadPoolExecutor, TimeUnit}
import javax.annotation.{PostConstruct, PreDestroy}

import org.apache.commons.io.FileUtils
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.core.CoreContainer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.{INode, RepositoryRevisionNumber}
import org.trancemountain.storageservice.repository.store.{BinaryReference, StoredStrongReference, StoredWeakReference}

import scala.collection.JavaConverters
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionException

/**
	* @author michaelcoddington
	*/
@Lazy
@Service
@ConditionalOnProperty(name = Array(SpringConfigKeys.TM_SEARCH_SERVICE_TYPE), havingValue = "embedded")
class EmbeddedSolrRepositorySearchService extends IRepositorySearchService {

	private val LOG = LoggerFactory.getLogger(getClass)

	private val INDEXING_THREAD_IDLE_SECONDS = 10

	private val ID_KEY = "id"
	private val PATH_KEY = "path"
	private val REVISION_KEY = "revision"
	private val PATH_REVISION_KEY = "pathRevision"
	private val NAME_KEY = "name"
	private val PRIMARY_NODETYPE_KEY = "primaryNodeType"
	private val MIXIN_NODETYPE_KEY = "mixinNodeType"
	private val IS_DELETED_KEY = "isDeleted"

	@Value(SpringConfigKeys.TM_SEARCH_SERVICE_INDEXING_THREAD_COUNT_VALUE)
	private val indexingThreadCount: Int = 10

	@Value(SpringConfigKeys.TM_SEARCH_SERVICE_EMBEDDED_CONTAINER_LOCATION_VALUE)
	private val embeddedSolrContainerLocation: String = null

	private var containerFolder: File = _

	private var executor: ThreadPoolExecutor = _

	private var solrServer: EmbeddedSolrServer = _

	@PostConstruct
	private def start(): Unit = {
		executor = new ThreadPoolExecutor(0, indexingThreadCount, INDEXING_THREAD_IDLE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingDeque[Runnable]())
		val solrConfigFolderURL = getClass.getClassLoader.getResource("embedded-solr-home")
		val solrConfigFolder = new File(solrConfigFolderURL.toURI)
		LOG.info(s"Initializing embedded SOLR using config files at $solrConfigFolderURL, copying to $embeddedSolrContainerLocation")
		containerFolder = new File(embeddedSolrContainerLocation)
		for (file <- solrConfigFolder.listFiles) {
			if (file.isFile) FileUtils.copyFileToDirectory(file, containerFolder)
			else FileUtils.copyDirectoryToDirectory(file, containerFolder)
		}
		for (file <- containerFolder.listFiles()) {
			LOG.debug(s"Container folder now has file $file")
		}
		val coreContainer = new CoreContainer(embeddedSolrContainerLocation)
		coreContainer.load()
		solrServer = new EmbeddedSolrServer(coreContainer, "tranceMountain")
	}

	@PreDestroy
	private def stop(): Unit = {
		executor.shutdownNow()
		solrServer.close()
	}

	def nodeDocumentCount: Long = {
		val query = new SolrQuery()
		query.setQuery("*:*")
		val res = solrServer.query(query)
		res.getResults.getNumFound
	}

	protected[repository] def reset(): Unit = {
		solrServer.deleteByQuery("*:*")
		solrServer.commit()
	}

	override def indexNodes(nodes: Seq[INode], asynchronous: Boolean = false): Unit = {

		def arrayToList[T](arr: Array[T]): java.util.List[T] = java.util.Arrays.asList(arr:_*)

		class IndexingCallable(nodesToIndex: Seq[INode]) extends Callable[Unit] {
			override def call(): Unit = {
				val docs: Seq[SolrInputDocument] = nodesToIndex.map(nodeToIndex => {
					val props = nodeToIndex.properties
					val path = nodeToIndex.path
					val revision = nodeToIndex.revision.toString
					val doc = new SolrInputDocument
					doc.addField(ID_KEY, nodeToIndex.id)
					doc.addField(NAME_KEY, nodeToIndex.name)
					doc.addField(PATH_KEY, path)
					doc.addField(REVISION_KEY, revision)
					doc.addField(PATH_REVISION_KEY, s"$path-$revision")
					doc.addField(PRIMARY_NODETYPE_KEY, nodeToIndex.primaryNodeType)
					for (mixinType <- nodeToIndex.mixinNodeTypes) doc.addField(MIXIN_NODETYPE_KEY, mixinType)
					doc.addField(IS_DELETED_KEY, nodeToIndex.isDeleted())
					for ((propName, propVal) <- props if propVal != null) {
						val (indexedPropertyName, indexedPropertyVal) = propVal match {
							case i: Int => (s"${propName}_i", i)
							case ia: Array[Int] => (s"${propName}_is", arrayToList(ia))
							case l: Long => (s"${propName}_l", l)
							case la: Array[Long] => (s"${propName}_ls", arrayToList(la))
							case s: String => (s"${propName}_s", s)
							case sa: Array[String] => (s"${propName}_ss", arrayToList(sa))
							case d: Double => (s"${propName}_d", d)
							case da: Array[Double] => (s"${propName}_ds", arrayToList(da))
							case b: Boolean => (s"${propName}_b", b)
							case sr: StoredStrongReference => (s"${propName}_sr", sr.targetNodePath)
							case wr: StoredWeakReference => (s"${propName}_wr", wr.targetNodePath)
							case br: BinaryReference => (s"${propName}_br", s"${nodeToIndex.path}/$propName")
						}
						doc.addField(indexedPropertyName, indexedPropertyVal)
					}
					LOG.info(s"Using doc $doc")
					doc
				})

				// check for existing path/revisions
				val pathRevisions: Seq[String] = docs.map(doc => "\"" + doc.getFieldValue(PATH_REVISION_KEY).asInstanceOf[String] + "\"")
				val pathRevisionQuery = pathRevisions.mkString(" OR ")
				val query = new SolrQuery()
				query.setQuery(s"$PATH_REVISION_KEY:($pathRevisionQuery)")
				query.setFields(PATH_KEY, REVISION_KEY)
				query.setRows(Integer.MAX_VALUE)
				val queryResponse = solrServer.query(query)
				val queryResults = queryResponse.getResults
				if (queryResults.getNumFound > 0) {
					val docIter = queryResults.iterator()
					val tupleList = ListBuffer.empty[(String, String)]
					while (docIter.hasNext) {
						val doc = docIter.next()
						tupleList += ((doc.get(PATH_KEY).asInstanceOf[String], doc.get(REVISION_KEY).asInstanceOf[String]))
					}
					throw new SearchIndexUniqueConstraintException(tupleList)
				}

				val javaDocList = JavaConverters.asJavaCollection(docs)
				solrServer.add(javaDocList)
				val updateResponse = solrServer.commit()
				LOG.debug(s"Got update response $updateResponse")
			}
		}

		val indexingCallable = new IndexingCallable(nodes)
		val future = executor.submit(indexingCallable)
		if (!asynchronous) {
			try future.get()
			catch {
				case ee: ExecutionException =>
					throw ee.getCause
			}
		}
	}

	override def nodeIDs(query: SolrQuery, revision: RepositoryRevisionNumber): Seq[String] = {
		val execQuery = s"${query.getQuery} AND $REVISION_KEY:[* TO $revision]"
		query.setQuery(execQuery)
		query.setFields(ID_KEY, IS_DELETED_KEY)
		query.set("group", true)
		query.set("group.field", PATH_KEY)
		query.set("group.sort", s"$REVISION_KEY DESC")
		query.setRows(Integer.MAX_VALUE)
		val queryResponse = solrServer.query(query)
		val groupCommand = queryResponse.getGroupResponse.getValues.get(0)

		val idList = ListBuffer.empty[String]
		val groups = groupCommand.getValues
		val groupIter = groups.iterator()
		while (groupIter.hasNext) {
			val group = groupIter.next()
			val firstDoc = group.getResult.get(0)
			val isDeleted = firstDoc.get(IS_DELETED_KEY).asInstanceOf[Boolean]
			if (!isDeleted) idList += firstDoc.get(ID_KEY).asInstanceOf[String]
		}
		idList
	}

	override def truncateToMinimumRevision(revision: RepositoryRevisionNumber): Unit = {
		val execQuery = s"$REVISION_KEY:[* TO ${revision - 1}]"
		solrServer.deleteByQuery(execQuery)
		solrServer.commit()
	}

}
