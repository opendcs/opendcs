package org.opendcs.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDataException;

class ResultTest
{
    @Test
    void test_result()
    {
        Result<Integer, Double> success = Result.success(5);
        Result<Integer, Double> other = Result.failure(3.5);

        assertTrue(success.isSuccess());
        assertTrue(other.isFailure());

        assertThrows(IllegalStateException.class, () -> success.getFailure());
        assertThrows(IllegalStateException.class, () -> other.getSuccess());

        assertEquals(5, success.orElseThrowing((e) ->
        {
            throw AssertionFailureBuilder.assertionFailure().message("should not have been called.").build();
        }));

        assertThrows(Exception.class,
                     () -> other.orElseThrowing(e ->
                     {
                        throw new Exception("" + e);
                     }));
        var value = other.orElse(e -> 3);
        assertEquals(3, value);


        Result<Integer, OpenDcsDataException> resultException = Result.failure(new OpenDcsDataException("test"));
        assertThrows(OpenDcsDataException.class,
                    () -> resultException.orElseThrowing(e ->
                    {
                        throw e;
                    }));
    }

    @Test
    void test_result_callbacks()
    {
        Result<Integer, Double> success = Result.success(5);
        Result<Integer, Double> other = Result.failure(3.5);

        final AtomicInteger successInt = new AtomicInteger(0);
        final AtomicReference<Double> otherDouble = new AtomicReference<>(0.0);

        success.onSuccess(value -> successInt.set(value));
        other.handleError(error -> otherDouble.set(error));

        assertEquals(success.getSuccess(), successInt.get());
        assertEquals(other.getFailure(), otherDouble.get());

        final AtomicInteger failInt = new AtomicInteger(-1);
        final AtomicReference<Double> notFailDouble = new AtomicReference<>(-10.0);
        other.onSuccess(value -> failInt.set(value));
        success.handleError(error -> notFailDouble.set(error));

        assertEquals(-1, failInt.get());
        assertEquals(-10.0, notFailDouble.get());
    }


    @Test
    void test_exception_handlers()
    {
        Result<Integer, OpenDcsDataException> success = Result.success(100);
        Result<Integer, OpenDcsDataException> failure = Result.failure(new OpenDcsDataException("I failed."));

        assertDoesNotThrow(() -> failure.orElseThrowing(e -> 10));
        assertThrows(OpenDcsDataException.class,
                        () -> failure.orElseThrowing(e ->
                        {
                           throw e;
                        }));

        var result = assertDoesNotThrow(() -> success.orElseThrowing(e -> 50));
        assertEquals(success.getSuccess(), result);

        var result2 = success.orElse(e -> 150);
        assertEquals(success.getSuccess(), result2);
    }
}
