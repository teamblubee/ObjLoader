package com.blubee.ObjLoader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

public class MyRenderer implements GLSurfaceView.Renderer{

	Context context;
	int fn = 0;
	int bb = 0;
	ObjLoader loader;
	int sProgram, vShader, fShader, posHandle, modMatHandle, viewMatHandle, projMatHandle, textureHandle, texCoordHandle, texId;
	float[] modMat, viewMat, projMat;
	
	float[] verts = {-0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, 0.0f, 0.5f, 0.0f};
	
	short[] ind = {0, 1, 2};
	FloatBuffer vertsBuff;
	ShortBuffer indicesBuff;
	
	public MyRenderer(Context context)
	{
		this.context = context;
		loader = new ObjLoader(context);
		loader.load(R.raw.monkey);
		verts = loader.vtx;
		vertsBuff = ByteBuffer.allocateDirect(verts.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		vertsBuff.put(verts).position(0);
		
		indicesBuff = ByteBuffer.allocateDirect(ind.length*2).order(ByteOrder.nativeOrder()).asShortBuffer();
		indicesBuff.put(ind).position(0);
		
		modMat = new float[16];
		viewMat = new float[16];
		projMat = new float[16];
	}
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		Log.v("renderer", "on surfacecreated");
		final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 3.0f;
        final float centerX = 0.0f;
        final float centerY = 0.0f;
        final float centerZ = 0.0f;
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;
        
        Matrix.setLookAtM(viewMat, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        
		vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		GLES20.glShaderSource(vShader, vCode);
		GLES20.glCompileShader(vShader);

		fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		GLES20.glShaderSource(fShader, fCode);
		GLES20.glCompileShader(fShader);

		sProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(sProgram, vShader);
		GLES20.glAttachShader(sProgram, fShader);
		GLES20.glLinkProgram(sProgram);

		posHandle = GLES20.glGetAttribLocation(sProgram, "aPos");
		texCoordHandle = GLES20.glGetAttribLocation(sProgram, "aTexPos");
		
		modMatHandle = GLES20.glGetUniformLocation(sProgram, "uModMat");
		viewMatHandle = GLES20.glGetUniformLocation(sProgram, "uViewMat");
		projMatHandle = GLES20.glGetUniformLocation(sProgram, "uProjMat");
		textureHandle = GLES20.glGetUniformLocation(sProgram, "texture");
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		Log.v("renderer", "on surfacechanged "+width+" "+height);
		GLES20.glViewport(0, 0, width, height);
		final float ratio = (float)width/height;
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = 1.0f;
		final float far = 100.0f;
		Matrix.frustumM(projMat, 0, left, right, bottom, top, near, far);
		
		GLES20.glUseProgram(sProgram);
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		
	}

	public void onDrawFrame(GL10 gl) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		
		long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
        
		Matrix.setIdentityM(modMat, 0);
		Matrix.translateM(modMat, 0, 0.0f, 0.0f, -1.0f);
		Matrix.rotateM(modMat, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
		
		//vertsBuff.position(0);
		//GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, vertsBuff);
		loader.vertsBuffer.position(0);
		GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, loader.vertsBuffer);
        GLES20.glEnableVertexAttribArray(posHandle);
        
        GLES20.glUniformMatrix4fv(modMatHandle, 1, false, modMat, 0);
        GLES20.glUniformMatrix4fv(viewMatHandle, 1, false, viewMat, 0);
        GLES20.glUniformMatrix4fv(projMatHandle, 1, false, projMat, 0);
	
        GLES20.glUniform1i(textureHandle, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, loader.numFaces*3, GLES20.GL_UNSIGNED_SHORT, loader.indicesBuffer);
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, loader.numFaces*3);
       
	}
	
	private final String vCode = 
			  "uniform mat4 uModMat;            \n"
			+ "uniform mat4 uViewMat;           \n"
			+ "uniform mat4 uProjMat;           \n"
			+ "attribute vec4 aPos;             \n"
			+ "attribute vec4 aCol;             \n"
			+ "attribute vec2 aTexPos;          \n"
			+ "varying vec2 vTexPos;            \n"
			+ "varying vec4 vCol;               \n"
			+ "void main(){                     \n"
			+ " vCol = aCol;                    \n"
			+ " mat4 mv = uViewMat * uModMat;   \n"
			+ " mat4 mvp = uProjMat * mv;       \n"
			//+ " gl_Position =  aPos;          \n"
			+ " vTexPos = aTexPos;              \n"
			+ " gl_Position = mvp * aPos;       \n"
			+ " }                               \n";

	private final String fCode = 
			  "precision mediump float;        \n"
			+ "uniform sampler2D texture;      \n"
			+ "varying vec4 vCol;              \n"
			+ "varying vec2 vTexPos;           \n"
			+ "void main(){                    \n"
			+ " gl_FragColor = vec4(1.0, 1.0, 0.0, 1.0);       \n"
			//+ " gl_FragColor = texture2D(texture, vTexPos);  \n"
			+ " }                              \n";
}