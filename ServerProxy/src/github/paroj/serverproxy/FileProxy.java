/*
	This file is part of ServerProxy.
	SocketProxy is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	Subsonic is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU General Public License for more details.
	You should have received a copy of the GNU General Public License
	along with Subsonic. If not, see <http://www.gnu.org/licenses/>.
	Copyright 2014 (C) Scott Jackson
*/

package github.paroj.serverproxy;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import android.content.Context;
import android.util.Log;

public class FileProxy extends ServerProxy {
	private static final String TAG = FileProxy.class.getSimpleName();

	public FileProxy(Context context) {
		super(context);
	}

	protected ProxyTask getTask(Socket client) {
		return new StreamFileTask(client);
	}

	protected class StreamFileTask extends ProxyTask {
		File file;

		public StreamFileTask(Socket client) {
			super(client);
		}

		@Override
		public boolean processRequest() {
			if(!super.processRequest()) {
				return false;
			}

			Log.i(TAG, "Processing request for file " + path);
			file = getFile(path);
			if (file == null || !file.exists()) {
				Log.e(TAG, "File " + path + " does not exist");
				return false;
			}

			Long contentLength = getContentLength();
			if(cbSkip != 0 && ((contentLength != null && cbSkip >= contentLength)
					|| (cbSkip >= file.length() && isWorkDone()))) {
				Log.w(TAG, "Requested position is beyond the completed stream");
				return false;
			}

			return true;
		}

		File getFile(String path) {
			return new File(path);
		}

		Long getContentLength() {
			return file.length();
		}
		long getFileSize() {
			return file.length();
		}

		@Override
		public void run() {
			Long contentLength = getContentLength();
			// A growing stream without a known end cannot advertise a valid
			// Content-Range. Send it from the start and let the client skip.
			if (contentLength == null) {
				cbSkip = 0;
			}

			// Create HTTP header
			String headers;
			if(cbSkip == 0) {
				headers = "HTTP/1.0 200 OK\r\n";
			} else {
				headers = "HTTP/1.0 206 OK\r\n";
				headers += "Content-Range: bytes " + cbSkip + "-" + (contentLength - 1)
						+ "/" + contentLength + "\r\n";

				Log.i(TAG, "Streaming starts from: " + cbSkip);
			}

			String name = file.getPath();
			int index = name.lastIndexOf('.');
			String ext = "";
			if(index != -1) {
				ext = name.substring(index + 1).toLowerCase();
			}
			if("mp3".equals(ext)) {
				headers += "Content-Type: audio/mpeg\r\n";
			} else {
				headers += "Content-Type: " + "application/octet-stream" + "\r\n";
			}

			long fileSize;
			if(contentLength == null) {
				fileSize = getFileSize();
			} else {
				fileSize = contentLength;
				if(cbSkip > 0) {
					headers += "Content-Length: " + (fileSize - cbSkip) + "\r\n";
				} else {
					headers += "Content-Length: " + fileSize + "\r\n";
				}
				headers += "Accept-Ranges: bytes \r\n";
			}
			Log.i(TAG, "Streaming fileSize: " + fileSize);

			headers += "Connection: close\r\n";
			headers += "\r\n";

			long cbToSend = fileSize - cbSkip;
			FileInputStream input = null;
			OutputStream output = null;
			byte[] buff = new byte[64 * 1024];
			try {
				output = new BufferedOutputStream(client.getOutputStream(), 32*1024);
				output.write(headers.getBytes());

				// Make sure to have file lock
				onStart();

				// Loop as long as there's stuff to send
				while (isRunning && !client.isClosed()) {
					onResume();

					// See if there's more to send
					int cbSentThisBatch = 0;
					if (file.exists()) {
						input = new FileInputStream(file);
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
						input = null;
					}

					// Done regardless of whether or not it thinks it is
					if(isWorkDone()) {
						break;
					}

					// If we did nothing this batch, block for a second
					if (cbSentThisBatch == 0) {
						Log.d(TAG, "Blocking until more data appears (" + cbToSend + ")");
						Thread.sleep(1000);
					}
				}

				// Release file lock, use of stream proxy means nothing else is using it
				onStop();
			}
			catch (SocketException socketException) {
				Log.e(TAG, "SocketException() thrown, proxy client has probably closed. This can exit harmlessly");

				// Release file lock, use of stream proxy means nothing else is using it
				onStop();
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
				if(input != null) {
					input.close();
				}
				client.close();
			}
			catch (IOException e) {
				Log.e(TAG, "IOException while cleaning up streaming task:");
				Log.e(TAG, e.getClass().getName() + " : " + e.getLocalizedMessage());
			}
		}

		public void onStart() {

		}
		public void onStop() {

		}
		public void onResume() {

		}
		public boolean isWorkDone() {
			return cbSkip >= file.length();
		}
	}
}
