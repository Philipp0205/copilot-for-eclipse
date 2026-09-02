// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.services.CopilotSession.Status;

/**
 * Registry of open Copilot sessions and their runtime state.
 */
public final class CopilotSessionRegistry {
  private final ConcurrentHashMap<String, CopilotSession> sessions = new ConcurrentHashMap<>();
  private final CopyOnWriteArraySet<Runnable> listeners = new CopyOnWriteArraySet<>();
  private volatile String activeSessionId;

  /** Register an open view. */
  public CopilotSession registerView(String sessionId, ChatView view) {
    CopilotSession session = getOrCreate(sessionId);
    session.setView(view);
    fireChanged();
    return session;
  }

  /** Find or create runtime session metadata. */
  public CopilotSession getOrCreate(String sessionId) {
    return sessions.computeIfAbsent(sessionId, CopilotSession::new);
  }

  /** Unregister a closed view. */
  public void unregisterView(String sessionId, ChatView view) {
    CopilotSession session = sessions.get(sessionId);
    if (session != null && session.getView() == view) {
      session.setView(null);
      if (session.getStatus() != Status.RUNNING
          && session.getStatus() != Status.AWAITING_CONFIRMATION) {
        sessions.remove(sessionId, session);
      }
      fireChanged();
    }
  }

  /** Mark a session as the most recently active session. */
  public void setActive(String sessionId) {
    activeSessionId = sessionId;
    fireChanged();
  }

  /** Return the most recently active session. */
  public CopilotSession getActive() {
    return sessions.get(activeSessionId);
  }

  /** Return the most recently active chat view. */
  public ChatView getActiveView() {
    CopilotSession active = getActive();
    return active != null ? active.getView() : null;
  }

  /** Update conversation aliases for a session. */
  public void updateConversation(String sessionId, String conversationId,
      String subagentConversationId) {
    getOrCreate(sessionId).setConversationIds(conversationId, subagentConversationId);
    fireChanged();
  }

  /** Update the title shown in the overview. */
  public void updateTitle(String sessionId, String title) {
    getOrCreate(sessionId).setTitle(title);
    fireChanged();
  }

  /** Update live execution status. */
  public void updateStatus(String sessionId, Status status) {
    getOrCreate(sessionId).setStatus(status);
    fireChanged();
  }

  /** Find an open session by main or subagent conversation id. */
  public CopilotSession findByConversation(String conversationId) {
    if (StringUtils.isBlank(conversationId)) {
      return null;
    }
    return sessions.values().stream()
        .filter(session -> Objects.equals(conversationId, session.getConversationId())
            || Objects.equals(conversationId, session.getSubagentConversationId()))
        .findFirst()
        .orElse(null);
  }

  /** Return open sessions, newest first. */
  public List<CopilotSession> getOpenSessions() {
    List<CopilotSession> result = new ArrayList<>();
    sessions.values().stream()
        .filter(CopilotSession::isOpen)
        .sorted(Comparator.comparing(CopilotSession::getUpdatedAt).reversed())
        .forEach(result::add);
    return result;
  }

  /** Subscribe to registry changes. */
  public void addListener(Runnable listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  /** Unsubscribe from registry changes. */
  public void removeListener(Runnable listener) {
    listeners.remove(listener);
  }

  private void fireChanged() {
    for (Runnable listener : listeners) {
      listener.run();
    }
  }
}
