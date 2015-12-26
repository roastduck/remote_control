package rd.vehicle;

import android.util.Log;
//import android.widget.Toast;

public class Movement
{
    private BluetoothChatService mChatService;

    private static final float Length = 0.38f; // metres

    public int brk = 0;
    public float angle = 0.0f, speed= 0.0f;
    public float vBar = 0.0f, omega = 0.0f; // positive angle means left
    public String msg;

    private Runnable trySend = null;

    public synchronized void send()
    {
        // 2 m/s -> 4000
        // 1 m/s -> 2000 half to adapt rotating
        msg = "%" + (int)(angle) + " " + (int)(speed) + " " + brk + "#";
        Log.i("bluetooth", "sent " + msg);
//
//        Toast.makeText(getApplicationContext(), msg,
//                Toast.LENGTH_SHORT).show();

        mChatService.write(msg.getBytes());
    }

    public void setRequire(float v, float w, float _brk)
    {
        brk = (int)_brk;
        vBar = v; omega = w;    //omega return angle(-180~+180) starts from x axis.
        angle = omega;
        speed = vBar*2000;      //v return speed(-2~+2)
        Log.i("Movement set", "angle=" + angle + ",speed=" + speed + ",vBar=" + vBar + ",omega=" + omega);
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
