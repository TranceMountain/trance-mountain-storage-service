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

package org.trancemountain.storageservice.repository.nodetype

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.trancemountain.storageservice.SpringConfigKeys
import org.trancemountain.storageservice.repository.ISessionNode
import org.trancemountain.storageservice.repository.store.{INodeTypeStore, NodeTypeStore}
import org.trancemountain.storageservice.repository.store.memory.MemoryNodeTypeDataStore

/**
	* @author michaelcoddington
	*/
@RunWith(classOf[JUnitRunner])
@ContextConfiguration(
	loader = classOf[AnnotationConfigContextLoader],
	classes = Array(classOf[NodeTypeStore], classOf[MemoryNodeTypeDataStore],
		classOf[NodeTypeConstraintValidator]),
	initializers = Array(classOf[ConfigFileApplicationContextInitializer]))
class NodeTypeConstraintValidatorTest extends FlatSpec with Matchers with BeforeAndAfterAll with MockitoSugar {

	@Autowired
	private val nodeTypeStore: INodeTypeStore = null

	@Autowired
	private val validator: NodeTypeConstraintValidator = null

	var UNSTRUCTURED_NODE_TYPE: String = "tm:unstructured:1"

	override def beforeAll(): Unit = {
		System.setProperty(SpringConfigKeys.TM_NODETYPE_DATA_STORE_TYPE, "memory")
		val mgr = new TestContextManager(this.getClass)
		mgr.prepareTestInstance(this)

		nodeTypeStore.createNodeTypes(
			"""---
				|nodetype: "tm:testprop:1"
				|properties:
				|  - name: propA
				|    type: int
				|---
				|nodetype: "tm:structured:1"
				|properties:
				|  - name: propA
				|    type: string
				|    optional: true
				|---
				|nodetype: "mix:testprop:1"
				|mixin: true
				|properties:
				|  - name: propA
				|    type: string
				|---
				|nodetype: "mix:testprop2:1"
				|mixin: true
				|properties:
				|  - name: propA
				|    type: string
				|    optional: true
				|---
				|nodetype: "tm:testchild:1"
				|children:
				|  - name: child1
				|    nodetype: "tm:unstructured:1"
				|---
				|nodetype: "tm:testchild:2"
				|children:
				|  - name: child1
				|    nodetype: "tm:unstructured:1"
				|    minCount: 0
				|---
				|nodetype: "mix:testchild:1"
				|mixin: true
				|children:
				|  - name: child1
				|    nodetype: "tm:unstructured:1"
				|---
				|nodetype: "mix:testchild:2"
				|mixin: true
				|children:
				|  - name: child1
				|    nodetype: "tm:unstructured:1"
				|    minCount: 0
				|---
				|nodetype: "mix:unstructured:1"
				|mixin: true
				|children: "*"
				|---
				|nodetype: "tm:typedchildren:1"
				|children:
				|   - nodetype: "tm:unstructured:1"
				|     minCount: 1
				|     maxCount: 2
				|---
				|nodetype: "mix:typedchildren:1"
				|mixin: true
				|children:
				|   - nodetype: "tm:unstructured:1"
				|     minCount: 1
				|     maxCount: 2
				|""".stripMargin)
	}

	"A NodeTypeConstraintValidator" should "fail a node that is missing required properties in its primary type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:testprop:1")
		when(node.mixinNodeTypes).thenReturn(Set.empty[String])
		when(node.apply("propA")).thenReturn(None)
		when(node.properties).thenReturn(Map.empty[String, Any])
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "fail a node that is missing required children in its primary type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:testchild:1")
		when(node.mixinNodeTypes).thenReturn(Set.empty[String])
		when(node.apply(anyString())).thenReturn(None)
		when(node.children).thenReturn(None)
		when(node.child(anyString)).thenReturn(None)
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "fail a node that is missing required properties in a mixin type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.mixinNodeTypes).thenReturn(Set("mix:testprop:1"))
		when(node.apply("propA")).thenReturn(None)
		when(node.properties).thenReturn(Map.empty[String, Any])
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "fail a node that is missing required children in a mixin type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.mixinNodeTypes).thenReturn(Set("mix:testchild:1"))
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(node.child("child1")).thenReturn(None)
		when(node.children).thenReturn(None)
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "pass a node that is missing optional properties in its primary type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:structured:1")
		when(node.mixinNodeTypes).thenReturn(Set.empty[String])
		when(node.apply("propA")).thenReturn(None)
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(node.children).thenReturn(None)
		validator.validate(node)
	}

	it should "pass a node that is missing optional children in its primary type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:testchild:2")
		when(node.mixinNodeTypes).thenReturn(Set.empty[String])
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(node.child("child1")).thenReturn(None)
		when(node.children).thenReturn(None)
		validator.validate(node)
	}

	it should "pass a node that is missing optional properties in a mixin type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.mixinNodeTypes).thenReturn(Set("mix:testprop2:1"))
		when(node.apply(anyString())).thenReturn(None)
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(node.children).thenReturn(None)
		validator.validate(node)
	}

	it should "pass a node that is missing optional children in a mixin type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.mixinNodeTypes).thenReturn(Set("mix:testchild:2"))
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(node.child("child1")).thenReturn(None)
		when(node.children).thenReturn(None)
		validator.validate(node)
	}

	it should "fail a node that has a required primary property of the wrong data type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:testprop:1")
		when(node.mixinNodeTypes).thenReturn(Set.empty[String])
		when(node.apply("propA")).thenReturn(Some("string"))
		when(node.properties).thenReturn(Map[String, Any]("propA" -> "string"))
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "fail a node that has an optional primary property of the wrong data type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:structured:1")
		when(node.mixinNodeTypes).thenReturn(Set.empty[String])
		when(node.apply("propA")).thenReturn(Some(3))
		when(node.properties).thenReturn(Map[String, Any]("propA" -> 3))
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "fail a node that has a required mixin property of the the wrong data type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.mixinNodeTypes).thenReturn(Set("mix:testprop:1"))
		when(node.apply("propA")).thenReturn(Some(3))
		when(node.properties).thenReturn(Map[String, Any]("propA" -> 3))
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "fail a node that has an optional mixin property of the the wrong data type" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.mixinNodeTypes).thenReturn(Set("mix:testprop2:1"))
		when(node.apply("propA")).thenReturn(Some(42))
		when(node.properties).thenReturn(Map[String, Any]("propA" -> 42))
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "fail a non-wildcard node that has children that are undefined in its primary nodetype" in {
		val node = mock[ISessionNode]
		val child1Node = mock[ISessionNode]
		val child2Node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:testchild:1")
		when(node.mixinNodeTypes).thenReturn(Set.empty[String])
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(child1Node.name).thenReturn("child1")
		when(child1Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(child2Node.name).thenReturn("child2")
		when(child2Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.children).thenReturn(Some(Iterator(child1Node, child2Node)))
		when(node.child("child1")).thenReturn(Some(child1Node))
		when(node.child("child2")).thenReturn(Some(child2Node))
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "fail a non-wildcard node that has children that are undefined in its mixin type" in {
		val node = mock[ISessionNode]
		val child1Node = mock[ISessionNode]
		val child2Node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:structured:1")
		when(node.mixinNodeTypes).thenReturn(Set("mix:testchild:1"))
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(child1Node.name).thenReturn("child1")
		when(child1Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(child2Node.name).thenReturn("child2")
		when(child2Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.children).thenReturn(Some(Iterator(child1Node, child2Node)))
		when(node.child("child1")).thenReturn(Some(child1Node))
		when(node.child("child2")).thenReturn(Some(child2Node))
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "pass a wildcarded node that has children that are undefined in its primary nodetype" in {
		val node = mock[ISessionNode]
		val child1Node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.mixinNodeTypes).thenReturn(Set.empty[String])
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(child1Node.name).thenReturn("child1")
		when(child1Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.children).thenReturn(Some(Iterator(child1Node)))
		when(node.child("child1")).thenReturn(Some(child1Node))
		validator.validate(node)
	}

	it should "pass a wildcarded node that has children that are undefined in its mixin type" in {
		val node = mock[ISessionNode]
		val child1Node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:structured:1")
		when(node.mixinNodeTypes).thenReturn(Set("mix:unstructured:1"))
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(child1Node.name).thenReturn("child1")
		when(child1Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.children).thenReturn(Some(Iterator(child1Node)))
		when(node.child("child1")).thenReturn(Some(child1Node))
		validator.validate(node)
	}

	it should "fail a node that has more typed children than are allowed by its primary nodetype" in {
		val node = mock[ISessionNode]
		val child1Node = mock[ISessionNode]
		val child2Node = mock[ISessionNode]
		val child3Node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:typedchildren:1")
		when(node.mixinNodeTypes).thenReturn(Set.empty[String])
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(child1Node.name).thenReturn("child1")
		when(child1Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(child2Node.name).thenReturn("child2")
		when(child2Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(child3Node.name).thenReturn("child3")
		when(child3Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.children).thenReturn(Some(Iterator(child1Node, child2Node, child3Node)))
		when(node.child("child1")).thenReturn(Some(child1Node))
		when(node.child("child2")).thenReturn(Some(child2Node))
		when(node.child("child3")).thenReturn(Some(child3Node))
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "fail a node that has fewer typed children than are allowed by its primary nodetype" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:typedchildren:1")
		when(node.mixinNodeTypes).thenReturn(Set.empty[String])
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(node.children).thenReturn(None)
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "fail a node that has more typed children than are allowed by its mixin nodetype" in {
		val node = mock[ISessionNode]
		val child1Node = mock[ISessionNode]
		val child2Node = mock[ISessionNode]
		val child3Node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.mixinNodeTypes).thenReturn(Set("mix:typedchildren:1"))
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(child1Node.name).thenReturn("child1")
		when(child1Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(child2Node.name).thenReturn("child2")
		when(child2Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(child3Node.name).thenReturn("child3")
		when(child3Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.children).thenReturn(Some(Iterator(child1Node, child2Node, child3Node)))
		when(node.child("child1")).thenReturn(Some(child1Node))
		when(node.child("child2")).thenReturn(Some(child2Node))
		when(node.child("child3")).thenReturn(Some(child3Node))
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "fail a node that has fewer typed children than are allowed by its mixin nodetype" in {
		val node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.mixinNodeTypes).thenReturn(Set("mix:typedchildren:1"))
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(node.children).thenReturn(None)
		assertThrows[NodeTypeConstraintViolationException] {
			validator.validate(node)
		}
	}

	it should "pass a node that has the correct number of typed children allowed by its primary nodetype" in {
		val node = mock[ISessionNode]
		val child1Node = mock[ISessionNode]
		val child2Node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn("tm:typedchildren:1")
		when(node.mixinNodeTypes).thenReturn(Set.empty[String])
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(child1Node.name).thenReturn("child1")
		when(child1Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(child2Node.name).thenReturn("child2")
		when(child2Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.children).thenReturn(Some(Iterator(child1Node, child2Node)))
		when(node.child("child1")).thenReturn(Some(child1Node))
		when(node.child("child2")).thenReturn(Some(child2Node))
		validator.validate(node)
	}

	it should "pass a node that has the correct number of typed children allowed by its mixin nodetype" in {
		val node = mock[ISessionNode]
		val child1Node = mock[ISessionNode]
		val child2Node = mock[ISessionNode]
		when(node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.mixinNodeTypes).thenReturn(Set("mix:typedchildren:1"))
		when(node.properties).thenReturn(Map.empty[String, Any])
		when(child1Node.name).thenReturn("child1")
		when(child1Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(child2Node.name).thenReturn("child2")
		when(child2Node.primaryNodeType).thenReturn(UNSTRUCTURED_NODE_TYPE)
		when(node.children).thenReturn(Some(Iterator(child1Node, child2Node)))
		when(node.child("child1")).thenReturn(Some(child1Node))
		when(node.child("child2")).thenReturn(Some(child2Node))
		validator.validate(node)
	}


}
