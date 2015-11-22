package rd.vehicle;

import android.util.Log;

public class Movement
{
    private static final float Length = 0.38f; // metres

    public float vLeft = 0.0f, vRight = 0.0f;
    public float vBar = 0.0f, omega = 0.0f; // positive angle means left

    public void setRequire(float v, float w)
    {
        vBar = v; omega = w;
        vLeft = vBar-0.5f*omega*Length;
        vRight = vBar+0.5f*omega*Length;
        Log.i("Movement set", "vLeft=" + vLeft + ",vRight=" + vRight + ",vBar=" + vBar + ",omega=" + omega);
    }

    /*
    public void setCurrent(float vL, float vR)
    {
        vLeft = vL; vRight = vR;
        vBar = 0.5f*(vLeft+vRight);
        omega = (vRight-vLeft)/Length;
    }
    */
}