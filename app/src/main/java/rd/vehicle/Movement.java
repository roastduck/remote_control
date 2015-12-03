package rd.vehicle;

import android.util.Log;

public class Movement
{
    private BluetoothChatService mChatService;

    private static final float Length = 0.38f; // metres

    public int brk = 0;
    public float vLeft = 0.0f, vRight = 0.0f;
    public float vBar = 0.0f, omega = 0.0f; // positive angle means left

    public void send()
    {
        // 2 m/s -> 200
        String msg = (int)(vLeft*100) + " " + (int)(vRight*100) + " " + brk;
        mChatService.write(msg.getBytes());
    }

    public void setRequire(float v, float w)
    {
        vBar = v; omega = w;
        vLeft = vBar-0.5f*omega*Length;
        vRight = vBar+0.5f*omega*Length;
        Log.i("Movement set", "vLeft=" + vLeft + ",vRight=" + vRight + ",vBar=" + vBar + ",omega=" + omega);
        send();
    }

    /*
    public void setCurrent(float vL, float vR)
    {
        vLeft = vL; vRight = vR;
        vBar = 0.5f*(vLeft+vRight);
        omega = (vRight-vLeft)/Length;
    }
    */

    public Movement(BluetoothChatService _ChatService)
    {
        mChatService = _ChatService;
    }
}