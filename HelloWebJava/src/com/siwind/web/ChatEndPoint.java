package com.siwind.web;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/WSChat")
public class ChatEndPoint {

	private static final String SESSION_PREFIX = "Client";
	private static final AtomicInteger connIndex = new AtomicInteger(0);
	private static final Set<ChatEndPoint> connections = new CopyOnWriteArraySet<>();

	private final String sessionName;
	private Session session;

	public ChatEndPoint() {
		sessionName = SESSION_PREFIX + connIndex.getAndIncrement();
	}

	@OnOpen
	public void start(Session session) {
		this.session = session;
		connections.add(this);
		String message = String.format("* %s %s", sessionName, "has been connected.");
		System.out.println(message);
		broadcast(message);
	}

	@OnClose
	public void end() {
		connections.remove(this);
		String message = String.format("* %s %s", sessionName, "has disconnected.");
		broadcast(message);
	}

	@OnMessage
	public void incoming(String message) {
		// Never trust the client
		//String filteredMessage = String.format("%s: %s", nickname, HTMLFilter.filter(message.toString()));
		String filteredMessage = String.format("%s: %s", sessionName, (message.toString()));
		System.out.println(filteredMessage);
		broadcast(filteredMessage);
	}

	@OnError
	public void onError(Throwable t) throws Throwable {
		System.out.println("Chat Error: " + t.toString());
	}

	private static void broadcast(String msg) {
		for (ChatEndPoint client : connections) {
			try {
				synchronized (client) {
					client.session.getBasicRemote().sendText(msg);
				}
			} catch (IOException e) {
				System.out.println("Chat Error: Failed to send message to client");
				connections.remove(client);
				try {
					client.session.close();
				} catch (IOException e1) {
					// Ignore
				}
				String message = String.format("* %s %s", client.sessionName, "has been disconnected.");
				broadcast(message);
			}
		}
	}
}
