ServerProxy
===========

ServerProxy is a Android library to provide easy to use classes to create a local server proxies.  The following classes exist:

**ServerProxy**: abstract class which creates a socket and has methods to get a url to reference it.
- start: Start the proxy listening for requests
- stop: Stop the proxy listening for requests
- getPrivateAddress: Get a local url good for being passed internally to another application on the same device
- getPublicAddress: Get a url good for being passed to another device on the same LAN
	
**FileProxy**: Streams whichever file is referenced in the url.  Can handle partial requests.

**BufferProxy**: Streams a file which is being concurrently downloaded.  Takes a BufferProgress object to determine when the file is finished downloading.  This is useful for serving a song to a MediaPlayer which is still being downloaded.  Can only handle one at a time.
- setBufferFile: Set the buffer file which is used to query for the state of the download


**BufferFile**: Interface to be implemented, which is queried to get various information about the file being downloaded
- getFile: Return the file which is being buffered
- getContentLength: The content length which is passed as a header. ONLY set if you absolutely know for certain the end size of the file
- getEstimatedSize: The guessed size of the file.  It is used to keep track of how much more data needs to be processed.  It should be relatively close.
- isWorkDone: Whether or not the file is done being downloaded
- onStart: called when the streaming starts
- onStop: called when the streaming is done
