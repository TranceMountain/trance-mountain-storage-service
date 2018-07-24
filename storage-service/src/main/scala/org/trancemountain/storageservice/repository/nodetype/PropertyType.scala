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

/**
	* @author michaelcoddington
	*/
sealed trait PropertyType {
	val typeName: String
}

object PropertyType {
	val BOOLEAN_NAME = "boolean"
	val STRING_NAME = "string"
	val STRING_ARRAY_NAME = "string[]"
	val INT_NAME = "int"
	val INT_ARRAY_NAME = "int[]"
	val LONG_NAME = "long"
	val LONG_ARRAY_NAME = "long[]"
	val DOUBLE_NAME = "double"
	val DOUBLE_ARRAY_NAME = "double[]"
	val DATE_NAME = "date"
	val BINARY_NAME = "binary"
	val STRONG_REF_NAME = "strongReference"
	val WEAK_REF_NAME = "weakReference"

	case object BOOLEAN extends PropertyType { val typeName = BOOLEAN_NAME }
	case object STRING extends PropertyType { val typeName = STRING_NAME }
	case object STRING_ARRAY extends PropertyType { val typeName =  STRING_ARRAY_NAME }
	case object INT extends PropertyType { val typeName = INT_NAME }
	case object INT_ARRAY extends PropertyType { val typeName = INT_ARRAY_NAME }
	case object LONG extends PropertyType { val typeName = LONG_NAME }
	case object LONG_ARRAY extends PropertyType { val typeName = LONG_ARRAY_NAME }
	case object DOUBLE extends PropertyType { val typeName = DOUBLE_NAME }
	case object DOUBLE_ARRAY extends PropertyType { val typeName = DOUBLE_ARRAY_NAME }
	case object DATE extends PropertyType { val typeName = DATE_NAME }
	case object BINARY extends PropertyType { val typeName = BINARY_NAME }
	case object STRONG_REF extends PropertyType { val typeName = STRONG_REF_NAME }
	case object WEAK_REF extends PropertyType { val typeName = WEAK_REF_NAME }

	def withName(s: String): PropertyType = s match {
		case BOOLEAN_NAME => BOOLEAN
		case STRING_NAME => STRING
		case STRING_ARRAY_NAME => STRING_ARRAY
		case INT_NAME => INT
		case INT_ARRAY_NAME => INT_ARRAY
		case LONG_NAME => LONG
		case LONG_ARRAY_NAME => LONG_ARRAY
		case DOUBLE_NAME => DOUBLE
		case DOUBLE_ARRAY_NAME => DOUBLE_ARRAY
		case DATE_NAME => DATE
		case BINARY_NAME => BINARY
		case STRONG_REF_NAME => STRONG_REF
		case WEAK_REF_NAME => WEAK_REF
	}

}
