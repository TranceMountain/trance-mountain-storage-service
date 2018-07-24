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

import java.math.BigInteger

/**
	* @author michaelcoddington
	*/
class RepositoryRevisionNumber(protected val bigInteger: BigInteger) extends Ordered[RepositoryRevisionNumber] {
	require(bigInteger.compareTo(new BigInteger("-1", 10)) == 1, "Repository revision numbers must be non-negative")

	def this(hexString: String) = this(new BigInteger(hexString, 16))

	def this(i: Int) = this(new BigInteger(i.toString, 10))

	def +(i: Int) = new RepositoryRevisionNumber(bigInteger.add(new BigInteger(i.toString, 10)))

	def -(i: Int) = new RepositoryRevisionNumber(bigInteger.subtract(new BigInteger(i.toString, 10)))

	override def toString: String = bigInteger.toString(16)

	override def equals(obj: Any): Boolean = {
		obj match {
			case rev: RepositoryRevisionNumber => rev.bigInteger.equals(bigInteger)
			case i: Int => bigInteger.intValue() == i
			case l: Long => bigInteger.longValue() == l
			case _ => false
		}
	}

	override def compare(that: RepositoryRevisionNumber): Int = this.bigInteger.compareTo(that.bigInteger)

	def compareToHex(hexString: String) = this.compare(new RepositoryRevisionNumber(hexString))

}
