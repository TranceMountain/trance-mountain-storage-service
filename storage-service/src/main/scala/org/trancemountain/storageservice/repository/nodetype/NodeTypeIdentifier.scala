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

import java.util.regex.Pattern

object NodeTypeIdentifier {
	private val nodeTypePattern = Pattern.compile("([A-Za-z]+):([\\w]+):([\\d]+)")

	def fromString(str: String): NodeTypeIdentifier = {
			val matcher = nodeTypePattern.matcher(str)
			if (matcher.matches()) {
				val namespace = matcher.group(1)
				val name = matcher.group(2)
				val version = matcher.group(3).toInt
				NodeTypeIdentifier(namespace, name, version)
			} else throw new RuntimeException(s"Cannot parse nodetype string $str")
	}
}

/**
	* @author michaelcoddington
	*/
case class NodeTypeIdentifier(namespace: String, name: String, version: Int = 1) {
	require(namespace != null, "Cannot create nodetype identifier with null namespace")
	require(name != null, "Cannot create nodetype identifier with null name")

	override def toString: String = s"$namespace:$name:$version"
}
