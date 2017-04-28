/*
 * Copyright 2016 nekocode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bills.billslib.Camera;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;


public class RenderBuffer {
    private int _texId = 0;
    private int _activeTexUnit = 0;
    private int renderBufferId = 0;
    private int frameBufferId = 0;

    private int _width, _height;

    public RenderBuffer(int width, int height, int activeTexUnit) {
        _width = width;
        _height = height;
        _activeTexUnit = activeTexUnit;
        int[] genbuf = new int[1];

        // Generate and bind 2d texture
        GLES20.glActiveTexture(activeTexUnit);
        _texId = MyGLUtils.genTexture();
        IntBuffer texBuffer =
                ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, texBuffer);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        // Generate frame buffer
        GLES20.glGenFramebuffers(1, genbuf, 0);
        frameBufferId = genbuf[0];
        // Bind frame buffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);

        // Generate render buffer
        GLES20.glGenRenderbuffers(1, genbuf, 0);
        renderBufferId = genbuf[0];
        // Bind render buffer
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBufferId);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);

        unbind();
    }

    public int get_texId() {
        return _texId;
    }

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    public int getActiveTexUnit() {
        return _activeTexUnit;
    }

    public void bind() {
        GLES20.glViewport(0, 0, _width, _height);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, _texId, 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, renderBufferId);
    }

    public void unbind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
}
