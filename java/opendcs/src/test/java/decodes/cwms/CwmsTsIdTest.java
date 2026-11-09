package decodes.cwms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import decodes.sql.DbKey;
import opendcs.opentsdb.OffsetErrorAction;

class CwmsTsIdTest
{
    @Test
    void copyNoKey_preserves_settings()
    {
        var original = new CwmsTsId();
        original.setUniqueString("SolarFarm-Array1.Code-OperatingMode.Inst.1Hour.0.Test");
        original.setKey(DbKey.createDbKey(123));
        original.setStorageType('S');
        original.setActive(false);
        original.setAllowDstOffsetVariation(true);
        original.setOffsetErrorAction(OffsetErrorAction.REJECT);

        var copy = (CwmsTsId)original.copyNoKey();

        assertTrue(DbKey.isNull(copy.getKey()));
        assertEquals('S', copy.getStorageType());
        assertFalse(copy.isActive());
        assertTrue(copy.isAllowDstOffsetVariation());
        assertEquals(OffsetErrorAction.REJECT, copy.getOffsetErrorAction());
    }
}
