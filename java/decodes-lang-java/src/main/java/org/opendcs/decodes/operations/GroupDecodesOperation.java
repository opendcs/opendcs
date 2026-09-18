package org.opendcs.decodes.operations;

import java.util.ArrayList;
import java.util.List;

public class GroupDecodesOperation extends AbstractDecodesOperation
{
    public static final String OPERATION_NAME = "group";

    public GroupDecodesOperation(int repeat)
    {
        super(repeat, OPERATION_NAME);
    }

    private final List<DecodesOperation> operations = new ArrayList<>();


    public List<DecodesOperation> getOperations()
    {
        return this.operations;
    }

    public void addOperation(DecodesOperation operation)
    {
        operations.add(operation);
    }

    @Override
    public String toString()
    {
        final var sb = new StringBuilder();
        sb.append(repeat).append(" -> group{");
        sb.append(String.join(",", operations.stream().map(op -> op.toString()).toList()));
        sb.append("}");
        return sb.toString();
    }
}
