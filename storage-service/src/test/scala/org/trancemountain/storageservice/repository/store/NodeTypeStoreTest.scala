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

import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.RepositoryException
import org.trancemountain.storageservice.repository.nodetype.{NodeTypeIdentifier, _}
import org.trancemountain.storageservice.repository.store.memory.MemoryNodeTypeDataStore

import scala.io.Source

@Configuration
class NodeTypeStoreConfig {
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
@ContextConfiguration(loader = classOf[AnnotationConfigContextLoader], classes = Array(classOf[NodeTypeStoreConfig], classOf[NodeTypeStore], classOf[MemoryNodeTypeDataStore]), initializers = Array(classOf[ConfigFileApplicationContextInitializer]))
class NodeTypeStoreTest extends FlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with MockitoSugar {

	@Autowired
	private val store: INodeTypeStore = null

	override def beforeAll(): Unit = {
		System.setProperty(SpringConfigKeys.TM_NODETYPE_DATA_STORE_TYPE, "memory")

		val mgr = new TestContextManager(this.getClass)
		mgr.prepareTestInstance(this)
	}

	override def beforeEach(): Unit = {
		store.clear()
	}

	private def createNodeType(filePath: String): INodeTypeDefinition = {
		val markerDef = Source.fromURL(getClass.getClassLoader.getResource(filePath)).getLines().mkString("\n")
		store.createNodeType(markerDef)
	}

	private def createNodeTypes(filePath: String): Set[INodeTypeDefinition] = {
		val markerDef = Source.fromURL(getClass.getClassLoader.getResource(filePath)).getLines().mkString("\n")
		store.createNodeTypes(markerDef)
	}

	"A NodeTypeStore" should "parse a marker node type definition" in {
		val nodeTypeDef = createNodeType("NodeTypeDefinitions/nodetype_marker.yaml")
		val id = nodeTypeDef.nodeTypeIdentifier
		id shouldBe NodeTypeIdentifier("tm", "marker", 1)
		nodeTypeDef.isMixin shouldBe false
		nodeTypeDef.parentDefinition shouldBe None
		nodeTypeDef.childDefinitions shouldBe None
		nodeTypeDef.propertyDefinitions shouldBe None
	}

	it should "parse a node type definition with any properties and no children" in {
		val nodeTypeDef = createNodeType("NodeTypeDefinitions/nodetype_any_properties_no_children.yaml")
		val id = nodeTypeDef.nodeTypeIdentifier
		id shouldBe NodeTypeIdentifier("tm", "nodetype", 1)
		nodeTypeDef.isMixin shouldBe false
		nodeTypeDef.parentDefinition shouldBe None
		nodeTypeDef.childDefinitions shouldBe None
		nodeTypeDef.propertyDefinitions shouldBe Some(Set(WildcardNodePropertyDefinition))
	}

	it should "parse a node type definition with any children and no properties" in {
		val nodeTypeDef = createNodeType("NodeTypeDefinitions/nodetype_any_children_no_properties.yaml")
		val id = nodeTypeDef.nodeTypeIdentifier
		id shouldBe NodeTypeIdentifier("tm", "nodetype", 1)
		nodeTypeDef.isMixin shouldBe false
		nodeTypeDef.parentDefinition shouldBe None
		nodeTypeDef.childDefinitions shouldBe Some(Set(WildcardNodeChildDefinition))
		nodeTypeDef.propertyDefinitions shouldBe None
	}

	it should "parse a node type definition with specific properties and no children" in {
		val nodeTypeDef = createNodeType("NodeTypeDefinitions/nodetype_specific_props_no_children.yaml")
		val id = nodeTypeDef.nodeTypeIdentifier
		id shouldBe NodeTypeIdentifier("tm", "sampletype", 1)
		nodeTypeDef.isMixin shouldBe false
		nodeTypeDef.parentDefinition shouldBe None
		nodeTypeDef.childDefinitions shouldBe None
		val propDefOption = nodeTypeDef.propertyDefinitions
		propDefOption.isDefined shouldBe true
		val propDefSet = propDefOption.get
		propDefSet.size shouldBe 2
		propDefSet should contain(NodePropertyDefinition(name = "prop1", propertyType = PropertyType.STRING, required = true))
		propDefSet should contain(NodePropertyDefinition(name = "prop2", propertyType = PropertyType.LONG, required = true))
	}

	it should "parse a node type definition with specific children and specific properties" in {
		val nodeTypeDef = createNodeType("NodeTypeDefinitions/nodetype_specific_children_specific_props.yaml")
		val id = nodeTypeDef.nodeTypeIdentifier
		id shouldBe NodeTypeIdentifier("tm", "sampletype", 1)
		nodeTypeDef.isMixin shouldBe false
		nodeTypeDef.parentDefinition shouldBe None
		val propDefOption = nodeTypeDef.propertyDefinitions
		propDefOption.isDefined shouldBe true
		val propDefSet = propDefOption.get
		propDefSet.size shouldBe 2
		propDefSet should contain(NodePropertyDefinition(name = "prop1", propertyType = PropertyType.STRING, required = true))
		propDefSet should contain(NodePropertyDefinition(name = "prop2", propertyType = PropertyType.BINARY, required = true))
		val childDefOption = nodeTypeDef.childDefinitions
		childDefOption.isDefined shouldBe true
		val childDefSet = childDefOption.get
		childDefSet.size shouldBe 2
		childDefSet should contain(NodeChildDefinition(name = Some("childA"), NodeTypeIdentifier("tm", "childtype", 1), None, None))
		childDefSet should contain(NodeChildDefinition(name = Some("childB"), NodeTypeIdentifier("tm", "secondchild", 1), None, None))
	}

	it should "parse a node type definition with specific children and no properties" in {
		val nodeTypeDef = createNodeType("NodeTypeDefinitions/nodetype_specific_children_no_props.yaml")
		val id = nodeTypeDef.nodeTypeIdentifier
		id shouldBe NodeTypeIdentifier("tm", "sampletype", 1)
		nodeTypeDef.isMixin shouldBe false
		nodeTypeDef.parentDefinition shouldBe None
		nodeTypeDef.propertyDefinitions shouldBe None
		val childDefOption = nodeTypeDef.childDefinitions
		childDefOption.isDefined shouldBe true
		val childDefSet = childDefOption.get
		childDefSet.size shouldBe 2
		childDefSet should contain(NodeChildDefinition(name = Some("childA"), NodeTypeIdentifier("tm", "childtype", 1), None, None))
		childDefSet should contain(NodeChildDefinition(name = Some("childB"), NodeTypeIdentifier("tm", "secondchild", 1), None, None))
	}

	it should "parse a node type definition with optional properties" in {
		val nodeTypeDef = createNodeType("NodeTypeDefinitions/nodetype_optional_props.yaml")
		val id = nodeTypeDef.nodeTypeIdentifier
		id shouldBe NodeTypeIdentifier("tm", "sampletype", 1)
		nodeTypeDef.isMixin shouldBe false
		nodeTypeDef.parentDefinition shouldBe None
		nodeTypeDef.childDefinitions shouldBe None
		val propDefOption = nodeTypeDef.propertyDefinitions
		propDefOption.isDefined shouldBe true
		val propDefSet = propDefOption.get
		propDefSet.size shouldBe 2
		propDefSet should contain(NodePropertyDefinition(name = "prop1", propertyType = PropertyType.STRING, required = false))
		propDefSet should contain(NodePropertyDefinition(name = "prop2", propertyType = PropertyType.BINARY, required = false))
	}

	it should "parse a node type definition with optional children" in {
		val nodeTypeDef = createNodeType("NodeTypeDefinitions/nodetype_optional_children.yaml")
		val id = nodeTypeDef.nodeTypeIdentifier
		id shouldBe NodeTypeIdentifier("tm", "sampletype", 1)
		nodeTypeDef.isMixin shouldBe false
		nodeTypeDef.parentDefinition shouldBe None
		nodeTypeDef.propertyDefinitions shouldBe None
		val childDefOption = nodeTypeDef.childDefinitions
		childDefOption.isDefined shouldBe true
		val childDefSet = childDefOption.get
		childDefSet.size shouldBe 2
		childDefSet should contain(NodeChildDefinition(name = Some("childA"), NodeTypeIdentifier("tm", "childtype", 1), Some(0), Some(1)))
		childDefSet should contain(NodeChildDefinition(name = Some("childB"), NodeTypeIdentifier("tm", "secondchild", 1), Some(0), Some(1)))
	}

	it should "parse a node type definition with unnamed children of specific types" in {
		val nodeTypeDef = createNodeType("NodeTypeDefinitions/nodetype_any_children_of_type.yaml")
		val id = nodeTypeDef.nodeTypeIdentifier
		id shouldBe NodeTypeIdentifier("tm", "parent", 1)
		val childDefOption = nodeTypeDef.childDefinitions
		childDefOption.isDefined shouldBe true
		val childDefSet = childDefOption.get
		childDefSet.size shouldBe 4
		childDefSet should contain(NodeChildDefinition(name = None, NodeTypeIdentifier("tm", "child", 1), None, Some(3)))
		childDefSet should contain(NodeChildDefinition(name = None, NodeTypeIdentifier("tm", "parent", 1), Some(3), None))
		childDefSet should contain(NodeChildDefinition(name = None, NodeTypeIdentifier("tm", "symlink", 1), Some(1), Some(2)))
		childDefSet should contain(NodeChildDefinition(name = None, NodeTypeIdentifier("tm", "other", 1), None, None))
	}

	it should "parse a mixin node type" in {
		val nodeTypeDef = createNodeType("NodeTypeDefinitions/nodetype_mixin.yaml")
		val id = nodeTypeDef.nodeTypeIdentifier
		id shouldBe NodeTypeIdentifier("tm", "mixinExample", 1)
		nodeTypeDef.isMixin shouldBe true
		nodeTypeDef.childDefinitions shouldBe None
		nodeTypeDef.propertyDefinitions shouldBe None
	}

	it should "parse multiple node type definitions at once" in {
		val nodeTypeDefs = createNodeTypes("NodeTypeDefinitions/nodetype_multiple.yaml")
		nodeTypeDefs should contain(NodeTypeDefinition(NodeTypeIdentifier("tm", "marker", 1), isMixin = false, None, None, None))
		nodeTypeDefs should contain(NodeTypeDefinition(NodeTypeIdentifier("tm", "mixinExample", 1), isMixin = true, None, None, None))
	}

	it should "parse multiple types of properties" in {
		val nodeTypeDef = createNodeType("NodeTypeDefinitions/nodetype_all_props.yaml")
		val propDefOpt = nodeTypeDef.propertyDefinitions
		propDefOpt.isDefined shouldBe true
		val propDefs = propDefOpt.get
		propDefs should contain(NodePropertyDefinition("bprop", PropertyType.BOOLEAN, required = true))
		propDefs should contain(NodePropertyDefinition("intprop", PropertyType.INT, required = true))
		propDefs should contain(NodePropertyDefinition("iaprop", PropertyType.INT_ARRAY, required = true))
		propDefs should contain(NodePropertyDefinition("lprop", PropertyType.LONG, required = true))
		propDefs should contain(NodePropertyDefinition("laprop", PropertyType.LONG_ARRAY, required = true))
		propDefs should contain(NodePropertyDefinition("dprop", PropertyType.DOUBLE, required = true))
		propDefs should contain(NodePropertyDefinition("daprop", PropertyType.DOUBLE_ARRAY, required = true))
		propDefs should contain(NodePropertyDefinition("sprop", PropertyType.STRING, required = true))
		propDefs should contain(NodePropertyDefinition("saprop", PropertyType.STRING_ARRAY, required = true))
		propDefs should contain(NodePropertyDefinition("dateprop", PropertyType.DATE, required = true))
		propDefs should contain(NodePropertyDefinition("binprop", PropertyType.BINARY, required = true))
		propDefs should contain(NodePropertyDefinition("wrprop", PropertyType.WEAK_REF, required = true))
		propDefs should contain(NodePropertyDefinition("srprop", PropertyType.STRONG_REF, required = true))
	}

	it should "parse a node type definition that extends another node type" in {
		val nodeTypeDefs = createNodeTypes("NodeTypeDefinitions/nodetype_extension.yaml")
		val lockableOpt = nodeTypeDefs.find(_.nodeTypeIdentifier == NodeTypeIdentifier("tm", "lockable", 1))
		val versionableOpt = nodeTypeDefs.find(_.nodeTypeIdentifier == NodeTypeIdentifier("tm", "versionable", 1))
		lockableOpt.isDefined shouldBe true
		versionableOpt.isDefined shouldBe true
		val lockable = lockableOpt.get
		val versionable = versionableOpt.get
		val id = versionable.nodeTypeIdentifier
		id.namespace shouldBe "tm"
		id.name shouldBe "versionable"
		id.version shouldBe 1
		versionable.isMixin shouldBe true
		val parentDefOpt = versionable.parentDefinition
		parentDefOpt.isDefined shouldBe true
		val parentDef = parentDefOpt.get
		parentDef shouldBe lockable
	}
	
	it should "only allow a mixin type to extend another mixin type" in {
		assertThrows[RepositoryException] {
			createNodeTypes("NodeTypeDefinitions/nodetype_extension_nonmixin_to_mixin.yaml")
		}
	}

	it should "should only allow a non-mixin type to extend another non-mixin type" in {
		assertThrows[RepositoryException] {
			createNodeTypes("NodeTypeDefinitions/nodetype_extension_mixin_to_nonmixin.yaml")
		}
	}
	
	it should "be able to retrieve a previously created node type" in {
		createNodeType("NodeTypeDefinitions/nodetype_marker.yaml")
		val id = NodeTypeIdentifier("tm", "marker", 1)
		val markerDef = store.getNodeTypeDefinition(id)
		markerDef.isDefined shouldBe true
		markerDef.get shouldBe NodeTypeDefinition(id, isMixin = false, None, None, None)
	}

	it should "create default node types on startup" in {
		for (nid <- Array(
			NodeTypeIdentifier("mix", "lockable", 1),
			NodeTypeIdentifier("mix", "versionable", 1),
			NodeTypeIdentifier("tm", "unstructured", 1),
			NodeTypeIdentifier("tm", "folder", 1),
			NodeTypeIdentifier("tm", "file", 1)))
			store.getNodeTypeDefinition(nid).isDefined shouldBe true
	}


}
