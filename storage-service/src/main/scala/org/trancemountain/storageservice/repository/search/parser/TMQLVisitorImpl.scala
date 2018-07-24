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

package org.trancemountain.storageservice.repository.search.parser

import java.io.StringReader

import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream, Token}
import org.apache.commons.io.input.ReaderInputStream
import org.slf4j.LoggerFactory
import org.trancemountain.storageservice.repository.nodetype.NodeTypeIdentifier
import org.trancemountain.storageservice.repository.search.IQuery
import org.trancemountain.storageservice.repository.search.parser.antlr.TMQLParser._
import org.trancemountain.storageservice.repository.search.parser.antlr.{TMQLBaseVisitor, TMQLLexer, TMQLParser}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
	* @author michaelcoddington
	*/
object TMQLVisitorImpl {
	private val LOG = LoggerFactory.getLogger(getClass)
}

class TMQLVisitorImpl extends TMQLBaseVisitor[Unit] {

	import TMQLVisitorImpl.LOG

	private val selectedAliasList = ListBuffer.empty[String]
	private val fromClauseIdentifierList = ListBuffer.empty[FromClauseIdentifier]
	private val filterPredicateCollector = new FilterPredicateCollector
	private val joinClauseCollector = new JoinClauseCollector

	/**
		* Collects and groups filter predicates.
		*/
	private class FilterPredicateCollector {
		private var filterPredicateList = ListBuffer.empty[IFilterPredicate]
		private val filterPredicateBufferStack = new mutable.Stack[mutable.ListBuffer[IFilterPredicate]]

		def startFilterPredicateCollection(): Unit = {
			pushFilterPredicateStack()
		}

		def endFilterPredicateCollection(predFunction: (Seq[IFilterPredicate]) => IFilterPredicate): Unit = {
			val previousSeq = popFilterPredicateStack()
			addFilterPredicate(predFunction(previousSeq))
		}

		def addFilterPredicate(pred: IFilterPredicate): Unit = filterPredicateList += pred

		def filterPredicates: List[IFilterPredicate] = filterPredicateList.toList

		def clear(): Unit = {
			filterPredicateList.clear()
			filterPredicateBufferStack.clear()
		}

		private def pushFilterPredicateStack(): Unit = {
			filterPredicateBufferStack.push(filterPredicateList)
			filterPredicateList = new mutable.ListBuffer[IFilterPredicate]
		}

		private def popFilterPredicateStack(): mutable.ListBuffer[IFilterPredicate] = {
			LOG.debug("Popping filter predicate stack")
			val currentBuf = filterPredicateList
			filterPredicateList = filterPredicateBufferStack.pop()
			currentBuf
		}
	}

	/**
		* Collects join statements.
		*/
	private class JoinClauseCollector {

		private val joinClauseBuffer = ListBuffer.empty[JoinClause]
		private var joinPredicateCollector: JoinPredicateCollector = _

		def startJoinClause(): Unit = {
			LOG.info("starting join statement")
			joinPredicateCollector = new JoinPredicateCollector
		}

		def startJoinPredicateCollection(): Unit = {
			joinPredicateCollector.pushJoinPredicateStack()
		}

		def endJoinPredicateCollection(predFunction: (Seq[IJoinPredicate]) => IJoinPredicate): Unit = {
			val previousSeq = joinPredicateCollector.popJoinPredicateStack()
			addJoinPredicate(predFunction(previousSeq))
		}

		def addJoinPredicate(pred: IJoinPredicate): Unit = joinPredicateCollector.addJoinPredicate(pred)

		def endJoinClause(joinType: String): Unit = {
			LOG.info("ending join statement")
			joinClauseBuffer += JoinClause(joinType, joinPredicateCollector.joinPredicates.map(_.simplify()))
			joinPredicateCollector.clear()
		}

		def clear(): Unit = {
			joinClauseBuffer.clear()
			if (joinPredicateCollector != null) joinPredicateCollector.clear()
		}

		def joinClauses: List[JoinClause] = joinClauseBuffer.size match {
			case 0 => null
			case _ => joinClauseBuffer.toList
		}

		/**
			* Collects and groups join predicates.
			*/
		class JoinPredicateCollector {
			private var joinPredicateList = ListBuffer.empty[IJoinPredicate]
			private val joinPredicateBufferStack = new mutable.Stack[mutable.ListBuffer[IJoinPredicate]]

			def startJoinPredicateCollection(): Unit = {
				pushJoinPredicateStack()
			}

			def endJoinPredicateCollection(predFunction: (Seq[IJoinPredicate]) => IJoinPredicate): Unit = {
				val previousSeq = popJoinPredicateStack()
				addJoinPredicate(predFunction(previousSeq))
			}

			def addJoinPredicate(pred: IJoinPredicate): Unit = joinPredicateList += pred

			def joinPredicates: List[IJoinPredicate] = joinPredicateList.toList

			def clear(): Unit = {
				joinPredicateList.clear()
				joinPredicateBufferStack.clear()
			}

			def pushJoinPredicateStack(): Unit = {
				joinPredicateBufferStack.push(joinPredicateList)
				joinPredicateList = new mutable.ListBuffer[IJoinPredicate]
			}

			def popJoinPredicateStack(): mutable.ListBuffer[IJoinPredicate] = {
				val currentBuf = joinPredicateList
				joinPredicateList = joinPredicateBufferStack.pop()
				currentBuf
			}
		}

	}


	def parse(queryString: String): IQuery = {
		val ais = new ANTLRInputStream(new ReaderInputStream(new StringReader(queryString)))
		val lexer = new TMQLLexer(ais)
		val tokens = new CommonTokenStream(lexer)
		val parser = new TMQLParser(tokens)
		val statementTree = parser.selectStatement()

		LOG.info("Starting parse")

		visit(statementTree)

		LOG.info("Parse complete")
		val select = SelectClause(selectedAliasList.toList)
		LOG.info(s"Using select $select")
		val from = FromClause(fromClauseIdentifierList.toList)
		LOG.info(s"Using from $from")
		val filterPredicates = filterPredicateCollector.filterPredicates
		val filter = filterPredicates.size match {
			case 0 => null
			case _ => FilterClause(filterPredicates.map(_.simplify()))
		}
		val joinClauses = joinClauseCollector.joinClauses

		selectedAliasList.clear()
		fromClauseIdentifierList.clear()
		filterPredicateCollector.clear()
		joinClauseCollector.clear()

		Query(select, from, filter, joinClauses)
	}

	/**
		* Extracts named select parameters from the parse tree
		* @param ctx the parse tree
		*/
	override def visitNamedIDs(ctx: NamedIDsContext): Unit = {
		super.visitNamedIDs(ctx)
		val selectNames = for (i <- 0 until ctx.ids.size()) yield ctx.ids.get(i).getText
		selectedAliasList ++= selectNames
	}

	override def visitWildcardID(ctx: WildcardIDContext): Unit = {
		super.visitWildcardID(ctx)
		selectedAliasList += ctx.getText
	}

	override def visitAliasedNodeTypeIdentifier(ctx: AliasedNodeTypeIdentifierContext): Unit = {
		super.visitAliasedNodeTypeIdentifier(ctx)
		val nodeType = ctx.nodetype
		val namespace = nodeType.namespace.getText
		val name = nodeType.name.getText
		val version = nodeType.version match {
			case t: Token => t.getText.toInt
			case null => 1
		}
		val identifier = ctx.identifier.getText
		fromClauseIdentifierList += FromClauseIdentifier(NodeTypeIdentifier(namespace, name, version), identifier)
	}

	override def visitFilterStatement(ctx: FilterStatementContext): Unit = {
		LOG.info("Visiting filter statement")
		super.visitFilterStatement(ctx)
	}

	override def visitGroupFilterPredicate(ctx: GroupFilterPredicateContext): Unit = {
		LOG.info("Visiting group filter predicate")
		filterPredicateCollector.startFilterPredicateCollection()
		super.visitGroupFilterPredicate(ctx)
		filterPredicateCollector.endFilterPredicateCollection((groupPredicates) => FilterGroupPredicate(groupPredicates.head))
	}

	override def visitFilterOrPredicate(ctx: FilterOrPredicateContext): Unit = {
		LOG.info("Visiting or filter predicate")
		filterPredicateCollector.startFilterPredicateCollection()
		super.visitFilterOrPredicate(ctx)
		filterPredicateCollector.endFilterPredicateCollection((orPredicates) => FilterOrPredicate(orPredicates))
	}

	override def visitFilterAndPredicate(ctx: FilterAndPredicateContext): Unit = {
		LOG.info(s"Visiting and filter predicate")
		filterPredicateCollector.startFilterPredicateCollection()
		super.visitFilterAndPredicate(ctx)
		filterPredicateCollector.endFilterPredicateCollection((andPredicates) => FilterAndPredicate(andPredicates))
	}

	override def visitFilterNotPredicate(ctx: FilterNotPredicateContext): Unit = {
		LOG.info("Visiting not filter predicate")
		filterPredicateCollector.startFilterPredicateCollection()
		super.visitFilterNotPredicate(ctx)
		val notText = ctx.notFlag match {
			case t: Token => t.getText
			case null => null
		}
		filterPredicateCollector.endFilterPredicateCollection((notPredicates) => FilterNotPredicate(notText, notPredicates.head))
	}

	override def visitFilterPropertyUnaryClause(ctx: FilterPropertyUnaryClauseContext): Unit = {
		LOG.info(s"Visiting unary property predicate: ${ctx.alias.getText}.${ctx.property.getText} ${ctx.unaryOperator}")
		super.visitFilterPropertyUnaryClause(ctx)
		val simpleUnaryPredicate = FilterUnaryPropertyPredicate(ctx.alias.getText, ctx.property.getText, ctx.unaryOperator.getText)
		filterPredicateCollector.addFilterPredicate(simpleUnaryPredicate)
	}

	override def visitFilterPropertyBinaryClause(ctx: FilterPropertyBinaryClauseContext): Unit = {
		LOG.info(s"Visiting binary property predicate: ${ctx.alias.getText}.${ctx.property.getText} ${ctx.binaryOperator.getText} ${ctx.value.getText}")
		super.visitFilterPropertyBinaryClause(ctx)
		val simpleBinaryPredicate = FilterBinaryPropertyPredicate(ctx.alias.getText, ctx.property.getText, ctx.binaryOperator.getText, ctx.value.getText)
		filterPredicateCollector.addFilterPredicate(simpleBinaryPredicate)
	}

	override def visitFilterPropertyUnaryFunctionClause(ctx: FilterPropertyUnaryFunctionClauseContext): Unit = {
		LOG.info(s"Visiting unary function predicate")
		super.visitFilterPropertyUnaryFunctionClause(ctx)
		val propertyArgsTexts = scala.collection.JavaConverters.asScalaBuffer(ctx.propertyArgs).map(_.getText).toList
		val simpleUnaryFunctionPredicate = FilterUnaryPropertyFunctionPredicate(ctx.alias.getText, ctx.property.getText, ctx.propertyFunction.getText, propertyArgsTexts)
		filterPredicateCollector.addFilterPredicate(simpleUnaryFunctionPredicate)
	}

	override def visitFilterPropertyBinaryFunctionClause(ctx: FilterPropertyBinaryFunctionClauseContext): Unit = {
		LOG.info(s"Visiting binary function predicate")
		super.visitFilterPropertyBinaryFunctionClause(ctx)
		val binFunctionPred = FilterBinaryPropertyFunctionPredicate(ctx.alias.getText, ctx.property.getText, ctx.propertyFunction.getText, ctx.binaryOperator.getText, ctx.value.getText)
		filterPredicateCollector.addFilterPredicate(binFunctionPred)
	}

	override def visitJoinStatement(ctx: JoinStatementContext): Unit = {
		val joinType = ctx.joinType.getText
		LOG.info(s"Visiting join statement with type $joinType")
		joinClauseCollector.startJoinClause()
		super.visitJoinStatement(ctx)
		joinClauseCollector.endJoinClause(joinType)
	}

	override def visitGroupJoinPredicate(ctx: GroupJoinPredicateContext): Unit = {
		joinClauseCollector.startJoinPredicateCollection()
		super.visitGroupJoinPredicate(ctx)
		joinClauseCollector.endJoinPredicateCollection((groupPredicates) => JoinGroupPredicate(groupPredicates.head))
	}

	override def visitJoinAndPredicate(ctx: JoinAndPredicateContext): Unit = {
		LOG.info(s"Visiting and group predicate")
		joinClauseCollector.startJoinPredicateCollection()
		super.visitJoinAndPredicate(ctx)
		joinClauseCollector.endJoinPredicateCollection((andPredicates) => JoinAndPredicate(andPredicates))
	}

	override def visitJoinNotPredicate(ctx: JoinNotPredicateContext): Unit = {
		LOG.info("Visiting not join predicate")
		joinClauseCollector.startJoinPredicateCollection()
		super.visitJoinNotPredicate(ctx)
		val notText = ctx.notFlag match {
			case t: Token => t.getText
			case null => null
		}
		joinClauseCollector.endJoinPredicateCollection((notPredicates) => JoinNotPredicate(notText, notPredicates.head))
	}

	override def visitJoinOrPredicate(ctx: JoinOrPredicateContext): Unit = {
		LOG.info("Visiting or filter predicate")
		joinClauseCollector.startJoinPredicateCollection()
		super.visitJoinOrPredicate(ctx)
		joinClauseCollector.endJoinPredicateCollection((orPredicates) => JoinOrPredicate(orPredicates))
	}

	override def visitJoinCriteria(ctx: JoinCriteriaContext): Unit = {
		LOG.info("Visiting join criteria")
		super.visitJoinCriteria(ctx)
		val functionParam = ctx.functionParam match {
			case token: Token => token.getText
			case null => null
		}
		val atParam = ctx.atParam match {
			case token: Token => token.getText
			case null => null
		}
		val joinPred = JoinCriteriaPredicate(ctx.identifier.getText, ctx.functionName.getText, ctx.functionIdentifier.getText, functionParam, atParam)
		joinClauseCollector.addJoinPredicate(joinPred)
	}

}
