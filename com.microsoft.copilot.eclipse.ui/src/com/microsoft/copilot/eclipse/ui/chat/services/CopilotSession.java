// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.copilot.eclipse.ui.chat.ChatView;

/**
 * Runtime metadata for one independently running Copilot chat session.
 *
 * <p>The session id is stable for the lifetime of an Eclipse view and is
 * deliberately separate from the conversation id, which is replaced by CLS
 * after the first turn begins.</p>
 */
public final class CopilotSession {
  /** Runtime state displayed in the sessions overview. */
  public enum Status {
    IDLE("Idle"),
    RUNNING("Running"),
    AWAITING_CONFIRMATION("Awaiting confirmation"),
    FAILED("Failed");

    private final String label;

    Status(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }
  }

  private final String sessionId;
  private volatile String conversationId = "";
  private volatile String subagentConversationId;
  private volatile String title = "";
  private volatile Status status = Status.IDLE;
  private volatile Instant updatedAt = Instant.now();
  private volatile ChatView view;

  CopilotSession(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getConversationId() {
    return conversationId;
  }

  public String getSubagentConversationId() {
    return subagentConversationId;
  }

  public String getTitle() {
    return title;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public ChatView getView() {
    return view;
  }

  public boolean isOpen() {
    return view != null;
  }

  void setView(ChatView view) {
    this.view = view;
    touch();
  }

  void setConversationIds(String conversationId, String subagentConversationId) {
    this.conversationId = StringUtils.defaultString(conversationId);
    this.subagentConversationId = subagentConversationId;
    touch();
  }

  void setTitle(String title) {
    this.title = StringUtils.defaultString(title);
    touch();
  }

  void setStatus(Status status) {
    this.status = status != null ? status : Status.IDLE;
    touch();
  }

  private void touch() {
    updatedAt = Instant.now();
  }
}
