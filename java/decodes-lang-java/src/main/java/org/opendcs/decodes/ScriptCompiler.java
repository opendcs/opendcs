package org.opendcs.decodes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.opendcs.decodes.operations.AbstractDecodesOperation;
import org.opendcs.decodes.operations.SKIP_DIRECTION;
import org.opendcs.decodes.operations.SkipCharacterOperation;
import org.opendcs.decodes.operations.DecodesOperation;
import org.opendcs.decodes.operations.FunctionDecodesOperation;
import org.opendcs.decodes.operations.GroupDecodesOperation;
import org.opendcs.decodes.operations.SkipLineOperation;
import org.opendcs.decodes.parser.decodesBaseListener;
import org.opendcs.decodes.parser.decodesLexer;
import org.opendcs.decodes.parser.decodesParser;
import org.opendcs.decodes.parser.decodesParser.FormatStatementContext;
import org.opendcs.decodes.parser.decodesParser.FunctionContext;
import org.opendcs.decodes.parser.decodesParser.GroupContext;
import org.opendcs.decodes.parser.decodesParser.OperationContext;
import org.opendcs.decodes.parser.decodesParser.PositionContext;

public class ScriptCompiler extends decodesBaseListener
{
    LinkedHashMap<String, List<DecodesOperation>> decodesScript = new LinkedHashMap<>();
    List<DecodesOperation> currentList;

    Stack<GroupDecodesOperation> currentGroups = new Stack<>();

    @Override
    public void enterFormatStatement(FormatStatementContext ctx)
    {
        currentList = decodesScript.computeIfAbsent(ctx.IDENTIFIER().getText(), label -> new ArrayList<>());
    }

    @Override
    public void exitFormatStatement(FormatStatementContext ctx)
    {
        currentList = null;
    }    

    @Override
    public void exitPosition(PositionContext ctx)
    {
        int repeat = getRepeat(ctx.repeat);
        var operation = switch (ctx.identifier.getText())
        {
            case "/" -> new SkipLineOperation(repeat, SKIP_DIRECTION.FORWARD);
            case "\\" -> new SkipLineOperation(repeat, SKIP_DIRECTION.BACKWARDS);
            case "X" , "x" -> new SkipCharacterOperation(repeat, SKIP_DIRECTION.FORWARD);
            case "W" , "w" -> new AbstractDecodesOperation(repeat, "SKIP_WHITESPACE") {};
            case "P" , "p" -> new AbstractDecodesOperation(repeat, "POSITION") {};
            default -> new AbstractDecodesOperation(repeat, "invalid:"+ctx.IDENTIFIER().getText()) {
            };
        };
        addOperation(ctx, operation);
    }

    
    @Override
    public void exitFunction(FunctionContext ctx) 
    {
        int repeat = getRepeat(ctx.repeat);
        var args = ctx.children
                      .stream()
                      .filter(c -> !",".equals(c.getText()))
                      .filter(c -> !"(".equals(c.getText()))
                      .filter(c -> !")".equals(c.getText()))
                      .map(c -> c.getText())
                      .peek(System.out::println)
                      .toList();
        System.out.println("Processing " + ctx.getText());
        System.out.println("Args " + String.join(",", args));
        addOperation(ctx, new FunctionDecodesOperation(repeat, ctx.identifier.getText(), args));
    }

    private int getRepeat(Token repeatVal)
    {        
        return repeatVal != null ? Integer.parseInt(repeatVal.getText()) : 1;
    }

    private void addOperation(ParserRuleContext ctx, DecodesOperation operation)
    {
        if (currentGroups.isEmpty())
        {
            currentList.add(operation);
        }
        else if (!(ctx instanceof GroupContext)) // don't want to add the group to itself
        {
            currentGroups.peek().addOperation(operation);
        }
    }

    @Override
    public void enterGroup(GroupContext ctx)
    {
        var repeat = getRepeat(ctx.repeat);
        var newGroup = new GroupDecodesOperation(repeat);
        
        if (!currentGroups.isEmpty())
        {
            // add this group to the list of operations before moving on
            currentGroups.peek().addOperation(newGroup);
        }
        currentGroups.add(newGroup);
    }

    @Override
    public void exitGroup(GroupContext ctx)
    {
        var group = currentGroups.pop();
        if (currentGroups.empty() && group != null)
        {
            currentList.add(group);
        }
    }
    

    public LinkedHashMap<String, List<DecodesOperation>> getOperations()
    {
        return this.decodesScript;
    }

    public static void main(String[] args) throws Exception
    {
        var stream = CharStreams.fromFileName(args[0]);

        var lexer = new decodesLexer(stream);
        var tokens = new CommonTokenStream(lexer);
        var parser = new decodesParser(tokens);
        parser.setTrace(true);
        var decodesScript = parser.decodesScript();

        var listener = new ScriptCompiler();
        ParseTreeWalker.DEFAULT.walk(listener, decodesScript);
        
        var ops = listener.getOperations();

        ops.forEach((k,v) ->
        {
            System.out.println(k);
            v.forEach(op ->
            {
                System.out.println("\t" + op.toString());
            });
        });
    }
}
