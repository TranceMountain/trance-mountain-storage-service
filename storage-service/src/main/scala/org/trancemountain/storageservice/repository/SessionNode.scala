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

import java.util.Date

import org.slf4j.LoggerFactory
import org.trancemountain.storageservice.repository.store.{StoredStrongReference, StoredWeakReference}

import scala.reflect.ClassTag

object SessionNode {
	private val LOG = LoggerFactory.getLogger(getClass)
}

/**
	* @author michaelcoddington
	*/
class SessionNode protected[repository](protected val session: ISession, protected val baseNode: INode) extends ISessionNode {

	override def parent: ISessionNode = path match {
		case "/" => null
		case s: String =>
			val pathParts = s.split("/").dropRight(1)
			if (pathParts.length == 1 && pathParts(0) == "") session.rootNode
			else session.node(pathParts.mkString("/")).get
	}

	override def createChild(name: String, nodeType: String, properties: Map[String, Any]): ISessionNode = throw new RuntimeException("not implemented")

	override def id: String = baseNode.id

	override def children: Option[Iterator[ISessionNode]] = session.children(this)

	override def child(name: String): Option[ISessionNode] = throw new RuntimeException("not implemented")

	override def update(name: String, value: Any): Unit = {
		value match {
			case sr: StrongReference => baseNode(name) = StoredStrongReference(path, name, sr.targetNode.path)
			case wr: WeakReference => baseNode(name) = StoredWeakReference(path, name, wr.targetNode.path)
			case other => baseNode(name) = other
		}
	}

	override def apply(name: String): Option[Any] = baseNode(name)

	override def path: String = baseNode.path

	override def name: String = baseNode.name

	override def revision: RepositoryRevisionNumber = baseNode.revision

	override def primaryNodeType: String = baseNode.primaryNodeType

	override def mixinNodeTypes: Set[String] = baseNode.mixinNodeTypes

	override def addMixinNodeType(mixinType: String): Unit = baseNode.addMixinNodeType(mixinType)

	override def removeMixinNodeType(mixinType: String): Unit = baseNode.removeMixinNodeType(mixinType)

	override def properties: Map[String, Any] = baseNode.properties

	override def delete(): Unit = session.delete(this)

	override def isDeleted() = baseNode.isDeleted()

	override def toString = s"Node [session=$session, baseNode=$baseNode]"

	override def equals(obj: scala.Any): Boolean = {
		obj match {
			case n: SessionNode => session == n.session && baseNode == n.baseNode
			case bn: INode => baseNode == bn
			case _ => false
		}
	}

	override def int(name: String): Int = getProperty(name, classOf[Int])

	override def intArray(name: String): Array[Int]= getProperty(name, classOf[Array[Int]])

	override def long(name: String): Long = getProperty(name, classOf[Long])

	override def longArray(name: String): Array[Long] = getProperty(name, classOf[Array[Long]])

	override def double(name: String): Double = getProperty(name, classOf[Double])

	override def doubleArray(name: String): Array[Double] = getProperty(name, classOf[Array[Double]])

	override def string(name: String): String = getProperty(name, classOf[String])

	override def stringArray(name: String): Array[String] = getProperty(name, classOf[Array[String]])

	override def date(name: String): Date = getProperty(name, classOf[Date])

	override def boolean(name: String): Boolean = getProperty(name, classOf[Boolean])

	override def binary(name: String): IBinary = getProperty(name, classOf[IBinary])

	override protected[repository] def setRevision(revision: RepositoryRevisionNumber): Unit = {
		throw new RuntimeException("not allowed")
	}

	override def strongReference(name: String): StrongReference = {
		val ssr = getProperty(name, classOf[StoredStrongReference])
		StrongReference(session.node(ssr.targetNodePath).get)
	}

	override def weakReference(name: String): WeakReference = {
		val swr = getProperty(name, classOf[StoredWeakReference])
		WeakReference(session.node(swr.targetNodePath).get)
	}

	private def getProperty[T](name: String, clazz: Class[T])(implicit tag: ClassTag[T]): T = {
		val propOptional = this(name)
		propOptional match {
			case Some(t: T) => t
			case Some(other) =>
				throw new RuntimeException(s"Typecast exception. Expected: $clazz, Received: ${other.getClass}")
			case other => throw new RuntimeException(s"Typecast exception. Expected: $clazz, Received: ${other.getClass}")
		}
	}
}
