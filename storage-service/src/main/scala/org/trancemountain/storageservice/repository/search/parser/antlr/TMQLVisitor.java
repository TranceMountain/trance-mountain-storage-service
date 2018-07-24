// Generated from /Users/michaelcoddington/IdeaProjects/Trance Mountain/trance-mountain-storage-service/src/main/resources/tmql/TMQL.g4 by ANTLR 4.5.3
package org.trancemountain.storageservice.repository.search.parser.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link TMQLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface TMQLVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link TMQLParser#selectStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectStatement(TMQLParser.SelectStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code wildcardID}
	 * labeled alternative in {@link TMQLParser#identifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWildcardID(TMQLParser.WildcardIDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code namedIDs}
	 * labeled alternative in {@link TMQLParser#identifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedIDs(TMQLParser.NamedIDsContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#nodeTypeIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNodeTypeIdentifier(TMQLParser.NodeTypeIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#aliasedNodeTypeIdentifierSequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasedNodeTypeIdentifierSequence(TMQLParser.AliasedNodeTypeIdentifierSequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#aliasedNodeTypeIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasedNodeTypeIdentifier(TMQLParser.AliasedNodeTypeIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#filterStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterStatement(TMQLParser.FilterStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#filterOrPredicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterOrPredicate(TMQLParser.FilterOrPredicateContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#filterAndPredicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterAndPredicate(TMQLParser.FilterAndPredicateContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#filterNotPredicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterNotPredicate(TMQLParser.FilterNotPredicateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code groupFilterPredicate}
	 * labeled alternative in {@link TMQLParser#filterBasePredicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupFilterPredicate(TMQLParser.GroupFilterPredicateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleFilterPredicate}
	 * labeled alternative in {@link TMQLParser#filterBasePredicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleFilterPredicate(TMQLParser.SimpleFilterPredicateContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#filterPropertyUnaryClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterPropertyUnaryClause(TMQLParser.FilterPropertyUnaryClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#filterPropertyBinaryClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterPropertyBinaryClause(TMQLParser.FilterPropertyBinaryClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#filterPropertyUnaryFunctionClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterPropertyUnaryFunctionClause(TMQLParser.FilterPropertyUnaryFunctionClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#filterPropertyBinaryFunctionClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterPropertyBinaryFunctionClause(TMQLParser.FilterPropertyBinaryFunctionClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#joinStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinStatement(TMQLParser.JoinStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#joinOrPredicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinOrPredicate(TMQLParser.JoinOrPredicateContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#joinAndPredicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinAndPredicate(TMQLParser.JoinAndPredicateContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#joinNotPredicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinNotPredicate(TMQLParser.JoinNotPredicateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code groupJoinPredicate}
	 * labeled alternative in {@link TMQLParser#joinBasePredicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupJoinPredicate(TMQLParser.GroupJoinPredicateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleJoinPredicate}
	 * labeled alternative in {@link TMQLParser#joinBasePredicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleJoinPredicate(TMQLParser.SimpleJoinPredicateContext ctx);
	/**
	 * Visit a parse tree produced by {@link TMQLParser#joinCriteria}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinCriteria(TMQLParser.JoinCriteriaContext ctx);
}