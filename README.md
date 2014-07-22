Volume Rendering for LWJGL
====================

This is an example source code of Volume Rendering for LWJGL which was originally written by Divine Augustine.
The original article is http://www.codeproject.com/Articles/352270/Getting-started-with-Volume-Rendering.
In the article, he explains how to cope with volume rendering in VC++.

Don't forget to add org.lwjgl.lwjgl:lwjgl:2.9.1 and org.lwjgl.lwjgl:lwjgl_util:2.9.1 in your java library. 
The runtime VM option needs "-Xmx2g -Djava.library.path=$MODULE_DIR$/lib/native/macosx". In my case, I use macosx. Change the folder name depending on your host operating system.

Additionally, I added panning and zooming by mouse-right button pressing/move and wheeling.
I thank Divine Augustine for sharing his precious code. 

![Alt head](http://www.codeproject.com/KB/openGL/352270/3D.gif)

