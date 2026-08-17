package org.opendcs.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

        assertThrows(IllegalStateException.class, success::failure);
        assertThrows(IllegalStateException.class, other::success);

        assertEquals(5, success.orElseThrowing(e ->
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

        success.onSuccess(successInt::set);
        other.onError(otherDouble::set);

        assertEquals(success.success(), successInt.get());
        assertEquals(other.failure(), otherDouble.get());

        final AtomicInteger failInt = new AtomicInteger(-1);
        final AtomicReference<Double> notFailDouble = new AtomicReference<>(-10.0);
        other.onSuccess(failInt::set);
        success.onError(notFailDouble::set);

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
        assertEquals(success.success(), result);

        var result2 = success.orElse(e -> 150);
        assertEquals(success.success(), result2);
    }


    @Test
    void test_create_with_nulls()
    {
        assertThrows(NullPointerException.class, () -> Result.success(null));
        assertThrows(NullPointerException.class, () -> Result.failure(null));

        assertDoesNotThrow(() -> Result.success(1)).onError(e -> fail("Should not fail."));
        assertDoesNotThrow(() -> Result.failure(2)).onSuccess(s -> fail("Should not succeed."));
    }
}
