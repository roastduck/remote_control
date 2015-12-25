package rd.vehicle;

import android.util.Log;

public class Movement
{
    private BluetoothChatService mChatService;

    private static final float Length = 0.38f; // metres

    public int brk = 0;
    public float vLeft = 0.0f, vRight = 0.0f;
    public float vBar = 0.0f, omega = 0.0f; // positive angle means left

    private Runnable trySend = null;

    public synchronized void send()
    {
        // 2 m/s -> 4000
        // 1 m/s -> 2000 half to adapt rotating
        String msg = "%" + (int)(vLeft*1000) + " " + (int)(vRight*1000) + " " + brk + "#";
        Log.i("bluetooth", "sent " + msg);
        mChatService.write(msg.getBytes());
    }

    public void setRequire(float v, float w, float _brk)
    {
        brk = (int)_brk;
        vBar = v; omega = w;
        vLeft = vBar+2.0f*omega*Length;
        vRight = vBar-2.0f*omega*Length;
        Log.i("Movement set", "vLeft=" + vLeft + ",vRight=" + vRight + ",vBar=" + vBar + ",omega=" + omega);
        if (trySend==null) {
            trySend = new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        send();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Thread th = new Thread(trySend);
            th.start();
        }
        //send();
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
