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

/**
	* Extends [[org.trancemountain.storageservice.repository.INode]] with convenience methods
	* for navigating parent/child relationships and quickly extracting properties with known
	* types (rather than getting properties as Any and having to cast them).
	*
	* @author michaelcoddington
	*/
trait ISessionNode extends INode {
	def parent: ISessionNode
	def children: Option[Iterator[ISessionNode]]
	def child(name: String): Option[ISessionNode]

	def createChild(name: String, nodeType: String, properties: Map[String, Any]): ISessionNode

	def int(name: String): Int
	def intArray(name: String): Array[Int]
	def long(name: String): Long
	def longArray(name: String): Array[Long]
	def double(name: String): Double
	def doubleArray(name: String): Array[Double]
	def string(name: String): String
	def stringArray(name: String): Array[String]
	def date(name: String): Date
	def boolean(name: String): Boolean
	def binary(name: String): IBinary
	def strongReference(name: String): StrongReference
	def weakReference(name: String): WeakReference

	def delete()

}
