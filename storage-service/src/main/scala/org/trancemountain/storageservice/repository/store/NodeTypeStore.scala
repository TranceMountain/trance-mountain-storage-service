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

import javax.annotation.PostConstruct

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.trancemountain.storageservice.repository.RepositoryException
import org.trancemountain.storageservice.repository.nodetype._
import org.yaml.snakeyaml.Yaml

import scala.collection.mutable
import scala.io.Source

/**
	* @author michaelcoddington
	*/
@Service
class NodeTypeStore extends INodeTypeStore {

	private val log = LoggerFactory.getLogger(getClass)

	@Value("classpath:tm_default_nodetypes.yml")
	private val defaultNodeTypesResource: Resource = null

	@Autowired
	private val dataStore: INodeTypeDataStore = null

	private val translator = new NodeTypeDefinitionTranslator

	@PostConstruct
	private def start(): Unit = {
		log.info(s"Got default node types resource $defaultNodeTypesResource")
		loadDefaultNodeTypes()
	}

	private def loadDefaultNodeTypes(): Unit = {
		val markerYML = Source.fromURL(defaultNodeTypesResource.getURL).getLines().mkString("\n")
		createNodeTypes(markerYML)
	}

	override def createNodeType(yamlString: String): INodeTypeDefinition = {
		val nt: INodeTypeDefinition = translator.yamlToNodeType(yamlString)
		dataStore.createNodeType(nt)
		nt
	}

	override def createNodeTypes(yamlString: String): Set[INodeTypeDefinition] = {
		val nodeTypes = translator.yamlToNodeTypes(yamlString: String)
		for (nt <- nodeTypes) dataStore.createNodeType(nt)
		nodeTypes
	}

	def getNodeTypeDefinition(nodeTypeIdentifier: NodeTypeIdentifier): Option[INodeTypeDefinition] = {
		dataStore.nodeType(nodeTypeIdentifier)
	}

	def removeNodeTypeDefinition(nodeTypeIdentifier: NodeTypeIdentifier) = {
		dataStore.removeNodeType(nodeTypeIdentifier)
	}

	override protected[store] def clear(): Unit = {
		dataStore.clear()
		loadDefaultNodeTypes()
	}

	private class NodeTypeDefinitionTranslator {
		private val LOG = LoggerFactory.getLogger(getClass)

		private val yamlParser = new Yaml()

		@unchecked
		def yamlToNodeType(yamlString: String): INodeTypeDefinition = {
			val retObj = yamlParser.load(yamlString)
			LOG.info(s"$retObj is a ${retObj.getClass}")
			nodeTypeDefFromObject(retObj, null)
		}

		def yamlToNodeTypes(yamlString: String): Set[INodeTypeDefinition] = {
			val retObjIter = scala.collection.JavaConverters.asScalaIteratorConverter(yamlParser.loadAll(yamlString).iterator()).asScala
			val workingMap = mutable.Map.empty[NodeTypeIdentifier, INodeTypeDefinition]
			val nodeTypes = for (obj <- retObjIter) yield nodeTypeDefFromObject(obj, workingMap)
			nodeTypes.toSet
		}

		private def nodeTypeDefFromObject(obj: AnyRef, workingNodeTypes: mutable.Map[NodeTypeIdentifier, INodeTypeDefinition]): INodeTypeDefinition = {
			obj match {
				case lhm: java.util.Map[String, Any] =>
					val nodetype = lhm.get("nodetype").asInstanceOf[String]
					val nodeTypeID = NodeTypeIdentifier.fromString(nodetype)

					val properties = lhm.get("properties")
					val propOption = properties match {
						case s: String =>
							if (s == "*") Some(Set[INodePropertyDefinition](WildcardNodePropertyDefinition))
							else throw new RuntimeException(s"Unknown string property type $s")
						case list: java.util.List[java.util.Map[String, Any]] =>
							val scalaBuf = scala.collection.JavaConverters.asScalaBufferConverter(list).asScala
							val retIter = scalaBuf.map((propMap: java.util.Map[String, Any]) => {
								val propName = propMap.get("name").asInstanceOf[String]
								if (propName == "*") {
									WildcardNodePropertyDefinition
								} else {
									val propTypeName = propMap.get("type").asInstanceOf[String]
									val propType = PropertyType.withName(propTypeName)
									val optional = if (propMap.containsKey("optional")) propMap.get("optional").asInstanceOf[Boolean] else false
									NodePropertyDefinition(name = propName, propertyType = propType, required = !optional)
								}
							})
							Some(Set.empty[INodePropertyDefinition] ++ retIter)
						case null => None
					}

					val children = lhm.get("children")
					val childOption = children match {
						case s: String =>
							if (s == "*") Some(Set[INodeChildDefinition](WildcardNodeChildDefinition))
							else throw new RuntimeException(s"Unknown string child type $s")
						case list: java.util.List[java.util.HashMap[String, Any]] =>
							val scalaBuf = scala.collection.JavaConverters.asScalaBufferConverter(list).asScala
							val retIter = scalaBuf.map((childMap: java.util.Map[String, Any]) => {
								val childTypeId = NodeTypeIdentifier.fromString(childMap.get("nodetype").asInstanceOf[String])
								val childName = childMap.get("name").asInstanceOf[String]
								val childNameOpt = childName match {
									case s: String => Some(s)
									case null => None
								}
								val minCount = if (childMap.containsKey("minCount")) Some(childMap.get("minCount").asInstanceOf[Int]) else None
								val maxCount = if (childMap.containsKey("maxCount")) Some(childMap.get("maxCount").asInstanceOf[Int]) else None
								NodeChildDefinition(childNameOpt, childTypeId, minCount, maxCount)
							})
							Some(Set.empty[INodeChildDefinition] ++ retIter)
						case null => None
					}

					val extendsProp = lhm.get("extends")
					val parentOption = extendsProp match {
						case s: String =>
							val parentTypeID = NodeTypeIdentifier.fromString(s)
							val workingDef = if (workingNodeTypes != null) {
								workingNodeTypes.get(parentTypeID)
							}
							workingDef match {
								case s: Some[INodeTypeDefinition] => s
								case None => dataStore.nodeType(parentTypeID) match {
									case s2: Some[INodeTypeDefinition] => s2
									case None => throw new RepositoryException(s"Cannot locate parent node type $parentTypeID for node type $nodeTypeID")
								}
							}
						case _ => None
					}

					val mixinProp = lhm.get("mixin")
					val isMixin = if (mixinProp != null) {
						mixinProp.asInstanceOf[Boolean]
					} else false

					if (parentOption.isDefined) {
						val parent = parentOption.get
						if(parent.isMixin != isMixin) throw new RepositoryException(s"Node/parent mixin type mismatch (nodetype is $isMixin, parent is ${parent.isMixin}")
					}

					val retDef = NodeTypeDefinition(nodeTypeID, isMixin = isMixin, parentOption, propOption, childOption)
					if (workingNodeTypes != null) workingNodeTypes.put(retDef.nodeTypeIdentifier, retDef)
					retDef

				case not_a_map => throw new RuntimeException(s"Unknown object type $not_a_map")
			}
		}

		def nodeTypeToYAML(nodeTypeDef: NodeTypeDefinition): String = {
			throw new RuntimeException("not implemented")
		}
	}
}
