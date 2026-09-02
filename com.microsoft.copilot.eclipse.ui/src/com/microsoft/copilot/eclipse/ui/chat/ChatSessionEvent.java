// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.Event;

/**
 * Helpers for scoping workbench events to one chat view.
 */
public final class ChatSessionEvent {
  public static final String SESSION_ID = "copilotSessionId";
  public static final String PAYLOAD = "payload";

  private ChatSessionEvent() {
  }

  public static Map<String, Object> forSession(String sessionId) {
    Map<String, Object> data = new HashMap<>();
    data.put(SESSION_ID, sessionId);
    return data;
  }

  public static Map<String, Object> forSession(String sessionId, Object payload) {
    Map<String, Object> data = forSession(sessionId);
    data.put(PAYLOAD, payload);
    return data;
  }

  public static boolean isForSession(Event event, String sessionId) {
    Object data = event.getProperty(IEventBroker.DATA);
    return data instanceof Map<?, ?> map && Objects.equals(sessionId, map.get(SESSION_ID));
  }

  public static String sessionId(Event event) {
    Object data = event.getProperty(IEventBroker.DATA);
    Object value = data instanceof Map<?, ?> map ? map.get(SESSION_ID) : null;
    return value instanceof String id ? id : null;
  }

  public static Object payload(Event event) {
    Object data = event.getProperty(IEventBroker.DATA);
    return data instanceof Map<?, ?> map ? map.get(PAYLOAD) : data;
  }
}
