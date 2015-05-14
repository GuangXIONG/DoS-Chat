package edu.nyu.guangxiong.doschat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

/**
 * Author: GuangXIONG
 * Date: 05/05/2015
 * Time: 11:00 AM
 */

public class ChatServer implements Runnable {

	private static final int MAXIMUM_WORKLOAD = 16;


	private static final long THRESHOLD_IDLE = 30 * 1000L;

	private static final long TIME_OUT = THRESHOLD_IDLE * 2;

	public static final String SERVER_HOST = "localhost";

	public static final int SERVER_PORT = 11111;

	private static final int READ_BUFFER_SIZE = 2048;

	private static final Charset UTF8 = Charset.forName("UTF-8");

	public static void main(String[] args) throws IOException {
		System.out.println("A raw Chat Server is ON, waiting for clients to chat...");
		ChatServer server = new ChatServer();
		server.run();
	}

	private final ServerSocketChannel channel;

	private final Selector selector;

	private final ByteBuffer readBuffer;

	private final Map<SocketChannel, ByteBuffer> writeBuffers;

	public ChatServer() throws IOException {
		channel = ServerSocketChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(new java.net.InetSocketAddress(SERVER_HOST, SERVER_PORT));
		selector = Selector.open();
		channel.register(selector, SelectionKey.OP_ACCEPT);
		readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
		this.writeBuffers = new HashMap<>();
	}

	@Override public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				process();
			} catch (IOException ioe) {
				System.out.printf("Exception - %s%n", ioe.getMessage());
				Thread.currentThread().interrupt();
			}
		}
	}

	private void process() throws IOException {
		int events = selector.select(TIME_OUT);
		if (events < 1) {
			System.out.println("Raw Chat Server have nothing to do for a while~!");
			return;
		}
		if (events > MAXIMUM_WORKLOAD) {
			System.out.println("\n\n\nChat Server CRASHED~!! CAN NOT handle too much workload~!!");
			Thread.currentThread().interrupt();
		}
		final long selectTime = System.currentTimeMillis();
		System.out.println("Current time stamp: " + selectTime);

		Set<SelectionKey> SelectedKeys = selector.selectedKeys();
		Iterator<SelectionKey> iteratorSelectedKey = SelectedKeys.iterator();

		while (iteratorSelectedKey.hasNext()) {
			SelectionKey key = iteratorSelectedKey.next();

			if (key.isAcceptable()) {
				System.out.println("another chat client is accepted");
				SocketChannel client = channel.accept();
				client.configureBlocking(false);
				client.register(selector, SelectionKey.OP_READ);
				writeBuffers.put(client, ByteBuffer.allocate(READ_BUFFER_SIZE));
			}
			else if (key.isReadable()) {
				System.out.println("another chat client is being read...");
				SocketChannel client = (SocketChannel) key.channel();
				readBuffer.clear();
				int result = client.read(readBuffer);
				if (result == -1) {
					writeBuffers.remove(client);
					key.cancel();
					continue;
				}
				for (Map.Entry<SocketChannel, ByteBuffer> entry : writeBuffers.entrySet()) {
					SocketChannel otherClient = entry.getKey();
					if (otherClient != client) {
						ByteBuffer writeBuffer = entry.getValue();
						readBuffer.flip();
						writeBuffer.put(String.format("[%s] says:  ", client.getRemoteAddress().toString()).getBytes());
						writeBuffer.put(readBuffer);
						writeBuffer.put((byte)'\n');
						writeBuffer.flip();
						otherClient.write(writeBuffer);
						writeBuffer.clear();
					}
				}
				readBuffer.flip();
				CharsetDecoder decoder = UTF8.newDecoder();
				CharBuffer charBuffer = decoder.decode(readBuffer);
				System.out.printf("[%s] %s%n", client.getRemoteAddress().toString(), charBuffer.toString());
			}
			iteratorSelectedKey.remove();
		}
	}
}
