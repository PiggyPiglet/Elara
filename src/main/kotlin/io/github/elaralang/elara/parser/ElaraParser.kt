package io.github.elaralang.elara.parser

import io.github.elaralang.elara.exceptions.invalidSyntax
import io.github.elaralang.elara.lexer.ElaraLexer
import io.github.elaralang.elara.lexer.Token
import io.github.elaralang.elara.lexer.TokenType
import java.util.*

class ElaraParser(tokenList: List<Token>) {

    private val tokens = Stack<Token>()

    init {
        tokens.addAll(tokenList)
        tokens.reverse()
    }

    fun parse(): RootNode {
        val rootNode = RootNode()
        while (tokens.isNotEmpty()) {
            val child = parseExpression()
            child?.let { rootNode.addChild(it) }

        }
        return rootNode
    }

    private fun parseToken(token: Token?): ASTNode? {
        if (token == null) return null
        return when (token.type) {
            TokenType.IDENTIFIER -> IdentifierNode(token.text)
            TokenType.NUMBER -> NumberNode(token.text.toLong())
            TokenType.STRING -> StringNode(token.text)
            else -> invalidSyntax("Invalid expression at ${token.text}")
        }
    }

    private fun parseExpression(lastToken: Token? = null,endTokens: Set<TokenType>? = null): ASTNode? {
        val currentToken = tokens.pop()
        if (endTokens != null && currentToken.type in endTokens) {
            tokens.add(currentToken)
            return lastToken?.let { parseToken(it) ?: invalidSyntax("Invalid Expression!") }
        }

        if (lastToken == null) {
            //No prior context

            return when (currentToken.type) {
                // let test = <expression>
                TokenType.LET -> {
                    parseDeclaration()
                }
                // ambiguous => requires next Token for context
                TokenType.IDENTIFIER -> {
                    parseExpression(currentToken, endTokens)
                }
                TokenType.NUMBER -> {
                    NumberNode(currentToken.text.toLong())
                }
                TokenType.LBRACE -> {
                    parseScope()
                }
                TokenType.COLON -> {
                    parseFunction()
                }
                TokenType.NEWLINE -> {
                    parseExpression(endTokens =  endTokens)
                }
                else -> null
            }

        } else {
            // Check last context

            return when (lastToken.type) {
                TokenType.IDENTIFIER -> {
                    when (currentToken.type) {
                        TokenType.LPAREN -> {
                            parseFunctionCall(lastToken, TokenType.COMMA, TokenType.RPAREN)
                        }
                        TokenType.DEF -> {
                            parseAssignment(lastToken)
                        }
                        TokenType.NEWLINE -> {
                            parseToken(lastToken)
                        }
                        else -> null
                    }
                }
                else -> null
            }
        }
    }

    private fun parseScope(): ASTNode {
        val scope = ScopeNode()
        while (tokens.peek().type != TokenType.RBRACE) {
            val expression = parseExpression(endTokens = setOf(TokenType.RBRACE))
            if (expression != null)
                scope.addChild(expression)
        }
        tokens.pop()
        return scope
    }

    private fun parseParams(separator: TokenType?, endType: TokenType): ParameterNode {
        val paramNode = ParameterNode()
        val paramClosers = mutableSetOf(endType)
        if (separator != null) paramClosers.add(separator)
        while (tokens.peek().type != endType) {

            val param = parseExpression(null, paramClosers) ?: invalidSyntax("Invalid argument in function call")
            paramNode.addChild(param)
            if (separator != null) {
                if (tokens.peek().type !in setOf(separator,endType)) invalidSyntax("Invalid separator in function!")
                if (tokens.peek().type == separator) tokens.pop()
            }
        }
        tokens.pop()
        return paramNode
    }

    private fun parseFunctionCall(identifier: Token, separator: TokenType, endtype: TokenType): FunctionCallNode {
        val params = parseParams(separator, endtype)
        return FunctionCallNode(identifier.text, params)
    }

    private fun parseFunction(): FunctionNode {
        val lToken = tokens.pop()
        if (lToken.type != TokenType.LPAREN) invalidSyntax("Parameter not specified for function definition")
        val params = parseParams(TokenType.COMMA, TokenType.RPAREN)
        val arrow = tokens.pop()
        if (arrow.type != TokenType.ARROW) invalidSyntax("Function execution not defined!")
        val expression = parseExpression() ?: invalidSyntax("Function not defined properly!")
        return FunctionNode(params, expression)
    }

    private fun parseDeclaration(): DeclarationNode {
        var id = tokens.pop()

        val mutable: Boolean
        if (id.type == TokenType.MUT) {
            mutable = true
            id = tokens.pop()
        } else {
            mutable = false
        }

        if (id.type != TokenType.IDENTIFIER) {
            invalidSyntax("Identifier expected on declaration, found ${id.text} of type ${id.type}")
        }

        val eql = tokens.pop()

        if (eql.type != TokenType.DEF) {
            invalidSyntax("'=' expected on declaration, found ${id.text} of type ${id.type}")
        }

        val value = parseExpression() ?: invalidSyntax("Could not find expression to assign to ${id.text}")
        return DeclarationNode(id.text, mutable, value)
    }

    private fun parseAssignment(lastToken: Token): AssignmentNode {
        val value = parseExpression() ?: invalidSyntax("Value expected for assignment")
        return AssignmentNode(lastToken.text, value)
    }
}

