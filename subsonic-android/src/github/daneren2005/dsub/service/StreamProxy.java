package github.daneren2005.dsub.service;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;

import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.Constants;

public class StreamProxy implements Runnable {
	private static final String TAG = StreamProxy.class.getSimpleName();

	private Thread thread;
	private boolean isRunning;
	private ServerSocket socket;
	private int port;
	private DownloadService downloadService;

	public StreamProxy(DownloadService downloadService) {

		// Create listening socket
		try {
			socket = new ServerSocket(0, 0, InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
			socket.setSoTimeout(5000);
			port = socket.getLocalPort();
			this.downloadService = downloadService;
		} catch (UnknownHostException e) { // impossible
		} catch (IOException e) {
			Log.e(TAG, "IOException initializing server", e);
		}
	}
	
	public int getPort() {
		return port;
	}

	public void start() {
		thread = new Thread(this);
		thread.start();
	}

	public void stop() {
		isRunning = false;
		thread.interrupt();
		try {
			thread.join(5000);
		} catch (InterruptedException e) {
			Log.e(TAG, "Exception stopping server", e);
		}
	}

	@Override
	public void run() {
		Looper.prepare();
		isRunning = true;
		while (isRunning) {
			try {
				Socket client = socket.accept();
				if (client == null) {
					continue;
				}
				Log.d(TAG, "client connected");

				StreamToMediaPlayerTask task = new StreamToMediaPlayerTask(client);
				if (task.processRequest()) {
					task.execute();
				}

			} catch (SocketTimeoutException e) {
				// Do nothing
			} catch (IOException e) {
				Log.e(TAG, "Error connecting to client", e);
			}
		}
		Log.d(TAG, "Proxy interrupted. Shutting down.");
	}

	private class StreamToMediaPlayerTask extends AsyncTask<String, Void, Integer> {

		String localPath;
		Socket client;
		int cbSkip;

		public StreamToMediaPlayerTask(Socket client) {
			this.client = client;
		}
		
		private HttpRequest readRequest() {
			HttpRequest request = null;
			InputStream is;
			String firstLine;
			try {
				is = client.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
				firstLine = reader.readLine();
			} catch (IOException e) {
				Log.e(TAG, "Error parsing request", e);
				return request;
			}

			if (firstLine == null) {
				Log.i(TAG, "Proxy client closed connection without a request.");
				return request;
			}

			StringTokenizer st = new StringTokenizer(firstLine);
			String method = st.nextToken();
			String uri = st.nextToken();
			String realUri = uri.substring(1);
			Log.i(TAG, realUri);
			request = new BasicHttpRequest(method, realUri);
			return request;
		}

		public boolean processRequest() {
			HttpRequest request = readRequest();
			if (request == null) {
				return false;
			}
			
			// Read HTTP headers
			Log.i(TAG, "Processing request");

			try {
				localPath = URLDecoder.decode(request.getRequestLine().getUri(), Constants.UTF_8);
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "Unsupported encoding", e);
				return false;
			}
			
			Log.i(TAG, "Processing request for file " + localPath);
			File file = new File(localPath);
			if (!file.exists()) {
				Log.e(TAG, "File " + localPath + " does not exist");
				return false;
			}
			
			return true;
		}

		@Override
        protected Integer doInBackground(String... params) {
			Log.i(TAG, "Streaming song in background");
			DownloadFile downloadFile = downloadService.getCurrentPlaying();
			MusicDirectory.Entry song = downloadFile.getSong();
			long fileSize = downloadFile.getBitRate() * ((song.getDuration() != null) ? song.getDuration() : 0) * 1000 / 8;
			Log.i(TAG, "Streaming fileSize: " + fileSize);

            // Create HTTP header
            String headers = "HTTP/1.0 200 OK\r\n";
            headers += "Content-Type: " + "application/octet-stream" + "\r\n";
            
            headers += "Connection: close\r\n";
            headers += "\r\n";

            long cbToSend = fileSize - cbSkip;
            OutputStream output = null;
            byte[] buff = new byte[64 * 1024];
            try {
                output = new BufferedOutputStream(client.getOutputStream(), 32*1024);                           
                output.write(headers.getBytes());

				if(!downloadFile.isWorkDone()) {
					// Loop as long as there's stuff to send
					while (isRunning && !client.isClosed()) {

						// See if there's more to send
						File file = new File(localPath);
						int cbSentThisBatch = 0;
						if (file.exists()) {
							FileInputStream input = new FileInputStream(file);
							input.skip(cbSkip);
							int cbToSendThisBatch = input.available();
							while (cbToSendThisBatch > 0) {
								int cbToRead = Math.min(cbToSendThisBatch, buff.length);
								int cbRead = input.read(buff, 0, cbToRead);
								if (cbRead == -1) {
									break;
								}
								cbToSendThisBatch -= cbRead;
								cbToSend -= cbRead;
								output.write(buff, 0, cbRead);
								output.flush();
								cbSkip += cbRead;
								cbSentThisBatch += cbRead;
							}
							input.close();
						}

						// Done regardless of whether or not it thinks it is
						if(downloadFile.isWorkDone() && cbSkip >= file.length()) {
							break;
						}

						// If we did nothing this batch, block for a second
						if (cbSentThisBatch == 0) {
							Log.d(TAG, "Blocking until more data appears (" + cbToSend + ")");
							Thread.sleep(1000);
						}
					}
				} else {
					Log.w(TAG, "Requesting data for completely downloaded file");
				}
            }
            catch (SocketException socketException) {
                Log.e(TAG, "SocketException() thrown, proxy client has probably closed. This can exit harmlessly");
            }
            catch (Exception e) {
                Log.e(TAG, "Exception thrown from streaming task:");
                Log.e(TAG, e.getClass().getName() + " : " + e.getLocalizedMessage());
            }

            // Cleanup
            try {
                if (output != null) {
                    output.close();
                }
                client.close();
            }
            catch (IOException e) {
                Log.e(TAG, "IOException while cleaning up streaming task:");                
                Log.e(TAG, e.getClass().getName() + " : " + e.getLocalizedMessage());
            }

            return 1;
        }
	}
}
