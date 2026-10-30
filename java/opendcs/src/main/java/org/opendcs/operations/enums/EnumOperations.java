package org.opendcs.operations.enums;

import java.util.ArrayList;
import java.util.Optional;

import org.opendcs.operations.OpenDcsOperations;
import org.opendcs.util.Result;

import decodes.db.DbEnum;

/**
 * Tooling to support retrieving Enum data and types. As Enums rarely
 * change most usages at runtime don't require access through a transaction just to get data.
 * Additionally, a given implementation can, and should, cache critical information.
 * EnumOperations
 */
public interface EnumOperations extends OpenDcsOperations
{
    /**
     * Retrieve the specific enum, getting all values.
     * @param enumName Name of the DbEnumSet desired.
     * @return the DbEnum if found, otherwise Optional.empty().
     */
    public Optional<DbEnum> getEnum(String enumName);

    /**
     * Create an instance of an enum of the given enum type and name.
     * @param <T>
     * @param enumName name of the DbEnum set to search
     * @param enumValue name of DbEnumValue desired
     * @param targetType instance of the target class type to help with type checking.
     * @param args arguments for the constructor of T
     * @return Result.success of type T if the instance can create created, otherwise the failure reason.
     */
    @SuppressWarnings("unchecked") // we manually type check
    default <T> Result<T, Exception> instanceFromEnum(String enumName, String enumValue, Class<T> targetType, Object... args)
    {
        var dbEnumOpt = getEnum(enumName);
        if (dbEnumOpt.isEmpty())
        {
            return Result.failure(new Exception("No enum set registered for '" + enumName + "'"));
        }
        var dbEnum = dbEnumOpt.get();
        var type = dbEnum.findEnumValue(enumValue);
        if (type == null)
        {
            return Result.failure(new Exception("No value '" + enumValue + "' for type '" + enumName + "' is registered"));
        }

        try
        {
            Class<?> targetClass = type.getExecClass();
            if (!targetType.isAssignableFrom(targetClass))
            {
                throw new ClassCastException("Cannot create instance. Incompatible types.");
            }
            ArrayList<Class<?>> argTypes = new ArrayList<>();
            for (var arg: args)
            {
                argTypes.add(arg.getClass());
            }
            var constructor = targetClass.getConstructor(argTypes.toArray(new Class<?>[0]));

            return Result.success((T)constructor.newInstance(args));
        }
        catch (Exception ex)
        {
            return Result.failure(ex);
        }
    }
}
