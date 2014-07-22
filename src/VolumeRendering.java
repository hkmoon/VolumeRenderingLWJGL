import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.Point;

import java.io.*;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;


/**
 * Created by moon on 7/7/14.
 */
public class VolumeRendering {
    int textureID;
    int framebufferID;
    int depthRenderBufferID;
    Point rotPoint = new Point();

    /** frames per second */
    int fps;
    /** last fps time */
    long lastFPS;

    /** is VSync Enabled */
    boolean vsyncEnabled;

    Transform transform = new Transform();
    String inputFile = null;

    public VolumeRendering(String filename) {
        inputFile = filename;
    }

    public void start() {
        try {
            Display.setDisplayMode(new DisplayMode(800, 600));
            Display.setResizable(true);
            Display.create();
        } catch (LWJGLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        initTexture2D(inputFile);
        lastFPS = getTime(); // call before loop to initialise fps timer

        while (!Display.isCloseRequested()) {
            update();
            //renderGL();
            renderGLFO();

            Display.update();
            Display.sync(60); // cap fps to 60fps
        }

        Display.destroy();
    }

    public void update() {

        if (Mouse.isButtonDown(0)) {
            int x = Mouse.getDX();
            int y = Mouse.getDY();

            transform.rotate(rotPoint.getY(), -rotPoint.getX(), 0);

            rotPoint.setX(x);
            rotPoint.setY(y);
        }

        if (Mouse.isButtonDown(1)) {
            int x = -Mouse.getDX();
            int y = -Mouse.getDY();

            transform.translate((float)x/800, (float)y/600, 0);
        }

        int wheel = Mouse.getDWheel();
        if (wheel > 0)
        {
            transform.scale(0.9f, 0.9f, 0.9f);
        }
        else if(wheel < 0)
        {
            transform.scale(1.1f, 1.1f, 1.1f);
        }

        if (Display.wasResized())
        {
            resize(Display.getWidth(), Display.getHeight());
        }

        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_F) {
                    setDisplayMode(800, 600, !Display.isFullscreen());
                }
                else if (Keyboard.getEventKey() == Keyboard.KEY_V) {
                    vsyncEnabled = !vsyncEnabled;
                    Display.setVSyncEnabled(vsyncEnabled);
                }
            }
        }

        updateFPS(); // update FPS Counter
    }

    float dOrthoSize = 1f;
    int imageCount;
    int imageWidth;
    int imageHeight;

    public boolean initTexture2D(String filename)
    {
        imageCount = 109;
        imageWidth = 256;
        imageHeight = 256;

//        glViewport (0, 0, 800, 600);								// Reset The Current Viewport
//        glMatrixMode (GL_PROJECTION);								// Select The Projection Matrix
//        glLoadIdentity ();											// Reset The Projection Matrix
//        glOrtho(0, 800, 0, 600, 1, 1);
//        GLU.gluPerspective (1.0f, 800f/600f, 1.0f, 1.0f);		    // Calculate The Aspect Ratio Of The Window
//        glMatrixMode (GL_MODELVIEW);								// Select The Modelview Matrix
//        glLoadIdentity ();											// Reset The Modelview Matrix
//
//        glClearColor (0.0f, 0.0f, 0.0f, 0.5f);						// Black Background
//        glClearDepth (1.0f);										// Depth Buffer Setup
//        glDepthFunc (GL_LEQUAL);									// The Type Of Depth Testing (Less Or Equal)
//        glEnable (GL_DEPTH_TEST);									// Enable Depth Testing
        glShadeModel (GL_SMOOTH);									// Select Smooth Shading
        glHint (GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);			// Set Perspective Calculations To Most Accurate

        // check if GL_EXT_framebuffer_object can be use on this system
        if (!GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            System.out.println("FBO not supported!!!");
            System.exit(0);
        }
        else {

            // Holds the luminance buffer
            byte[] chBuffer = read(filename);

            // Holds the RGBA buffer
            java.nio.ByteBuffer pRGBABuffer = BufferUtils.createByteBuffer(imageCount * imageWidth * imageHeight * 4);

            for (int nIndx = 0; nIndx < imageWidth * imageHeight * imageCount; ++nIndx) {
                pRGBABuffer.put(chBuffer[nIndx]);
                pRGBABuffer.put(chBuffer[nIndx]);
                pRGBABuffer.put(chBuffer[nIndx]);
                pRGBABuffer.put(chBuffer[nIndx]);
            }

            pRGBABuffer.flip();


            framebufferID = glGenFramebuffersEXT();											// create a new framebuffer
            textureID = glGenTextures();                                                        // and a new texture used as a color buffer
            depthRenderBufferID = glGenRenderbuffersEXT();									// And finally a new depthbuffer

            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, framebufferID); 						// switch to the new framebuffer

            glBindTexture(GL_TEXTURE_3D, textureID);
            glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

            glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA, imageWidth, imageHeight, imageCount, 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, pRGBABuffer);

            glFramebufferTexture3DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_3D, textureID, 0, 0); // attach it to the framebuffer


            glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, depthRenderBufferID);				// bind the depth renderbuffer
            glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL14.GL_DEPTH_COMPONENT24, 800, 600);	// get the data space for it
            glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT,GL_DEPTH_ATTACHMENT_EXT,GL_RENDERBUFFER_EXT, depthRenderBufferID); // bind it to the renderbuffer

            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);									// Swithch back to normal framebuffer rendering
        }

        return true;
    }

    void log(String str)
    {
        System.out.println(str);
    }

    void log(Exception ex)
    {
        ex.printStackTrace();
    }

    /** Read the given binary file, and return its contents as a byte array.*/
    byte[] read(String aInputFileName){
        log("Reading in binary file named : " + aInputFileName);
        File file = new File(aInputFileName);
        log("File size: " + file.length());
        byte[] result = new byte[(int)file.length()];
        try {
            InputStream input = null;
            try {
                int totalBytesRead = 0;
                input = new BufferedInputStream(new FileInputStream(file));
                while(totalBytesRead < result.length){
                    int bytesRemaining = result.length - totalBytesRead;
                    //input.read() returns -1, 0, or more :
                    int bytesRead = input.read(result, totalBytesRead, bytesRemaining);
                    if (bytesRead > 0){
                        totalBytesRead = totalBytesRead + bytesRead;
                    }
                }
                /*
                 the above style is a bit tricky: it places bytes into the 'result' array;
                 'result' is an output parameter;
                 the while loop usually has a single iteration only.
                */
                log("Num bytes read: " + totalBytesRead);
            }
            finally {
                log("Closing input stream.");
                input.close();
            }
        }
        catch (FileNotFoundException ex) {
            log("File not found.");
        }
        catch (IOException ex) {
            log(ex);
        }
        return result;
    }

    public void renderGLFO() {

        glBindTexture(GL_TEXTURE_3D, 0);								// unlink textures because if we dont it all is gonna fail
        glClear( GL_COLOR_BUFFER_BIT  | GL_DEPTH_BUFFER_BIT );

        // Process for framebuffer
        glEnable( GL_ALPHA_TEST );
        glAlphaFunc( GL_GREATER, 0.05f );

        glEnable(GL_BLEND);
        glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA );

        glMatrixMode( GL_TEXTURE );
        glLoadIdentity();

        // Translate and make 0.5f as the center
        // (texture co ordinate is from 0 to 1. so center of rotation has to be 0.5f)
        glTranslatef( 0.5f, 0.5f, 0.5f );

        // A scaling applied to normalize the axis
        // (Usually the number of slices will be less so if this is not -
        // normalized then the z axis will look bulky)
        // Flipping of the y axis is done by giving a negative value in y axis.
        // This can be achieved either by changing the y co ordinates in -
        // texture mapping or by negative scaling of y axis
        glScaled( (float)imageWidth/(float)imageWidth,
                -1.0f*(float)imageWidth/(float)imageHeight,
                (float)imageWidth/(float)imageCount);

        // Apply the user provided transformations
        glMultMatrix(transform.getMat());

        glTranslatef( -0.5f,-0.5f, -0.5f );

        glEnable(GL_TEXTURE_3D);
        glBindTexture( GL_TEXTURE_3D, textureID);

        for ( float fIndx = -1.0f; fIndx <= 1.0f; fIndx+=0.01f )
        {
            glBegin(GL_QUADS);
            glTexCoord3f(0.0f, 0.0f, (fIndx + 1.0f) / 2.0f);
            glVertex3f(-dOrthoSize,-dOrthoSize,fIndx);
            glTexCoord3f(1.0f, 0.0f, (fIndx + 1.0f) / 2.0f);
            glVertex3f(dOrthoSize,-dOrthoSize,fIndx);
            glTexCoord3f(1.0f, 1.0f, (fIndx + 1.0f) / 2.0f);
            glVertex3f(dOrthoSize,dOrthoSize,fIndx);
            glTexCoord3f(0.0f, 1.0f, (fIndx + 1.0f) / 2.0f);
            glVertex3f(-dOrthoSize,dOrthoSize,fIndx);
            glEnd();
        }

        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);					// switch to rendering on the framebuffer

        glDisable(GL_TEXTURE_3D);

        glFlush();
    }

    public void renderGL() {

        glClear( GL_COLOR_BUFFER_BIT  | GL_DEPTH_BUFFER_BIT );

        glEnable( GL_ALPHA_TEST );
        glAlphaFunc( GL_GREATER, 0.05f );

        glEnable(GL_BLEND);
        glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA );

        glMatrixMode( GL_TEXTURE );
        glLoadIdentity();

        glPushMatrix();

        // Translate and make 0.5f as the center
        // (texture co ordinate is from 0 to 1. so center of rotation has to be 0.5f)
        glTranslatef( 0.5f, 0.5f, 0.5f );

        // A scaling applied to normalize the axis
        // (Usually the number of slices will be less so if this is not -
        // normalized then the z axis will look bulky)
        // Flipping of the y axis is done by giving a negative value in y axis.
        // This can be achieved either by changing the y co ordinates in -
        // texture mapping or by negative scaling of y axis
        glScaled( (float)imageWidth/(float)imageWidth,
                -1.0f*(float)imageWidth/(float)imageHeight,
                (float)imageWidth/(float)imageCount);

        // Apply the user provided transformations
        glMultMatrix(transform.getMat());

        glTranslatef( -0.5f,-0.5f, -0.5f );

        glEnable(GL_TEXTURE_3D);
        glBindTexture( GL_TEXTURE_3D, textureID);

        for ( float fIndx = -1.0f; fIndx <= 1.0f; fIndx+=0.01f )
        {
            glBegin(GL_QUADS);
            glTexCoord3f(0.0f, 0.0f, (fIndx + 1.0f) / 2.0f);
            glVertex3f(-dOrthoSize,-dOrthoSize,fIndx);
            glTexCoord3f(1.0f, 0.0f, (fIndx + 1.0f) / 2.0f);
            glVertex3f(dOrthoSize,-dOrthoSize,fIndx);
            glTexCoord3f(1.0f, 1.0f, (fIndx + 1.0f) / 2.0f);
            glVertex3f(dOrthoSize,dOrthoSize,fIndx);
            glTexCoord3f(0.0f, 1.0f, (fIndx + 1.0f) / 2.0f);
            glVertex3f(-dOrthoSize,dOrthoSize,fIndx);
            glEnd();
        }

        glPopMatrix();

        glFlush();
    }

    class Transform {
        float rot[];
        FloatBuffer rotMat;

        public FloatBuffer getMat() {
            return rotMat;
        }

        public Transform()
        {
            rotMat = BufferUtils.createFloatBuffer(16);
            reset();

            rot = new float[3];
            rot[0] = rot[1] = rot[2] = 0.0f;
        }

        public void rotate(float fx_i, float fy_i, float fz_i)
        {
            rot[0] = fx_i;
            rot[1] = fy_i;
            rot[2] = fz_i;

            glMatrixMode( GL_MODELVIEW );
            glLoadMatrix(rotMat);
            glRotated( rot[0], 1.0f, 0, 0 );
            glRotated( rot[1], 0, 1.0f, 0 );
            glRotated( rot[2], 0, 0, 1.0f );

            glGetFloat(GL_MODELVIEW_MATRIX, rotMat);
            glLoadIdentity();
        }

        public void translate(float fx_i, float fy_i, float fz_i)
        {
            glMatrixMode( GL_MODELVIEW );
            glLoadMatrix(rotMat);
            glTranslatef(fx_i, fy_i, fz_i);

            glGetFloat(GL_MODELVIEW_MATRIX, rotMat);
            glLoadIdentity();
        }

        public void scale(float fx_i, float fy_i, float fz_i)
        {
            glMatrixMode( GL_MODELVIEW );
            glLoadMatrix(rotMat);
            glScalef(fx_i, fy_i, fz_i);

            glGetFloat(GL_MODELVIEW_MATRIX, rotMat);
            glLoadIdentity();
        }

        public void reset()
        {
            rotMat.put(0, 1.0f);
            rotMat.put(5, 1.0f);
            rotMat.put(10, 1.0f);
            rotMat.put(15, 1.0f);

            rotMat.put(1, 0.0f);
            rotMat.put(2, 0.0f);
            rotMat.put(3, 0.0f);
            rotMat.put(4, 0.0f);

            rotMat.put(6, 0.0f);
            rotMat.put(7, 0.0f);
            rotMat.put(8, 0.0f);
            rotMat.put(9, 0.0f);

            rotMat.put(11, 0.0f);
            rotMat.put(12, 0.0f);
            rotMat.put(13, 0.0f);
            rotMat.put(14, 0.0f);
        }
    }

    /**
     * Set the display mode to be used
     *
     * @param width The width of the display required
     * @param height The height of the display required
     * @param fullscreen True if we want fullscreen mode
     */
    public void setDisplayMode(int width, int height, boolean fullscreen) {

        // return if requested DisplayMode is already set
        if ((Display.getDisplayMode().getWidth() == width) &&
                (Display.getDisplayMode().getHeight() == height) &&
                (Display.isFullscreen() == fullscreen)) {
            return;
        }

        try {
            DisplayMode targetDisplayMode = null;

            if (fullscreen) {
                DisplayMode[] modes = Display.getAvailableDisplayModes();
                int freq = 0;

                for (int i=0;i<modes.length;i++) {
                    DisplayMode current = modes[i];

                    if ((current.getWidth() == width) && (current.getHeight() == height)) {
                        if ((targetDisplayMode == null) || (current.getFrequency() >= freq)) {
                            if ((targetDisplayMode == null) || (current.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel())) {
                                targetDisplayMode = current;
                                freq = targetDisplayMode.getFrequency();
                            }
                        }

                        // if we've found a match for bpp and frequence against the
                        // original display mode then it's probably best to go for this one
                        // since it's most likely compatible with the monitor
                        if ((current.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel()) &&
                                (current.getFrequency() == Display.getDesktopDisplayMode().getFrequency())) {
                            targetDisplayMode = current;
                            break;
                        }
                    }
                }
            } else {
                targetDisplayMode = new DisplayMode(width,height);
            }

            if (targetDisplayMode == null) {
                System.out.println("Failed to find value mode: "+width+"x"+height+" fs="+fullscreen);
                return;
            }

            Display.setDisplayMode(targetDisplayMode);
            Display.setFullscreen(fullscreen);
            if(fullscreen)
                fullscreen(Display.getDesktopDisplayMode().getWidth(), Display.getDesktopDisplayMode().getHeight());
            else
                resize(Display.getWidth(), Display.getHeight());

        } catch (LWJGLException e) {
            System.out.println("Unable to setup mode "+width+"x"+height+" fullscreen="+fullscreen + e);
        }
    }

    public void resize(int width, int height)
    {
        //Find the aspect ratio of the window.
        double AspectRatio = ( double )(width) / ( double )(height);
        glViewport( 0, 0, width, height);
        glMatrixMode( GL_PROJECTION );
        glLoadIdentity();

        //Set the orthographic projection.
        if( width <= height )
        {
            glOrtho( dOrthoSize, dOrthoSize, -( dOrthoSize / AspectRatio ) ,
                    dOrthoSize / AspectRatio, 2.0f*-dOrthoSize, 2.0f*dOrthoSize );
        }
        else
        {
            glOrtho( -dOrthoSize * AspectRatio, dOrthoSize * AspectRatio,
                    -dOrthoSize, dOrthoSize, 2.0f*-dOrthoSize, 2.0f*dOrthoSize );
        }

        glMatrixMode( GL_MODELVIEW );
        glLoadIdentity();
    }

    public void fullscreen(int width, int height)
    {
        glViewport( 0, 0, width, height);
        glMatrixMode( GL_PROJECTION );
        glLoadIdentity();

        glOrtho( -2, 10, -2, 7, -1, 1 );

        glMatrixMode( GL_MODELVIEW );
        glLoadIdentity();
    }

    /**
     * Get the accurate system time
     *
     * @return The system time in milliseconds
     */
    public long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    /**
     * Calculate the FPS and set it in the title bar
     */
    public void updateFPS() {
        if (getTime() - lastFPS > 1000) {
            Display.setTitle("FPS: " + fps);
            fps = 0;
            lastFPS += 1000;
        }
        fps++;
    }

    public static void main(String[] argv) {
		// Please, change here in order to load head256x256x109 raw file
		// The file can be obtained in http://www.codeproject.com/KB/openGL/352270/head256x256x109.zip
        VolumeRendering vol = new VolumeRendering("/Users/usr/head256x256x109");
        vol.start();
    }
}
