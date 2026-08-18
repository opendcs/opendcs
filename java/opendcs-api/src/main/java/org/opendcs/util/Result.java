/*
* Where Applicable, Copyright 2025-2026 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package org.opendcs.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Objects;

import org.opendcs.util.functional.ThrowingFunction;

/**
 * Pair objects used for returning Successful and failed results for processing in a stream.
 * @param <S> Desired Object
 * @param <F> Object containing error details. Most commonly an Exception, but can be anything.
 */
public final class Result<S, F>
{
    private final S successResult;
    private final F failResult;

    private Result(S successResult, F failResult)
    {
        this.successResult = successResult;
        this.failResult = failResult;
    }

    public boolean isSuccess()
    {
        return failResult == null;
    }

    public boolean isFailure()
    {
        return failResult != null;
    }

    public S success()
    {
        if (isFailure())
        {
            throw new IllegalStateException("Attempt to retrieve 'success' result of a failure result.");
        }
        return successResult;
    }

    /**
     * Either result the success result, or another value. Error result is
     * provided if necessary to make a determination about the response.
     *
     * @param func Function that takes the Error and returns the desired instance of S.
     * @return
     */
    public S orElse(Function<F,S> func)
    {
        if (isSuccess())
        {
            return successResult;
        }
        else
        {
            return func.apply(failResult);
        }
    }

    /**
     * As {@see orElse} except that a checked exception can be throw. Error instance is provided.
     * If error type E is an exception it can be thrown directly.
     * @param <E>
     * @param func
     * @return
     * @throws E
     */
    public <E extends Exception> S orElseThrowing(ThrowingFunction<F,S,E> func) throws E
    {
        if (isSuccess())
        {
            return successResult;
        }
        else
        {
            return func.apply(failResult);
        }
    }

    /**
     * Simple callback for any SuccessResults
     * @param handleSuccess
     */
    public void onSuccess(Consumer<S> handleSuccess)
    {
        if (isSuccess())
        {
            handleSuccess.accept(successResult);
        }
    }

    public F failure()
    {
        if (!isFailure())
        {
            throw new IllegalStateException("Attempt to retrieve 'failure' result of a successful result.");
        }
        return failResult;
    }

    /**
     * Simple callback for any failure results.
     * @param consumer
     */
    public void onError(Consumer<F> consumer)
    {
        if (isFailure())
        {
            consumer.accept(failResult);
        }
    }

    /**
     * Create Result instance with successful value.
     *
     * @param <S>
     * @param <F>
     * @param successResult can't be null
     * @return
     * @throws NullPointerException if successResult is null
     */
    public static <S, F> Result<S, F> success(S successResult)
    {
        return new Result<>(
            Objects.requireNonNull(successResult,
                                   "Success Result can't be null. Use Optional if successful state can be empty"),
            null);
    }

    /**
     * Create result instances with "fail" value.
     *
     * @param <S>
     * @param <F>
     * @param failResult
     * @return
     */
    public static <S, F> Result<S, F> failure(F failResult)
    {
        return new Result<>(null, 
                            Objects.requireNonNull(failResult,
                            "Fail Result cannot be null."
                            ));
    }
}
