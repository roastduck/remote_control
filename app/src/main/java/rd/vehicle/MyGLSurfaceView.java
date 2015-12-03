package rd.vehicle;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

abstract class Figure
{
    private static final int COORDS_PER_VERTEX = 3;

    private static final String vertexShaderCode =
            "attribute vec4 vPosition;" +
            "uniform mat4 vTransform;" +
            "void main() {" +
            "  gl_Position = vTransform * vPosition;" +
            "}";

    private static final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

    private FloatBuffer vertexBuffer;
    private int mProgram;

    abstract protected float[] triangleCoords();
    abstract protected float[] color();
    abstract protected float[] transform();
    protected float touchCenterX() { return 0; }
    protected float touchCenterY() { return 0; }
    protected float touchRadiusSqure() { return 0; }

    public boolean validTouch(float touchingX, float touchingY, int width, int height)
    {
        return false;
    }

    public float[] origin()
    {
        return new float[] {0.0f, 0.0f};
    }

    public void draw()
    {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);
        // get handle to vertex shader's vPosition member
        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        // Set color
        int mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, color(), 0);
        // Set Transform
        int mTransformHandle = GLES20.glGetUniformLocation(mProgram, "vTransform");
        GLES20.glUniformMatrix4fv(mTransformHandle, 1, false, transform(), 0);
        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, triangleCoords().length / COORDS_PER_VERTEX);
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    private int loadShader(int type, String shaderCode)
    {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);
        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0)
        {
            Log.e("opengl", "Could not compile shader");
            Log.e("opengl", GLES20.glGetShaderInfoLog(shader));
            Log.e("opengl", shaderCode);
        }

        return shader;
    }

    protected Figure()
    {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords().length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());
        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(triangleCoords());
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }
}

abstract class FigureWithSpin extends Figure
{
    private float vTransform[];
    private double curDgr = 0;

    public void addSpin(double dgree)
    {
        Matrix.rotateM(vTransform, 0, (float)dgree, 0, 0, 1);
        curDgr += dgree;
    }

    public void setSpin(double dgree)
    {
        addSpin(dgree-curDgr);
    }

    @Override
    protected float[] transform()
    {
        return vTransform;
    }

    public FigureWithSpin()
    {
        vTransform = new float[16];
        Matrix.setIdentityM(vTransform, 0);
    }
}

abstract class FigureWithScale extends FigureWithSpin
{
    private float vTransform[];
    private float curLB, curRB, curUB, curDB;

    public void setScale(float lBound, float rBound, float uBound, float dBound)
    {
        Matrix.orthoM(vTransform, 0, lBound, rBound, dBound, uBound, -1, 1);
        curLB = lBound; curRB = rBound; curDB = dBound; curUB = uBound;
    }

    public void addScale(float lBound, float rBound, float uBound, float dBound)
    {
        setScale(curLB+lBound, curRB+rBound, curUB+uBound, curDB+dBound);
    }

    @Override
    protected float[] transform()
    {
        float Mul[] = new float[16];
        Matrix.multiplyMM(Mul, 0, vTransform, 0, super.transform(), 0);
        return Mul;
    }

    @Override
    public float[] origin()
    {
        float old[] = super.origin();
        float vec4Old[] = {old[0], old[1], 0.0f, 1.0f};
        float vec4Cur[] = new float[4];
        Matrix.multiplyMV(vec4Cur, 0, transform(), 0, vec4Old, 0);
        return new float[] {vec4Cur[0], vec4Cur[1]};
    }

    @Override
    public boolean validTouch(float touchingX, float touchingY, int width, int height)
    {
        float touchCenter[] = {touchCenterX(), touchCenterY(), 0.0f, 1.0f};
        float touchingPoint[] = {touchingX, touchingY, 0.0f, 1.0f};
        float curCenter[] = new float[4];
        float curPointing[] = new float[4];
        float unitTransform[] = new float[16];
        Matrix.orthoM(unitTransform, 0, 0, width, height, 0, -1, 1);
        Matrix.multiplyMV(curCenter, 0, transform(), 0, touchCenter, 0);
        Matrix.multiplyMV(curPointing, 0, unitTransform, 0, touchingPoint, 0);
        float curDistanceSquare = 0.0f;
        curDistanceSquare += (curCenter[0]-curPointing[0])*(curCenter[0]-curPointing[0]);
        curDistanceSquare += (curCenter[1]-curPointing[1])*(curCenter[1]-curPointing[1]);
        Log.d("validTouch", "curDistanceSquare = " + curDistanceSquare);
        return (curDistanceSquare<=touchRadiusSqure());
    }

    public FigureWithScale()
    {
        vTransform = new float[16];
        Matrix.setIdentityM(vTransform, 0);
    }
}

final class LongIndicator extends FigureWithScale
{
    private static float vTriangleCoords[] =
    {
        0.00f,  1.00f,  0.00f,
        -0.02f, 0.00f,  0.00f,
        0.02f,  0.00f,  0.00f,

        0.00f,  0.50f,  0.00f,
        -0.10f, 0.60f,  0.00f,
        0.10f,  0.60f,  0.00f,

        0.00f,  0.70f,  0.00f,
        -0.10f, 0.60f,  0.00f,
        0.10f,  0.60f,  0.00f,
    };
    private static float vColor[] = {0.0f, 0.0f, 0.0f, 1.0f};

    protected float[] triangleCoords() { return vTriangleCoords; }
    protected float[] color() { return vColor; }
    protected float touchCenterX() { return 0.0f; }
    protected float touchCenterY() { return 0.6f; }
    protected float touchRadiusSqure() { return 0.1f; }
}

final class SpeedMark extends FigureWithScale
{
    private static float vTriangleCoords[] =
    {
        -1.0f,   -1.0f,    0.0f,
        1.0f,    -1.0f,    0.0f,
        0.0f,    1.0f,     0.0f
    };
    private static float vColor[] = {0.0f, 0.0f, 0.0f, 1.0f};

    protected float[] triangleCoords() { return vTriangleCoords; }
    protected float[] color() { return vColor; }
    protected float touchCenterX() { return 0.0f; }
    protected float touchCenterY() { return 0.0f; }
    protected float touchRadiusSqure() { return 0.1f; }
}

final class BreakButton extends FigureWithScale
{
    private static float vTriangleCoords[] =
    {
        -1.0f,   -1.0f,    0.0f,
        1.0f,    -1.0f,    0.0f,
        -1.0f,   1.0f,     0.0f,
        1.0f,    -1.0f,    0.0f,
        -1.0f,   1.0f,     0.0f,
        1.0f,    1.0f,     0.0f
    };
    private float vColor[] = {1.0f, 0.0f, 0.0f, 1.0f};

    public void press() { vColor[0] = 0.5f; }
    public void unpress() { vColor[0] = 1.0f; }

    protected float[] triangleCoords() { return vTriangleCoords; }
    protected float[] color() { return vColor; }
    protected float touchCenterX() { return 0.0f; }
    protected float touchCenterY() { return 0.0f; }
    protected float touchRadiusSqure() { return 0.1f; }
}

abstract class FigureControl
{
    protected int tracking;
    protected float lastX, lastY;
    protected Figure fig;

    abstract public float getVal();

    public void onDown(float x, float y, int width, int height, int id)
    {
        if (! fig.validTouch(x, y, width, height)) return;
        tracking = id;
        lastX = x;
        lastY = y;
        Log.d("onDown", "Valid");
    }

    public void onMove(float x, float y, int width, int height, int id)
    {
        if (tracking!=id) return;
        lastX = x;
        lastY = y;
        Log.d("onMove", "Valid");
    }

    public void onUp(int id)
    {
        if (tracking!=id) return;
        tracking = -1;
        Log.d("onUp", "Valid");
    }

    public FigureControl(Figure _fig)
    {
        fig = _fig;
        tracking = -1;
    }
}

class FigureControlMoveX extends FigureControl
{
    double lastX;

    @Override
    public void onDown(float x, float y, int width, int height, int id)
    {
        super.onDown(x, y, width, height, id);
        if (tracking==id) lastX = x/width*2-1;
    }

    @Override
    public void onMove(float x, float y, int width, int height, int id)
    {
        if (tracking!=id) return;
        double curX = x/width*2-1;
        if (curX<-0.7 || curX>0.7) return;
        ((FigureWithScale)fig).addScale(-(float)(curX-lastX)*10, -(float)(curX-lastX)*10, 0.0f, 0.0f);
        Log.d("onMove", "X : " + lastX + " -> " + curX);
        lastX = curX;
        super.onMove(x, y, width, height, id);
    }

    @Override
    public float getVal() { return (float)(lastX*2); } // -2~2 m/s

    public FigureControlMoveX(Figure _fig)
    {
        super(_fig);
    }
}

class FigureControlSpin extends FigureControl
{
    double lastDgr;

    private double findDgr(float touchingX, float touchingY, int width, int height)
    {
        touchingY = height-touchingY;
        float[] origin = fig.origin();
        float[] oldOrigin = {origin[0], origin[1], 0.0f, 1.0f};
        float[] curOrigin = new float[4];
        float[] unitTransform = new float[16];
        float[] invTransform = new float[16];
        Matrix.orthoM(unitTransform, 0, 0, width, 0, height, -1, 1);
        Matrix.invertM(invTransform, 0, unitTransform, 0);
        Matrix.multiplyMV(curOrigin, 0, invTransform, 0, oldOrigin, 0);
        Log.v("findDgr", "touching("+touchingX+","+touchingY+"), origin("+curOrigin[0]+","+curOrigin[1]+")");
        return Math.atan2(touchingY-curOrigin[1], touchingX-curOrigin[0])/Math.PI*180;
    }

    @Override
    public void onDown(float x, float y, int width, int height, int id)
    {
        super.onDown(x, y, width, height, id);
        if (tracking==id) lastDgr = findDgr(x, y, width, height);
    }

    @Override
    public void onMove(float x, float y, int width, int height, int id)
    {
        if (tracking!=id) return;
        double curDgr = findDgr(x, y, width, height);
        ((FigureWithSpin)fig).addSpin(curDgr-lastDgr);
        Log.d("onMove", "dgr : " + lastDgr + " -> " + curDgr);
        lastDgr = curDgr;
        super.onMove(x, y, width, height, id);
    }

    @Override
    public float getVal() { return (float)(lastDgr/180*Math.PI); }

    public FigureControlSpin(Figure _fig)
    {
        super(_fig);
    }
}

class FigureControlButton extends FigureControl
{
    private int pressing = 0;

    @Override
    public void onDown(float x, float y, int width, int height, int id)
    {
        super.onDown(x, y, width, height, id);
        pressing = 1;
        ((BreakButton)fig).press();
    }

    @Override
    public void onMove(float x, float y, int width, int height, int id)
    {
        super.onMove(x, y, width, height, id);
        if (fig.validTouch(x, y, width, height)) return;
        pressing = 0;
        ((BreakButton)fig).unpress();
    }

    @Override
    public void onUp(int id)
    {
        super.onUp(id);
        pressing = 0;
        ((BreakButton)fig).unpress();
    }

    @Override
    public float getVal() { return pressing; }

    public FigureControlButton(Figure _fig)
    {
        super(_fig);
    }
}

class MyGLRenderer implements GLSurfaceView.Renderer
{
    public Figure figs[];
    public FigureControl figCtrls[];

    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        // Set the background frame color
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        LongIndicator mLongIndicator = new LongIndicator();
        mLongIndicator.setScale(-1.1f, 1.1f, 3.3f, -1.1f);
        mLongIndicator.setSpin(-90);

        SpeedMark mSpeedMark = new SpeedMark();
        mSpeedMark.setScale(-10.0f, 10.0f, 6.0f, -24.0f); // don't change this scale.
        mSpeedMark.setSpin(-90);

        BreakButton mBreakButton = new BreakButton();
        mBreakButton.setScale( 5.0f, -5.0f, -6.5f, 8.5f);

        figs = new Figure[] {mLongIndicator, mSpeedMark, mBreakButton};
        figCtrls = new FigureControl[]
        {
                new FigureControlSpin(mLongIndicator),
                new FigureControlMoveX(mSpeedMark),
                new FigureControlButton(mBreakButton)
        };
    }

    public void onDrawFrame(GL10 unused)
    {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        for (Figure fig : figs)
            fig.draw();
    }

    public void onSurfaceChanged(GL10 unused, int width, int height)
    {
        GLES20.glViewport(0, 0, width, height);
    }
}

public class MyGLSurfaceView extends GLSurfaceView
{
    MyGLRenderer mRenderer;
    BluetoothChatService mChatService;
    Movement mMovement;

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent e)
    {
        int pointerCount = e.getPointerCount();
        for (int p=0; p<pointerCount; p++)
        {
            float x = e.getX(p), y = e.getY(p);
            int id = e.getPointerId(p), action = e.getAction();
            if ((action & MotionEvent.ACTION_POINTER_INDEX_MASK)>0 && (action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT)!=p)
                continue;
            for (FigureControl figCtrl : mRenderer.figCtrls)
                switch (action & MotionEvent.ACTION_MASK)
                {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        figCtrl.onDown(x, y, getWidth(), getHeight(), id);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        figCtrl.onMove(x, y, getWidth(), getHeight(), id);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        figCtrl.onUp(id);
                }
        }
        requestRender();
        mMovement.setRequire(mRenderer.figCtrls[1].getVal(), mRenderer.figCtrls[0].getVal());
        return true;
    }

    public MyGLSurfaceView(Context context)
    {
        super(context);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);
        mRenderer = new MyGLRenderer();
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public MyGLSurfaceView(Context context, BluetoothChatService _ChatService)
    {
        this(context);

        mChatService = _ChatService;
        mMovement = new Movement(mChatService);
    }
}