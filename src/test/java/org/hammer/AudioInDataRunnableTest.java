package org.hammer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.swing.JPanel;
import java.lang.reflect.*;

public class AudioInDataRunnableTest {

    @Test
    void recomputeXValues_distributesEvenly() throws Exception {
        Class<?> cls = Class.forName("org.hammer.AudioInDataRunnable");
        Object instance = obtainInstance(cls);

        // set numberOfPoints = 5
        Field numField = cls.getDeclaredField("numberOfPoints");
        numField.setAccessible(true);
        numField.setInt(instance, 5);

        // prepare a JPanel with width 200 and set into the instance
        JPanel panel = new JPanel();
        panel.setSize(200, 100);

        try {
            Field panelField = cls.getDeclaredField("panel");
            panelField.setAccessible(true);
            panelField.set(instance, panel);
        } catch (NoSuchFieldException ex) {
            fail("Expected field 'panel' not found on AudioInDataRunnable: " + ex.getMessage());
        }

        // invoke recomputeXValues
        Method recompute = cls.getDeclaredMethod("recomputeXValues");
        recompute.setAccessible(true);
        recompute.invoke(instance);

        // retrieve snapshot
        Method getX = cls.getDeclaredMethod("getXPointsSnapshot");
        getX.setAccessible(true);
        int[] x = (int[]) getX.invoke(instance);

        assertEquals(5, x.length);

        int width = 200;
        for (int i = 0; i < 5; i++) {
            int expected = Math.round((width - 1) * ((float) i / (5 - 1)));
            assertEquals(expected, x[i], "x[" + i + "] mismatch");
        }
    }

    @Test
    void getXPointsSnapshot_returnsDefensiveCopy() throws Exception {
        Class<?> cls = Class.forName("org.hammer.AudioInDataRunnable");
        Object instance = obtainInstance(cls);

        Field numField = cls.getDeclaredField("numberOfPoints");
        numField.setAccessible(true);
        numField.setInt(instance, 3);

        JPanel panel = new JPanel();
        panel.setSize(100, 50);
        Field panelField = cls.getDeclaredField("panel");
        panelField.setAccessible(true);
        panelField.set(instance, panel);

        Method recompute = cls.getDeclaredMethod("recomputeXValues");
        recompute.setAccessible(true);
        recompute.invoke(instance);

        Method getX = cls.getDeclaredMethod("getXPointsSnapshot");
        getX.setAccessible(true);
        int[] a = (int[]) getX.invoke(instance);
        int[] b = (int[]) getX.invoke(instance);

        // mutate first array and ensure second remains unaffected
        a[0] = a[0] + 9999;
        assertNotEquals(a[0], b[0], "Expected defensive copy: mutation of returned array should not affect subsequent snapshot");
    }

    private Object obtainInstance(Class<?> cls) throws Exception {
        // try singleton INSTANCE first, otherwise invoke default ctor
        try {
            Field inst = cls.getField("INSTANCE");
            return inst.get(null);
        } catch (NoSuchFieldException e) {
            Constructor<?> ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        }
    }
}
