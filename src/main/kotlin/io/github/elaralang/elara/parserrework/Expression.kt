package io.github.elaralang.elara.parserrework

import io.github.elaralang.elara.evaluator.ElaraEvaluator
import io.github.elaralang.elara.lexer.Token


sealed class Expression {
    abstract fun accept(elaraEvaluator: ElaraEvaluator): Any
}

class Operation(token: Token){
    val op = Operator.parseFrom(token)

    override fun toString(): String {
        return "Operator($op)"
    }
}
class BinaryExpression(val lhs: Expression, val op: Operation, val rhs: Expression): Expression() {
    override fun accept(elaraEvaluator: ElaraEvaluator): Any {
        return elaraEvaluator.visitBinaryExpr(this)
    }

    override fun toString(): String {
        return "($lhs : ${op.op.name} : $rhs)"
    }
}
class UnaryExpression(val op: Operation, val rhs: Expression): Expression() {
    override fun accept(elaraEvaluator: ElaraEvaluator): Any {
        return elaraEvaluator.visitUnaryExpr(this)
    }

    override fun toString(): String {
        return "($op : $rhs)"
    }
}
class LogicalExpression(val lhs: Expression, val op: Operation, val rhs: Expression): Expression() {
    override fun accept(elaraEvaluator: ElaraEvaluator): Any {
        return elaraEvaluator.visitLogicalExpr(this)
    }

    override fun toString(): String {
        return "($lhs : ${op.op.name} : $rhs)"
    }
}
class GroupExpression(val expr: Expression): Expression() {
    override fun accept(elaraEvaluator: ElaraEvaluator): Any {
        return elaraEvaluator.evaluate(expr)
    }

    override fun toString(): String {
        return "(Grouped ($expr))"
    }
}
class Literal<T>(val value: T): Expression() where T: Any {
    override fun accept(elaraEvaluator: ElaraEvaluator): Any {
        return elaraEvaluator.visitLiteral(this)
    }

    override fun toString(): String {
        return "(Literal ($value))"
    }
}
class Variable(val identifier: String): Expression()  {
    override fun accept(elaraEvaluator: ElaraEvaluator): Any {
        return elaraEvaluator.visitVariable(this)
    }

    override fun toString(): String {
        return "(Variable [$identifier])"
    }
}

class Assignment(val identifier: String, val value: Expression): Expression()  {
    override fun accept(elaraEvaluator: ElaraEvaluator): Any {
        return elaraEvaluator.visitAssignment(this)
    }

    override fun toString(): String {
        return "(Assignment [$identifier] = [$value])"
    }
}
class FunctionInvocation(val invoker: Expression, val parameters: List<Expression>): Expression()  {
    override fun accept(elaraEvaluator: ElaraEvaluator): Any {
        return elaraEvaluator.visitInvocation(this)
    }

    override fun toString(): String {
        return "([$invoker] FunctionInvocation => [$parameters])"
    }
}
class FunctionDefinition(val returnType: Expression, val arguments: List<FunctionArgument>, val body: Statement): Expression()  {
    override fun accept(elaraEvaluator: ElaraEvaluator): Any {
        return elaraEvaluator.visitFunctionDefinition(this)
    }

    override fun toString(): String {
        return "([$returnType] FunctionDefinition [$arguments] => { $body })"
    }
}

object ElaraUnit : Expression() {
    override fun accept(elaraEvaluator: ElaraEvaluator): Any {
        return Unit
    }

    override fun toString(): String {
        return "(ElaraUnit)"
    }
}

interface ExpressionVisitor<T> {
    fun visitLiteral(literal: Literal<*>): T
    fun visitGroupExpr(grpExpr: GroupExpression): T
    fun visitBinaryExpr(binaryExpr: BinaryExpression): T
    fun visitUnaryExpr(unaryExpr: UnaryExpression): T
    fun visitLogicalExpr(assignment: LogicalExpression): T
    fun visitVariable(variableExpr: Variable): T
    fun visitAssignment(assignment: Assignment): T
    fun visitInvocation(invoke: FunctionInvocation): T
    fun visitFunctionDefinition(funcDef: FunctionDefinition): T
}
