package dugu9sword.esplayer;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLUtils;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;


/**
 * ����ǰ��renderer�����ã���ʼ��EGL����ʼһ�������߳�.
 * �������Ҫ����ȥʵ����Ӧ�Ļ��ƹ���.
 *
 * �������̿��Բο�http://www.cnblogs.com/kiffa/archive/2013/02/21/2921123.html
 * ��Ӧ�ĺ������Բ鿴�� https://www.khronos.org/registry/egl/sdk/docs/man/
 */
public abstract class TextureSurfaceRenderer implements Runnable{
    public static String LOG_TAG = TextureSurfaceRenderer.class.getSimpleName();

    protected final SurfaceTexture surfaceTexture;
    protected int width;
    protected int height;

    private EGL10 egl;
    private EGLContext eglContext;
    private EGLDisplay eglDisplay;
    private EGLSurface eglSurface;
    /***
     * �Ƿ����ڻ���(draw)
     */
    private boolean running = false;

    public TextureSurfaceRenderer(SurfaceTexture surfaceTexture, int width, int height) {
        this.surfaceTexture = surfaceTexture;
        this.width = width;
        this.height = height;
        this.running = true;
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        initEGL();
        initGLComponents();
        Log.d(LOG_TAG, "OpenGL init OK. start draw...");

        while (running) {
            if (draw()) {
                egl.eglSwapBuffers(eglDisplay, eglSurface);
            }
        }

        deinitGLComponents();
        deinitEGL();
    }

    private void initEGL() {
        egl = (EGL10)EGLContext.getEGL();
        //��ȡ��ʾ�豸
        eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        //version�д��EGL �汾�ţ�int[0]Ϊ���汾�ţ�int[1]Ϊ�Ӱ汾��
        int version[] = new int[2];
        egl.eglInitialize(eglDisplay, version);

        EGLConfig eglConfig = chooseEglConfig();
        //����EGL ��window surface ���ҷ�������handles(eslSurface)
        eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, null);

        eglContext = createContext(egl, eglDisplay, eglConfig);

        //���õ�ǰ����Ⱦ����
        try {
            if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
                throw new RuntimeException("GL error:" + GLUtils.getEGLErrorString(egl.eglGetError()));
            }
            if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throw new RuntimeException("GL Make current Error"+ GLUtils.getEGLErrorString(egl.eglGetError()));
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deinitEGL() {
        egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(eglDisplay, eglSurface);
        egl.eglDestroyContext(eglDisplay, eglContext);
        egl.eglTerminate(eglDisplay);
        Log.d(LOG_TAG, "OpenGL deinit OK.");
    }

    /**
     * ��Ҫ�Ļ��ƺ����� ����������ȥʵ�ֻ���
     */
    protected abstract boolean draw();

    /***
     * ��ʼ��opengl��һЩ�������vertextBuffer,sharders,textures�ȣ�
     * ͨ����Opengl context ��ʼ���Ժ󱻵��ã���Ҫ����ȥʵ��
     */
    protected abstract void initGLComponents();
    protected abstract void deinitGLComponents();

    public abstract SurfaceTexture getSurfaceTexture();

    /**
     * Ϊ��ǰ��Ⱦ��API����һ����Ⱦ������
     * @return a handle to the context
     */
    private EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        int[] attrs = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };
        return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrs);
    }

    /***
     *  refer to https://www.khronos.org/registry/egl/sdk/docs/man/
     * @return a EGL frame buffer configurations that match specified attributes
     */
    private EGLConfig chooseEglConfig() {
        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] attributes = getAttributes();
        int confSize = 1;

        if (!egl.eglChooseConfig(eglDisplay, attributes, configs, confSize, configsCount)) {    //��ȡ����attributes��config����
            throw new IllegalArgumentException("Failed to choose config:"+ GLUtils.getEGLErrorString(egl.eglGetError()));
        }
        else if (configsCount[0] > 0) {
            return configs[0];
        }

        return null;
    }

    /**
     * ���������Ҫ�������б�,ARGB,DEPTH...
     */
    private int[] getAttributes()
    {
        return new int[] {
                EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,  //ָ����Ⱦapi���
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE      //������EGL10.EGL_NONE��β
        };
    }

    /**
     * Call when activity pauses. This stops the rendering thread and deinitializes OpenGL.
     */
    public void onPause()
    {
        running = false;
    }
    
    @Override
    protected  void finalize() throws Throwable {
        super.finalize();
        running = false;
    }
}
