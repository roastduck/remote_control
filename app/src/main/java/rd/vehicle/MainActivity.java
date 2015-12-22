package rd.vehicle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

class MyHandler extends Handler
{
    public Context context;

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case BluetoothChatService.MESSAGE_STATE_CHANGE:
                Log.i("handleMessage", "MESSAGE_STATE_CHANGE: " + msg.arg1);
                break;
            case BluetoothChatService.MESSAGE_READ:
                String textRead = String.valueOf((char[])msg.obj, 0, msg.arg1);
                Log.i("handleMessage", "MESSAGE_READ: " + textRead);
                break;
            case BluetoothChatService.MESSAGE_WRITE:
                Log.i("handleMessage", "MESSAGE_READ");
                break;
            case BluetoothChatService.MESSAGE_TOAST:
                Toast.makeText(context,(String) msg.obj, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}

public class MainActivity extends AppCompatActivity
{
    private final static int REQUEST_ENABLE_BT = 100;
    private final static int REQUEST_LIST_BT = 101;

    private BluetoothChatService mChatService;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case REQUEST_LIST_BT:
                if (resultCode != Activity.RESULT_OK)
                {
                    Log.e("bluetooth", "failed to list devices");
                    return;
                }
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                Log.e("device", address);
                // Get the BluetoothDevice object
                BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
                mChatService.connect(device);
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode != RESULT_OK)
                {
                    Log.e("bluetooth", "failed to enable bluetooth");
                    return;
                }
                Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_LIST_BT);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        MyHandler mHandler = new MyHandler();
        mHandler.context = getApplicationContext();
        mChatService = new BluetoothChatService(this, mHandler);
        GLSurfaceView mGLView = new MyGLSurfaceView(this, mChatService);
        setContentView(mGLView);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        {
            Log.e("bluetooth", "No bluetooth device found");
            return;
        }
        if (!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else
            onActivityResult(REQUEST_ENABLE_BT, RESULT_OK, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
