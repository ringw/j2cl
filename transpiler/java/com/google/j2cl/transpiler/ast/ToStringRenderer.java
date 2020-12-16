/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.ast;

import com.google.common.base.Strings;
import java.util.List;

/**
 * Produces a source code like representation of the AST for debugging purposes (not intending to
 * be completely valid Java).
 */
class ToStringRenderer {

  static String render(Node node) {
    final StringBuilder result = new StringBuilder();

    new AbstractVisitor() {
      int currentIndent = 0;

      @Override
      public boolean enterArrayAccess(ArrayAccess arrayAccess) {
        accept(arrayAccess.getArrayExpression());
        print("[");
        accept(arrayAccess.getIndexExpression());
        print("]");
        return false;
      }

      @Override
      public boolean enterArrayLiteral(ArrayLiteral arrayLiteral) {
        print("[");
        printSeparated(",", arrayLiteral.getValueExpressions());
        print("]");
        return false;
      }

      @Override
      public boolean enterAssertStatement(AssertStatement assertStatement) {
        print("assert ");
        accept(assertStatement.getExpression());
        if (assertStatement.getMessage() != null) {
          print(" : ");
          accept(assertStatement.getExpression());
        }
        print(";");
        return false;
      }

      @Override
      public boolean enterBinaryExpression(BinaryExpression binaryExpression) {
        print("(");
        accept(binaryExpression.getLeftOperand());
        print(" ");
        print(binaryExpression.getOperator().getSymbol());
        print(" ");
        accept(binaryExpression.getRightOperand());
        print(")");
        return false;
      }

      @Override
      public boolean enterBlock(Block block) {
        print("{");
        indent();
        for (Statement statement : block.getStatements()) {
          newLine();
          accept(statement);
        }
        unIndent();
        newLine();
        print("}");
        return false;
      }

      @Override
      public boolean enterBooleanLiteral(BooleanLiteral booleanLiteral) {
        print(booleanLiteral.getValue() ? "true" : "false");
        return false;
      }

      @Override
      public boolean enterBreakStatement(BreakStatement breakStatement) {
        print("break");
        if (breakStatement.getLabelReference() != null) {
          print(" ");
          accept(breakStatement.getLabelReference());
        }
        print(";");
        return false;
      }

      @Override
      public boolean enterCatchClause(CatchClause catchClause) {
        print(" catch (");
        print(catchClause.getExceptionVariable().getTypeDescriptor());
        print(" ");
        print(catchClause.getExceptionVariable().getName());
        print(") ");
        accept(catchClause.getBody());
        return false;
      }

      @Override
      public boolean enterCastExpression(CastExpression castExpression) {
        print("((");
        print(castExpression.getCastTypeDescriptor());
        print(") ");
        accept(castExpression.getExpression());
        print(")");
        return false;
      }

      @Override
      public boolean enterJsDocCastExpression(JsDocCastExpression castExpression) {
        print(
            String.format(
                "/** @type {%s} */ (",
                castExpression.getTypeDescriptor().getReadableDescription()));
        accept(castExpression.getExpression());
        print(")");
        return false;
      }

      @Override
      public boolean enterFieldDeclarationStatement(FieldDeclarationStatement fieldDeclaration) {
        print(
            String.format(
                "/** %s {%s} %s */ ",
                fieldDeclaration.isPublic() ? "@public" : "@private",
                fieldDeclaration.getFieldDescriptor().getTypeDescriptor().getReadableDescription(),
                fieldDeclaration.isConst() ? "@const" : ""));
        accept(fieldDeclaration.getExpression());
        return false;
      }

      @Override
      public boolean enterCompilationUnit(CompilationUnit compilationUnit) {
        print("package  " + compilationUnit.getPackageName() + ";");
        newLine();
        newLine();
        for (Type type : compilationUnit.getTypes()) {
          accept(type);
          newLine();
        }
        return false;
      }

      @Override
      public boolean enterConditionalExpression(ConditionalExpression conditionalExpression) {
        print("(");
        accept(conditionalExpression.getConditionExpression());
        print(") ? (");
        accept(conditionalExpression.getTrueExpression());
        print(") : (");
        accept(conditionalExpression.getFalseExpression());
        print(")");
        return false;
      }

      @Override
      public boolean enterContinueStatement(ContinueStatement continueStatement) {
        print("continue");
        if (continueStatement.getLabelReference() != null) {
          print(" ");
          accept(continueStatement.getLabelReference());
        }
        print(";");
        return false;
      }

      @Override
      public boolean enterDoWhileStatement(DoWhileStatement doWhileStatement) {
        print("do ");
        accept(doWhileStatement.getBody());
        print(" while (");
        accept(doWhileStatement.getConditionExpression());
        print(");");
        return false;
      }

      @Override
      public boolean enterExpression(Expression expression) {
        print("<expr>");
        return false;
      }

      @Override
      public boolean enterExpressionStatement(ExpressionStatement expressionStatement) {
        accept(expressionStatement.getExpression());
        print(";");
        return false;
      }

      @Override
      public boolean enterExpressionWithComment(ExpressionWithComment expression) {
        print("/*");
        print(expression.getComment());
        print("*/");
        accept(expression.getExpression());
        return false;
      }

      @Override
      public boolean enterField(Field field) {
        print(field.getReadableDescription());
        indent();
        if (field.getInitializer() != null) {
          print(" = ");
          accept(field.getInitializer());
        }
        print(";");
        unIndent();
        return false;
      }

      @Override
      public boolean enterFieldAccess(FieldAccess fieldAccess) {
        if (fieldAccess.getQualifier() != null) {
          accept(fieldAccess.getQualifier());
          print(".");
        }
        print(fieldAccess.getTarget().getName());
        return false;
      }

      @Override
      public boolean enterForStatement(ForStatement forStatement) {
        print("for (");
        printSeparated(",", forStatement.getInitializers());
        print(";");
        accept(forStatement.getConditionExpression());
        print(";");
        printSeparated(",", forStatement.getUpdates());
        print(") ");
        accept(forStatement.getBody());
        return false;
      }

      @Override
      public boolean enterFunctionExpression(FunctionExpression functionExpression) {
        print("function (");
        printSeparated(",", functionExpression.getParameters());
        print(")");
        accept(functionExpression.getBody());
        return false;
      }

      @Override
      public boolean enterIfStatement(IfStatement ifStatement) {
        print("if (");
        accept(ifStatement.getConditionExpression());
        print(") ");
        accept(ifStatement.getThenStatement());
        if (ifStatement.getElseStatement() != null) {
          print(" else ");
          accept(ifStatement.getElseStatement());
        }
        return false;
      }

      @Override
      public boolean enterInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
        accept(instanceOfExpression.getExpression());
        print(" instanceof ");
        print(instanceOfExpression.getTestTypeDescriptor());
        return false;
      }

      @Override
      public boolean enterLabeledStatement(LabeledStatement labeledStatement) {
        accept(labeledStatement.getLabel());
        print(": ");
        accept(labeledStatement.getStatement());
        return false;
      }

      @Override
      public boolean enterInitializerBlock(InitializerBlock initializerBlock) {
        if (initializerBlock.isStatic()) {
          print("static ");
        }
        accept(initializerBlock.getBlock());
        return false;
      }

      @Override
      public boolean enterLabel(Label label) {
        print(label.getName());
        return false;
      }

      @Override
      public boolean enterLabelReference(LabelReference labelReference) {
        print(labelReference.getTarget().getName());
        return false;
      }

      @Override
      public boolean enterMethod(Method method) {
        print(method.getReadableDescription() + " ");
        accept(method.body);
        return false;
      }

      @Override
      public boolean enterMethodCall(MethodCall methodCall) {
        if (methodCall.getQualifier() != null) {
          accept(methodCall.qualifier);
          print(".");
        }
        printInvocation(methodCall);
        return false;
      }

      @Override
      public boolean enterMultiExpression(MultiExpression multiExpression) {
        print("(");
        printSeparated(",", multiExpression.getExpressions());
        print(")");
        return false;
      }

      @Override
      public boolean enterNewArray(NewArray newArray) {
        print("new ");
        print(newArray.getLeafTypeDescriptor());
        List<Expression> dimensionExpressions = newArray.getDimensionExpressions();
        for (Expression expression : dimensionExpressions) {
          print("[");
          if (!(expression instanceof NullLiteral)) {
            accept(expression);
          }
          print("]");
        }
        if (newArray.getArrayLiteral() != null) {
          print(" {");
          printSeparated(",", newArray.getArrayLiteral().getValueExpressions());
          print("}");
        }
        return false;
      }

      @Override
      public boolean enterNewInstance(NewInstance newInstance) {
        if (newInstance.getQualifier() != null) {
          accept(newInstance.qualifier);
          print(".");
        }
        print("new ");
        printInvocation(newInstance);
        return false;
      }

      @Override
      public boolean enterNode(Node node) {
        print("<node>");
        return false;
      }

      @Override
      public boolean enterNullLiteral(NullLiteral nullLiteral) {
        print("null");
        return false;
      }

      @Override
      public boolean enterNumberLiteral(NumberLiteral numberLiteral) {
        print(numberLiteral.getValue().toString());
        return false;
      }

      @Override
      public boolean enterPostfixExpression(PostfixExpression binaryExpression) {
        accept(binaryExpression.getOperand());
        print(binaryExpression.getOperator().getSymbol());
        return false;
      }

      @Override
      public boolean enterPrefixExpression(PrefixExpression binaryExpression) {
        print(binaryExpression.getOperator().getSymbol());
        accept(binaryExpression.getOperand());
        return false;
      }

      @Override
      public boolean enterReturnStatement(ReturnStatement returnStatement) {
        print("return");
        if (returnStatement.getExpression() != null) {
          print(" ");
          accept(returnStatement.getExpression());
        }
        print(";");
        return false;
      }

      @Override
      public boolean enterStatement(Statement statement) {
        print("<statement>");
        return false;
      }

      @Override
      public boolean enterStringLiteral(StringLiteral stringLiteral) {
        print(stringLiteral.getEscapedValue());
        return false;
      }

      @Override
      public boolean enterSuperReference(SuperReference superReference) {
        print("super");
        return false;
      }

      @Override
      public boolean enterSwitchCase(SwitchCase switchCase) {
        if (switchCase.getCaseExpression() != null) {
          print("case ");
          accept(switchCase.getCaseExpression());
        } else {
          print("default");
        }
        print(":");
        indent();
        for (Statement statement : switchCase.getStatements()) {
          newLine();
          accept(statement);
        }
        unIndent();

        return false;
      }

      @Override
      public boolean enterSwitchStatement(SwitchStatement switchStatement) {
        print("switch (");
        accept(switchStatement.getSwitchExpression());
        print(") {");
        indent();
        for (SwitchCase switchCase : switchStatement.getCases()) {
          newLine();
          accept(switchCase);
        }
        unIndent();
        newLine();
        print("}");
        return false;
      }

      @Override
      public boolean enterSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
        print("synchronized (");
        accept(synchronizedStatement.getExpression());
        print(");");
        accept(synchronizedStatement.getBody());
        return false;
      }

      @Override
      public boolean enterThisReference(ThisReference thisReference) {
        print(thisReference.getTypeDescriptor().getSimpleSourceName() + ".this");
        return false;
      }

      @Override
      public boolean enterThrowStatement(ThrowStatement throwStatement) {
        print("throw ");
        accept(throwStatement.getExpression());
        print(";");
        return false;
      }

      @Override
      public boolean enterTryStatement(TryStatement tryStatement) {
        print("try ");
        if (!tryStatement.getResourceDeclarations().isEmpty()) {
          print("(");
          indent();
          indent();
          newLine();
          printSeparated(";\n", tryStatement.getResourceDeclarations());
          unIndent();
          unIndent();
          print(") ");
        }
        accept(tryStatement.getBody());
        for (CatchClause catchClause : tryStatement.getCatchClauses()) {
          accept(catchClause);
        }
        if (tryStatement.getFinallyBlock() != null) {
          print(" finally ");
          accept(tryStatement.getFinallyBlock());
        }
        return false;
      }

      @Override
      public boolean enterType(Type type) {
        print(type.isInterface() ? "interface " : (type.isEnum() ? "enum " : "class "));
        print(type.getDeclaration().getReadableDescription());
        if (type.getSuperTypeDescriptor() != null) {
          print(" extends ");
          print(type.getSuperTypeDescriptor());
        }
        String separator = " implements ";
        for (TypeDescriptor interfaceTypeDescriptor : type.getSuperInterfaceTypeDescriptors()) {
          print(separator);
          separator = ", ";
          print(interfaceTypeDescriptor);
        }
        print(" {");
        indent();
        newLine();
        for (Member member : type.getMembers()) {
          accept(member);
          newLine();
        }

        unIndent();
        newLine();
        print("}");
        return false;
      }

      @Override
      public boolean enterJavaScriptConstructorReference(
          JavaScriptConstructorReference constructorReference) {
        print(constructorReference.getReferencedTypeDeclaration().getQualifiedSourceName());
        return false;
      }

      @Override
      public boolean enterVariable(Variable variable) {
        print(variable.getTypeDescriptor());
        print(" ");
        print(variable.getName());
        return false;
      }

      @Override
      public boolean enterVariableDeclarationFragment(
          VariableDeclarationFragment variableDeclarationFragment) {
        accept(variableDeclarationFragment.getVariable());
        if (variableDeclarationFragment.getInitializer() != null) {
          print(" = ");
          accept(variableDeclarationFragment.getInitializer());
        }
        return false;
      }

      @Override
      public boolean enterVariableDeclarationExpression(
          VariableDeclarationExpression variableDeclarationExpression) {
        String separator = "";
        for (VariableDeclarationFragment variableDeclarationFragment :
            variableDeclarationExpression.getFragments()) {
          print(separator);
          separator = ",";
          accept(variableDeclarationFragment);
        }
        return false;
      }

      @Override
      public boolean enterVariableReference(VariableReference variableReference) {
        print(variableReference.getTarget().getName());
        return false;
      }

      @Override
      public boolean enterWhileStatement(WhileStatement whileStatement) {
        print("while (");
        accept(whileStatement.getConditionExpression());
        print(") ");
        accept(whileStatement.getBody());
        return false;
      }

      private void accept(Node node) {
        node.accept(this);
      }

      private void indent() {
        currentIndent++;
      }

      private void newLine() {
        print("\n");
      }

      private void print(String string) {
        result.append(string.replace("\n", "\n" + Strings.repeat("  ", currentIndent)));
      }

      private void print(TypeDescriptor typeDescriptor) {
        print(typeDescriptor.getReadableDescription());
      }

      private void printSeparated(String separator, List<? extends Node> expressions) {
        String nextSeparator = "";
        for (Node argument : expressions) {
          print(nextSeparator);
          nextSeparator = separator;
          accept(argument);
        }
      }

      private void printInvocation(Invocation invocation) {
        MethodDescriptor target = invocation.getTarget();
        if (target.isConstructor()) {
          print(target.getEnclosingTypeDescriptor().getSimpleSourceName() + ".");
        }
        print(target.getName());
        print("(");
        printSeparated(",", invocation.getArguments());
        print(")");
      }

      private void unIndent() {
        currentIndent--;
      }
    }.accept(node);
    return result.toString();
  }

  private ToStringRenderer() {}
}
